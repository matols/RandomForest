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
		ctrl.isCalculateOOB = false;

		int numberOfForests = 100;
		double[] fractionPositiveNeeded = new double[]{0.5, 0.55, 0.6, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1.0};

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
		String resultsLoc = resultsDirLoc + "/Results.txt";
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the indices for the unlabelled observations.
		List<Integer> unlabelledObservations = new ArrayList<Integer>();
		Map<Integer, Map<String, Double>> observationWeightings = new HashMap<Integer, Map<String, Double>>();
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
			}
		}

		// Create the forests.
		Random seedGenerator = new Random();
		for (int i = 0; i < numberOfForests; i++)
		{
			System.out.format("Now generating forest %d.\n", i);
			Forest forest = new Forest(processedDataForLearning, ctrl, seedGenerator.nextLong());
			forest.setWeightsByClass(weights);
			forest.growForest();

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
			}
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
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}