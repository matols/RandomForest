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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

		// Setup the results output files.
		String fullDatasetResultsLocation = resultsDir + "/FullDatasetResults.txt";
		String fullDatasetMCCResultsLocation = resultsDir + "/FullDatasetMCCResults.txt";
		try
		{
			FileWriter resultsOutputFile = new FileWriter(fullDatasetResultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN\tTimeTaken(ms)");
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();

			resultsOutputFile = new FileWriter(fullDatasetMCCResultsLocation);
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

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int repetitions = 50;
		Double[] weightsToUse = {2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.0, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 4.0};
		Integer[] mtryToUse = {5, 10, 15, 20};
		Integer[] trainingObsToUse = {7669, 3721, 531, 9635, 7802, 7491, 2163, 1864, 918, 8494, 5298, 5813, 7665, 3097, 4061, 760, 6784, 5038, 3108, 3499, 750, 1978, 2645, 9791, 4722, 5737, 6868, 4656, 4342, 288, 160, 6248, 9585, 6564, 3642, 8732, 2434, 2683, 5325, 9368, 1549, 1287, 9734, 3096, 876, 7260, 982, 5431, 10167, 4384, 2933, 1172, 7972, 675, 1794, 2430, 1921, 8402, 7666, 8947, 7537, 3355, 4793, 3452, 6900, 1266, 1653, 1607, 5006, 9456, 1707, 1274, 7389, 8489, 7730, 4640, 844, 590, 8272, 8982, 5947, 6995, 8881, 453, 619, 2939, 2243, 10005, 1184, 8405, 6167, 7703, 733, 1813, 3710, 6297, 9705, 427, 7150, 9827, 2534, 9633, 4898, 649, 7065, 122, 6176, 8308, 9611, 6764, 6836, 387, 3899, 5880, 5285, 8719, 4557, 7056, 154, 7655, 7302, 4002, 1045, 7986, 7086, 8397, 7264, 4406, 3739, 111, 5151, 2549, 7829, 6635, 5017, 1916, 8196, 3408, 9926, 7266, 5184, 3194, 2615, 9866, 3640, 5581, 826, 6095, 3671, 2137, 5763, 3727, 4359, 8363, 416, 8825, 7453, 9907, 1538, 9025, 3708, 544, 4290, 5638, 299, 1338, 7657, 9170, 4214, 2254, 2464, 9769, 173, 856, 8125, 566, 6163, 297, 5599, 4634, 3205, 7298, 1545, 6287, 5133, 5275, 5983, 3563, 1402, 8384, 5399, 9932, 3620, 6576, 7975, 7375, 3243, 3807, 7905, 7943, 6235, 2263, 2789, 2341, 1898, 694, 4959, 2286, 7001, 8366, 8960, 838, 9126, 9545, 3889, 1228, 1808, 3234, 3093, 6488, 3615, 2385, 3020, 9856, 6071, 9395, 9691, 1028, 8777, 2823, 1766, 1773, 857, 3521, 9447, 6486, 3814, 5243, 4682, 9317, 3166, 1971, 234, 2311, 1133, 1830, 7645, 2336, 3822, 1499, 8179, 1295, 6425, 4036, 5444, 6457, 6986, 2419, 1461, 74, 6430, 7351, 6518, 174, 2961, 540, 6536, 7089, 4251, 717, 3091, 5428, 9028, 2853, 1857, 3597, 1168, 6599, 300, 5491, 54, 4925, 4575, 3165, 7288, 7860, 5782, 1657, 4840, 2746, 9733, 207, 5369, 3472, 9661, 7553, 10032, 8370, 6059, 7334, 3351, 5765, 3711, 715, 7618, 9104, 2741, 5963, 4762, 4870, 2319, 2325, 2171, 7974, 422, 8431, 1670, 7719, 3273, 3490, 7616, 7199, 6737, 3509, 7508, 8821, 5226, 2775, 5402, 6346, 8624, 3619, 8607, 4379, 6280, 6989, 388, 3534, 9016, 9287, 9757, 6601, 3434, 3153, 7547, 5663, 3703, 4727, 3767, 4209, 1798, 6592, 9059, 4866, 2959, 3225, 6783, 7754, 3840, 119, 6570, 7469, 2920, 1192, 9081, 2776, 2281, 10091, 4864, 1354, 2852, 5618, 7272, 5796, 198, 4028, 9107, 5319, 3327, 6561, 8365, 5379, 7336, 7903, 2754, 7475, 5375, 8605, 1308, 9197, 9968, 2687, 5623, 10057, 8315, 1993, 4272, 7493, 5904, 5356, 5654, 9777, 1852, 773, 484, 9453, 3438, 8161, 8415, 8236, 10017, 8991, 8500, 5649, 5186, 726, 5831, 6716, 4087, 9744, 8496, 2758, 8639, 445, 6156, 9504, 7007, 2611, 2153, 3424, 6776, 5459, 8530, 9320, 6631, 1624, 7263, 3135, 1515, 7173, 6194, 8039, 7753, 3834, 937, 4600, 129, 4795, 3312, 4386, 9859, 1147, 3144, 6964};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 500;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.minNodeSize = 1;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);

		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputFile, ctrl);
		String negClass = "Unlabelled";
		String posClass = "Positive";

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

		// Determine the subset of feature to remove.
		boolean isSubsetUsed = false;
		String subsetResultsLocation = resultsDir + "/SubsetResults.txt";
		String subsetMCCResultsLocation = resultsDir + "/SubsetMCCResults.txt";
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
				resultsOutputWriter.write("Weight\tMtry\tMCC\tF0.5\tF1\tF2\tAccuracy\tOOBError\tPrecision\tSensitivity\tSpecificity\tNPV\tTP\tFP\tTN\tFN\tTimeTaken(ms)");
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
				FileWriter resultsOutputFile = new FileWriter(subsetMCCResultsLocation);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write("Weight\tMtry");
				resultsOutputWriter.newLine();
				resultsOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		// Setup alternate forest growth control objects.
		TreeGrowthControl subsetCtrl = new TreeGrowthControl();
		subsetCtrl.isReplacementUsed = ctrl.isReplacementUsed;
		subsetCtrl.numberOfTreesToGrow = ctrl.numberOfTreesToGrow;
		subsetCtrl.variablesToIgnore = covarsToRemove;
		subsetCtrl.isStratifiedBootstrapUsed = true;

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

		Map<String, Map<String, Double>> confusionMatrix;

		for (Integer mtry : mtryToUse)
		{
			ctrl.mtry = mtry;
			subsetCtrl.mtry = mtry;

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
				confusionMatrix = new HashMap<String, Map<String, Double>>();
				for (String s : new HashSet<String>(procData.responseData))
				{
					confusionMatrix.put(s, new HashMap<String, Double>());
					confusionMatrix.get(s).put("TruePositive", 0.0);
					confusionMatrix.get(s).put("FalsePositive", 0.0);
				}
				MultipleForestRunAndTest.forestTraining(confusionMatrix, weights, ctrl, inputFile, seeds, repetitions, negClass, posClass, fullDatasetResultsLocation, fullDatasetMCCResultsLocation, 1);

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
					MultipleForestRunAndTest.forestTraining(confusionMatrix, weights, subsetCtrl, inputFile, seeds, repetitions, negClass, posClass, subsetResultsLocation, subsetMCCResultsLocation, 1);
				}
			}
		}
	}

}
