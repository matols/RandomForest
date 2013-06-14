package analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import datasetgeneration.CrossValidationFoldGenerationMultiClass;

import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class SampleSizeTesting
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String inputFile = args[0];
		String resultsDir = args[1];
		main(inputFile, resultsDir);
	}

	public static void main(String inputFile, String resultsDir)
	{
		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
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

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int repetitions = 10;
		int cvFoldsToUse = 10;
		Integer[] sizeOfDatasets = {50, 75};
		Double[] fractionOfPositives = {0.5};
		Double[] weightsToUse = {1.0, 2.0, 3.0, 4.0, 5.0};
		Integer[] trainingObsToUse = {};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 1000;
		ctrl.mtry = 10;
		ctrl.isCalculateOOB = true;  // Set this to false to use cross-validation, and true to use OOB observations.
		ctrl.minNodeSize = 1;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);

		String majorityClass = "Unlabelled";
		String minorityClass = "Positive";
		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Determine the observations of each class.
		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputFile, ctrl);
		List<String> responseClasses = new ArrayList<String>(new HashSet<String>(procData.responseData));
		Collections.sort(responseClasses);
		Map<String, List<Integer>> responseSplits = new HashMap<String, List<Integer>>();
		for (String s : responseClasses)
		{
			responseSplits.put(s, new ArrayList<Integer>());
		}
		for (int i = 0; i < procData.numberObservations; i++)
		{
			responseSplits.get(procData.responseData.get(i)).add(i);
		}
		int numberOfObservations = procData.numberObservations;
		int numberPosObs = responseSplits.get(minorityClass).size();
		int numberUnlabObs = responseSplits.get(majorityClass).size();

		// Setup the results output files.
		String fullDatasetResultsLocation = resultsDir + "/FullDatasetResults.txt";
		String fullDatasetGMeanResultsLocation = resultsDir + "/FullDatasetAllValues.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(fullDatasetResultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("SampleSize\tPositiveFraction\tWeight\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError");
			for (String s : responseClasses)
			{
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(s);
				resultsOutputWriter.write("\t");
			}
			resultsOutputWriter.write("\tTimeTaken(ms)");
			resultsOutputWriter.newLine();
			resultsOutputWriter.write("\t\t\t\t\t\t\t\t\t");
			for (String s : responseClasses)
			{
				resultsOutputWriter.write("\tTrue\tFalse");
			}
			resultsOutputWriter.write("\t");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();

			resultsOutputFile = new FileWriter(fullDatasetGMeanResultsLocation);
			resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("SampleSize\tPositiveFraction\tWeight");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Generate the seeds for the repetitions, and the CV folds for each repetition.
		Random randGen = new Random();
		List<Long> seeds = new ArrayList<Long>();
		for (int i = 0; i < repetitions; i++)
		{
			long seedToUse = randGen.nextLong();
			while (seeds.contains(seedToUse))
			{
				seedToUse = randGen.nextLong();
			}
			seeds.add(seedToUse);
		}

		// Generate CV folds.
		String cvFoldLocation = resultsDir + "/CVFolds-Repetition";
		if (!ctrl.isCalculateOOB)
		{
			for (int i = 0; i < repetitions; i++)
			{
				String repCvFoldLoc = cvFoldLocation + Integer.toString(i);
				File cvFoldDir = new File(repCvFoldLoc);
				if (!cvFoldDir.exists())
				{
					boolean isDirCreated = cvFoldDir.mkdirs();
					if (!isDirCreated)
					{
						System.out.println("The CV fold directory does not exist, and could not be created.");
						System.exit(0);
					}
				}
				CrossValidationFoldGenerationMultiClass.main(inputFile, repCvFoldLoc, cvFoldsToUse);
			}
		}

		// Write out the parameters used.
		String parameterLocation = resultsDir + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Trees grown - " + Integer.toString(ctrl.numberOfTreesToGrow));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Replacement used - " + Boolean.toString(ctrl.isReplacementUsed));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Repetitions used - " + Integer.toString(repetitions));
			parameterOutputWriter.newLine();
			if (ctrl.isCalculateOOB)
			{
				parameterOutputWriter.write("CV not used");
				parameterOutputWriter.newLine();
			}
			else
			{
				parameterOutputWriter.write("CV used with " + Integer.toString(cvFoldsToUse) + " folds");
				parameterOutputWriter.newLine();
			}
			parameterOutputWriter.write("Mtry used - " + Integer.toString(ctrl.mtry));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Sizes of datasets used - " + Arrays.toString(sizeOfDatasets));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Fractions of dataset that is positive observations used - " + Arrays.toString(fractionOfPositives));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used - " + weights.toString());
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Generate the subsets.
		for (Integer datasetSize : sizeOfDatasets)
		{
			System.out.format("Now working on dataset size - %d.\n", datasetSize);

			for (Double positiveFraction : fractionOfPositives)
			{
			    Date startTime = new Date();
			    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    String strDate = sdfDate.format(startTime);
				System.out.format("\tNow starting positive fraction %f at %s.\n", positiveFraction, strDate);

				if (positiveFraction == 0.0)
				{
					// If the fraction of the minority class to include is 0, then set the minority class to be the same fraction as it is in the whole dataset.
					positiveFraction = ((double) numberPosObs) / numberOfObservations;
				}
				int positiveObservationsToUse = (int) Math.floor(positiveFraction * datasetSize);
				int unlabelledObservationsToUse = datasetSize - positiveObservationsToUse;

				// Setup the sample size constraints.
				ctrl.sampSize.put(minorityClass, positiveObservationsToUse);
				ctrl.sampSize.put(majorityClass, unlabelledObservationsToUse);

				for (Double posWeight : weightsToUse)
				{
					startTime = new Date();
				    sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				    strDate = sdfDate.format(startTime);
					System.out.format("\t\tNow starting positive weight %f at %s.\n", posWeight, strDate);

					weights.put("Positive", posWeight);
	
					// Perform the analysis for the entire dataset.
					MultipleForestRunAndTest.forestTraining( weights, ctrl, cvFoldLocation, inputFile, seeds, repetitions, cvFoldsToUse, fullDatasetResultsLocation, fullDatasetGMeanResultsLocation, 2, positiveFraction);
				}
			}
		}
	}

}
