package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.ImmutableTwoValues;
import tree.IndexedDoubleData;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class KMeansWithNegative
{

	/**
	 * @param args
	 */
	public ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> main(String dataForLearning, int numberOfMeans, int clusteringRepetitions)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isStandardised = true;

		String[] variablesToIgnore = new String[]{"OGlycosylation"};  // Make sure to ignore any variables that are constant. Otherwise the standardised value of the variable will be NaN.
		ctrl.variablesToIgnore = Arrays.asList(variablesToIgnore);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Process the input data.
		ProcessDataForGrowing processedDataForLearning = new ProcessDataForGrowing(dataForLearning, ctrl);

		// Setup the results.
		Map<Integer, Double> weightModifiers = new HashMap<Integer, Double>();
		Set<Integer> finalPositiveSet = new HashSet<Integer>();
		Set<Integer> finalNegativeSet = new HashSet<Integer>();

		// Determine the indices for the positive, unlabelled and all observations.
		List<Integer> allObservations = new ArrayList<Integer>();
		List<Integer> positiveObservations = new ArrayList<Integer>();
		List<Integer> unlabelledObservations = new ArrayList<Integer>();
		List<Integer> reliableNegativeSet = new ArrayList<Integer>();
		for (int i = 0; i < processedDataForLearning.numberObservations; i++)
		{
			allObservations.add(i);
			if (processedDataForLearning.responseData.get(i).equals("Positive"))
			{
				// If the observation is in the 'Positive' class.
				positiveObservations.add(i);
				finalPositiveSet.add(i);
				weightModifiers.put(i, 1.0);
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
		int numberNegativeObservations = 0;

		// Generate the mean vector for the positive observations.
		Map<String, Double> meanPositiveVector = calculateMeanVector(positiveObservations, processedDataForLearning);

		// Calculate the reliable negative set and U-RN.
		reliableNegativeSet = determineReliableNegative(unlabelledObservations, meanPositiveVector, processedDataForLearning);
		numberNegativeObservations = reliableNegativeSet.size();
		for (Integer i : reliableNegativeSet)
		{
			finalNegativeSet.add(i);
			weightModifiers.put(i, 1.0);
		}
		List<Integer> unlabelledMinusRN = new ArrayList<Integer>(unlabelledObservations);
		unlabelledMinusRN.removeAll(reliableNegativeSet);

		// Sanity check on the number of means.
		if (numberOfMeans > unlabelledMinusRN.size())
		{
			System.out.format("You specified %d means, but only have %d observations in U - RN.\n", numberOfMeans, unlabelledMinusRN.size());
			System.exit(0);
		}

		// Generate the mean vector for the reliable negative observations.
		Map<String, Double> meanNegativeVector = calculateMeanVector(reliableNegativeSet, processedDataForLearning);

		// Cluster the unlabelled observations clusteringRepetitions times.
		Map<Integer, Map<String, Double>> observationWeightings = new HashMap<Integer, Map<String, Double>>();
		Map<String, Double> baseWeighting = new HashMap<String, Double>();
		baseWeighting.put("Positive", 0.0);
		baseWeighting.put("Negative", 0.0);
		for (Integer i : unlabelledMinusRN)
		{
			observationWeightings.put(i, new HashMap<String, Double>(baseWeighting));
		}
		for (int i = 0; i < clusteringRepetitions; i++)
		{
			// Initialise the means.
			List<Map<String, Double>> clusterMeans = initialiseClusterMeans(numberOfMeans, processedDataForLearning, unlabelledMinusRN);

			// Perform the initial determination of the cluster assignments.
			Map<Integer, Set<Integer>> clusterAssignment = assignCluster(clusterMeans, processedDataForLearning, unlabelledMinusRN);  // A mapping from observation indices to the index of the cluster they belong to.
			boolean isAssignmentChanged = true;

			// Update the clusters until convergence.
			while (isAssignmentChanged)
			{
				isAssignmentChanged = false;

				// Update means.
				clusterMeans = updateClusterMeans(clusterAssignment, processedDataForLearning);

				// Update the assignments.
				Map<Integer, Set<Integer>> newAssignment = assignCluster(clusterMeans, processedDataForLearning, unlabelledMinusRN);

				// Determine if the assignment has changed.
				for (Integer j : newAssignment.keySet())
				{
					if (!newAssignment.get(j).equals(clusterAssignment.get(j)))
					{
						// If the assignment is not the same.
						isAssignmentChanged = true;
						break;
					}
				}
				clusterAssignment = newAssignment;
			}

			// Determine the distance of each cluster from the mean positive and mean negative vectors.
			// From this determine the negative and positive weight for each observation in U-RN.
			Map<Integer, Map<String, Double>> clusterDistances = new HashMap<Integer, Map<String, Double>>();
			for (int j = 0; j < numberOfMeans; j++)
			{
				Map<String, Double> mean = clusterMeans.get(j);
				Map<String, Double> distanceToThisCluster = new HashMap<String, Double>(baseWeighting);
				distanceToThisCluster.put("Positive", distanceBetweenMeans(meanPositiveVector, mean));
				distanceToThisCluster.put("Negative", distanceBetweenMeans(meanNegativeVector, mean));
				clusterDistances.put(j, distanceToThisCluster);
			}

			// Update the distance to the positive and negative mean vectors for each observation in U-RN.
			for (Integer clusterIndex : clusterAssignment.keySet())
			{
				for (Integer obs : clusterAssignment.get(clusterIndex))
				{
					observationWeightings.get(obs).put("Positive", observationWeightings.get(obs).get("Positive") + clusterDistances.get(clusterIndex).get("Positive"));
					observationWeightings.get(obs).put("Negative", observationWeightings.get(obs).get("Negative") + clusterDistances.get(clusterIndex).get("Negative"));
				}
			}
		}

		// Determine the final positive and negative sets, and the weightings for the observations.
		for (Integer i : unlabelledMinusRN)
		{
			double positiveWeight = observationWeightings.get(i).get("Positive");
			double negativeWeight = observationWeightings.get(i).get("Negative");
			if (positiveWeight > negativeWeight)
			{
				finalPositiveSet.add(i);
				weightModifiers.put(i, (positiveWeight - negativeWeight) / (positiveWeight + negativeWeight));
			}
			else
			{
				finalNegativeSet.add(i);
				weightModifiers.put(i, (negativeWeight - positiveWeight) / (positiveWeight + negativeWeight));
			}
		}

		return new ImmutableTwoValues<Set<Integer>, Map<Integer, Double>>(finalPositiveSet, weightModifiers);
	}


	private Map<Integer, Set<Integer>> assignCluster(List<Map<String, Double>> clusterMeans, ProcessDataForGrowing dataset,
			List<Integer> observationsToCluster)
	{
		List<Integer> observationIndices = new ArrayList<Integer>(observationsToCluster);
		Map<Integer, Double> distancesToCluster = new HashMap<Integer, Double>();
		Map<Integer, Integer> clusterAssignment = new HashMap<Integer, Integer>();
		for (Integer i : observationsToCluster)
		{
			distancesToCluster.put(i, Double.MAX_VALUE);
			clusterAssignment.put(i, 0);
		}

		for (int i = 0; i < clusterMeans.size(); i++)
		{
			Map<String, Double> mean = clusterMeans.get(i);
			Map<Integer, Double> distances = distanceFromMean(observationIndices, mean, dataset);
			// Determine if the distance to this cluster is less than the distance to all other clusters checked so far.
			for (Integer j : observationIndices)
			{
				if (distances.get(j) < distancesToCluster.get(j))
				{
					// If the distance to the curretn cluster mean is less than the distance to the closest mean checked so far,
					// then change the closest cluster mean to this one.
					distancesToCluster.put(j, distances.get(j));
					clusterAssignment.put(j, i);
				}
			}
		}

		Map<Integer, Set<Integer>> clusters = new HashMap<Integer, Set<Integer>>();
		for (int i = 0; i < clusterMeans.size(); i++)
		{
			Set<Integer> observationsInCluster = new HashSet<Integer>();
			for (Integer j : observationIndices)
			{
				if (clusterAssignment.get(j) == i)
				{
					observationsInCluster.add(j);
				}
			}
			clusters.put(i, observationsInCluster);
			observationIndices.removeAll(observationsInCluster);
		}
		return clusters;
	}

	private Map<String, Double> calculateMeanVector(List<Integer> observationsToGetMeanFor, ProcessDataForGrowing dataset)
	{
		Map<String, Double> meanVector = new HashMap<String, Double>();
		for (String s : dataset.covariableData.keySet())
		{
			double expectedValue = 0.0;
			for (Integer i : observationsToGetMeanFor)
			{
				expectedValue += dataset.covariableData.get(s).get(i);
			}
			expectedValue /= observationsToGetMeanFor.size();
			meanVector.put(s, expectedValue);
		}
		return meanVector;
	}

	private List<Integer> determineReliableNegative(List<Integer> unlabelledObservations, Map<String, Double> meanPositiveVector,
			ProcessDataForGrowing processedDataForLearning)
	{
		// Determine the distance of all unlabelled observations from the mean positive vector.
		Map<Integer, Double> distsanceToPositiveCluster = distanceFromMean(unlabelledObservations, meanPositiveVector, processedDataForLearning);
		List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
		double meanDistanceToPositive = 0.0;
		for (Integer i : unlabelledObservations)
		{
			meanDistanceToPositive += distsanceToPositiveCluster.get(i);
			sortedDistances.add(new IndexedDoubleData(distsanceToPositiveCluster.get(i), i));
		}
		Collections.sort(sortedDistances);
		meanDistanceToPositive /= unlabelledObservations.size();

		// The reliable negative set is all the observations with a distance to the positive cluster greater than the mean distance.
		List<Integer> reliableNegativeSet = new ArrayList<Integer>();
		for (int i = 0; i < sortedDistances.size(); i++)
		{
			if (sortedDistances.get(i).getData() > meanDistanceToPositive)
			{
				reliableNegativeSet.add(sortedDistances.get(i).getIndex());
			}
		}
		return reliableNegativeSet;
	}

	private double distanceBetweenMeans(Map<String, Double> meanA, Map<String, Double> meanB)
	{
		double distance = 0.0;
		for (String s : meanA.keySet())
		{
			distance += Math.pow(meanA.get(s) - meanB.get(s), 2);
		}
		return Math.pow(distance, 0.5);
	}

	private Map<Integer, Double> distanceFromMean(List<Integer> observationIndices, Map<String, Double> meanPositiveVector, ProcessDataForGrowing dataset)
	{
		Map<Integer, Double> distances = new HashMap<Integer, Double>();
		for (Integer i : observationIndices)
		{
			double obsDistance = 0.0;
			for (String s : dataset.covariableData.keySet())
			{
				obsDistance += Math.pow(meanPositiveVector.get(s) - dataset.covariableData.get(s).get(i), 2);
			}
			obsDistance = Math.pow(obsDistance, 0.5);
			distances.put(i, obsDistance);
		}
		return distances;
	}

	private List<Map<String, Double>> initialiseClusterMeans(int numberOfMeans, ProcessDataForGrowing dataset, List<Integer> possibleMeans)
	{
		// Randomise the list of indices of the observations in the dataset, and then take the first numberOfMeans observations
		// to be the observations that the cluster means are initialised to.
		List<Integer> observationIndices = new ArrayList<Integer>(possibleMeans);
		Collections.shuffle(observationIndices);

		List<Map<String, Double>> clusterMeans = new ArrayList<Map<String, Double>>();
		for (int i = 0; i < numberOfMeans; i++)
		{
			int observationIndex = observationIndices.get(i);
			Map<String, Double> newMean = new HashMap<String, Double>();
			for (String s : dataset.covariableData.keySet())
			{
				newMean.put(s, dataset.covariableData.get(s).get(observationIndex));
			}
			clusterMeans.add(newMean);
		}

		return clusterMeans;
	}

	private List<Map<String, Double>> updateClusterMeans(Map<Integer, Set<Integer>> clusterAssignment, ProcessDataForGrowing dataset)
	{
		List<Map<String, Double>> clusterMeans = new ArrayList<Map<String, Double>>();
		for (int i = 0; i < clusterAssignment.size(); i++)
		{
			clusterMeans.add(calculateMeanVector(new ArrayList<Integer>(clusterAssignment.get(i)), dataset));
		}
		return clusterMeans;
	}

}
