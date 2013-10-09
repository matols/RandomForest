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

import utilities.DetermineDatasetProperties;

/**
 * Implements the feature selection using a genetic algorithm.
 */
public class GAFeatureSelection
{

	/**
	 * Runs a genetic algorithm based feature selection.
	 * 
	 * @param args		The file system locations of the files and directories used in the GA feature selection.
	 */
	public static final void main(String[] args)
	{
		String inputDir = args[0];  // The location of the datasets used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the optimisation will be written.
		String parameterFile = args[2];  // The location where the parameters for the optimisation are recorded.
		
		// Parse the parameters.
		int numberOfTreesPerForest = 1000;  // The number of trees to grow in each forest.
		int mtry = 10;  // The number of features to consider at each split in a tree.
		int numberOfThreads = 1;  // The number of threads to use when growing the trees.
		Map<String, Double> positiveWeights = new HashMap<String, Double>();  // The weight for the positive class observations for each cutoff.
		List<String> cutoffsToUse = new ArrayList<String>();  // The cutoffs to use ordered as they are found in the parameter file.
															  // If a run continuation is used, then the ordering in the parameter file must be the same as the first run, or the ordering will be different and the continuation will fail.
		List<String> featuresToRemove = new ArrayList<String>();  // The features to not consider.
		boolean isNewRunBeingPerformed = false;  // Whether a previous run should be continued or not.
		int populationSize = 50;  // The number of individuals in the population.
		boolean isVerboseOutput = false;  // Whether status updates should be printed.
		int generationsWithoutChange = 10;  // The maximum number of attempts that will be made in each generation to generate an offspring that is fitter than at least one member of the parent population.
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
					positiveWeights.put(chunks[1], Double.parseDouble(chunks[2]));
					cutoffsToUse.add(chunks[1]);
				}
				else if (chunks[0].equals("NewRun"))
				{
					if (chunks[1].equals("True"))
					{
						isNewRunBeingPerformed = true;
					}
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
		
		// Define the starting cutoff.
		String startingCutoff = cutoffsToUse.get(0);
		
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
				for (String s : positiveWeights.keySet())
				{
					parameterOutputWriter.write("Weight\t" + s + "\t" + Double.toString(positiveWeights.get(s)));
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
			
			// Determine the last cutoff used (this will be the cutoff that is started from).
			File[] previousCutoffs = resultsDirectory.listFiles();
			for (File f : previousCutoffs)
			{
				if (f.isDirectory())
				{
					int iterationNumber = Integer.parseInt(f.getName());
					if (iterationNumber > Integer.parseInt(startingCutoff))
					{
						startingCutoff = f.getName();
					}
				}
			}
			
			// Load the parameters from the run that is being continued.
			positiveWeights = new HashMap<String, Double>();
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
						numberOfTreesPerForest = Integer.parseInt(chunks[1]);
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
						positiveWeights.put(chunks[1], Double.parseDouble(chunks[2]));
					}
					else if (chunks[0].equals("Attempts"))
					{
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
		
		// Setup the mapping of class names to weights.
		Map<String, Double> classWeights = new HashMap<String, Double>();
		classWeights.put("Positive", 1.0);
		classWeights.put("Unlabelled", 1.0);
		
		// Run the GA for each cutoff.
		for (String s : cutoffsToUse)
		{
			classWeights.put("Positive", positiveWeights.get(s));
			if (Integer.parseInt(s) >= Integer.parseInt(startingCutoff))
			{
				String inputFile = inputDir + "/NonRedundant_" + s + ".txt";
				double[] weights = DetermineDatasetProperties.determineObservationWeights(inputFile, classWeights);
				featureselection.CHCGeneticAlgorithm.main(inputFile, resultsDir + "/" + s, populationSize, isVerboseOutput, mtry,
						numberOfTreesPerForest, numberOfThreads, weights, featuresToRemove, generationsWithoutChange);
			}
		}
	}

}
