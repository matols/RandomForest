package analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import datasetgeneration.CrossValidationFoldGenerationMultiClass;

import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class NodeSizeTesting
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String inputFile = args[0];
		String resultsDir = args[1];
		List<String> covarsToKeep = new ArrayList<String>();
		if (args.length == 3)
		{
			String keepFile = args[2];
			try
			{
				String line;
				BufferedReader featureSubSetReader = new BufferedReader(new FileReader(keepFile));
				while ((line = featureSubSetReader.readLine()) != null)
				{
					line = line.trim();
					covarsToKeep.add(line);
				}
				featureSubSetReader.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
		main(inputFile, resultsDir, covarsToKeep);
	}

	public static void main(String inputFile, String resultsDir)
	{
		main(inputFile, resultsDir, new ArrayList<String>());
	}

	public static void main(String inputFile, String resultsDir, List<String> covarsToKeep)
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
		Integer[] nodeSizeValues = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
		Double[] weightsToUse = {0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0};
		Integer[] trainingObsToUse = {};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 1000;
		ctrl.mtry = 10;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputFile, ctrl);
		Set<String> classesInDataset = new HashSet<String>(procData.responseData);

		// Setup the results output files.
		String fullDatasetResultsLocation = resultsDir + "/FullDatasetResults.txt";
		String fullDatasetGMeanResultsLocation = resultsDir + "/FullDatasetAllValues.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(fullDatasetResultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("NodeSize\tWeight\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError");
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
			resultsOutputWriter.write("NodeSize\tWeight");
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

		// Determine the subset of feature to remove.
		boolean isSubsetUsed = false;
		String subsetResultsLocation = resultsDir + "/SubsetResults.txt";
		String subsetGMeanResultsLocation = resultsDir + "/SubsetAllValues.txt";
		List<String> covarsToRemove = new ArrayList<String>();
		if (!covarsToKeep.isEmpty())
		{
			// If covarsToKeep is empty, then this won't be needed.
			isSubsetUsed = true;
			for (String s : procData.covariableData.keySet())
			{
				if (!covarsToKeep.contains(s))
				{
					covarsToRemove.add(s);
				}
			}
			// Setup the subset results output files.
			try
			{
				FileWriter resultsOutputFile = new FileWriter(subsetResultsLocation);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write("NodeSize\tWeight\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError");
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
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			try
			{
				FileWriter resultsOutputFile = new FileWriter(subsetGMeanResultsLocation);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write("NodeSize\tWeight");
				resultsOutputWriter.newLine();
				resultsOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
		TreeGrowthControl subsetCtrl = new TreeGrowthControl();
		subsetCtrl.isReplacementUsed = ctrl.isReplacementUsed;
		subsetCtrl.numberOfTreesToGrow = ctrl.numberOfTreesToGrow;
		subsetCtrl.mtry = ctrl.mtry;
		subsetCtrl.isStratifiedBootstrapUsed = ctrl.isStratifiedBootstrapUsed;
		subsetCtrl.variablesToIgnore = covarsToRemove;

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
			parameterOutputWriter.write("CV folds used - " + Integer.toString(cvFoldsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used - " + weights.toString());
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Mtry used - " + Integer.toString(ctrl.mtry));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Sizes of datasets used - " + Arrays.toString(nodeSizeValues));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		Map<String, Map<String, Double>> confusionMatrix;

		// Generate the subsets.
		for (Integer nodeSize : nodeSizeValues)
		{
			Date startTime = new Date();
		    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String strDate = sdfDate.format(startTime);
			System.out.format("Now working on node size %d at %s.\n", nodeSize, strDate);

			// Setup the sample size constraints.
			ctrl.minNodeSize = nodeSize;
			subsetCtrl.minNodeSize = nodeSize;

			for (Double posWeight : weightsToUse)
			{
				startTime = new Date();
			    sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    strDate = sdfDate.format(startTime);
				System.out.format("\tNow starting positive weight %f at %s.\n", posWeight, strDate);

				weights.put("Positive", posWeight);
				
				// Perform the analysis for the entire dataset.
				confusionMatrix = new HashMap<String, Map<String, Double>>();
				for (String s : new HashSet<String>(procData.responseData))
				{
					confusionMatrix.put(s, new HashMap<String, Double>());
					confusionMatrix.get(s).put("TruePositive", 0.0);
					confusionMatrix.get(s).put("FalsePositive", 0.0);
				}
				MultipleForestRunAndTest.forestTraining(confusionMatrix, weights, ctrl, cvFoldLocation, inputFile, seeds, repetitions, cvFoldsToUse, fullDatasetResultsLocation, fullDatasetGMeanResultsLocation, 3);
	
				if (isSubsetUsed)
				{
					// Perform the analysis for the chosen subset.
					confusionMatrix = new HashMap<String, Map<String, Double>>();
					for (String s : new HashSet<String>(procData.responseData))
					{
						confusionMatrix.put(s, new HashMap<String, Double>());
						confusionMatrix.get(s).put("TruePositive", 0.0);
						confusionMatrix.get(s).put("FalsePositive", 0.0);
					}
					MultipleForestRunAndTest.forestTraining(confusionMatrix, weights, subsetCtrl, cvFoldLocation, inputFile, seeds, repetitions, cvFoldsToUse, subsetResultsLocation, subsetGMeanResultsLocation, 3);
				}
			}
		}
	}

}
