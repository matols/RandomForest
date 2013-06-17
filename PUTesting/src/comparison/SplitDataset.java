package comparison;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class SplitDataset
{

	/**
	 * @param args
	 */
	public Map<String, String> main(String inputFile, String outputLocation, double[] fractionsOfPositives, int samplesToGenerate)
	{
		// Ensure that the output location exists.
		File outputDir = new File(outputLocation);
		if (!outputDir.exists())
		{
			boolean isDirCreated = outputDir.mkdirs();
			if (!isDirCreated)
			{
				System.out.format("The output directory (%s) does not exist, and could not be created.\n", outputLocation);
				System.exit(0);
			}
		}
		else
		{
			removeDirectoryContent(outputDir);
			boolean isDirCreated = outputDir.mkdirs();
			if (!isDirCreated)
			{
				System.out.format("The output directory (%s) was removed, but could not then be created.\n", outputLocation);
				System.exit(0);
			}
		}

		// Get the names and types of the features (the types are so that you know which features are the response and which aren't used).
		String featureNames = null;
		String featureTypes = null;
		String featureCategories = null;
		Map<Integer, String> indexToObservationMapping = new HashMap<Integer, String>();
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			featureNames = line;

			line = reader.readLine();
			line = line.toLowerCase();
			line = line.replaceAll("\n", "");
			featureTypes = line;

			line = reader.readLine();
			line = line.replaceAll("\n", "");
			featureCategories = line;

			int currentObservationIndex = 0;  // Used to determine the index of the current observation in the file.
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.replaceAll("\n", "");
				indexToObservationMapping.put(currentObservationIndex, line);
				currentObservationIndex += 1;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		featureTypes = featureTypes.replace("r", "x");  // x out the old response class.

		// Process the input file.
		ProcessDataForGrowing processedDataset = new ProcessDataForGrowing(inputFile, new TreeGrowthControl());

		// Determine the different classes.
		Set<String> classesInDataset = new HashSet<String>(processedDataset.responseData);

		// For each class in the dataset, generate a new dataset where it is the positive class and all other classes are negative.
		Map<String, String> newDatasetsForS = new HashMap<String, String>();
		for (String s : classesInDataset)
		{
			// Create the directory for when s is being treated as the positive class.
			String newOutputLocation = outputLocation + "/" + s + "-Positive";
			File newOutputDir = new File(newOutputLocation);
			if (!newOutputDir.exists())
			{
				boolean isDirCreated = newOutputDir.mkdirs();
				if (!isDirCreated)
				{
					System.out.format("The class output directory (%s) does not exist, and could not be created.\n", newOutputLocation);
					System.exit(0);
				}
			}
			newDatasetsForS.put(s, newOutputLocation);

			// Get the indices of the observations from class s.
			List<Integer> positiveObservationIndices = new ArrayList<Integer>();
			for (int i = 0; i < processedDataset.numberObservations; i++)
			{
				if (processedDataset.responseData.get(i).equals(s))
				{
					positiveObservationIndices.add(i);
				}
			}
			int numberOfPositiveObservations = positiveObservationIndices.size();

			// Create the directory for the positive fraction.
			for (double posFrac : fractionsOfPositives)
			{
				String posFracLocation = newOutputLocation + "/PositiveFraction-" + Double.toString(posFrac);
				File posFracDir = new File(posFracLocation);
				if (!posFracDir.exists())
				{
					boolean isDirCreated = posFracDir.mkdirs();
					if (!isDirCreated)
					{
						System.out.format("The class output directory (%s) does not exist, and could not be created.\n", posFracLocation);
						System.exit(0);
					}
				}
			}

			for (int sampNum = 0; sampNum < samplesToGenerate; sampNum++)
			{
				Collections.shuffle(positiveObservationIndices);
	
				// Write out the dataset for each positive fraction.
				for (double posFrac : fractionsOfPositives)
				{
					String posFracLocation = newOutputLocation + "/PositiveFraction-" + Double.toString(posFrac);
	
					int numberOfPosObsToUse = (int) Math.floor(posFrac * numberOfPositiveObservations);
					List<Integer> positiveObsToUse = positiveObservationIndices.subList(0, numberOfPosObsToUse);
					String positiveFractionOutputLoc = posFracLocation + "/Dataset" + Integer.toString(sampNum) + ".txt";
					try
					{
						FileWriter posFracFile = new FileWriter(positiveFractionOutputLoc);
						BufferedWriter posFracWriter = new BufferedWriter(posFracFile);
						posFracWriter.write(featureNames);
						posFracWriter.write("\tNewClass");
						posFracWriter.newLine();
						posFracWriter.write(featureTypes);
						posFracWriter.write("\tr");
						posFracWriter.newLine();
						posFracWriter.write(featureCategories);
						posFracWriter.write("\t");
						posFracWriter.newLine();
						for (int i = 0; i < processedDataset.numberObservations; i++)
						{
							String observationValues = indexToObservationMapping.get(i);
							String[] splitObservation = observationValues.split("\t");
							String observationOutput = "";
							for (int j = 0; j < splitObservation.length; j++)
							{
								observationOutput += splitObservation[j] + "\t";
							}
							if (positiveObsToUse.contains(i))
							{
								observationOutput += "Positive";
							}
							else
							{
								observationOutput += "Unlabelled";
							}
							posFracWriter.write(observationOutput);
							posFracWriter.newLine();
						}
						posFracWriter.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						System.exit(0);
					}
				}
			}
		}

		return newDatasetsForS;
	}


	void removeDirectoryContent(File directory)
	{
		String directoryLocation = directory.getAbsolutePath();
		if (directory.isDirectory())
		{
			String dirFiles[] = directory.list();
			for (String s : dirFiles)
			{
				File subFile = new File(directoryLocation + "/" + s);
				if (subFile.isDirectory())
				{
					removeDirectoryContent(subFile);
				}
				else
				{
					subFile.delete();
				}
			}
		}
		directory.delete();
	}

}
