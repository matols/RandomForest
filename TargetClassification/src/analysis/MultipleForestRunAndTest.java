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
	 * @param analysisBeingRun - 1 for weight and mtry, 2 for sample size and 3 for node size.
	 */
	static List<Double> forestTraining(Map<String, Map<String, Double>> confusionMatrix,
			Map<String, Double> weights, TreeGrowthControl ctrl, String inputFile, List<Long> seeds, int repetitions,
			String resultsLocation, String mccResultsLocation, int analysisBeingRun)
	{
		return forestTraining(confusionMatrix, weights, ctrl, inputFile, seeds, repetitions, resultsLocation, mccResultsLocation, analysisBeingRun, 0.0);
	}

	static List<Double> forestTraining(Map<String, Map<String, Double>> confusionMatrix,
			Map<String, Double> weights, TreeGrowthControl ctrl, String inputFile, List<Long> seeds, int repetitions,
			String resultsLocation, String mccResultsLocation, int analysisBeingRun, double callSpecificValue)
	{
		double cumulativeOOBError = 0.0;
		List<Double> gMeanValues = new ArrayList<Double>();
		long oobTime = 0l;
		ProcessDataForGrowing processedInputFile = new ProcessDataForGrowing(inputFile, ctrl);
		Map<String, Integer> countsOfClass = new HashMap<String, Integer>();
		for (String s : weights.keySet())
		{
			countsOfClass.put(s, Collections.frequency(processedInputFile.responseData, s));
		}

		Date startTime;
		Date endTime;
		for (int j = 0; j < repetitions; j++)
		{
			// Get the seed for this repetition.
			long seed = seeds.get(j);

			startTime = new Date();
			Forest forest = new Forest(inputFile, ctrl, seed);
			forest.setWeightsByClass(weights);
			forest.growForest();
			cumulativeOOBError += forest.oobErrorEstimate;
			Map<String, Map<String, Double>> oobConfMatrix = forest.oobConfusionMatrix;
    		for (String s : oobConfMatrix.keySet())
    		{
    			Double oldTruePos = confusionMatrix.get(s).get("TruePositive");
    			Double newTruePos = oldTruePos + oobConfMatrix.get(s).get("TruePositive");
    			confusionMatrix.get(s).put("TruePositive", newTruePos);
    			Double oldFalsePos = confusionMatrix.get(s).get("FalsePositive");
    			Double newFalsePos = oldFalsePos + oobConfMatrix.get(s).get("FalsePositive");
    			confusionMatrix.get(s).put("FalsePositive", newFalsePos);
    		}
    		endTime = new Date();
    		oobTime += endTime.getTime() - startTime.getTime();
    		double macroGMean = 1.0;
    		for (String s : oobConfMatrix.keySet())
	    	{
	    		double TP = oobConfMatrix.get(s).get("TruePositive");
	    		double FN = countsOfClass.get(s) - TP;  // The number of false positives is the number of observations from the class  - the number of true positives.
	    		double recall = TP / (TP + FN);
	    		macroGMean *= recall;
	    	}
	    	macroGMean = Math.pow(macroGMean, (1.0 / oobConfMatrix.size()));
			gMeanValues.add(macroGMean);
		}
		oobTime /= (double) repetitions;

		// Aggregate OOB results over all the repetitions.
		cumulativeOOBError /= repetitions;
		double macroRecall = 0.0;
		double macroPrecision = 0.0;
		double macroGMean = 1.0;
		for (String s : confusionMatrix.keySet())
		{
			double TP = confusionMatrix.get(s).get("TruePositive");
			double FP = confusionMatrix.get(s).get("FalsePositive");
    		double FN = (repetitions * countsOfClass.get(s)) - TP;  // The number of false positives is the number of observations from the class  - the number of true positives.
    		double recall = (TP / (TP + FN));
    		macroRecall += recall;
    		double precision = (TP / (TP + FP));
    		macroPrecision += precision;
    		macroGMean *= recall;
		}
		macroRecall /= confusionMatrix.size();
		macroPrecision /= confusionMatrix.size();
		double fHalf = (1 + (0.5 * 0.5)) * ((macroPrecision * macroRecall) / ((0.5 * 0.5 * macroPrecision) + macroRecall));;
		double fOne = 2 * ((macroPrecision * macroRecall) / (macroPrecision + macroRecall));
		double fTwo = (1 + (2 * 2)) * ((macroPrecision * macroRecall) / ((2 * 2 * macroPrecision) + macroRecall));
		double gMean = Math.pow(macroGMean, (1.0 / confusionMatrix.size()));;

		// Write out the OOB results for this set of repetitions.
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
			resultsOutputWriter.write(String.format("%.5f", fHalf));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fOne));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", fTwo));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", 1 - cumulativeOOBError));
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(String.format("%.5f", cumulativeOOBError));
			for (String s : confusionMatrix.keySet())
			{
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", confusionMatrix.get(s).get("TruePositive")));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(String.format("%.5f", confusionMatrix.get(s).get("FalsePositive")));
			}
			resultsOutputWriter.write("\t");
			resultsOutputWriter.write(Long.toString(oobTime));
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
			for (Double d : gMeanValues)
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

		return gMeanValues;
	}
}
