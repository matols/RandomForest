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

		System.out.println(meanPositiveVector);

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

		System.out.println(distsanceToPositiveCluster);
		System.out.println(meanDistanceToPositive);
		System.out.println(reliableNegativeSet);
		System.out.println(reliableNegativeSet.size());

		determineWeights(processedDataForLearning, numberTopConnectionsToKeep);
	}

	static void determineWeights(ProcessDataForGrowing dataset, int numberTopConnectionsToKeep)
	{
		// Get abase list of all observation indices.
		List<Integer> observationIndices = new ArrayList<Integer>();
		for (int i = 0; i < dataset.numberObservations; i++)
		{
			observationIndices.add(i);
		}

		Map<Integer, Map<Integer, Double>> closestDistances = new HashMap<Integer, Map<Integer, Double>>();
		double maximumDistance = 0.0;
		double minimumDistance = Integer.MAX_VALUE;
		for (int i = 0; i < dataset.numberObservations; i++)
		{
			// Get the distance between observation i and all other observations (excluding itself).
			List<Integer> otherObservations = new ArrayList<Integer>(observationIndices);
			otherObservations.remove(i);
			Map<Integer, Double> distances = distanceBetweenObservations(dataset, i, otherObservations);

			// Select the shortest numberTopConnectionsToKeep distances to keep.
			List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
			for (Integer j : distances.keySet())
			{
				sortedDistances.add(new IndexedDoubleData(distances.get(j), j));
			}
			Collections.sort(sortedDistances);
			Map<Integer, Double> closestToI = new HashMap<Integer, Double>();
			for (int j = 0; j < numberTopConnectionsToKeep; j++)
			{
				double distanceToJ = sortedDistances.get(j).getData();
				closestToI.put(sortedDistances.get(j).getIndex(), distanceToJ);
				if (distanceToJ > maximumDistance)
				{
					maximumDistance = distanceToJ;
				}
				if (distanceToJ < minimumDistance)
				{
					minimumDistance = distanceToJ;
				}
			}
			closestDistances.put(i, closestToI);
		}

		System.out.println(closestDistances);
		System.out.println(maximumDistance);
		System.out.println(minimumDistance);

		// Determine weights from the distances.
		Map<Integer, Map<Integer, Double>> weights = new HashMap<Integer, Map<Integer, Double>>();
		for (Integer i : closestDistances.keySet())
		{
			Map<Integer, Double> weightingsForI = new HashMap<Integer, Double>();
			for (Integer j : closestDistances.get(i).keySet())
			{
				weightingsForI.put(j, 1 - (closestDistances.get(i).get(j) / (maximumDistance - minimumDistance)));
			}
			weights.put(i, weightingsForI);
		}

		System.out.println(weights);
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

}
