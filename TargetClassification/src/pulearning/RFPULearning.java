package pulearning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import tree.Forest;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class RFPULearning
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
		ctrl.numberOfTreesToGrow = 1000;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.mtry = 10;
		ctrl.isCalculateOOB = true;

		int numberOfForests = 100;
		double[] fractionPositiveNeeded = new double[]{0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1.0};

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Process the input arguments.
		String dataForLearning = args[0];
		ProcessDataForGrowing processedDataForLearning = new ProcessDataForGrowing(dataForLearning, ctrl);
		String resultsDirLoc = args[1];
		File resultsDirectory = new File(resultsDirLoc);
		if (!resultsDirectory.exists())
		{
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The output directory could not be created.");
				System.exit(0);
			}
		}
		else if (!resultsDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
			System.exit(0);
		}
		String ctrlSaveLoc = resultsDirLoc + "/CtrlUsed.txt";
		String parameterSaveLoc = resultsDirLoc + "/ParametersUsed.txt";
		String resultsLoc = resultsDirLoc + "/ObservationsToRemove.txt";
		String removalResultsLoc = resultsDirLoc + "/EffectOfRemoval.txt";
		ctrl.save(ctrlSaveLoc);
		try
		{
			FileWriter paramOutputFile = new FileWriter(parameterSaveLoc);
			BufferedWriter paramOutputWriter = new BufferedWriter(paramOutputFile);
			paramOutputWriter.write("Number Of Forests Used - " + Integer.toString(numberOfForests));
			paramOutputWriter.newLine();
			paramOutputWriter.write("Positive Fractions Ued - " + Arrays.toString(fractionPositiveNeeded));
			paramOutputWriter.newLine();
			paramOutputWriter.newLine();
			paramOutputWriter.write("Weights - " + weights.toString());
			paramOutputWriter.close();

			FileWriter resultsOutputFile = new FileWriter(resultsLoc);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("PositiveFraction\tObservationIndices");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();

			resultsOutputFile = new FileWriter(removalResultsLoc);
			resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("PositiveFraction\tTP\tFP\tTN\tFN");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the indices for the unlabelled observations.
		List<Integer> unlabelledObservations = new ArrayList<Integer>();
		Map<Integer, Map<String, Double>> observationWeightings = new HashMap<Integer, Map<String, Double>>();
		Map<Integer, Map<String, Double>> observationPredictions = new HashMap<Integer, Map<String, Double>>();  // The record of false positives and true negatives that are being discarded if an observation is removed.
		for (int i = 0; i < processedDataForLearning.numberObservations; i++)
		{
			if (processedDataForLearning.responseData.get(i).equals("Unlabelled"))
			{
				// If the observation is in the 'Unlabelled' class.
				unlabelledObservations.add(i);
				Map<String, Double> currentObsWeighting = new HashMap<String, Double>();
				currentObsWeighting.put("Positive", 0.0);
				currentObsWeighting.put("Negative", 0.0);
				observationWeightings.put(i, currentObsWeighting);
				Map<String, Double> currentObsPredictions = new HashMap<String, Double>();
				currentObsPredictions.put("FalsePositive", 0.0);
				currentObsPredictions.put("TrueNegative", 0.0);
				observationPredictions.put(i, currentObsPredictions);
			}
		}

		// Generate the seeds to use.
		Random seedGenerator = new Random();
		List<Long> seedsToUse = new ArrayList<Long>();
		while (seedsToUse.size() < numberOfForests)
		{
			long potentialSeed = seedGenerator.nextLong();
			if (!seedsToUse.contains(potentialSeed))
			{
				seedsToUse.add(potentialSeed);
			}
		}

		// Create the forests.
		Map<String, Map<String, Double>> cummulativeConfusionMatrix = new HashMap<String, Map<String, Double>>();
		cummulativeConfusionMatrix.put("Positive", new HashMap<String, Double>());
		cummulativeConfusionMatrix.get("Positive").put("TruePositive", 0.0);
		cummulativeConfusionMatrix.get("Positive").put("FalsePositive", 0.0);
		cummulativeConfusionMatrix.put("Unlabelled", new HashMap<String, Double>());
		cummulativeConfusionMatrix.get("Unlabelled").put("TruePositive", 0.0);
		cummulativeConfusionMatrix.get("Unlabelled").put("FalsePositive", 0.0);
		for (int i = 0; i < numberOfForests; i++)
		{
			System.out.format("Now generating forest %d.\n", i);
			Forest forest = new Forest(processedDataForLearning, ctrl, seedsToUse.get(i));
			forest.setWeightsByClass(weights);
			forest.growForest();

			// Add the results of the confusion matrix to the cumulative confusion matrix.
			for (String s : forest.oobConfusionMatrix.keySet())
			{
				for (String p : forest.oobConfusionMatrix.get(s).keySet())
				{
					Double oldValue = cummulativeConfusionMatrix.get(s).get(p);
					Double newValue = forest.oobConfusionMatrix.get(s).get(p);
					cummulativeConfusionMatrix.get(s).put(p, oldValue + newValue);
				}
			}

			for (Integer j : unlabelledObservations)
			{
				List<Integer> currentObservation = new ArrayList<Integer>();
				currentObservation.add(j);

				// Get the trees that this observation is OOB on for this forest.
				List<Integer> treesObservationIsOOBOn = forest.oobOnTree.get(j);

				// Predict the class of the observation on the trees it is OOB on.
				Map<Integer, Map<String, Double>> predResults = forest.predictRaw(processedDataForLearning, currentObservation, treesObservationIsOOBOn);

				// Get the current prediction weighting for observation j.
				double currentPosWeight = observationWeightings.get(j).get("Positive");
				double currentNegWeight = observationWeightings.get(j).get("Negative");

				// Update the weightings for the observation.
				observationWeightings.get(j).put("Positive", currentPosWeight + predResults.get(j).get("Positive"));
				observationWeightings.get(j).put("Negative", currentNegWeight + predResults.get(j).get("Unlabelled"));

				// Update the record of false positive and true negative predictions.
				if (predResults.get(j).get("Positive") > predResults.get(j).get("Unlabelled"))
				{
					// If the observation would be predicted to be positive by this forest.
					Double oldValue = observationPredictions.get(j).get("FalsePositive");
					observationPredictions.get(j).put("FalsePositive", oldValue + 1);
				}
				else
				{
					Double oldValue = observationPredictions.get(j).get("TrueNegative");
					observationPredictions.get(j).put("TrueNegative", oldValue + 1);
				}
			}
		}

		// Write out the cumulative confusion matrix for the vanilla predictions.
		try
		{
			FileWriter resultsOutputFile = new FileWriter(removalResultsLoc, true);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("NoPU\t");
			resultsOutputWriter.write(Double.toString(cummulativeConfusionMatrix.get("Positive").get("TruePositive")));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(Double.toString(cummulativeConfusionMatrix.get("Positive").get("FalsePositive")));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(Double.toString(cummulativeConfusionMatrix.get("Unlabelled").get("TruePositive")));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(Double.toString(cummulativeConfusionMatrix.get("Unlabelled").get("FalsePositive")));
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the observations to remove from the unlabelled set.
		System.out.println("Now determining the observations to remove from the unlabelled set.");
		for (double posFracNeeded : fractionPositiveNeeded)
		{
			Set<Integer> observationsNoLongerUnlabelled = new HashSet<Integer>();
			for (Integer i : unlabelledObservations)
			{
				double positiveWeight = observationWeightings.get(i).get("Positive");
				double negativeWeight = observationWeightings.get(i).get("Negative");
				double fractionPositive = positiveWeight / (positiveWeight + negativeWeight);
				if (fractionPositive >= posFracNeeded)
				{
					observationsNoLongerUnlabelled.add(i);
				}
			}

			// Write out the indices of the observations to remove.
			try
			{
				FileWriter resultsOutputFile = new FileWriter(resultsLoc, true);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write(Double.toString(posFracNeeded));
				resultsOutputWriter.write("\t");
				String observationIndices = "";
				for (Integer i : observationsNoLongerUnlabelled)
				{
					observationIndices += Integer.toString(i);
					observationIndices += ",";
				}
				observationIndices = observationIndices.substring(0, Math.max(0, observationIndices.length() - 1));  // Chop off the last comma (use max as some strings will be of length 0).
				resultsOutputWriter.write(observationIndices);
				resultsOutputWriter.newLine();
				resultsOutputWriter.close();

				resultsOutputFile = new FileWriter(removalResultsLoc, true);
				resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write(Double.toString(posFracNeeded));
				resultsOutputWriter.write("\t");
				double FP = 0.0;
				double TN = 0.0;
				for (Integer i : observationsNoLongerUnlabelled)
				{
					FP += observationPredictions.get(i).get("FalsePositive");
					TN += observationPredictions.get(i).get("TrueNegative");
				}
				resultsOutputWriter.write("0");
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Double.toString(FP));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Double.toString(TN));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write("0");
				resultsOutputWriter.newLine();
				resultsOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}