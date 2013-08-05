package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
		int startingIterationNumber = 0;  // The number of the directory for the first iteration.
		
		// Specify the genetic algorithm control parameters.
		int populationSize = 50;  // The number of individuals in the population.
		boolean isVerboseOutput = true;  // Whether status updates should be printed.
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		
		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
		String parameterLocation = resultsDir + "/Parameters.txt";
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
			try
			{
				FileWriter parameterOutputFile = new FileWriter(parameterLocation);
				BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
				parameterOutputWriter.write("Trees\t" + Integer.toString(numberOfTreesToGrow));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Population\t" + Integer.toString(populationSize));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Mtry\t" + Integer.toString(mtry));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Features\t" + featuresToRemove.toString());
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
			
			// Determine the last iteration performed (this will be the iteration that is started from).
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
			
			// Load the parameters from the run that is being continued.
			classWeights = new HashMap<String, Double>();
			BufferedReader reader = null;
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
					
					// Enter the feature values for this observation into the mapping of the temporary processing of the data.
					String[] chunks = line.split("\t");
					if (chunks[0].equals("Trees"))
					{
						numberOfTreesToGrow = Integer.parseInt(chunks[1]);
					}
					else if (chunks[0].equals("Population"))
					{
						populationSize = Integer.parseInt(chunks[1]);
					}
					else if (chunks[0].equals("Mtry"))
					{
						mtry = Integer.parseInt(chunks[1]);
					}
					else if (chunks[0].equals("Features"))
					{
						String features = chunks[1].substring(1, chunks[1].length() - 1);  // String off the enclosing [...].
						String[] individualFeatures = features.split(", ");
						featuresToRemove = Arrays.asList(individualFeatures);
						
					}
					else if (chunks[0].equals("Weight"))
					{
						classWeights.put(chunks[1], Double.parseDouble(chunks[2]));
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
		
		// Determine the weight vector.
		List<String> classOfObservations = DetermineObservationProperties.determineObservationClasses(inputFile);
		double[] weights = DetermineObservationProperties.determineObservationWeights(classOfObservations, "Positive",
				classWeights.get("Positive"), "Unlabelled", classWeights.get("Unlabelled"));
		
		for (int i = startingIterationNumber; i < numberOfRepetitionsToPerform; i++)
		{
			CHCGeneticAlgorithm.main(inputFile, resultsDir + "/" + Integer.toString(i), populationSize, isVerboseOutput, mtry, numberOfTreesToGrow, numberOfThreads, weights, featuresToRemove);
		}
	}

}
