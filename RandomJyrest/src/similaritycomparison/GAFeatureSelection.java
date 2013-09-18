package similaritycomparison;

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
		String inputDir = args[0];  // The location of the datasets used to grow the forests.
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
		String[] unwantedFeatures = new String[]{"UPAccession"};
		List<String> featuresToRemove = Arrays.asList(unwantedFeatures);
		
		// Define the weights for each class in the input dataset.
		String[] cutoffsToUse = new String[]{"20", "30", "40", "50", "60", "70", "80", "90", "100"};
		Map<String, Double> positiveWeights = new HashMap<String, Double>();
		positiveWeights.put("20", 1.0);
		positiveWeights.put("30", 1.0);
		positiveWeights.put("40", 1.0);
		positiveWeights.put("50", 1.0);
		positiveWeights.put("60", 1.0);
		positiveWeights.put("70", 1.0);
		positiveWeights.put("80", 1.0);
		positiveWeights.put("90", 1.0);
		positiveWeights.put("100", 1.0);
		
		// Define whether a previous run should be continued or not.
		boolean isNewRunBeingPerformed = false;
		
		// Specify the genetic algorithm control parameters.
		int populationSize = 50;  // The number of individuals in the population.
		boolean isVerboseOutput = false;  // Whether status updates should be printed.
		int generationsWithoutChange = 10;  // The maximum number of attempts that will be made in each generation to generate an offspring that is fitter than at least one member of the parent population.
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
				
				// Enter the feature values for this observation into the mapping of the temporary processing of the data.
				String[] chunks = line.split("\t");
				if (chunks[0].equals("Weight"))
				{
					positiveWeights.put(chunks[1], Double.parseDouble(chunks[2]));
				}
				else if (chunks[0].equals("NewRun"))
				{
					if (chunks[1].equals("True"))
					{
						isNewRunBeingPerformed = true;
					}
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
		String startingCutoff = cutoffsToUse[0];
		
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
		
		// Run the GA for each cutoff.
		for (String s : cutoffsToUse)
		{
			if (Integer.parseInt(s) >= Integer.parseInt(startingCutoff))
			{
				String inputFile = inputDir + "/NonRedundant-" + s + ".txt";
				List<String> classOfObservations = DetermineObservationProperties.determineObservationClasses(inputFile);
				double[] weights = DetermineObservationProperties.determineObservationWeights(classOfObservations, "Positive",
						positiveWeights.get(s), "Unlabelled", 1.0);
				featureselection.CHCGeneticAlgorithm.main(inputFile, resultsDir + "/" + s, populationSize, isVerboseOutput, mtry,
						numberOfTreesPerForest, numberOfThreads, weights, featuresToRemove, generationsWithoutChange);
			}
		}
	}

}
