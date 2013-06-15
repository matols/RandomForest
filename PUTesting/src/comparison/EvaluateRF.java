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

	public void runRFPULearning(String originalDatasetLoc, String maskedDatasetLoc, String outputFile)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		double[] positiveThreshold = new double[]{0.5, 0.75};
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

			File outputLDir = new File(outputFile);
			try
			{
				FileWriter resultsFile;
				BufferedWriter resultsWriter;
				if (outputLDir.exists())
				{
					resultsFile = new FileWriter(outputFile, true);
					resultsWriter = new BufferedWriter(resultsFile);
				}
				else
				{
					resultsFile = new FileWriter(outputFile);
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
