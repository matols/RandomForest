package SyntheticData;

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

public class KNNPULearning
{

	/**
	 * @param args
	 */
	public ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> main(String dataForLearning, int numberOfNeighbours, boolean isReliableNegativesGenerated)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isStandardised = true;

		String[] variablesToIgnore = new String[]{};  // Make sure to ignore any variables that are constant. Otherwise the standardised value of the variable will be NaN.
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

		if (isReliableNegativesGenerated)
		{
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

			// Calculate the reliable negative set.
			reliableNegativeSet = determineReliableNegative(unlabelledObservations, meanPositiveVector,
					processedDataForLearning);
			numberNegativeObservations = reliableNegativeSet.size();
			for (Integer i : reliableNegativeSet)
			{
				finalNegativeSet.add(i);
				weightModifiers.put(i, 1.0);
			}
		}

		List<Integer> observationsToCheck = new ArrayList<Integer>(unlabelledObservations);
		List<Integer> observationsToCheckAgainst = new ArrayList<Integer>();
		if (isReliableNegativesGenerated)
		{
			observationsToCheck.removeAll(reliableNegativeSet);
			observationsToCheckAgainst.addAll(positiveObservations);
			observationsToCheckAgainst.addAll(reliableNegativeSet);
		}
		else
		{
			observationsToCheckAgainst.addAll(allObservations);
		}

		for (Integer i : observationsToCheck)
		{
			List<Integer> nonI = new ArrayList<Integer>(observationsToCheckAgainst);
			nonI.remove(i);
			Map<Integer, Double> distancesFromI = distanceBetweenObservations(processedDataForLearning, i, nonI);

			List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
			for (Integer j : distancesFromI.keySet())
			{
				sortedDistances.add(new IndexedDoubleData(distancesFromI.get(j), j));
			}
			Collections.sort(sortedDistances);

			double numberPositive = 0.0;
			double numberNonPositive = 0.0;
			for (int j = 0; j < numberOfNeighbours; j++)
			{
				Integer observationIndex = sortedDistances.get(j).getIndex();
				if (positiveObservations.contains(observationIndex))
				{
					numberPositive += 1;
				}
				else
				{
					numberNonPositive += 1;
				}
			}

			if (numberPositive > numberNonPositive)
			{
				finalPositiveSet.add(i);
				weightModifiers.put(i, (numberPositive - numberNonPositive) / numberOfNeighbours);
			}
			else
			{
				finalNegativeSet.add(i);
				weightModifiers.put(i, (numberNonPositive - numberPositive) / numberOfNeighbours);
			}
		}

		return new ImmutableTwoValues<Set<Integer>, Map<Integer, Double>>(finalPositiveSet, weightModifiers);
	}


	private Map<Integer, Double> distanceBetweenObservations(ProcessDataForGrowing dataset, int observation, List<Integer> obsToCompareTo)
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

	private List<Integer> determineReliableNegative(List<Integer> unlabelledObservations, Map<String, Double> meanPositiveVector,
			ProcessDataForGrowing processedDataForLearning)
	{
		// Determine the distance of all unlabelled observations from the mean positive vector.
		List<Double> distsanceToPositiveCluster = distanceFromMean(unlabelledObservations, meanPositiveVector, processedDataForLearning);
		List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
		double meanDistanceToPositive = 0.0;
		for (int i = 0; i < unlabelledObservations.size(); i++)
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

	private List<Double> distanceFromMean(List<Integer> observationIndices, Map<String, Double> meanPositiveVector, ProcessDataForGrowing dataset)
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
