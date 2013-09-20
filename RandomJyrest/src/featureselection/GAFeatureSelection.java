package featureselection;

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

import utilities.DetermineDatasetProperties;

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
		String parameterFile = args[2];  // The location where the parameters for the optimisation are recorded.
		
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		// Specify the random forest control parameters.
		int numberOfTreesPerForest = 1000;  // The number of trees to grow in each forest.
		int mtry = 10;  // The number of features to consider at each split in a tree.
		int numberOfThreads = 1;  // The number of threads to use when growing the trees.
		
		// Specify the features in the input dataset that should be ignored.
		List<String> featuresToRemove = new ArrayList<String>();
		
		// Define the weights for each class in the input dataset.
		Map<String, Double> classWeights = new HashMap<String, Double>();
		
		int numberOfRepetitionsToPerform = 20;  // The number of runs of the GA feature selection to perform.
		boolean isNewRunBeingPerformed = false;  // Whether the runs should continue on from a previous set of runs.
		
		// Specify the genetic algorithm control parameters.
		int populationSize = 50;  // The number of individuals in the population.
		boolean isVerboseOutput = false;  // Whether status updates should be printed.
		int generationsWithoutChange = 10;  // The maximum number of attempts that will be made in each generation to generate an
											// offspring that is fitter than at least one member of the parent population.
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		
		// Parse the parameters.
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
					numberOfTreesPerForest = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Mtry"))
				{
					// If the first entry on the line is Mtry, then the line contains the value of the mtry parameter.
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
					// If the first entry on the line is Weight, then the line contains a weight (third entry) for a class (second entry).
					classWeights.put(chunks[1], Double.parseDouble(chunks[2]));
				}
				else if (chunks[0].equals("Repetitions"))
				{
					// If the first entry on the line is Repetitions, then the line contains the number of GA runs to perform.
					numberOfRepetitionsToPerform = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("NewRun"))
				{
					// If the first entry on the line is NewRun and the second is True, then a new set of runs is being performed,
					// else a previous set of runs is being continued from.
					if (chunks[1].equals("True"))
					{
						isNewRunBeingPerformed = true;
					}
				}
				else if (chunks[0].equals("Population"))
				{
					// If the first entry on the line is Population, then the line contains the number of individuals in the population.
					populationSize = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Verbose"))
				{
					// If the first entry on the line is Verbose and the second is True, then status updates should be printed,
					// else no status updates should be printed.
					if (chunks[1].equals("True"))
					{
						isVerboseOutput = true;
					}
				}
				else if (chunks[0].equals("Attempts"))
				{
					// If the first entry on the line is Attempts, then the line contains the number of attempts that should be made
					// per generation at improving on a member of the parent population.
					generationsWithoutChange = Integer.parseInt(chunks[1]);
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

		int startingIterationNumber = 0;  // The number of the directory for the first iteration.
		
		// Setup the directory for the results and write out the parameters, or setup the continuation from a previous run.
		File resultsDirectory = new File(resultsDir);
		String parameterLocation = resultsDir + "/Parameters.txt";
		if (isNewRunBeingPerformed)
		{
			// A previous set of runs is not being continued.
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
			try
			{
				FileWriter parameterOutputFile = new FileWriter(parameterLocation);
				BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
				parameterOutputWriter.write("Trees\t" + Integer.toString(numberOfTreesPerForest));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Population\t" + Integer.toString(populationSize));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Mtry\t" + Integer.toString(mtry));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Features\t" + featuresToRemove.toString());
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Attempts\t" + Integer.toString(generationsWithoutChange));
				parameterOutputWriter.newLine();
				for (String s : classWeights.keySet())
				{
					parameterOutputWriter.write("Weight\t" + s + "\t" + Double.toString(classWeights.get(s)));
					parameterOutputWriter.newLine();
				}
				parameterOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
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
			
			// Determine the last run iteration performed (this will be the iteration that is started from).
			startingIterationNumber = 0;
			File[] previousIteratations = resultsDirectory.listFiles();
			for (File f : previousIteratations)
			{
				if (f.isDirectory())
				{
					int iterationNumber = Integer.parseInt(f.getName());
					if (iterationNumber > startingIterationNumber)
					{
						startingIterationNumber = iterationNumber;
					}
				}
			}
			
			// Load the parameters from the run that is being continued. Only need the parameters that directly affect the GA or the
			// forest growing process. For example, the number of trees and the number of attempts at improving on the parent
			// population are needed, but the number of threads or runs to perform are not. The parameters not specified here are taken
			// from the input file of parameters, and can be changed between the first attempt at the set of runs and the continuation.
			classWeights = new HashMap<String, Double>();
			reader = null;
			try
			{
				reader = new BufferedReader(new FileReader(parameterLocation));
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
						numberOfTreesPerForest = Integer.parseInt(chunks[1]);
					}
					else if (chunks[0].equals("Mtry"))
					{
						// If the first entry on the line is Mtry, then the line contains the value of the mtry parameter.
						mtry = Integer.parseInt(chunks[1]);
					}
					else if (chunks[0].equals("Features"))
					{
						// If the first entry on the line is Features, then the line contains the features in the dataset to ignore.
						String features = chunks[1].substring(1, chunks[1].length() - 1);  // String off the enclosing [...].
						String[] individualFeatures = features.split(", ");
						featuresToRemove = Arrays.asList(individualFeatures);
					}
					else if (chunks[0].equals("Weight"))
					{
						// If the first entry on the line is Weight, then the line contains a weight (third entry) for a class (second entry).
						classWeights.put(chunks[1], Double.parseDouble(chunks[2]));
					}
					else if (chunks[0].equals("Population"))
					{
						// If the first entry on the line is Population, then the line contains the number of individuals in the population.
						populationSize = Integer.parseInt(chunks[1]);
					}
					else if (chunks[0].equals("Attempts"))
					{
						// If the first entry on the line is Attempts, then the line contains the number of attempts that should be made
						// per generation at improving on a member of the parent population.
						generationsWithoutChange = Integer.parseInt(chunks[1]);
					}
					else
					{
						// Got an unexpected line in the parameter file.
						System.out.println("An unexpected argument was found in the file of the parameters of the past run:");
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
		}
		
		// Determine the class of each observation.
		List<String> classOfObservations = DetermineDatasetProperties.determineObservationClasses(inputFile);
		
		// Determine the vector of weights for the observations.
		double[] weights = DetermineDatasetProperties.determineObservationWeights(classOfObservations, "Positive",
				classWeights.get("Positive"), "Unlabelled", classWeights.get("Unlabelled"));
		
		// Run the GA feature selection the specified number of times. If the startingIterationumber is not 0, then a continuation
		// is being performed.
		for (int i = startingIterationNumber; i < numberOfRepetitionsToPerform; i++)
		{
			CHCGeneticAlgorithm.main(inputFile, resultsDir + "/" + Integer.toString(i), populationSize, isVerboseOutput, mtry,
					numberOfTreesPerForest, numberOfThreads, weights, featuresToRemove, generationsWithoutChange);
		}
	}

}
