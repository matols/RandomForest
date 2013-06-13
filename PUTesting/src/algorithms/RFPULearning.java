package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import tree.Forest;
import tree.ImmutableTwoValues;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class RFPULearning
{

	/**
	 * @param args
	 */
	public ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> main(String dataForLearning, int numberOfTrees,
			int numberOfForests, int mtry, String[] variablesToIgnore, Map<String, Double> weights, double fractionPositiveNeeded)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.numberOfTreesToGrow = numberOfTrees;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.mtry = mtry;
		ctrl.isCalculateOOB = false;
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

		// Determine the indices for the unlabelled observations.
		List<Integer> unlabelledObservations = new ArrayList<Integer>();
		for (int i = 0; i < processedDataForLearning.numberObservations; i++)
		{
			weightModifiers.put(i, 1.0);
			if (processedDataForLearning.responseData.get(i).equals("Unlabelled"))
			{
				// If the observation is in the 'Unlabelled' class.
				unlabelledObservations.add(i);
			}
			else
			{
				finalPositiveSet.add(i);
			}
		}

		// Create the forests.
		List<Forest> forests = new ArrayList<Forest>();
		Random seedGenerator = new Random();
		for (int i = 0; i < numberOfForests; i++)
		{
			Forest newForest = new Forest(processedDataForLearning, ctrl, seedGenerator.nextLong());
			newForest.setWeightsByClass(weights);
			newForest.growForest();
			forests.add(newForest);
		}

		// Predict the class of each unlabelled observation using the forests.
		Map<Integer, Map<String, Double>> observationWeightings = new HashMap<Integer, Map<String, Double>>();
		for (Integer i : unlabelledObservations)
		{
			List<Integer> currentObservation = new ArrayList<Integer>();
			currentObservation.add(i);

			double positiveWeight = 0.0;
			double negativeWeight = 0.0;

			// Predict the class of observation i from each forest.
			for (Forest f : forests)
			{
				// Get the trees that this observation is OOB on for this forest.
				List<Integer> treesObservationIsOOBOn = f.oobOnTree.get(i);

				// Predict the class of the observation on the trees it is OOB on.
				Map<Integer, Map<String, Double>> predResults = f.predictRaw(processedDataForLearning, currentObservation, treesObservationIsOOBOn);

				// Update the weightings for the observation.
				positiveWeight += predResults.get(i).get("Positive");
				negativeWeight += predResults.get(i).get("Unlabelled");
			}

			Map<String, Double> currentObsWeighting = new HashMap<String, Double>();
			currentObsWeighting.put("Positive", positiveWeight);
			currentObsWeighting.put("Negative", negativeWeight);
			observationWeightings.put(i, currentObsWeighting);
		}

		// Determine the final positive and negative sets, and the weightings for the observations.
		for (Integer i : unlabelledObservations)
		{
			double positiveWeight = observationWeightings.get(i).get("Positive");
			double negativeWeight = observationWeightings.get(i).get("Negative");
			double fractionPositive = positiveWeight / (positiveWeight + negativeWeight);
			if (fractionPositive >= fractionPositiveNeeded)
			{
				finalPositiveSet.add(i);
			}
			else
			{
				finalNegativeSet.add(i);
			}
		}

		return new ImmutableTwoValues<Set<Integer>, Map<Integer, Double>>(finalPositiveSet, weightModifiers);
	}

}
