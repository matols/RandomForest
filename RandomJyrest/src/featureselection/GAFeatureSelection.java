package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import randomjyrest.DetermineObservationProperties;

public class GAFeatureSelection
{

	/**
	 * Runs a genetic algorithm based feature selection.
	 * 
	 * @param args		The file system locations of the files and directories used in the GA feature selection.
	 */
	public static final void main(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the optimisation will be written.
		runGA(inputFile, resultsDir);
	}
	
	
	/**
	 * Analyses the results of a set of runs of the genetic algorithm feature selection.
	 * 
	 * @param args		The file system locations of the files and directories used in the GA feature selection.
	 */
	public static final void gaAnalysis(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the optimisation will be written.
		runAnalysis(inputFile, resultsDir);
	}
	
	
	/**
	 * @param inputFile		The location of the dataset used to grow the forests.
	 * @param resultsDir	The location where the results of the feature selection will be written.
	 */
	private static final void runAnalysis(String inputFile, String resultsDir)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		// Specify the features in the input dataset that should be ignored.
		String[] unusedFeatures = new String[]{"UPAccession"};
		List<String> featuresToRemove = Arrays.asList(unusedFeatures);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		File outputDirectory = new File(resultsDir);
		if (!outputDirectory.isDirectory())
		{
			System.out.println("The location supplied for the results directory is not a valid directory location.");
			System.exit(0);
		}
		
		// Determine the features in the dataset.
		List<String> featuresInDataset = new ArrayList<String>();
		Path dataPath = Paths.get(inputFile);
		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");
			String classFeatureColumnName = "Classification";

			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					// Ignore the class column.
					;
				}
				else if (!featuresToRemove.contains(feature))
				{
					featuresInDataset.add(feature);
				}
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while determining the features to use.");
			e.printStackTrace();
			System.exit(0);
		}

		// Get the best individuals from the GA runs.
		List<List<String>> bestIndividuals = new ArrayList<List<String>>();
		//TODO get the directories in the output directory (can just ignore the Parameters.txt file)
		//TODO for each of the directories get the files in it
		//TODO get the file of the last generation
		//TODO the individual at the top of the file is the best individual from that run

		// Generate the output matrix.
		try
		{
			String matrixOutputLocation = resultsDir + "/MatrixOutput.txt";
			FileWriter matrixOutputFile = new FileWriter(matrixOutputLocation);
			BufferedWriter matrixOutputWriter = new BufferedWriter(matrixOutputFile);
			for (String s : featuresInDataset)
			{
				// Write out the feature name.
				matrixOutputWriter.write(s);
				matrixOutputWriter.write("\t");

				// Record whether the feature was present (0) or absent (1) in the individual. A 0 is used for a present feature
				// as the indivudals are the features that were not used.
				double featureOccurreces = 0.0;
				for (List<String> l : bestIndividuals)
				{
					int featurePresence = 1;
					if (l.contains(s))
					{
						featurePresence = 0;
					}
					featureOccurreces += featurePresence;
					matrixOutputWriter.write(Integer.toString(featurePresence));
					matrixOutputWriter.write("\t");
				}
				double featureFractions = featureOccurreces / bestIndividuals.size();
				matrixOutputWriter.write(Double.toString(featureFractions));
				matrixOutputWriter.newLine();
			}
			matrixOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	
	/**
	 * @param inputFile		The location of the dataset used to grow the forests.
	 * @param resultsDir	The location where the results of the feature selection will be written.
	 */
	private static final void runGA(String inputFile, String resultsDir)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		// Specify the random forest control parameters.
		int numberOfTreesToGrow = 1000;  // The number of trees to grow in each forest.
		int mtry = 10;  // The number of features to consider at each split in a tree.
		int numberOfThreads = 1;  // The number of threads to use when growing the trees.
		
		// Specify the features in the input dataset that should be ignored.
		String[] unusedFeatures = new String[]{"UPAccession"};
		List<String> featuresToRemove = Arrays.asList(unusedFeatures);
		
		// Define the weights for each class in the input dataset.
		Map<String, Double> classWeights = new HashMap<String, Double>();
		classWeights.put("Unlabelled", 1.0);
		classWeights.put("Positive", 1.0);
		
		int numberOfRepetitionsToPerform = 20;  // The number of runs of the GA feature selection to perform.
		boolean isNewRunBeingPerformed = false;  // Whether the run should be a continuation of a previous run or not.
		
		// Specify the genetic algorithm control parameters.
		int populationSize = 50;  // The number of individuals in the population.
		boolean isVerboseOutput = true;  // Whether status updates should be printed.
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		
		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
		if (isNewRunBeingPerformed)
		{
			// A previous run is not being continued.
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
				System.out.println("The results directory already exists, but a run continuation was not specified. " +
						"Please remove/rename the file before retrying or attempt to continue from a previous run (isNewRunBeingPerformed = false)");
				System.exit(0);
			}
			
			// Write out the parameters used.
			//TODO record the parameters (weights, population forest size, etc.)
		}
		else
		{
			// A previous run is being continued.
			if (!resultsDirectory.exists())
			{
				// The results directory does not exist.
				System.out.println("The results directory does not exist and a run continuation was specified. " +
						"Please supply the location of the results directory of a previous run or start a new one (isNewRunBeingPerformed = true)");
				System.exit(0);
			}
		}
		
		// Determine the weight vector.
		List<String> classOfObservations = DetermineObservationProperties.determineObservationClasses(inputFile);
		double[] weights = DetermineObservationProperties.determineObservationWeights(classOfObservations, "Positive",
				classWeights.get("Positive"), "Unlabelled", classWeights.get("Unlabelled"));
		
		for (int i = 0; i < numberOfRepetitionsToPerform; i++)
		{
			CHCGeneticAlgorithm.main(inputFile, resultsDir + "/" + Integer.toString(i), populationSize, isVerboseOutput, mtry, numberOfTreesToGrow, numberOfThreads, weights, featuresToRemove);
		}
	}

}
