package finalclassification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
		String resultsDirLocation = args[2];
		File resultsDirectory = new File(resultsDirLocation);
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

		// Write out the parameters used.
		String paramLocation = resultsDirLocation + "/Parameters.txt";
		String controllerLocation = resultsDirLocation + "/Controller.txt";
		try
		{
			FileWriter paramFile = new FileWriter(paramLocation);
			BufferedWriter paramWriter = new BufferedWriter(paramFile);
			paramWriter.write("Features used - ");
			paramWriter.write(Arrays.toString(featuresToUse));
			paramWriter.newLine();
			paramWriter.write("Observations used - ");
			paramWriter.write(Arrays.toString(observationsToUse));
			paramWriter.newLine();
			paramWriter.write("Seed used - ");
			paramWriter.write(Long.toString(seed));
			paramWriter.newLine();
			paramWriter.newLine();
			paramWriter.write(weights.toString());
			paramWriter.close();

			ctrl.save(controllerLocation);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

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
		String nonRedundantProteinPredictionLocation = resultsDirLocation + "/NonRedundantProteinPredictions.txt";
		String redundantProteinPredictionLocation = resultsDirLocation + "/RedundantProteinPredictions.txt";
		String allProteinPredictionLocation = resultsDirLocation + "/AllProteinPredictions.txt";
		try
		{
			FileWriter nrProteinPredictionFile = new FileWriter(nonRedundantProteinPredictionLocation);
			FileWriter rProteinPredictionFile = new FileWriter(redundantProteinPredictionLocation);
			FileWriter proteinPredictionFile = new FileWriter(allProteinPredictionLocation);
			BufferedWriter nrProteinPredictionWriter = new BufferedWriter(nrProteinPredictionFile);
			BufferedWriter rProteinPredictionWriter = new BufferedWriter(rProteinPredictionFile);
			BufferedWriter proteinPredictionWriter = new BufferedWriter(proteinPredictionFile);
			nrProteinPredictionWriter.write("UPAccession\tPositiveWeight\tUnlabelledWeight\tOriginalClass");
			nrProteinPredictionWriter.newLine();
			rProteinPredictionWriter.write("UPAccession\tPositiveWeight\tUnlabelledWeight\tOriginalClass");
			rProteinPredictionWriter.newLine();
			proteinPredictionWriter.write("UPAccession\tPositiveWeight\tUnlabelledWeight\tOriginalClass");
			proteinPredictionWriter.newLine();
			for (Integer i : nonredundantPredictions.keySet())
			{
				nrProteinPredictionWriter.write(nonRedundantProteins.get(i));
				nrProteinPredictionWriter.write("\t");
				nrProteinPredictionWriter.write(Double.toString(nonredundantPredictions.get(i).get("Positive")));
				nrProteinPredictionWriter.write("\t");
				nrProteinPredictionWriter.write(Double.toString(nonredundantPredictions.get(i).get("Unlabelled")));
				nrProteinPredictionWriter.write("\t");
				nrProteinPredictionWriter.write(nonRedundantProteinClasses.get(i));
				nrProteinPredictionWriter.newLine();

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
				rProteinPredictionWriter.write(redundantProteins.get(i));
				rProteinPredictionWriter.write("\t");
				rProteinPredictionWriter.write(Double.toString(predictedConfusionMatrix.get(i).get("Positive")));
				rProteinPredictionWriter.write("\t");
				rProteinPredictionWriter.write(Double.toString(predictedConfusionMatrix.get(i).get("Unlabelled")));
				rProteinPredictionWriter.write("\t");
				rProteinPredictionWriter.write(redundantProteinClasses.get(i));
				rProteinPredictionWriter.newLine();

				proteinPredictionWriter.write(redundantProteins.get(i));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(Double.toString(predictedConfusionMatrix.get(i).get("Positive")));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(Double.toString(predictedConfusionMatrix.get(i).get("Unlabelled")));
				proteinPredictionWriter.write("\t");
				proteinPredictionWriter.write(redundantProteinClasses.get(i));
				proteinPredictionWriter.newLine();
			}
			nrProteinPredictionWriter.close();
			rProteinPredictionWriter.close();
			proteinPredictionWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
