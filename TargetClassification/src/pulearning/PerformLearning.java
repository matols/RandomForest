package pulearning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.IndexedDoubleData;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class PerformLearning
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		TreeGrowthControl ctrl = new TreeGrowthControl();
		String[] variablesToIgnore = new String[]{"OGlycosylation"};  // Make sure to ignore any variables that are constant. Otherwise the standardised value of the variable will be NaN.
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse inputs.
		String dataForLearning = args[0];
		int numberTopConnectionsToKeep = Integer.parseInt(args[1]);  // Same as Q in the paper.
		double alpha = Double.parseDouble(args[2]);

		// Process the input data.
		ctrl.isStandardised = true;
		ctrl.variablesToIgnore = Arrays.asList(variablesToIgnore);
		ProcessDataForGrowing processedDataForLearning = new ProcessDataForGrowing(dataForLearning, ctrl);

		// Determine the indices for the positive, unlabelled and all observations.
		List<Integer> allObservations = new ArrayList<Integer>();
		List<Integer> positiveObservations = new ArrayList<Integer>();
		List<Integer> unlabelledObservations = new ArrayList<Integer>();
		for (int i = 0; i < processedDataForLearning.numberObservations; i++)
		{
			allObservations.add(i);
			if (processedDataForLearning.responseData.get(i).equals("Positive"))
			{
				// If the observation is in the 'Positive' class.
				positiveObservations.add(i);
			}
			else
			{
				// If the observation is in the 'Unlabelled' class.
				unlabelledObservations.add(i);
			}
		}
		int numberAllObservations = allObservations.size();
		int numberPositiveObservations = positiveObservations.size();
		int numberUnlabelledObservations = unlabelledObservations.size();

		// Determine the mean vector for the positive observations.
		Map<String, Double> meanPositiveVector = new HashMap<String, Double>();
		for (String s : processedDataForLearning.covariableData.keySet())
		{
			double expectedValue = 0.0;
			for (Integer i : positiveObservations)
			{
				expectedValue += processedDataForLearning.covariableData.get(s).get(i);
			}
			expectedValue /= numberPositiveObservations;
			meanPositiveVector.put(s, expectedValue);
		}

		List<Double> distsanceToPositiveCluster = distanceFromMean(unlabelledObservations, meanPositiveVector, processedDataForLearning);
		List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
		double meanDistanceToPositive = 0.0;
		for (int i = 0; i < unlabelledObservations.size(); i++)
		{
			meanDistanceToPositive += distsanceToPositiveCluster.get(i);
			sortedDistances.add(new IndexedDoubleData(distsanceToPositiveCluster.get(i), i));
		}
		Collections.sort(sortedDistances);
		meanDistanceToPositive /= numberUnlabelledObservations;

		// The reliable negative set is all the observations with a distance to the positive cluster greater than the mean distance.
		List<Integer> reliableNegativeSet = new ArrayList<Integer>();
		for (int i = 0; i < sortedDistances.size(); i++)
		{
			if (sortedDistances.get(i).getData() > meanDistanceToPositive)
			{
				reliableNegativeSet.add(sortedDistances.get(i).getIndex());
			}
		}
		int numberNegativeObservations = reliableNegativeSet.size();

		Map<Integer, Map<Integer, Double>> weights = determineWeights(processedDataForLearning, numberTopConnectionsToKeep);
		Map<Integer, Map<Integer, Double>> diagonalScalingWeights = new HashMap<Integer, Map<Integer, Double>>();
		for (Integer i : weights.keySet())
		{
			double summedWeight = 0.0;
			for (Integer j : weights.get(i).keySet())
			{
				summedWeight += weights.get(i).get(j);
			}
			Map<Integer, Double> totalWeight = new HashMap<Integer, Double>();
			totalWeight.put(i, 1 / summedWeight);
			diagonalScalingWeights.put(i, totalWeight);
		}
		AdjacencyList weightMatrix = new AdjacencyList(numberAllObservations, numberAllObservations, weights);
		AdjacencyList diagonalScalingMatrix = new AdjacencyList(numberAllObservations, numberAllObservations, diagonalScalingWeights);
		AdjacencyList scaledWeightMatrix = weightMatrix.multiply(diagonalScalingMatrix);

		// Initialise probabilities.
		Map<Integer, Map<Integer, Double>> initialProbabilityWeights = new HashMap<Integer, Map<Integer, Double>>();
		for (Integer i : allObservations)
		{
			initialProbabilityWeights.put(i, new HashMap<Integer, Double>());
			if (positiveObservations.contains(i))
			{
				initialProbabilityWeights.get(i).put(0, 1.0);
			}
			else if (reliableNegativeSet.contains(i))
			{
				initialProbabilityWeights.get(i).put(0, -((double) numberPositiveObservations) / numberNegativeObservations);
			}
			else
			{
				initialProbabilityWeights.get(i).put(0, 0.0);
			}
		}
		AdjacencyList initialProbabilities = new AdjacencyList(numberAllObservations, 1, initialProbabilityWeights);
		AdjacencyList scaledInitialProbabilities = initialProbabilities.scale(alpha);

		// Propagate the probabilities.
		AdjacencyList lastIterationProbabilities = new AdjacencyList(initialProbabilities);
		double stoppingValue = 0.0;//Math.pow(10, -6);
		double dif = 1.0;
		while (dif > stoppingValue)
		{
			AdjacencyList thisIterationProbabilities = ((scaledWeightMatrix.multiply(lastIterationProbabilities)).scale(1 - alpha)).add(scaledInitialProbabilities);
			dif = L1Norm(thisIterationProbabilities, lastIterationProbabilities);
			lastIterationProbabilities = new AdjacencyList(thisIterationProbabilities);
		}

		AdjacencyList posteriorProbabilities = new AdjacencyList(lastIterationProbabilities);
		System.out.println(posteriorProbabilities.getWeights());

		for (Integer i : allObservations)
		{
			if (positiveObservations.contains(i))
			{
				;//System.out.println("Positive - " + Integer.toString(i) + "\t" + Double.toString(lastIterationProbabilities.getWeights().get(i).get(0)));
			}
			else if (reliableNegativeSet.contains(i))
			{
				;//System.out.println("Negative - " + Integer.toString(i) + "\t" + Double.toString(lastIterationProbabilities.getWeights().get(i).get(0)));
			}
			else
			{
				System.out.println("Unknown - " + Integer.toString(i) + "\t" + Double.toString(lastIterationProbabilities.getWeights().get(i).get(0)));
			}
		}
	}

	static Map<Integer, Map<Integer, Double>> determineWeights(ProcessDataForGrowing dataset, int numberTopConnectionsToKeep)
	{
		// Get abase list of all observation indices.
		List<Integer> observationIndices = new ArrayList<Integer>();
		for (int i = 0; i < dataset.numberObservations; i++)
		{
			observationIndices.add(i);
		}

		Map<Integer, Map<Integer, Double>> weights = new HashMap<Integer, Map<Integer, Double>>();
		Map<Integer, Map<Integer, Double>> closestDistances = new HashMap<Integer, Map<Integer, Double>>();
		for (int i = 0; i < dataset.numberObservations; i++)
		{
			double maximumDistance = 0.0;
			double minimumDistance = Integer.MAX_VALUE;

			// Get the distance between observation i and all other observations (excluding itself).
			List<Integer> otherObservations = new ArrayList<Integer>(observationIndices);
			otherObservations.remove(i);
			Map<Integer, Double> distances = distanceBetweenObservations(dataset, i, otherObservations);

			// Select the shortest numberTopConnectionsToKeep distances to keep.
			List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
			for (Integer j : distances.keySet())
			{
				double distanceToJ = distances.get(j);
				sortedDistances.add(new IndexedDoubleData(distances.get(j), j));
				if (distanceToJ > maximumDistance)
				{
					maximumDistance = distanceToJ;
				}
				if (distanceToJ < minimumDistance)
				{
					minimumDistance = distanceToJ;
				}
			}
			Collections.sort(sortedDistances);

			Map<Integer, Double> closestToI = new HashMap<Integer, Double>();
			Map<Integer, Double> weightingsForI = new HashMap<Integer, Double>();
			for (int j = 0; j < numberTopConnectionsToKeep; j++)
			{
				double distanceToJ = sortedDistances.get(j).getData();
				int indexOfJ = sortedDistances.get(j).getIndex();
				closestToI.put(indexOfJ, distanceToJ);
				weightingsForI.put(indexOfJ, 1 - ((distanceToJ - minimumDistance) / (maximumDistance - minimumDistance)));
			}
			closestDistances.put(i, closestToI);
			weights.put(i, weightingsForI);
		}

		return weights;
	}

	static Map<Integer, Double> distanceBetweenObservations(ProcessDataForGrowing dataset, int observation, List<Integer> obsToCompareTo)
	{
		Map<Integer, Double> distances = new HashMap<Integer, Double>();
		for (Integer i : obsToCompareTo)
		{
			double obsDistance = 0.0;
			for (String s : dataset.covariableData.keySet())
			{
				obsDistance += Math.pow(dataset.covariableData.get(s).get(observation) - dataset.covariableData.get(s).get(i), 2);
			}
			obsDistance = Math.pow(obsDistance, 0.5);
			distances.put(i, obsDistance);
		}
		return distances;
	}

	static List<Double> distanceFromMean(List<Integer> observationIndices, Map<String, Double> meanPositiveVector, ProcessDataForGrowing dataset)
	{
		List<Double> distances = new ArrayList<Double>();
		for (Integer i : observationIndices)
		{
			double obsDistance = 0.0;
			for (String s : dataset.covariableData.keySet())
			{
				obsDistance += Math.pow(meanPositiveVector.get(s) - dataset.covariableData.get(s).get(i), 2);
			}
			obsDistance = Math.pow(obsDistance, 0.5);
			distances.add(obsDistance);
		}
		return distances;
	}

	static double L1Norm(AdjacencyList matA, AdjacencyList matB)
	{
		AdjacencyList difference = matA.subtract(matB);
		Map<Integer, Map<Integer, Double>> weights = difference.getWeights();
		double norm = 0.0;
		for (Integer i : weights.keySet())
		{
			for (Integer j : weights.get(i).keySet())
			{
				norm += Math.abs(weights.get(i).get(j));
			}
		}

		return norm;
	}

}
