package similaritycomparison;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import randomjyrest.Forest;
import utilities.DetermineDatasetProperties;

/**
 * Implements the classification of the observations in a dataset using a set of specific random forests.
 */
public class FinalClassification
{

	/**
	 * Produce a final prediction of the class of each observation in a dataset.
	 * 
	 * The prediction generated for each observation is the prediction weight for each class, rather than a single class value.
	 * 
	 * @param args		The file system locations of the files and directories used in the classification.
	 */
	public static void main(String[] args)
	{
		// Parse the input arguments.
		String datasetDirLocation = args[0];  // The location of the training and testing datasets.
		String resultsDirLocation = args[1];  // The location where the results will be written.
		String parameterFile = args[2];  // The location where the parameters for the classification are recorded.
		
		// Parse the parameters.
		int numberOfTrees = 1000;  // The number of trees in the forest.
		int mtry = 10;  // The number of features to consider at each split in a tree.
		int numberOfThreads = 3;  // The number of threads to use when growing the forest.
		Map<String, Long> seeds = new HashMap<String, Long>();  // The seeds used for growing the forest for each cutoff.
		Map<String, Map<String, Double>> classWeights = new HashMap<String, Map<String, Double>>();  // The weights for each class for each cutoff.
		Map<String, List<String>> featuresToRemove = new HashMap<String, List<String>>();  // The features to remove for each cutoff.
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(parameterFile));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}

				String[] chunks = line.split("\t");
				if (chunks[0].equals("Trees"))
				{
					// If the first entry on the line is Trees, then the line records the number of trees to use in each forest.
					numberOfTrees = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Mtry"))
				{
					// If the first entry on the line is Mtry, then the line contains the values of the mtry parameter to use.
					mtry = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Threads"))
				{
					// If the first entry on the line is Threads, then the line contains the number of threads to use when growing a forest.
					numberOfThreads = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Features"))
				{
					// If the first entry on the line is Features, then the line contains the features in the dataset to ignore for the given cutoff.
					String[] features = chunks[2].split(",");
					featuresToRemove.put(chunks[1], Arrays.asList(features));
				}
				else if (chunks[0].equals("Weight"))
				{
					// If the first entry on the line is Weight, then the line contains the weight to use for a class for a specific cutoff.
					String cutoff = chunks[1];
					String classification = chunks[2];
					Double weight = Double.parseDouble(chunks[3]);
					if (classWeights.containsKey(cutoff))
					{
						classWeights.get(cutoff).put(classification, weight);
					}
					else
					{
						classWeights.put(cutoff, new HashMap<String, Double>());
						classWeights.get(cutoff).put(classification, weight);
					}
				}
				else if (chunks[0].equals("Seed"))
				{
					// If the first entry on the line is Seed, then the line contains the seed to use for the given cutoff.
					seeds.put(chunks[1], Long.parseLong(chunks[2]));
				}
				else
				{
					// Got an unexpected line in the parameter file.
					System.out.println("An unexpected argument was found in the file of the parameters:");
					System.out.println(line);
					System.exit(0);
				}
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while extracting the parameters.");
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			try
			{
				if (reader != null)
				{
					reader.close();
				}
			}
			catch (IOException e)
			{
				// Caught an error while closing the file. Indicate this and exit.
				System.out.println("An error occurred while closing the parameters file.");
				e.printStackTrace();
				System.exit(0);
			}
		}

		// Setup the results directory.
		File resultsDirectory = new File(resultsDirLocation);
		if (!resultsDirectory.exists())
		{
			// The results directory does not exist.
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The results directory does not exist, but could not be created.");
				System.exit(0);
			}
		}
		else
		{
			// The results directory already exists.
			System.out.println("The results directory already exists. Please remove/rename the file or directory before retrying");
			System.exit(0);
		}
		
		boolean isCalculateOOB = true;  // OOB error is being calculated.
		
		for (String s : classWeights.keySet())
		{
			// Determine the datasets, weights, seed and features to ignore for this cutoff.
			String trainingDataset = datasetDirLocation + "/NonRedundant_" + s + ".txt";
			String testingDataset = datasetDirLocation + "/Redundant_" + s + ".txt";
			Map<String, Double> cutoffWeights = classWeights.get(s);
			long cutoffSeed = seeds.get(s);
			List<String> cutoffFeaturesToIgnore = featuresToRemove.get(s);
			
			// Determine the vector of weights for the observations.
			double[] weights = DetermineDatasetProperties.determineObservationWeights(trainingDataset, cutoffWeights);
			
			// Determine the OOB predictions (predictions for the training set) and the test set predictions.
			Forest forest = new Forest();
			Map<String, double[]> oobPredictions = forest.main(trainingDataset, numberOfTrees, mtry, cutoffFeaturesToIgnore, weights,
					cutoffSeed, numberOfThreads, isCalculateOOB);
			Map<String, double[]> testSetPredictions = forest.predict(testingDataset, cutoffFeaturesToIgnore);

			// Define the names of the class and UniProt accession columns in the datasets.
			String classFeatureColumnName = "Classification";
			String accessionColumnName = "UPAccession";
	
			// Determine the accessions and classes of the proteins in the training dataset.
			List<String> trainingDatasetAccessions = DetermineDatasetProperties.determineObservationAccessions(trainingDataset, accessionColumnName);
			List<String> trainingDatasetClasses = DetermineDatasetProperties.determineObservationClasses(trainingDataset, classFeatureColumnName);
			
			// Determine the accessions and classes of the proteins in the test set (if there is one).
			List<String> testingDatasetAccessions = new ArrayList<String>();
			List<String> testingDatasetClasses = new ArrayList<String>();
			if (testingDataset != null)
			{
				testingDatasetAccessions = DetermineDatasetProperties.determineObservationAccessions(testingDataset, accessionColumnName);
				testingDatasetClasses = DetermineDatasetProperties.determineObservationClasses(testingDataset, classFeatureColumnName);
			}
	
			// Write out the protein accessions, their classes and their predictions.
			String predictionResultsLocation = resultsDirLocation + "/Predictions_" + s + ".txt";
			try
			{
				FileWriter proteinPredictionFile = new FileWriter(predictionResultsLocation);
				BufferedWriter proteinPredictionWriter = new BufferedWriter(proteinPredictionFile);
				proteinPredictionWriter.write("UPAccession\tPositiveWeight\tUnlabelledWeight\tOriginalClass");
				proteinPredictionWriter.newLine();
				
				// Write out the predictions for the proteins in the training set.
				for (int i = 0; i < trainingDatasetAccessions.size(); i++)
				{
					String acc = trainingDatasetAccessions.get(i);
					String originalClass = trainingDatasetClasses.get(i);
					double posPredictionWeight = oobPredictions.get("Positive")[i];
					double unlabPredictionWeight = oobPredictions.get("Unlabelled")[i];
					proteinPredictionWriter.write(acc);
					proteinPredictionWriter.write("\t");
					proteinPredictionWriter.write(Double.toString(posPredictionWeight));
					proteinPredictionWriter.write("\t");
					proteinPredictionWriter.write(Double.toString(unlabPredictionWeight));
					proteinPredictionWriter.write("\t");
					proteinPredictionWriter.write(originalClass);
					proteinPredictionWriter.newLine();
				}
				
				// Write out the predictions for the proteins in the test set (nothing is written out if there is no test set).
				for (int i = 0; i < testingDatasetAccessions.size(); i++)
				{
					String acc = testingDatasetAccessions.get(i);
					String originalClass = testingDatasetClasses.get(i);
					double posPredictionWeight = testSetPredictions.get("Positive")[i];
					double unlabPredictionWeight = testSetPredictions.get("Unlabelled")[i];
					proteinPredictionWriter.write(acc);
					proteinPredictionWriter.write("\t");
					proteinPredictionWriter.write(Double.toString(posPredictionWeight));
					proteinPredictionWriter.write("\t");
					proteinPredictionWriter.write(Double.toString(unlabPredictionWeight));
					proteinPredictionWriter.write("\t");
					proteinPredictionWriter.write(originalClass);
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

}