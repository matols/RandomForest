package analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.Forest;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class MultipleForestRunAndTest
{
	/**
	 * @param confusionMatrix
	 * @param weights
	 * @param ctrl
	 * @param inputFile
	 * @param seeds
	 * @param repetitions
	 * @param negClass
	 * @param posClass
	 * @param resultsLocation
	 * @param mccResultsLocation
	 * @param analysisBeingRun - 1 for weight and mtry, 2 for sample size, 3 for node size, 4 for up or down sampling and 5 for tree depth.
	 */
	static List<Double> forestTraining(
			Map<String, Double> weights, TreeGrowthControl ctrl, String cvFoldLocation, String inputFile, List<Long> seeds, int repetitions, int cvFolds,
			String resultsLocation, String mccResultsLocation, int analysisBeingRun)
	{
		return forestTraining(weights, ctrl, cvFoldLocation, inputFile, seeds, repetitions, cvFolds, resultsLocation, mccResultsLocation,
				analysisBeingRun, 0.0);
	}

	static List<Double> forestTraining(
			Map<String, Double> weights, TreeGrowthControl ctrl, String cvFoldLocation, String inputFile, List<Long> seeds, int repetitions, int cvFolds,
			String resultsLocation, String mccResultsLocation, int analysisBeingRun, double callSpecificValue)
	{
		List<Double> allRepetitionResults = new ArrayList<Double>();
		long timeTaken = 0l;
		ProcessDataForGrowing processedInputFile = new ProcessDataForGrowing(inputFile, ctrl);
		Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
		Map<String, Integer> countsOfClass = new HashMap<String, Integer>();
		for (String s : weights.keySet())
		{
			countsOfClass.put(s, Collections.frequency(processedInputFile.responseData, s));
			confusionMatrix.put(s, new HashMap<String, Double>());
			confusionMatrix.get(s).put("TruePositive", 0.0);
			confusionMatrix.get(s).put("FalsePositive", 0.0);
		}

		Date startTime;
		Date endTime;
		
		for (int i = 0; i < repetitions; i++)
		{
			// Get the seed for this repetition.
			long seed = seeds.get(i);
			String currentCVFoldLocation = cvFoldLocation + Integer.toString(i);

			startTime = new Date();
			Forest forest;
			if (!ctrl.isCalculateOOB)
			{
				for (int j = 0; j < cvFolds; j++)
				{
					String trainingSet = currentCVFoldLocation + "/" + Integer.toString(j) + "/Train.txt";
					String testingSet = currentCVFoldLocation + "/" + Integer.toString(j) + "/Test.txt";
					forest = new Forest(trainingSet, ctrl, seed);
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
				forest = new Forest(processedInputFile, ctrl, seed);
				forest.setWeightsByClass(weights);
				forest.growForest();
				for (String s : forest.oobConfusionMatrix.keySet())
	    		{
	    			Double oldTruePos = confusionMatrix.get(s).get("TruePositive");
	    			Double newTruePos = oldTruePos + forest.oobConfusionMatrix.get(s).get("TruePositive");
	    			confusionMatrix.get(s).put("TruePositive", newTruePos);
	    			Double oldFalsePos = confusionMatrix.get(s).get("FalsePositive");
	    			Double newFalsePos = oldFalsePos + forest.oobConfusionMatrix.get(s).get("FalsePositive");
	    			confusionMatrix.get(s).put("FalsePositive", newFalsePos);
	    		}
			}
			endTime = new Date();
    		timeTaken += endTime.getTime() - startTime.getTime();
		}
		timeTaken /= (double) repetitions;

		// Aggregate prediction results over all the repetitions.
		double totalPredictions = 0.0;
		double incorrectPredictions = 0.0;
		double macroRecall = 0.0;
		double macroPrecision = 0.0;
		double macroGMean = 1.0;
		double MCC = 0.0;
		for (String s : confusionMatrix.keySet())
		{
			double TP = confusionMatrix.get(s).get("TruePositive");
			double FP = confusionMatrix.get(s).get("FalsePositive");
			// Use (repetitions * countsOfClass.get(s)) for the number of observations from a class as each observation is predicted once per repetition,
			// so is predicted a total of repetitions times.
    		double FN = (repetitions * countsOfClass.get(s)) - TP;  // The number of false negatives is the number of observations from the class  - the number of true positives.
    		double recall = (TP / (TP + FN));
    		macroRecall += recall;
    		double precision = (TP / (TP + FP));
    		macroPrecision += precision;
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
		macroRecall /= confusionMatrix.size();
		macroPrecision /= confusionMatrix.size();
		double fHalf = (1 + (0.5 * 0.5)) * ((macroPrecision * macroRecall) / ((0.5 * 0.5 * macroPrecision) + macroRecall));
		double fOne = 2 * ((macroPrecision * macroRecall) / (macroPrecision + macroRecall));
		double fTwo = (1 + (2 * 2)) * ((macroPrecision * macroRecall) / ((2 * 2 * macroPrecision) + macroRecall));
		double gMean = Math.pow(macroGMean, (1.0 / confusionMatrix.size()));
		double errorRate = incorrectPredictions / totalPredictions;

		// Write out the prediction results for this set of repetitions.
		try
		{
			FileWriter resultsOutputFile = new FileWriter(resultsLocation, true);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			if (analysisBeingRun == 1)
			{
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Integer.toString(ctrl.mtry));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 2)
			{
				int sampleSize = 0;
				for (String s : ctrl.sampSize.keySet())
				{
					sampleSize += ctrl.sampSize.get(s);
				}
				resultsOutputWriter.write(Integer.toString(sampleSize));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", callSpecificValue));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 3)
			{
				resultsOutputWriter.write(Integer.toString(ctrl.minNodeSize));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 4)
			{
				resultsOutputWriter.write(Integer.toString(ctrl.sampSize.get("Positive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Integer.toString(ctrl.sampSize.get("Unlabelled")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 5)
			{
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Integer.toString(ctrl.maxTreeDepth));
				resultsOutputWriter.write("\t");
			}
			resultsOutputWriter.write(String.format("%.5f", gMean));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", MCC));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fHalf));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fOne));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fTwo));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", 1 - errorRate));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", errorRate));
			List<String> classesUsed = new ArrayList<String>(confusionMatrix.keySet());
			Collections.sort(classesUsed);
			for (String s : classesUsed)
			{
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", confusionMatrix.get(s).get("TruePositive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", confusionMatrix.get(s).get("FalsePositive")));
			}
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(Long.toString(timeTaken));
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Write out the G mean results for this set of repetitions.
		try
		{
			FileWriter resultsOutputFile = new FileWriter(mccResultsLocation, true);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			if (analysisBeingRun == 1)
			{
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Integer.toString(ctrl.mtry));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 2)
			{
				int sampleSize = 0;
				for (String s : ctrl.sampSize.keySet())
				{
					sampleSize += ctrl.sampSize.get(s);
				}
				resultsOutputWriter.write(Integer.toString(sampleSize));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", callSpecificValue));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 3)
			{
				resultsOutputWriter.write(Integer.toString(ctrl.minNodeSize));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 4)
			{
				resultsOutputWriter.write(Integer.toString(ctrl.sampSize.get("Positive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Integer.toString(ctrl.sampSize.get("Unlabelled")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
			}
			else if (analysisBeingRun == 5)
			{
				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Integer.toString(ctrl.maxTreeDepth));
				resultsOutputWriter.write("\t");
			}
			for (Double d : allRepetitionResults)
			{
				resultsOutputWriter.write(String.format("%.5f", d));
				resultsOutputWriter.write("\t");
			}
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		return allRepetitionResults;
	}
}
