package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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
import java.util.Set;

import tree.Forest;
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

		// Setup the results output file.
		String cvResultsLocation = resultsDir + "/CVResults.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(cvResultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tPredictiveError\tOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN\tTimeTaken");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		String oobResultsLocation = resultsDir + "/OOBResults.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(oobResultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN\tTimeTaken");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int repetitions = 50;
		int crossValFolds = 10;
		Integer[] sizeOfDatasets = {};
		Double[] fractionOfPositives = {};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 500;
		ctrl.mtry = 10;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Positive", 1.0);
		weights.put("Unlabelled", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Determine the observations of each class.
		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputFile, ctrl);
		String negClass = "Unlabelled";
		String posClass = "Positive";
		Set<String> responseClasses = new HashSet<String>(procData.responseData);
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
		int numberPosObs = responseSplits.get(posClass).size();
		int numberUnlabObs = responseSplits.get(negClass).size();

		// Generate the seeds for the repetitions, and the CV folds for each repetition.
		Random randGen = new Random();
		List<Long> seeds = new ArrayList<Long>();
		List<List<List<String>>> crossValData = new ArrayList<List<List<String>>>();
		for (int i = 0; i < repetitions; i++)
		{
			long seedToUse = randGen.nextLong();
			while (seeds.contains(seedToUse))
			{
				seedToUse = randGen.nextLong();
			}
			seeds.add(seedToUse);

//			String currentCVDir = cvDir + "\\" + Integer.toString(i);
//			CrossValidationFoldGeneration.main(inputFile, currentCVDir, crossValFolds);
//
//			// Get the cross validation information
//			File crossValDir = new File(currentCVDir);
//			String subDirs[] = crossValDir.list();
//			List<List<String>> subsetFeaturCrossValFiles = new ArrayList<List<String>>();
//			for (String s : subDirs)
//			{
//				List<String> subsetFeatureTrainTestLocs = new ArrayList<String>();
//				subsetFeatureTrainTestLocs.add(currentCVDir + "/" + s + "/Train.txt");
//				subsetFeatureTrainTestLocs.add(currentCVDir + "/" + s + "/Test.txt");
//				subsetFeaturCrossValFiles.add(subsetFeatureTrainTestLocs);
//			}
//			crossValData.add(subsetFeaturCrossValFiles);
		}

		// Determine the subset of feature to remove.
		boolean isSubsetUsed = false;
		String subsetCVResultsLocation = resultsDir + "/SubsetCVResults.txt";
		String subsetOOBResultsLocation = resultsDir + "/SubsetOOBResults.txt";
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
				FileWriter resultsOutputFile = new FileWriter(subsetCVResultsLocation);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tPredictiveError\tOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN\tTimeTaken");
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
				FileWriter resultsOutputFile = new FileWriter(subsetOOBResultsLocation);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN\tTimeTaken");
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
			parameterOutputWriter.write("Weights used - " + weights.toString());
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Mtry used - " + Integer.toString(ctrl.mtry));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Sizes of datasets used - " + Arrays.toString(sizeOfDatasets));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Fractions of dataset that is positive observations used - " + Arrays.toString(fractionOfPositives));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		Map<String, Map<String, Double>> confusionMatrix;
		Map<String, Map<String, Double>> oobConfusionMatrix;

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

				if (positiveFraction == 0)
				{
					// If the fraction of the minority class to include is 0, then set the minority class to be the same fraction as it is in the whole dataset.
					positiveFraction = ((double) numberPosObs) / numberOfObservations;
				}
				int positiveObservationsToUse = (int) Math.floor(positiveFraction * datasetSize);
				positiveObservationsToUse = Math.min(positiveObservationsToUse, numberPosObs);  // Can't have more positive observations than there are.
				int unlabelledObservationsToUse = datasetSize - positiveObservationsToUse;

				// Setup the sample size constraints.
				ctrl.sampSize.put(posClass, positiveObservationsToUse);
				ctrl.sampSize.put(negClass, unlabelledObservationsToUse);
				subsetCtrl.sampSize.put(posClass, positiveObservationsToUse);
				subsetCtrl.sampSize.put(negClass, unlabelledObservationsToUse);

				// Setup the confusion matrices.
				confusionMatrix = new HashMap<String, Map<String, Double>>();
				oobConfusionMatrix = new HashMap<String, Map<String, Double>>();
				for (String s : new HashSet<String>(procData.responseData))
				{
					confusionMatrix.put(s, new HashMap<String, Double>());
					confusionMatrix.get(s).put("TruePositive", 0.0);
					confusionMatrix.get(s).put("FalsePositive", 0.0);
					oobConfusionMatrix.put(s, new HashMap<String, Double>());
					oobConfusionMatrix.get(s).put("TruePositive", 0.0);
					oobConfusionMatrix.get(s).put("FalsePositive", 0.0);
				}
				forestTraining(crossValData, oobConfusionMatrix, confusionMatrix, weights, ctrl, inputFile, seeds, repetitions, crossValFolds, negClass, posClass, cvResultsLocation, oobResultsLocation);

				if (isSubsetUsed)
				{
					// Setup the confusion matrices.
					confusionMatrix = new HashMap<String, Map<String, Double>>();
					oobConfusionMatrix = new HashMap<String, Map<String, Double>>();
					for (String s : new HashSet<String>(procData.responseData))
					{
						confusionMatrix.put(s, new HashMap<String, Double>());
						confusionMatrix.get(s).put("TruePositive", 0.0);
						confusionMatrix.get(s).put("FalsePositive", 0.0);
						oobConfusionMatrix.put(s, new HashMap<String, Double>());
						oobConfusionMatrix.get(s).put("TruePositive", 0.0);
						oobConfusionMatrix.get(s).put("FalsePositive", 0.0);
					}
					forestTraining(crossValData, oobConfusionMatrix, confusionMatrix, weights, subsetCtrl, inputFile, seeds, repetitions, crossValFolds, negClass, posClass, subsetCVResultsLocation, subsetOOBResultsLocation);
				}
			}
		}
	}

	static void forestTraining(List<List<List<String>>> crossValData, Map<String, Map<String, Double>> oobConfusionMatrix,
			Map<String, Map<String, Double>> confusionMatrix, Map<String, Double> weights,
			TreeGrowthControl ctrl, String inputFile, List<Long> seeds, int repetitions, int crossValFolds, String negClass,
			String posClass, String cvResultsLocation, String oobResultsLocation)
	{
		double cumulativeError = 0.0;
		double cumulativeCVOOBError = 0.0;
		double cumulativeOOBError = 0.0;
		long oobTime = 0l;
		long cvTime = 0l;

		Date startTime;
		Date endTime;
		for (int j = 0; j < repetitions; j++)
		{
			// Get the seed for this repetition.
			long seed = seeds.get(j);

			startTime = new Date();
			Forest forest;
			forest = new Forest(inputFile, ctrl, weights, seed);
			cumulativeOOBError += forest.oobErrorEstimate;
			Map<String, Map<String, Double>> oobConfMatrix = forest.oobConfusionMatrix;
    		for (String s : oobConfMatrix.keySet())
    		{
    			Double oldTruePos = oobConfusionMatrix.get(s).get("TruePositive");
    			Double newTruePos = oldTruePos + oobConfMatrix.get(s).get("TruePositive");
    			oobConfusionMatrix.get(s).put("TruePositive", newTruePos);
    			Double oldFalsePos = oobConfusionMatrix.get(s).get("FalsePositive");
    			Double newFalsePos = oldFalsePos + oobConfMatrix.get(s).get("FalsePositive");
    			oobConfusionMatrix.get(s).put("FalsePositive", newFalsePos);
    		}
    		endTime = new Date();
    		oobTime += endTime.getTime() - startTime.getTime();

//    		startTime = new Date();
//			for (List<String> l : crossValData.get(j))
//	    	{
//	    		forest = new Forest((String) l.get(0), ctrl, weights, seed);
//	    		ProcessDataForGrowing testDataset = new ProcessDataForGrowing(l.get(1), ctrl);
//	    		cumulativeError += forest.predict(testDataset).first;
//	    		cumulativeCVOOBError += forest.oobErrorEstimate;
//	    		Map<String, Map<String, Double>> confMatrix = forest.predict(testDataset).second;
//	    		for (String s : confMatrix.keySet())
//	    		{
//	    			Double oldTruePos = confusionMatrix.get(s).get("TruePositive");
//	    			Double newTruePos = oldTruePos + confMatrix.get(s).get("TruePositive");
//	    			confusionMatrix.get(s).put("TruePositive", newTruePos);
//	    			Double oldFalsePos = confusionMatrix.get(s).get("FalsePositive");
//	    			Double newFalsePos = oldFalsePos + confMatrix.get(s).get("FalsePositive");
//	    			confusionMatrix.get(s).put("FalsePositive", newFalsePos);
//	    		}
//	    	}
//    		endTime = new Date();
//    		cvTime += endTime.getTime() - startTime.getTime();
		}
		oobTime /= (double) repetitions;
		cvTime /= (double) repetitions;

		// Aggregate predicted results over all the repetitions.
		cumulativeError /= (crossValFolds * repetitions);
		cumulativeCVOOBError /= (crossValFolds * repetitions);
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

		// Aggregate OOB results over all the repetitions.
		cumulativeOOBError /= repetitions;
		Double oobTP = oobConfusionMatrix.get(posClass).get("TruePositive");
		Double oobFP = oobConfusionMatrix.get(posClass).get("FalsePositive");
		Double oobTN = oobConfusionMatrix.get(negClass).get("TruePositive");
		Double oobFN = oobConfusionMatrix.get(negClass).get("FalsePositive");
		Double oobSensitivityOrRecall = oobTP / (oobTP + oobFN);
		Double oobSpecificity = oobTN / (oobTN + oobFP);
		Double oobAccuracy = (oobTP + oobTN) / (oobTP + oobTN + oobFP + oobFN);
		Double oobPrecisionOrPPV = oobTP / (oobTP + oobFP);
		Double oobNegPredictiveVal = oobTN / (oobTN + oobFN);
		Double oobFHalf = (1 + (0.5 * 0.5)) * ((oobPrecisionOrPPV * oobSensitivityOrRecall) / ((0.5 * 0.5 * oobPrecisionOrPPV) + oobSensitivityOrRecall));;
		Double oobFOne = 2 * ((oobPrecisionOrPPV * oobSensitivityOrRecall) / (oobPrecisionOrPPV + oobSensitivityOrRecall));
		Double oobFTwo = (1 + (2 * 2)) * ((oobPrecisionOrPPV * oobSensitivityOrRecall) / ((2 * 2 * oobPrecisionOrPPV) + oobSensitivityOrRecall));
		Double oobMCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));

		// Write out the CV results for this weighting.
		try
		{
			FileWriter resultsOutputFile = new FileWriter(cvResultsLocation, true);
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
			resultsOutputWriter.write(Long.toString(cvTime));
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Write out the OOB results for this weighting.
		try
		{
			FileWriter resultsOutputFile = new FileWriter(oobResultsLocation, true);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(Integer.toString(ctrl.mtry));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobMCC));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobFHalf));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobFOne));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobFTwo));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobAccuracy));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", cumulativeOOBError));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobPrecisionOrPPV));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobSensitivityOrRecall));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobSpecificity));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobNegPredictiveVal));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobTP));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobFP));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobTN));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", oobFN));
			resultsOutputWriter.newLine();
			resultsOutputWriter.write(Long.toString(oobTime));
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
