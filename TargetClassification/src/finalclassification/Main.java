package finalclassification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.Forest;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class Main
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		String[] featuresToUse = new String[]{};
		Integer[] observationsToUse = new Integer[]{};

		long seed = 0L;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Positive", 1.0);
		weights.put("Unlabelled", 1.0);

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.variablesToUse = Arrays.asList(featuresToUse);
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.trainingObservations = Arrays.asList(observationsToUse);
		ctrl.mtry = 10;
		ctrl.numberOfTreesToGrow = 1000;
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse the input arguments.
		String nonRedundantDataset = args[0];
		String redundantDataset = args[1];
		String outputLocation = args[2];

		Forest forest = new Forest(nonRedundantDataset, ctrl, seed);
		forest.setWeightsByClass(weights);
		forest.growForest();

		// Predict the observations in the non-redundant dataset on their OOB trees.
		ProcessDataForGrowing processedNonRedundant = new ProcessDataForGrowing(nonRedundantDataset, ctrl);
		Map<Integer, Map<String, Double>> nonredundantPredictions = new HashMap<Integer, Map<String, Double>>();
		for (int i = 0; i < processedNonRedundant.numberObservations; i++)
		{
			List<Integer> currentObservation = new ArrayList<Integer>();
			currentObservation.add(i);
			
			// Get the trees that this observation is OOB on.
			List<Integer> treesObservationIsOOBOn = forest.oobOnTree.get(i);

			// Predict the class of the observation on the trees it is OOB on.
			Map<Integer, Map<String, Double>> predResults = forest.predictRaw(processedNonRedundant, currentObservation, treesObservationIsOOBOn);

			Map<String, Double> currentObsWeighting = new HashMap<String, Double>();
			currentObsWeighting.put("Positive", predResults.get(i).get("Positive"));
			currentObsWeighting.put("Unlabelled", predResults.get(i).get("Unlabelled"));
			nonredundantPredictions.put(i, currentObsWeighting);
		}

		// Record the accessions of the proteins in the non-redundant dataset.
		List<String> nonRedundantProteins = new ArrayList<String>();
		List<String> nonRedundantProteinClasses = new ArrayList<String>();
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(nonRedundantDataset), StandardCharsets.UTF_8))
		{
			String line = null;
			// Strip the three header lines.
			line = reader.readLine();
			int numberOfFeatures = line.split("\t").length;
			line = reader.readLine();
			line = reader.readLine();
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.trim();
				String[] splitLine = line.split("\t");
				nonRedundantProteins.add(splitLine[0]);
				nonRedundantProteinClasses.add(splitLine[numberOfFeatures - 1]);
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("There was an error while processing the non-redundant dataset file.");
			e.printStackTrace();
			System.exit(0);
		}

		// Predict the observations in the redundant dataset.
		ProcessDataForGrowing processedRedundant = new ProcessDataForGrowing(redundantDataset, ctrl);
		List<Integer> observationsToPredict = new ArrayList<Integer>();
		for (int i = 0; i < processedRedundant.numberObservations; i++)
		{
			observationsToPredict.add(i);
		}
		Map<Integer, Map<String, Double>> predictedConfusionMatrix = forest.predictRaw(processedRedundant, observationsToPredict);

		// Record the accessions and classes of the proteins in the redundant dataset.
		List<String> redundantProteins = new ArrayList<String>();
		List<String> redundantProteinClasses = new ArrayList<String>();
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(redundantDataset), StandardCharsets.UTF_8))
		{
			String line = null;
			// Strip the three header lines.
			line = reader.readLine();
			int numberOfFeatures = line.split("\t").length;
			line = reader.readLine();
			line = reader.readLine();
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.trim();
				String[] splitLine = line.split("\t");
				redundantProteins.add(splitLine[0]);
				redundantProteinClasses.add(splitLine[numberOfFeatures - 1]);
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("There was an error while processing the redundant dataset file.");
			e.printStackTrace();
			System.exit(0);
		}

		// Write out the protein accessions and their predictions.
		String proteinPredictionLocation = outputLocation + "/ProteinPredictions.txt";
		try
		{
			FileWriter proteinPredictionFile = new FileWriter(proteinPredictionLocation);
			BufferedWriter proteinPredictionWriter = new BufferedWriter(proteinPredictionFile);
			for (Integer i : nonredundantPredictions.keySet())
			{
				proteinPredictionWriter.write(nonRedundantProteins.get(i));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(Double.toString(nonredundantPredictions.get(i).get("Positive")));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(Double.toString(nonredundantPredictions.get(i).get("Unlabelled")));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(nonRedundantProteinClasses.get(i));
				proteinPredictionWriter.newLine();
			}
			for (Integer i : predictedConfusionMatrix.keySet())
			{
				proteinPredictionWriter.write(redundantProteins.get(i));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(Double.toString(predictedConfusionMatrix.get(i).get("Positive")));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(Double.toString(predictedConfusionMatrix.get(i).get("Unlabelled")));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(redundantProteinClasses.get(i));
				proteinPredictionWriter.newLine();
			}
			proteinPredictionWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
