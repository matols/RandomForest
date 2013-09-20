package finalclassification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import randomjyrest.Forest;
import utilities.DetermineDatasetProperties;
import utilities.ImmutableTwoValues;

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
		String testingDataset = null;  // An optional dataset that will be predicted.
		if (args.length > 2)
		{
			testingDataset = args[2];
		}

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfTrees = 1000;  // The number of trees in the forest.
		long seed = 0L;  // The seed used for growing the forest.

		int mtry = 10;  // The number of features to consider at each split in a tree.
		
		// Specify the features in the input dataset that should be ignored.
		String[] featuresToIgnore = new String[]{};
		List<String> featuresToRemove = Arrays.asList(featuresToIgnore);
		
		int numberOfThreads = 1;  // The number of threads to use when growing the forest.

		// Define the weights for each class in the input dataset.
		Map<String, Double> classWeights = new HashMap<String, Double>();
		classWeights.put("Positive", 1.0);
		classWeights.put("Unlabelled", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		
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
		
		// Determine the class of each observation.
		List<String> classOfObservations = DetermineDatasetProperties.determineObservationClasses(trainingDataset);
		
		// Determine the vector of weights for the observations.
		double[] weights = DetermineDatasetProperties.determineObservationWeights(classOfObservations, "Positive", classWeights.get("Positive"), "Unlabelled",
				classWeights.get("Unlabelled"));
		
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
		ImmutableTwoValues<List<String>, List<String>> accessionsAndClasses =
				DetermineDatasetProperties.determineObservationAccessionsAndClasses(trainingDataset, accessionColumnName, classFeatureColumnName);
		List<String> trainingDatasetAccessions = accessionsAndClasses.first;
		List<String> trainingDatasetClasses = accessionsAndClasses.second;
		
		// Determine the accessions and classes of the proteins in the test set (if there is one).
		List<String> testingDatasetAccessions = new ArrayList<String>();
		List<String> testingDatasetClasses = new ArrayList<String>();
		if (testingDataset != null)
		{
			accessionsAndClasses = DetermineDatasetProperties.determineObservationAccessionsAndClasses(testingDataset, accessionColumnName,
					classFeatureColumnName);
			testingDatasetAccessions = accessionsAndClasses.first;
			testingDatasetClasses = accessionsAndClasses.second;
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