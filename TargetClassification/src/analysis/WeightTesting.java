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

public class WeightTesting
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String inputFile = args[0];
		String resultsDir = args[1];
		String testFileLocation = null;
		if (args.length == 3)
		{
			testFileLocation = args[2];
		}
		main(inputFile, resultsDir, testFileLocation);
	}

	public static void main(String inputFile, String resultsDir, String testFileLocation)
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
		int repetitions = 100;
		int cvFoldsToUse = 10;
		Double[] weightsToUse = {1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9,
				2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.0};
		Integer[] mtryToUse = {5, 10, 15, 20, 25, 30};
		Integer[] trainingObsToUse = {};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 1500;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.isCalculateOOB = true;  // Set this to false to use cross-validation, and true to use OOB observations.
		ctrl.minNodeSize = 1;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);

		boolean isGMeanAveraged = false;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputFile, ctrl);
		List<String> classesInDataset = new ArrayList<String>(new HashSet<String>(procData.responseData));
		Collections.sort(classesInDataset);

		// Setup the results output files.
		String fullDatasetResultsLocation = resultsDir + "/FullDatasetResults.txt";
		String fullDatasetGMeanResultsLocation = resultsDir + "/FullDatasetAllValues.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(fullDatasetResultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Weight\tMtry\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError");
			for (String s : classesInDataset)
			{
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(s);
				resultsOutputWriter.write("\t");
			}
			resultsOutputWriter.write("\tTimeTaken(ms)");
			resultsOutputWriter.newLine();
			resultsOutputWriter.write("\t\t\t\t\t\t\t\t");
			for (String s : classesInDataset)
			{
				resultsOutputWriter.write("\tTrue\tFalse");
			}
			resultsOutputWriter.write("\t");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();

			resultsOutputFile = new FileWriter(fullDatasetGMeanResultsLocation);
			resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Weight\tMtry");
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
			parameterOutputWriter.write("Weights used - " + Arrays.toString(weightsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Mtry used - " + Arrays.toString(mtryToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Training observations used - " + Arrays.toString(trainingObsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		for (Integer mtry : mtryToUse)
		{
			ctrl.mtry = mtry;

			System.out.format("Now working on mtry - %d.\n", mtry);

			// Generate the results for this weighting.
			for (Double posWeight : weightsToUse)
			{
				weights.put("Positive", posWeight);

				DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    Date startTime = new Date();
			    String strDate = sdfDate.format(startTime);
				System.out.format("\tNow starting weight - %f at %s.\n", posWeight, strDate);

				// Perform the analysis for the entire dataset.
				if (testFileLocation != null)
				{
					MultipleForestRunAndTest.forestTraining(weights, ctrl, cvFoldLocation, inputFile, seeds, repetitions, cvFoldsToUse, fullDatasetResultsLocation, fullDatasetGMeanResultsLocation, 1, testFileLocation, isGMeanAveraged);
				}
				else
				{
					MultipleForestRunAndTest.forestTraining(weights, ctrl, cvFoldLocation, inputFile, seeds, repetitions, cvFoldsToUse, fullDatasetResultsLocation, fullDatasetGMeanResultsLocation, 1);
				}
			}
		}
	}

}
