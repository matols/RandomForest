package datasetgeneration;

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
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BootstrapGeneration
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String inputFileLocation = args[0];
		String outputLocation = args[1];
		int numberOfFolds = Integer.parseInt(args[2]);
		main(inputFileLocation, outputLocation, numberOfFolds, true, 0.0);
	}

	public static void main(String inputFileLocation, String outputLocation, int numberOfBootstraps, boolean isReplacementUsed, double fractionToSample)
	{
		// Check the input validity.
		File outputDirectory = new File(outputLocation);
		if (!outputDirectory.exists())
		{
			boolean isDirCreated = outputDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The output directory does not exist, but could not be created.");
				System.exit(0);
			}
		}
		else if (!outputDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
			System.exit(0);
		}

		Map<String, Map<Integer, String>> observationIndexToLine = new HashMap<String, Map<Integer, String>>();
		String headerOne = "";
		String headerTwo = "";
		String headerThree = "";
		// Sort the observations into the positive and unlabelled lists.
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFileLocation), StandardCharsets.UTF_8))
		{
			// Knock off the first three header lines.
			headerOne = reader.readLine();
			int indexOfClassification = headerOne.split("\t").length - 1;
			headerTwo = reader.readLine();
			headerThree = reader.readLine();

			String line;
			int observationIndex = 0;
			while ((line = reader.readLine()) != null)
			{
				String[] splitLine = (line.trim()).split("\t");
				String classification = splitLine[indexOfClassification];
				if (observationIndexToLine.containsKey(classification))
				{
					observationIndexToLine.get(classification).put(observationIndex, line);
				}
				else
				{
					observationIndexToLine.put(classification, new HashMap<Integer, String>());
					observationIndexToLine.get(classification).put(observationIndex, line);
				}
				observationIndex++;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		Map<String, Integer> classNumbers = new HashMap<String, Integer>();
		for (String s : observationIndexToLine.keySet())
		{
			classNumbers.put(s, observationIndexToLine.get(s).size());
		}

		// Write out the datasets.
		Random obsSelector = new Random();
		for (int i = 0; i < numberOfBootstraps; i++)
		{
			String bootstrapOutputLocation = outputLocation + "/" + Integer.toString(i);
			File bootstrapOutputDirectory = new File(bootstrapOutputLocation);
			if (!bootstrapOutputDirectory.exists())
			{
				boolean isDirCreated = bootstrapOutputDirectory.mkdirs();
				if (!isDirCreated)
				{
					System.out.println("The bootstrap output directory does not exist, but could not be created.");
					System.exit(0);
				}
			}
			else if (!bootstrapOutputDirectory.isDirectory())
			{
				// Exists and is not a directory.
				System.out.println("The bootstrap output directory location already exists, but is not a directory.");
				System.exit(0);
			}
			else
			{
				removeDirectoryContent(bootstrapOutputDirectory);
				boolean isDirCreated = bootstrapOutputDirectory.mkdirs();
				if (!isDirCreated)
				{
					System.out.println("The bootstrap output directory could not be created.");
					System.exit(0);
				}
			}

			String trainDataOutputLoc = bootstrapOutputLocation + "/Train.txt";
			String testDataOutputLoc = bootstrapOutputLocation + "/Test.txt";
			String originalTrainingDataIndicesOutputLoc = bootstrapOutputLocation + "/OriginalIndicesOfTrainingSetObs.txt";
			String originalTestDataIndicesOutputLoc = bootstrapOutputLocation + "/OriginalIndicesOfTestingSetObs.txt";
			List<Integer> trainingData = new ArrayList<Integer>();
			List<Integer> testData = new ArrayList<Integer>();
			int selectedObservation;
			for (String s : classNumbers.keySet())
			{
				int numberOfObsOfThisclass = classNumbers.get(s);
				List<Integer> thisClassObsIndices = new ArrayList<Integer>(observationIndexToLine.get(s).keySet());
				if (!isReplacementUsed)
				{
					Collections.shuffle(thisClassObsIndices);
					for (int j = 0; j < (fractionToSample * numberOfObsOfThisclass); j++)
					{
						trainingData.add(thisClassObsIndices.get(j));
					}
				}
				else
				{
					for (int j = 0; j < numberOfObsOfThisclass; j++)
					{
						selectedObservation = obsSelector.nextInt(numberOfObsOfThisclass);
						trainingData.add(thisClassObsIndices.get(selectedObservation));
					}
				}
				for (Integer j : thisClassObsIndices)
				{
					if (!trainingData.contains(j))
					{
						testData.add(j);
					}
				}
			}
			try
			{
				// Write out the training data.
				FileWriter trainDataOutputFile = new FileWriter(trainDataOutputLoc);
				BufferedWriter trainDataOutputWriter = new BufferedWriter(trainDataOutputFile);
				FileWriter originalIndicesOutputFile = new FileWriter(originalTrainingDataIndicesOutputLoc);
				BufferedWriter originalIndicesOutputWriter = new BufferedWriter(originalIndicesOutputFile);
				trainDataOutputWriter.write(headerOne);
				trainDataOutputWriter.newLine();
				trainDataOutputWriter.write(headerTwo);
				trainDataOutputWriter.newLine();
				trainDataOutputWriter.write(headerThree);
				trainDataOutputWriter.newLine();
				for (Integer j : trainingData)
				{
					originalIndicesOutputWriter.write(Integer.toString(j));
					originalIndicesOutputWriter.newLine();
					for (String s : observationIndexToLine.keySet())
					{
						if (observationIndexToLine.get(s).containsKey(j))
						{
							trainDataOutputWriter.write(observationIndexToLine.get(s).get(j));
							trainDataOutputWriter.newLine();
							break;
						}
					}
				}
				trainDataOutputWriter.close();
				originalIndicesOutputWriter.close();

				// Write out the testing data.
				FileWriter testDataOutputFile = new FileWriter(testDataOutputLoc);
				BufferedWriter testDataOutputWriter = new BufferedWriter(testDataOutputFile);
				originalIndicesOutputFile = new FileWriter(originalTestDataIndicesOutputLoc);
				originalIndicesOutputWriter = new BufferedWriter(originalIndicesOutputFile);
				testDataOutputWriter.write(headerOne);
				testDataOutputWriter.newLine();
				testDataOutputWriter.write(headerTwo);
				testDataOutputWriter.newLine();
				testDataOutputWriter.write(headerThree);
				testDataOutputWriter.newLine();
				for (Integer j : testData)
				{
					originalIndicesOutputWriter.write(Integer.toString(j));
					originalIndicesOutputWriter.newLine();
					for (String s : observationIndexToLine.keySet())
					{
						if (observationIndexToLine.get(s).containsKey(j))
						{
							testDataOutputWriter.write(observationIndexToLine.get(s).get(j));
							testDataOutputWriter.newLine();
							break;
						}
					}
				}
				testDataOutputWriter.close();
				originalIndicesOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	static void removeDirectoryContent(File directory)
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

