package finalclassification;

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

public class Main
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
		String trainingDataset = args[0];  // The dataset that is to be used to grow the forest.
		String resultsDirLocation = args[1];  // The location where the results will be written.
		String parameterFile = args[2];  // The location where the parameters for the classification are recorded.
		String testingDataset = null;  // An optional dataset that will be predicted.
		if (args.length > 3)
		{
			testingDataset = args[3];
		}
		
		// Parse the parameters.
		int numberOfTrees = 1000;  // The number of trees in the forest.
		int mtry = 10;  // The number of features to consider at each split in a tree.
		int numberOfThreads = 3;  // The number of threads to use when growing the forest.
		long seed = 0L;  // The seed used for growing the forest.
		Map<String, Double> classWeights = new HashMap<String, Double>();  // The weights for each class in the input dataset.
		List<String> featuresToRemove = new ArrayList<String>();  // The features in the input dataset that should be ignored.
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
					// If the first entry on the line is Features, then the line contains the features in the dataset to ignore.
					String[] features = chunks[1].split(",");
					featuresToRemove = Arrays.asList(features);
				}
				else if (chunks[0].equals("Weight"))
				{
					// If the first entry on the line is Weight, then the line contains the weight to use for a class.
					String classification = chunks[1];
					Double weight = Double.parseDouble(chunks[2]);
					classWeights.put(classification, weight);
				}
				else if (chunks[0].equals("Seed"))
				{
					// If the first entry on the line is Seed, then the line contains the seed to use.
					seed = Long.parseLong(chunks[1]);
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
		
		boolean isCalculateOOB = true;  // OOB error is being calculated.

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

		// Write out the parameters used.
		String parameterLocation = resultsDirLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Number of trees - " + Integer.toString(numberOfTrees));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used");
			parameterOutputWriter.newLine();
			for (String s : classWeights.keySet())
			{
				parameterOutputWriter.write(s + "\t" + Double.toString(classWeights.get(s)));
				parameterOutputWriter.newLine();
			}
			parameterOutputWriter.write("Mtry used - " + Integer.toString(mtry));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Features - " + featuresToRemove.toString());
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Seed - " + Long.toString(seed));
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
		// Determine the vector of weights for the observations.
		double[] weights = DetermineDatasetProperties.determineObservationWeights(trainingDataset, classWeights);
		
		// Determine the OOB predictions (predictions for the training set) and the test set predictions (if there is a test set).
		Forest forest = new Forest();
		Map<String, double[]> oobPredictions = forest.main(trainingDataset, numberOfTrees, mtry, featuresToRemove, weights,
				seed, numberOfThreads, isCalculateOOB);
		Map<String, double[]> testSetPredictions = null;
		if (testingDataset != null)
		{
			testSetPredictions = forest.predict(testingDataset, featuresToRemove);
		}
		
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
		String predictionResultsLocation = resultsDirLocation + "/Predictions.txt";
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