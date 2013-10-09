package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utilities.DetermineDatasetProperties;

/**
 * Implements the analysis of the feature selection performed using a genetic algorithm.
 */
public class GAAnalysis
{

	/**
	 * Analyses the result of a set of runs of the genetic algorithm feature selection.
	 * 
	 * @param args		The file system locations of the files and directories used in the GA feature selection.
	 */
	public static final void main(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset that was used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the feature selection were written.

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		// Specify the features in the input dataset that were ignored in the feature selection.
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
		
		// Determine the features used in the feature selection.
		List<String> featuresInDataset = DetermineDatasetProperties.determineDatasetFeatures(inputFile, featuresToRemove);

		// Get the best individuals from the GA runs.
		List<List<String>> bestIndividuals = new ArrayList<List<String>>();  // A list containing the most fit indivudal from each run.
		List<Double> indivudalsFitnesses = new ArrayList<Double>();  // The fitness of each of the most fit individuals.
		List<Long> indivudalSeeds = new ArrayList<Long>();  // The seed used to grow each of the most fit indivuals.
		File[] gaDirContents = outputDirectory.listFiles();  // Get the directories that contain the results of the GA runs.
		for (File f : gaDirContents)
		{
			if (f.isDirectory())
			{
				// The location is a directory (and therefore contains the results of a GA feature selection run).
				File[] gaGenerationRecords = f.listFiles();

				// Determine the final generation number.
				String finalGenerationLocation = "";
				int maxGeneration = 0;
				for (File g : gaGenerationRecords)
				{
					int currentGeneration = Integer.parseInt(g.getName());
					if (currentGeneration > maxGeneration)
					{
						maxGeneration = currentGeneration;
						finalGenerationLocation = g.getAbsolutePath();
					}
				}
				
				// Extract the information about the most fit individual in the final generation (fitness, seed and feature set used).
				BufferedReader reader = null;
				try
				{
					reader = new BufferedReader(new FileReader(finalGenerationLocation));
					reader.readLine();  // Strip the header line.
					String bestIndividualData = reader.readLine().trim();
					String[] bestIndividualInformation = bestIndividualData.split("\t");
					indivudalsFitnesses.add(Double.parseDouble(bestIndividualInformation[0]));
					indivudalSeeds.add(Long.parseLong(bestIndividualInformation[1]));
					String individual = bestIndividualInformation[2].substring(1, bestIndividualInformation[2].length() - 1);
					String[] featuresNotUsed = individual.split(", ");
					bestIndividuals.add(Arrays.asList(featuresNotUsed));
				}
				catch (IOException e)
				{
					// Caught an error while reading the file. Indicate this and exit.
					System.out.println("An error occurred while extracting the information from the GA generation located at: " + finalGenerationLocation);
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
						System.out.println("An error occurred while closing the file located at: " + finalGenerationLocation);
						e.printStackTrace();
						System.exit(0);
					}
				}
			}
		}

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
				// as the individuals represent the features that were not used in growing the forest.
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
			matrixOutputWriter.newLine();
			
			// Output the fitness for each individual.
			matrixOutputWriter.write("Fitness\t");
			for (Double d : indivudalsFitnesses)
			{
				matrixOutputWriter.write(Double.toString(d));
				matrixOutputWriter.write("\t");
			}
			matrixOutputWriter.newLine();
			
			// Output the seed used to grow each individual.
			matrixOutputWriter.write("Seed\t");
			for (Long l : indivudalSeeds)
			{
				matrixOutputWriter.write(Long.toString(l));
				matrixOutputWriter.write("\t");
			}
			matrixOutputWriter.newLine();
			
			matrixOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
