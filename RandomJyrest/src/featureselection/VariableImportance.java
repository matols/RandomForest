package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import randomjyrest.Forest;
import utilities.DetermineObservationProperties;
import utilities.StringsSortedByDoubles;

public class VariableImportance
{
	
	/**
	 * Calculates the importance of each feature in a dataset.
	 * 
	 * @param args		The file system locations of the files and directories used in the variable importance calculation.
	 */
	public static final void main(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the importance calculations will be written.
		String parameterFile = args[2];  // The location where the parameters for the optimisation are recorded.
		
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfForestsToCreate = 200;  // The number of forests to calculate the variable importance for.
		int numberOfTreesPerForest = 1000;  // The number of trees to grow in each forest.
		int mtry = 10;  // The number of features to consider at each split in a tree.
		
		int numberOfThreads = 1;  // The number of threads to use when growing the trees.
		
		// Specify the features in the input dataset that should be ignored.
		List<String> featuresToRemove = new ArrayList<String>();
		
		// Define the weights for each class in the input dataset.
		Map<String, Double> classWeights = new HashMap<String, Double>();
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
				if (chunks[0].equals("Forests"))
				{
					numberOfForestsToCreate = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Trees"))
				{
					numberOfTreesPerForest = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Mtry"))
				{
					mtry = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Threads"))
				{
					numberOfThreads = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Features"))
				{
					String[] features = chunks[1].split(",");
					featuresToRemove = Arrays.asList(features);
				}
				else if (chunks[0].equals("Weight"))
				{
					classWeights.put(chunks[1], Double.parseDouble(chunks[2]));
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

		// Mandatory controls for the forest growth.
		boolean isCalculateOOB = true;  // OOB error is being calculated.
		
		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
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
			System.out.println("The results directory already exists. Please remove/rename the file before retrying");
			System.exit(0);
		}
		
		// Record the parameters.
		String parameterLocation = resultsDir + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Number of trees grown - " + Integer.toString(numberOfTreesPerForest));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Number of forests grown - " + Integer.toString(numberOfForestsToCreate));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("mtry - " + Integer.toString(mtry));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used");
			parameterOutputWriter.newLine();
			for (String s : classWeights.keySet())
			{
				parameterOutputWriter.write("\t" + s + " - " + Double.toString(classWeights.get(s)));
				parameterOutputWriter.newLine();
			}
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the features in the dataset.
		List<String> featuresInDataset = new ArrayList<String>();
		reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(inputFile));
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
				System.out.println("An error occurred while closing the input data file.");
				e.printStackTrace();
				System.exit(0);
			}
		}

		// Write out the importance header
		String variableImportanceLocation = resultsDir + "/VariableImportances.txt";
		try
		{
			FileWriter varImpOutputFile = new FileWriter(variableImportanceLocation, true);
			BufferedWriter varImpOutputWriter = new BufferedWriter(varImpOutputFile);

			varImpOutputWriter.write(featuresInDataset.get(0));
			for (String s : featuresInDataset.subList(1, featuresInDataset.size()))
			{
				varImpOutputWriter.write("\t" + s);
			}
			varImpOutputWriter.newLine();

			varImpOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the seeds that will be used.
		String seedsLocation = resultsDir + "/SeedsUsed.txt";
		Random randGen = new Random();
		List<Long> seeds = new ArrayList<Long>();
		for (int i = 0; i < numberOfForestsToCreate; i++)
		{
			long seedToUse = randGen.nextLong();
			while (seeds.contains(seedToUse))
			{
				seedToUse = randGen.nextLong();
			}
			seeds.add(seedToUse);
		}
		
		// Determine the weight vector.
		List<String> classOfObservations = DetermineObservationProperties.determineObservationClasses(inputFile);
		double[] weights = DetermineObservationProperties.determineObservationWeights(classOfObservations, "Positive",
				classWeights.get("Positive"), "Unlabelled", classWeights.get("Unlabelled"));

		for (int i = 0; i < numberOfForestsToCreate; i++)
		{
			Date startTime = new Date();
		    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String strDate = sdfDate.format(startTime);
		    System.out.format("Now working on repetition %d at %s.\n", i, strDate);

		    // Grow the forest.
		    Forest forest = new Forest();
		    forest.main(inputFile, numberOfTreesPerForest, mtry, featuresToRemove, weights, seeds.get(i), numberOfThreads, isCalculateOOB);
			
		    // Determine the variable importance for the forest.
			System.out.println("\tNow determining variable importances.");
			Map<String, Double> variableImportances = forest.variableImportance();

			// Determine the importance ordering for the variables, largest importance first.
			List<StringsSortedByDoubles> sortedVariableImportances = new ArrayList<StringsSortedByDoubles>();
			for (Map.Entry<String, Double> entry : variableImportances.entrySet())
			{
				sortedVariableImportances.add(new StringsSortedByDoubles(entry.getValue(), entry.getKey()));
			}
			Collections.sort(sortedVariableImportances, Collections.reverseOrder());

			// Rank the features by their importance.
			Map<String, Integer> featureToImportanceRank = new HashMap<String, Integer>();
			for (int j = 0; j < sortedVariableImportances.size(); j++)
			{
				featureToImportanceRank.put(sortedVariableImportances.get(j).getId(), j + 1);
			}

			// Write out the results for this repetition.
			try
			{
				String importanceOutputString = "";
				for (String s : featuresInDataset)
				{
					importanceOutputString += featureToImportanceRank.get(s) + "\t";
				}

				FileWriter accVarImpOutputFile = new FileWriter(variableImportanceLocation, true);
				BufferedWriter accVarImpOutputWriter = new BufferedWriter(accVarImpOutputFile);
				accVarImpOutputWriter.write(importanceOutputString.substring(0, importanceOutputString.length() - 1));  // Strip the final tab off.
				accVarImpOutputWriter.newLine();
				accVarImpOutputWriter.close();

				FileWriter seedsOutputFile = new FileWriter(seedsLocation, true);
				BufferedWriter seedsOutputWriter = new BufferedWriter(seedsOutputFile);
				seedsOutputWriter.write(Long.toString(seeds.get(i)));
				seedsOutputWriter.newLine();
				seedsOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
}
