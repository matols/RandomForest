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

	public class CrossValidationFoldGenerationMultiClass
	{

		/**
		 * @param args
		 */
		public static void main(String[] args)
		{
			String inputFileLocation = args[0];
			String outputLocation = args[1];
			int numberOfFolds = Integer.parseInt(args[2]);
			main(inputFileLocation, outputLocation, numberOfFolds);
		}

		public static void main(String inputFileLocation, String outputLocation, int numberOfFolds)
		{
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

			Map<String, List<Integer>> classObsIndices = new HashMap<String, List<Integer>>();
			Map<String, Integer> classNumbers = new HashMap<String, Integer>();
			Map<String, Integer> classObsInFold = new HashMap<String, Integer>();
			Map<String, List<Integer>> classObsIndicesForFolds = new HashMap<String, List<Integer>>();
			Map<String, Integer> classObsLeftOver = new HashMap<String, Integer>();
			for (String s : observationIndexToLine.keySet())
			{
				List<Integer> classObservations = new ArrayList<Integer>(observationIndexToLine.get(s).keySet());
				Collections.shuffle(classObservations);  // Shuffle the observations to randomise the observations in each fold.
				classObsIndices.put(s, classObservations);
				classNumbers.put(s, classObsIndices.get(s).size());
			}
			for (String s : classNumbers.keySet())
			{
				classObsInFold.put(s, classNumbers.get(s) / numberOfFolds);
				classObsIndicesForFolds.put(s, new ArrayList<Integer>());
				classObsLeftOver.put(s, classNumbers.get(s) - (classObsInFold.get(s) * numberOfFolds));
			}
			for (int i = 0; i <= numberOfFolds; i++)
			{
				for (String s : classObsInFold.keySet())
				{
					classObsIndicesForFolds.get(s).add(i * classObsInFold.get(s));
				}
			}

			// Select the observations for each fold.
			List<List<Integer>> datasets = new ArrayList<List<Integer>>();
			for (int i = 0; i < numberOfFolds; i++)
			{
				List<Integer> currentFold = new ArrayList<Integer>();
				for (String s : classObsIndicesForFolds.keySet())
				{
					currentFold.addAll(classObsIndices.get(s).subList(classObsIndicesForFolds.get(s).get(i), classObsIndicesForFolds.get(s).get(i + 1)));
				}
				datasets.add(currentFold);
			}
			// Add the left over observations if there are any.
			for (String s : classObsLeftOver.keySet())
			{
				if (classObsLeftOver.get(s) != 0)
				{
					// There are left over observations for this class.
					int foldIndex = 0;
					int indexOfFirstLeftOver = numberOfFolds * classObsInFold.get(s);
					for (int i = indexOfFirstLeftOver; i < classNumbers.get(s); i++)
					{
						datasets.get(foldIndex).add(classObsIndices.get(s).get(i));
						foldIndex += 1;
					}
				}
			}

			// Write out the datasets.
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
			for (int i = 0; i < numberOfFolds; i++)
			{
				String foldOutputLocation = outputLocation + "/" + Integer.toString(i);
				File foldOutputDirectory = new File(foldOutputLocation);
				if (!foldOutputDirectory.exists())
				{
					boolean isDirCreated = foldOutputDirectory.mkdirs();
					if (!isDirCreated)
					{
						System.out.println("The fold output directory does not exist, but could not be created.");
						System.exit(0);
					}
				}
				else if (!foldOutputDirectory.isDirectory())
				{
					// Exists and is not a directory.
					System.out.println("The fold output directory location already exists, but is not a directory.");
					System.exit(0);
				}
				String trainDataOutputLoc = foldOutputLocation + "/Train.txt";
				String testDataOutputLoc = foldOutputLocation + "/Test.txt";
				String originalTrainingDataIndicesOutputLoc = foldOutputLocation + "/OriginalIndicesOfTrainingSetObs.txt";
				String originalTestDataIndicesOutputLoc = foldOutputLocation + "/OriginalIndicesOfTestingSetObs.txt";
				List<Integer> trainingData = new ArrayList<Integer>();
				List<Integer> testData = new ArrayList<Integer>();
				for (int j = 0; j < numberOfFolds; j++)
				{
					if (i != j)
					{
						trainingData.addAll(datasets.get(j));
					}
					else
					{
						testData.addAll(datasets.get(j));
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

	}

