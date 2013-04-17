package instanceselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import datasetgeneration.CrossValidationFoldGenerationMultiClass;

import tree.IndexedDoubleData;
import tree.TreeGrowthControl;

public class Controller
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Required inputs.
		String inputLocation = args[0];  // The location of the file containing the dataset to use in the instance selection.
		File inputFile = new File(inputLocation);
		if (!inputFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the dataset for instance selection.");
			System.exit(0);
		}
		String outputLocation = args[1];  // The location to store any and all results.
		File outputDirectory = new File(outputLocation);
		if (!outputDirectory.exists())
		{
			boolean isDirCreated = outputDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The output directory could not be created.");
				System.exit(0);
			}
		}
		else if (!outputDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
			System.exit(0);
		}

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int crossValidationsToDo = 0;
		int numberOfFolds = 0;

		Map<String, Integer> obsOfEachClass = new HashMap<String, Integer>();
		obsOfEachClass.put("Unlabelled", 20);
		obsOfEachClass.put("Positive", 20);

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 500;
		ctrl.mtry = 10;
		ctrl.isStratifiedBootstrapUsed = true;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		String[] newGAArgs = new String[args.length + 1];
		for (int k = 2; k < args.length; k++)
		{
			newGAArgs[k+1] = args[k];
		}
		
		// Run the GA multiple times.
		for (int i = 0; i < crossValidationsToDo; i++)
		{
			System.out.format("Cross validation iteration : %d.\n", i);
			String cvDir = outputLocation + "\\CV\\" + Integer.toString(i);
			// Generate external cross validation folds.
			CrossValidationFoldGenerationMultiClass.main(inputLocation, cvDir, numberOfFolds);

			newGAArgs[0] = args[0];
			newGAArgs[1] = cvDir;
			newGAArgs[2] = outputLocation + "\\Results\\" + Integer.toString(i);
			TreeGrowthControl thisGAControl = new TreeGrowthControl(ctrl);
			new chc.InstanceSelection(newGAArgs, thisGAControl, weights);

//			for (int j = 0; j < numberOfFolds; j++)
//			{
//				System.out.format("\tFold : %d.\n", j);
//				newGAArgs[0] = cvDir + "\\" + Integer.toString(j) + "\\Train.txt";
//				newGAArgs[1] = cvDir + "\\" + Integer.toString(j) + "\\Test.txt";
//				newGAArgs[2] = outputLocation + "\\Results\\" + Integer.toString(i) + "\\" + Integer.toString(j);
//				TreeGrowthControl thisGAControl = new TreeGrowthControl(ctrl);
//				new chc.RegularInstanceSelection(newGAArgs, thisGAControl, weights);
//			}
		}

		gaAnalysis(inputLocation, outputLocation, ctrl, obsOfEachClass);
	}


	static void gaAnalysis(String inputLocation, String resultsDirLoc, TreeGrowthControl ctrl, Map<String, Integer> obsOfEachClass)
	{
		File inputFile = new File(inputLocation);
		if (!inputFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the data for feature selection.");
			System.exit(0);
		}

		File outputDirectory = new File(resultsDirLoc);
		if (!outputDirectory.isDirectory())
		{
			System.out.println("The second argument must be a valid directory location.");
			System.exit(0);
		}

		List<List<Integer>> bestIndices = new ArrayList<List<Integer>>();

		String cvDir = resultsDirLoc + "\\CV";
		File cvDirectory = new File(cvDir);
		String selectionDir = resultsDirLoc + "\\Results";
		for (String s : cvDirectory.list())
		{
				
			String bestIndividualsLoc = selectionDir + "\\" + s + "\\BestIndividuals.txt";
			List<List<Integer>> bestIndividualIndices = new ArrayList<List<Integer>>();
			try (BufferedReader reader = Files.newBufferedReader(Paths.get(bestIndividualsLoc), StandardCharsets.UTF_8))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					line = line.trim();
					line = line.replace("[", "");
					line = line.replace("]", "");
					if (line.trim().length() == 0)
					{
						// If the line is made up of all whitespace, then ignore the line.
						continue;
					}
					else if (line.contains("Fitness"))
					{
						// The line indicates the fitness of the individual, so skip it.
						continue;
					}
					String[] splitLine = line.split("\t");
					List<Integer> individual = new ArrayList<Integer>();
					for (String r : splitLine[0].split(", "))
					{
						individual.add(Integer.parseInt(r));
					}
					bestIndividualIndices.add(individual);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			bestIndices.addAll(bestIndividualIndices);
		}

		int numberObservations = 0;
		Map<Integer, Integer> timesIndexKept = new HashMap<Integer, Integer>();
		Map<Integer, String> indexToLineMap = new HashMap<Integer, String>();
		Map<String, Map<Integer, String>> observationIndexToLine = new HashMap<String, Map<Integer, String>>();
		String headerOne = "";
		String headerTwo = "";
		String headerThree = "";
		try
		{
			BufferedReader inputReader = new BufferedReader(new FileReader(inputLocation));
			headerOne = inputReader.readLine();
			int indexOfClassification = headerOne.split("\t").length - 1;
			headerTwo = inputReader.readLine();
			headerThree = inputReader.readLine();
			String line;
			while ((line = inputReader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				indexToLineMap.put(numberObservations, line);
				timesIndexKept.put(numberObservations, 0);
				String[] splitLine = (line.trim()).split("\t");
				String classification = splitLine[indexOfClassification];
				if (observationIndexToLine.containsKey(classification))
				{
					observationIndexToLine.get(classification).put(numberObservations, line);
				}
				else
				{
					observationIndexToLine.put(classification, new HashMap<Integer, String>());
					observationIndexToLine.get(classification).put(numberObservations, line);
				}
				numberObservations += 1;
			}
			inputReader.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		for (int i = 0; i < bestIndices.size(); i++)
		{
			for (Integer j : bestIndices.get(i))
			{
				timesIndexKept.put(j, timesIndexKept.get(j) + 1);
			}
		}

		for (double d = 1; d < 11; d++)
		{
			double observationFraction = d / 10.0;
			int numberOfOccurences = (int) Math.floor(observationFraction * bestIndices.size());  // Determine the number of times an observation must occur for it to be included in the dataset.
			try
			{
				String trainingOutputLocation = resultsDirLoc + "\\" + Integer.toString((int) (observationFraction * 100)) + "%TrainingObservationSet.txt";
				FileWriter trainingOutputFile = new FileWriter(trainingOutputLocation);
				BufferedWriter trainingOutputWriter = new BufferedWriter(trainingOutputFile);
				String testingOutputLocation = resultsDirLoc + "\\" + Integer.toString((int) (observationFraction * 100)) + "%TestingObservationSet.txt";
				FileWriter testingOutputFile = new FileWriter(testingOutputLocation);
				BufferedWriter testingOutputWriter = new BufferedWriter(testingOutputFile);
				trainingOutputWriter.write(headerOne);
				trainingOutputWriter.newLine();
				trainingOutputWriter.write(headerTwo);
				trainingOutputWriter.newLine();
				trainingOutputWriter.write(headerThree);
				trainingOutputWriter.newLine();
				testingOutputWriter.write(headerOne);
				testingOutputWriter.newLine();
				testingOutputWriter.write(headerTwo);
				testingOutputWriter.newLine();
				testingOutputWriter.write(headerThree);
				testingOutputWriter.newLine();
				for (Integer i : indexToLineMap.keySet())
				{
					if (timesIndexKept.get(i) >= numberOfOccurences)
					{
						trainingOutputWriter.write(indexToLineMap.get(i));
						trainingOutputWriter.newLine();
					}
					else
					{
						testingOutputWriter.write(indexToLineMap.get(i));
						testingOutputWriter.newLine();
					}
				}
				trainingOutputWriter.close();
				testingOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		try
		{
			String trainingDatasetOutputLocation = resultsDirLoc + "\\TrainingDatasetOfDesiredComposition.txt";
			FileWriter trainingDatasetOutputFile = new FileWriter(trainingDatasetOutputLocation);
			BufferedWriter trainingDatasetOutputWriter = new BufferedWriter(trainingDatasetOutputFile);
			String testingDatasetOutputLocation = resultsDirLoc + "\\TestingDatasetOfDesiredComposition.txt";
			FileWriter testingDatasetOutputFile = new FileWriter(testingDatasetOutputLocation);
			BufferedWriter testingDatasetOutputWriter = new BufferedWriter(testingDatasetOutputFile);
			trainingDatasetOutputWriter.write(headerOne);
			trainingDatasetOutputWriter.newLine();
			trainingDatasetOutputWriter.write(headerTwo);
			trainingDatasetOutputWriter.newLine();
			trainingDatasetOutputWriter.write(headerThree);
			trainingDatasetOutputWriter.newLine();
			testingDatasetOutputWriter.write(headerOne);
			testingDatasetOutputWriter.newLine();
			testingDatasetOutputWriter.write(headerTwo);
			testingDatasetOutputWriter.newLine();
			testingDatasetOutputWriter.write(headerThree);
			testingDatasetOutputWriter.newLine();
			for (String s : observationIndexToLine.keySet())
			{
				List<IndexedDoubleData> sortedIndices = new ArrayList<IndexedDoubleData>();
				for (Integer i : observationIndexToLine.get(s).keySet())
				{
					sortedIndices.add(new IndexedDoubleData(timesIndexKept.get(i), i));
				}
				Collections.sort(sortedIndices);
				Collections.reverse(sortedIndices);
				for (int i = 0; i < obsOfEachClass.get(s); i++)
				{
					trainingDatasetOutputWriter.write(indexToLineMap.get(sortedIndices.get(i).getIndex()));
					trainingDatasetOutputWriter.newLine();
				}
				for (int i = obsOfEachClass.get(s); i < observationIndexToLine.get(s).size(); i++)
				{
					testingDatasetOutputWriter.write(indexToLineMap.get(sortedIndices.get(i).getIndex()));
					testingDatasetOutputWriter.newLine();
				}
			}
			trainingDatasetOutputWriter.close();
			testingDatasetOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
