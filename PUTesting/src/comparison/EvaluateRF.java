package comparison;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;
import algorithms.RFPULearning;

public class EvaluateRF
{

	public void runRFPULearning(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		double[] positiveThreshold = new double[]{0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9};
		Map<String, Double> weights = new HashMap<String, Double>();
		int numberOfTrees = 500;
		int numberOfForests = 10;
		int mtry = 10;
		String[] variablesToIgnore = new String[]{};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.variablesToIgnore = Arrays.asList(variablesToIgnore);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse input.
		String originalDatasetLoc = args[0];
		String maskedDatasetLoc = args[1];
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

		ProcessDataForGrowing processedOriginalDataset = new ProcessDataForGrowing(originalDatasetLoc, ctrl);
		List<Integer> originalPositiveIndices = new ArrayList<Integer>();
		List<Integer> originalNonPositiveIndices = new ArrayList<Integer>();
		for (int i = 0; i < processedOriginalDataset.numberObservations; i++)
		{
			String response = processedOriginalDataset.responseData.get(i);
			if (response.equals("Positive"))
			{
				originalPositiveIndices.add(i);
			}
			else
			{
				originalNonPositiveIndices.add(i);
			}
		}
		ProcessDataForGrowing processedMaskedDataset = new ProcessDataForGrowing(maskedDatasetLoc, ctrl);
		List<Integer> maskedPositiveIndices = new ArrayList<Integer>();
		for (int i = 0; i < processedMaskedDataset.numberObservations; i++)
		{
			String response = processedMaskedDataset.responseData.get(i);
			if (response.equals("Positive"))
			{
				maskedPositiveIndices.add(i);
			}
		}
		double positiveFraction = ((double) maskedPositiveIndices.size()) / originalPositiveIndices.size();

		// Run the algorithms for each positive threshold.
		RFPULearning RFULearner = new RFPULearning();
		for (double d : positiveThreshold)
		{
			Set<Integer> positiveSet = RFULearner.main(maskedDatasetLoc, numberOfTrees, numberOfForests, mtry, variablesToIgnore, weights, d).first;

			int TP = 0;
			int FP = 0;
			int TN = 0;
			int FN = 0;
			for (int i = 0; i < processedOriginalDataset.numberObservations; i++)
			{
				if (originalPositiveIndices.contains(i))
				{
					if (positiveSet.contains(i))
					{
						TP++;
					}
					else
					{
						FN++;
					}
				}
				else
				{
					if (positiveSet.contains(i))
					{
						FP++;
					}
					else
					{
						TN++;
					}
				}
			}

			try
			{
				FileWriter resultsFile;
				BufferedWriter resultsWriter;
				if (outputDir.exists())
				{
					resultsFile = new FileWriter(outputLocation, true);
					resultsWriter = new BufferedWriter(resultsFile);
				}
				else
				{
					resultsFile = new FileWriter(outputLocation);
					resultsWriter = new BufferedWriter(resultsFile);
					resultsWriter.write("PositiveFraction\tPositivesInEntireDataset\tNegativesInEntireDataset\tPositiveThreshold\tKnownPositives\tTruePositives\tFalsePositives\tTrueNegatives\tFalseNegatives");
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
				resultsWriter.write(Integer.toString(maskedPositiveIndices.size()));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(TP));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(FP));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(TN));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(FN));
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
