package comparison;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import tree.Forest;
import tree.TreeGrowthControl;

public class WeightAndMtryTesting
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String inputDir = args[0];
		File inputDirectory = new File(inputDir);
		String resultsLocation = args[1];
		// Setup the directory for the results.
		File resultsDirectory = new File(resultsLocation);
		if (!resultsDirectory.exists())
		{
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.format("The output directory (%s) does not exist, and could not be created.\n", resultsLocation);
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
		Double[] weightsToUse = {8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0};
		Integer[] mtryToUse = {6};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 1000;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.isCalculateOOB = true;
		ctrl.minNodeSize = 1;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Setup the results output files.
		String outputLocation = resultsLocation + "/Results.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(outputLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Weight\tMtry\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError\tPositive\t\tUnlabelled\t\tTimeTakenPerForest(ms)");
			resultsOutputWriter.newLine();
			resultsOutputWriter.write("\t\t\t\t\t\t\t\t");
			for (String s : weights.keySet())
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

		// Generate the seed to use for growing the RFs.
		Random randGen = new Random();
		long seedToUse = randGen.nextLong();

		// Write out the parameters used.
		String parameterLocation = resultsLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Trees grown - " + Integer.toString(ctrl.numberOfTreesToGrow));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Replacement used - " + Boolean.toString(ctrl.isReplacementUsed));
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

		// Determine the datasets in the input directory.
		String[] datasetsToTest = inputDirectory.list();

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

				long timeTaken = 0L;
				double TP = 0.0;
				double FP = 0.0;
				double TN = 0.0;
				double FN = 0.0;

				for (String s : datasetsToTest)
				{
					String currentDataset = inputDir + "/" + s;

					startTime = new Date();
					Forest forest = new Forest(currentDataset, ctrl, seedToUse);
					forest.setWeightsByClass(weights);
					forest.growForest();
					Date endTime = new Date();
					timeTaken += endTime.getTime() - startTime.getTime();
					TP += forest.oobConfusionMatrix.get("Positive").get("TruePositive");
					FP += forest.oobConfusionMatrix.get("Positive").get("FalsePositive");
					TN += forest.oobConfusionMatrix.get("Unlabelled").get("TruePositive");
					FN += forest.oobConfusionMatrix.get("Unlabelled").get("FalsePositive");
				}
				timeTaken /= datasetsToTest.length;
				double MCC = ((TP * TN) - (FP * FN)) / (Math.sqrt((TP + TN) * (TP + FN) * (TN + FP) * (TN + FN)));
				double positiveRecall = (TP / (TP + FN));
				double unlabelledRecall = (TN / (TN + FP));
				double precision = (TP / (TP + FP));
				double fHalf = (1 + (0.5 * 0.5)) * ((precision * positiveRecall) / ((0.5 * 0.5 * precision) + positiveRecall));
				double fOne = 2 * ((precision * positiveRecall) / (precision + positiveRecall));
				double fTwo = (1 + (2 * 2)) * ((precision * positiveRecall) / ((2 * 2 * precision) + positiveRecall));
				double gMean = Math.pow(positiveRecall * unlabelledRecall, 0.5);
				double errorRate = (FP + FN) / (TP + FP + TN + FN);

	    		// Write out the prediction results for this set of repetitions.
	    		try
	    		{
	    			FileWriter resultsOutputFile = new FileWriter(outputLocation, true);
	    			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
    				resultsOutputWriter.write(String.format("%.5f", weights.get("Positive")));
    				resultsOutputWriter.write("\t");
    				resultsOutputWriter.write(Integer.toString(ctrl.mtry));
    				resultsOutputWriter.write("\t");
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
    				resultsOutputWriter.write("\t");
    				resultsOutputWriter.write(String.format("%.5f", TP));
    				resultsOutputWriter.write("\t");
    				resultsOutputWriter.write(String.format("%.5f", FP));
    				resultsOutputWriter.write("\t");
    				resultsOutputWriter.write(String.format("%.5f", TN));
    				resultsOutputWriter.write("\t");
    				resultsOutputWriter.write(String.format("%.5f", FN));
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
			}
		}
	}

}

