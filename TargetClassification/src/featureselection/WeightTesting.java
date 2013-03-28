package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import datasetgeneration.CrossValidationFoldGeneration;

import tree.Forest;
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
		String cvDir = resultsDir + "/CV";
		File cvDirectory = new File(cvDir);
		if (!cvDirectory.exists())
		{
			boolean isDirCreated = cvDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The cross validation directory could not be created.");
				System.exit(0);
			}
		}
		else if (!cvDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("ERROR: The output directory contains a non-directory called CV.");
			System.exit(0);
		}

		// Setup the results output file.
		String resultsLocation = resultsDir + "/Results.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(resultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tPredictiveError\tCVOOBError\tSingleTreeOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 10;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);

		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputFile, ctrl);
		String negClass = "Unlabelled";
		String posClass = "Positive";

		int repetitions = 5;
		int crossValFolds = 2;
		Double[] weightsToUse = {1.0, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 2.0, 2.2, 2.4, 2.6, 2.8, 3.0};
		Integer[] mtryToUse = {5, 10, 15, 20, 25, 30, 35, 40};

		// Generate the seeds for the repetitions, and the CV folds for each repetition.
		Random randGen = new Random();
		List<Long> seeds = new ArrayList<Long>();
		List<List<List<Object>>> crossValData = new ArrayList<List<List<Object>>>();
		for (int i = 0; i < repetitions; i++)
		{
			seeds.add(randGen.nextLong());

			String currentCVDir = cvDir + "\\" + Integer.toString(i);
			CrossValidationFoldGeneration.main(inputFile, currentCVDir, crossValFolds);
	
			// Get the cross validation information
			File crossValDir = new File(currentCVDir);
			String subDirs[] = crossValDir.list();
			List<List<Object>> subsetFeaturCrossValFiles = new ArrayList<List<Object>>();
			for (String s : subDirs)
			{
				List<Object> subsetFeatureTrainTestLocs = new ArrayList<Object>();
				subsetFeatureTrainTestLocs.add(currentCVDir + "/" + s + "/Train.txt");
				subsetFeatureTrainTestLocs.add(new ProcessDataForGrowing(currentCVDir + "/" + s + "/Test.txt", ctrl));
				subsetFeaturCrossValFiles.add(subsetFeatureTrainTestLocs);
			}
			crossValData.add(subsetFeaturCrossValFiles);
		}

		// Determine the subset of feature to remove.
		boolean isSubsetUsed = false;
		String subsetResultsLocation = resultsDir + "/SubsetResults.txt";
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
			// Setup the subset results output file.
			try
			{
				FileWriter subsetResultsOutputFile = new FileWriter(subsetResultsLocation);
				BufferedWriter subsetResultsOutputWriter = new BufferedWriter(subsetResultsOutputFile);
				subsetResultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tPredictiveError\tCVOOBError\tSingleTreeOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN");
				subsetResultsOutputWriter.newLine();
				subsetResultsOutputWriter.close();
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
			parameterOutputWriter.write("Cross val folds used - " + Integer.toString(crossValFolds));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used - " + Arrays.toString(weightsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Mtry used - " + Arrays.toString(mtryToUse));
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
			Map<String, Map<String, Double>> confusionMatrix;
			ctrl.mtry = mtry;
			subsetCtrl.mtry = mtry;

			// Generate the results for this weighting.
			for (Double posWeight : weightsToUse)
			{
				weights.put("Positive", posWeight);

				// Setup the confusion matrix.
				confusionMatrix = new HashMap<String, Map<String, Double>>();
				for (String s : new HashSet<String>(procData.responseData))
				{
					confusionMatrix.put(s, new HashMap<String, Double>());
					confusionMatrix.get(s).put("TruePositive", 0.0);
					confusionMatrix.get(s).put("FalsePositive", 0.0);
				}
				forestTraining(crossValData, confusionMatrix, weights, ctrl, inputFile, seeds, repetitions, crossValFolds, negClass, posClass, resultsLocation);
				if (isSubsetUsed)
				{
					// Setup the confusion matrix.
					confusionMatrix = new HashMap<String, Map<String, Double>>();
					for (String s : new HashSet<String>(procData.responseData))
					{
						confusionMatrix.put(s, new HashMap<String, Double>());
						confusionMatrix.get(s).put("TruePositive", 0.0);
						confusionMatrix.get(s).put("FalsePositive", 0.0);
					}
					forestTraining(crossValData, confusionMatrix, weights, subsetCtrl, inputFile, seeds, repetitions, crossValFolds, negClass, posClass, subsetResultsLocation);
				}
			}
		}
	}

	static void forestTraining(List<List<List<Object>>> crossValData, Map<String, Map<String, Double>> confusionMatrix, Map<String, Double> weights,
			TreeGrowthControl ctrl, String inputFile, List<Long> seeds, int repetitions, int crossValFolds, String negClass,
			String posClass, String resultsLocation)
	{
		double cumulativeError = 0.0;
		double cumulativeCVOOBError = 0.0;
		double cumulativeOOBError = 0.0;

		for (int j = 0; j < repetitions; j++)
		{
			// Get the seed for this repetition.
			long seed = seeds.get(j);

			Forest forest;
			forest = new Forest(inputFile, ctrl, weights, seed);
			cumulativeOOBError += forest.oobErrorEstimate;
			for (List<Object> l : crossValData.get(j))
	    	{
	    		forest = new Forest((String) l.get(0), ctrl, weights, seed);
	    		cumulativeError += forest.predict((ProcessDataForGrowing) l.get(1)).first;
	    		cumulativeCVOOBError += forest.oobErrorEstimate;
	    		Map<String, Map<String, Double>> confMatrix = forest.predict((ProcessDataForGrowing) l.get(1)).second;
	    		for (String s : confMatrix.keySet())
	    		{
	    			Double oldTruePos = confusionMatrix.get(s).get("TruePositive");
	    			Double newTruePos = oldTruePos + confMatrix.get(s).get("TruePositive");
	    			confusionMatrix.get(s).put("TruePositive", newTruePos);
	    			Double oldFalsePos = confusionMatrix.get(s).get("FalsePositive");
	    			Double newFalsePos = oldFalsePos + confMatrix.get(s).get("FalsePositive");
	    			confusionMatrix.get(s).put("FalsePositive", newFalsePos);
	    		}
	    	}
		}
		// Aggregate results over all the repetitions.
		cumulativeError /= (crossValFolds * repetitions);
		cumulativeCVOOBError /= (crossValFolds * repetitions);
		cumulativeOOBError /= repetitions;
		Double TP = confusionMatrix.get(posClass).get("TruePositive");
		Double FP = confusionMatrix.get(posClass).get("FalsePositive");
		Double TN = confusionMatrix.get(negClass).get("TruePositive");
		Double FN = confusionMatrix.get(negClass).get("FalsePositive");
		Double sensitivityOrRecall = TP / (TP + FN);
		Double specificity = TN / (TN + FP);
		Double accuracy = (TP + TN) / (TP + TN + FP + FN);
		Double precisionOrPPV = TP / (TP + FP);
		Double negPredictiveVal = TN / (TN + FN);
		Double fHalf = (1 + (0.5 * 0.5)) * ((precisionOrPPV * sensitivityOrRecall) / ((0.5 * 0.5 * precisionOrPPV) + sensitivityOrRecall));;
		Double fOne = 2 * ((precisionOrPPV * sensitivityOrRecall) / (precisionOrPPV + sensitivityOrRecall));
		Double fTwo = (1 + (2 * 2)) * ((precisionOrPPV * sensitivityOrRecall) / ((2 * 2 * precisionOrPPV) + sensitivityOrRecall));
		Double MCC = (((TP * TN)  - (FP * FN)) / Math.sqrt((TP + FP) * (TP + FN) * (TN + FP) * (TN + FN)));

		// Write out the results for this weighting.
		try
		{
			FileWriter resultsOutputFile = new FileWriter(resultsLocation, true);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(Integer.toString(ctrl.mtry));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", MCC));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fHalf));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fOne));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fTwo));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", accuracy));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", cumulativeError));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", cumulativeCVOOBError));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", cumulativeOOBError));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", precisionOrPPV));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", sensitivityOrRecall));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", specificity));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", negPredictiveVal));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", TP));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", FP));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", TN));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", FN));
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
