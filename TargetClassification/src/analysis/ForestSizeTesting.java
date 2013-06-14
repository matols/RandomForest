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

import tree.Forest;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class ForestSizeTesting
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Parse the inputs.
		String inputFile = args[0];
		String resultsDir = args[1];
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
		Integer[] forestSizesToUse = {50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900,
				950, 1000, 1050, 1100, 1150, 1200, 1250, 1300, 1350, 1400, 1450, 1500, 1550, 1600, 1650, 1700, 1750, 1800, 1850,
				1900, 1950, 2000, 2050, 2100, 2150, 2200, 2250, 2300, 2350, 2400, 2450, 2500, 2550, 2600, 2650, 2700, 2750, 2800,
				2850, 2900, 2950, 3000, 3050, 3100, 3150, 3200, 3250, 3300, 3350, 3400, 3450, 3500, 3550, 3600, 3650, 3700, 3750,
				3800, 3850, 3900, 3950, 4000, 4050, 4100, 4150, 4200, 4250, 4300, 4350, 4400, 4450, 4500, 4550, 4600, 4650, 4700,
				4750, 4800, 4850, 4900, 4950, 5000};
		Integer[] trainingObsToUse = {};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.isCalculateOOB = true;  // Set this to false to use cross-validation, and true to use OOB observations.
		ctrl.minNodeSize = 1;
		ctrl.mtry = 10;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Write out the parameters used.
		String parameterLocation = resultsDir + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Forest sizes used - " + Arrays.toString(forestSizesToUse));
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
			parameterOutputWriter.write("Weights used - " + weights.toString());
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

		// Determine the numbers in each class.
		ProcessDataForGrowing processedInputFile = new ProcessDataForGrowing(inputFile, ctrl);
		Map<String, Integer> countsOfClass = new HashMap<String, Integer>();
		for (String s : weights.keySet())
		{
			countsOfClass.put(s, Collections.frequency(processedInputFile.responseData, s));
		}

		// Generate the seeds to use,
		List<Long> seedsToUse = new ArrayList<Long>();
		Random seedGenerator = new Random();
		for (int i = 0; i < repetitions; i++)
		{
			long seedToUse = seedGenerator.nextLong();
			while (seedsToUse.contains(seedToUse))
			{
				seedToUse = seedGenerator.nextLong();
			}
			seedsToUse.add(seedToUse);
		}
		String seedsLocation = resultsDir + "/SeedsUsed.txt";
		try
		{
			FileWriter seedOutputFile = new FileWriter(seedsLocation);
			BufferedWriter seedOutputWriter = new BufferedWriter(seedOutputFile);
			for (Long l : seedsToUse)
			{
				seedOutputWriter.write(Long.toString(l));
				seedOutputWriter.newLine();
			}
			seedOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
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

		String errorRateResultsLocation = resultsDir + "/ErrorResults.txt";
		String gMeanResultsLocation = resultsDir + "/GMeanResults.txt";
		String mccResultsLocation = resultsDir + "/MCCResults.txt";
		for (Integer i : forestSizesToUse)
		{
			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date startTime = new Date();
		    String strDate = sdfDate.format(startTime);
			System.out.format("Now testing forests of size %d at %s.\n", i, strDate);

			ctrl.numberOfTreesToGrow = i;
			List<Double> errorRates = new ArrayList<Double>();
			List<Double> gMeanResults = new ArrayList<Double>();
			List<Double> mccResults = new ArrayList<Double>();
			for (int j = 0; j < repetitions; j++)
			{
				Forest forest;
				Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
				for (String s : new HashSet<String>(processedInputFile.responseData))
				{
					confusionMatrix.put(s, new HashMap<String, Double>());
					confusionMatrix.get(s).put("TruePositive", 0.0);
					confusionMatrix.get(s).put("FalsePositive", 0.0);
				}

				if (!ctrl.isCalculateOOB)
				{
					// If the OOB stability is not being used.
					String currentCVFoldLocation = cvFoldLocation + Integer.toString(j);
					for (int k = 0; k < cvFoldsToUse; k++)
					{
						String trainingSet = currentCVFoldLocation + "/" + Integer.toString(k) + "/Train.txt";
						String testingSet = currentCVFoldLocation + "/" + Integer.toString(k) + "/Test.txt";
						forest = new Forest(trainingSet, ctrl, seedsToUse.get(j));
						forest.setWeightsByClass(weights);
						forest.growForest();
						Map<String, Map<String, Double>> confMatrix = forest.predict(new ProcessDataForGrowing(testingSet, new TreeGrowthControl())).second;
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
				else
				{
					// If the OOB stability is being determined.
					forest = new Forest(inputFile, ctrl, seedsToUse.get(j));
					forest.setWeightsByClass(weights);
					forest.growForest();
					confusionMatrix = forest.oobConfusionMatrix;
				}

				// Aggregate prediction results over all the repetitions.
				double totalPredictions = 0.0;
				double incorrectPredictions = 0.0;
				double macroGMean = 1.0;
				double MCC = 0.0;
				for (String s : confusionMatrix.keySet())
				{
					double TP = confusionMatrix.get(s).get("TruePositive");
					double FP = confusionMatrix.get(s).get("FalsePositive");
		    		double FN = countsOfClass.get(s) - TP;  // The number of false negatives is the number of observations from the class  - the number of true positives.
		    		double recall = (TP / (TP + FN));
		    		macroGMean *= recall;
		    		totalPredictions += TP + FP;
		    		incorrectPredictions += FP;
				}
				if (confusionMatrix.size() == 2)
				{
					// If there are only two classes, then calculate the MCC.
					List<Double> correctPredictionsMCC = new ArrayList<Double>();
					List<Double> incorrectPredictionsMCC = new ArrayList<Double>();
					for (String s : confusionMatrix.keySet())
					{
						correctPredictionsMCC.add(confusionMatrix.get(s).get("TruePositive"));
						incorrectPredictionsMCC.add(confusionMatrix.get(s).get("FalsePositive"));
					}
					double TP = correctPredictionsMCC.get(0);
					double FP = incorrectPredictionsMCC.get(0);
					double TN = correctPredictionsMCC.get(1);
					double FN = incorrectPredictionsMCC.get(1);
					MCC = ((TP * TN) - (FP * FN)) / (Math.sqrt((TP + TN) * (TP + FN) * (TN + FP) * (TN + FN)));
				}
				mccResults.add(MCC);
				gMeanResults.add(Math.pow(macroGMean, (1.0 / confusionMatrix.size())));
				errorRates.add(incorrectPredictions / totalPredictions);
			}

			try
			{
				FileWriter resultsOutputFile = new FileWriter(errorRateResultsLocation, true);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write(Integer.toString(i));
				for (Double d : errorRates)
				{
					resultsOutputWriter.write("\t" + Double.toString(d));
				}
				resultsOutputWriter.newLine();
				resultsOutputWriter.close();

				resultsOutputFile = new FileWriter(gMeanResultsLocation, true);
				resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write(Integer.toString(i));
				for (Double d : gMeanResults)
				{
					resultsOutputWriter.write("\t" + Double.toString(d));
				}
				resultsOutputWriter.newLine();
				resultsOutputWriter.close();

				resultsOutputFile = new FileWriter(mccResultsLocation, true);
				resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write(Integer.toString(i));
				for (Double d : mccResults)
				{
					resultsOutputWriter.write("\t" + Double.toString(d));
				}
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