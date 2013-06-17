package comparison;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;
import algorithms.RFPULearning;

public class EvaluateRF
{

	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		double[] positiveThreshold = new double[]{0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1.0};

		int numberOfTrees = 10;
		int numberOfForests = 2;
		int mtry = 6;
		String[] variablesToIgnore = new String[]{};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.variablesToIgnore = Arrays.asList(variablesToIgnore);

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);

		String positiveClass = "M";  // The class in the original dataset that is the positive class in the subsample datasets.
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse input.
		String originalDatasetLoc = args[0];
		String inputDirLoc = args[1];
		File inputDirectory = new File(inputDirLoc);
		String outputLocation = args[2];
		File outputDir = new File(outputLocation);
		if (!outputDir.exists())
		{
			boolean isDirCreated = outputDir.mkdirs();
			if (!isDirCreated)
			{
				System.out.format("The output directory (%s) does not exist, and could not be created.\n", outputLocation);
				System.exit(0);
			}
		}
		double positiveFraction = Double.parseDouble(args[3]);

		// Determine the datasets in the input directory.
		String[] datasetsToTest = inputDirectory.list();

		ProcessDataForGrowing processedOriginalDataset = new ProcessDataForGrowing(originalDatasetLoc, ctrl);
		List<Integer> originalPositiveIndices = new ArrayList<Integer>();
		List<Integer> originalNonPositiveIndices = new ArrayList<Integer>();
		for (int i = 0; i < processedOriginalDataset.numberObservations; i++)
		{
			String response = processedOriginalDataset.responseData.get(i);
			if (response.equals(positiveClass))
			{
				originalPositiveIndices.add(i);
			}
			else
			{
				originalNonPositiveIndices.add(i);
			}
		}
		ProcessDataForGrowing processedSubsampleDataset = new ProcessDataForGrowing(inputDirLoc + "/" + datasetsToTest[0], ctrl);
		int subsamplePositives = 0;
		for (int i = 0; i < processedSubsampleDataset.numberObservations; i++)
		{
			String response = processedSubsampleDataset.responseData.get(i);
			if (response.equals("Positive"))
			{
				subsamplePositives++;
			}
		}

		// Run the algorithms for each positive threshold.
		RFPULearning RFULearner = new RFPULearning();
		for (double d : positiveThreshold)
		{
			String posThresholdOutputLocation = outputLocation + "/PositiveThreshold-" + Double.toString(d) + ".txt";
			File posThresholdFile = new File(posThresholdOutputLocation);

			double positivesFoundAsPositives = 0.0;
			double negativesFoundAsPositives = 0.0;
			double negativesFoundAsNegatives = 0.0;
			double positivesFoundAsNegatives = 0.0;

			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date startTime = new Date();
		    String strDate = sdfDate.format(startTime);
			System.out.format("Now starting positive threshold - %f at %s.\n", d, strDate);

			for (String s : datasetsToTest)
			{
				String subsampleDatasetLoc = inputDirLoc + "/" + s;

				// Determine the positive set.
				Set<Integer> positiveSet = RFULearner.main(subsampleDatasetLoc, numberOfTrees, numberOfForests, mtry, variablesToIgnore, weights, d).first;
				for (int i = 0; i < processedOriginalDataset.numberObservations; i++)
				{
					if (originalPositiveIndices.contains(i))
					{
						if (positiveSet.contains(i))
						{
							positivesFoundAsPositives++;
						}
						else
						{
							positivesFoundAsNegatives++;
						}
					}
					else
					{
						if (positiveSet.contains(i))
						{
							negativesFoundAsPositives++;
						}
						else
						{
							negativesFoundAsNegatives++;
						}
					}
				}
			}

			positivesFoundAsPositives /= datasetsToTest.length;
			negativesFoundAsPositives /= datasetsToTest.length;
			negativesFoundAsNegatives /= datasetsToTest.length;
			positivesFoundAsNegatives /= datasetsToTest.length;

			try
			{
				FileWriter resultsFile;
				BufferedWriter resultsWriter;
				if (posThresholdFile.exists())
				{
					resultsFile = new FileWriter(posThresholdOutputLocation, true);
					resultsWriter = new BufferedWriter(resultsFile);
				}
				else
				{
					resultsFile = new FileWriter(posThresholdOutputLocation);
					resultsWriter = new BufferedWriter(resultsFile);
					resultsWriter.write("PositiveFraction\tPositivesInEntireDataset\tNegativesInEntireDataset\tPositiveThreshold\tKnownPositives\tAveragePositivesFoundAsPositives\tAverageNegativesFoundAsPositives\tAverageNegativesFoundAsNegatives\tAveragePositivesFoundAsNegatives");
					resultsWriter.newLine();
				}

				resultsWriter.write(Double.toString(positiveFraction));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(originalPositiveIndices.size()));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(originalNonPositiveIndices.size()));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(d));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(subsamplePositives));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(positivesFoundAsPositives));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(negativesFoundAsPositives));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(negativesFoundAsNegatives));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(positivesFoundAsNegatives));
				resultsWriter.newLine();
				resultsWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}
