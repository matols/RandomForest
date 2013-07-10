package analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tree.PUProcessDataForGrowing;
import tree.PUTreeGrowthControl;

public class PUWeightAndMtryOptimisation
{

	/**
	 * Used in the optimisation of the mtry parameter and the weights of the individual classes.
	 * 
	 * @param args - The file system locations of the files and directories used in the optimisation.
	 */
	public static void main(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset used to grow the forests.
		String resultsDir = args[1];  // The location where the results and records of the optimisation will go.
		String discountLoc = args[2];  // The location of a dataset containing the information to sue for the discounting.
		
		Map<String, Map<Integer, Double>> discounts = new HashMap<String, Map<Integer, Double>>();
		discounts.put("Positive", new HashMap<Integer, Double>());
		discounts.put("Unlabelled", new HashMap<Integer, Double>());

		Map<String, Integer> indexMapping = new HashMap<String, Integer>();
		Path dataPath = Paths.get(inputFile);
		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = null;
			line = reader.readLine();
			line = reader.readLine();
			line = reader.readLine();
			int index = 0;
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.trim();
				String[] splitLine = line.split("\t");
				String acc = splitLine[0];
				indexMapping.put(acc, index);
				index++;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		dataPath = Paths.get(discountLoc);
		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = null;
			line = reader.readLine();
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.trim();
				String[] splitLine = line.split("\t");
				if (splitLine[3].equals("Unlabelled"))
				{
					String acc = splitLine[0];
					double posWeight = Double.parseDouble(splitLine[1]);
					double unlabWeight = Double.parseDouble(splitLine[2]);
					double posFrac = posWeight / (posWeight + unlabWeight);
					discounts.get("Positive").put(indexMapping.get(acc), posFrac);
					discounts.get("Unlabelled").put(indexMapping.get(acc), 1 - posFrac);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		String testFileLocation = null;  // The location of a dataset to test on the forests grown, but not to use in their growing.
		if (args.length >= 4)
		{
			// Only record an actual location if there are at least three argument supplied.
			testFileLocation = args[3];
		}

		main(inputFile, resultsDir, discounts, testFileLocation);
	}

	/**
	 * @param inputFile - The location of the dataset used to grow the forests.
	 * @param resultsDir - The location where the results and records of the optimisation will go.
	 * @param discounts - The values to discount the positive and unlabelled weight with.
	 * @param testFileLocation - The location of a dataset to test on the forests grown, but not to use in their growing.
	 */
	public static void main(String inputFile, String resultsDir, Map<String, Map<Integer, Double>> discounts, String testFileLocation)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfForestsToCreate = 100;  // The number of forests to create for each weight/mtry combination.
		Integer[] mtryToUse = {5, 10, 15, 20, 25, 30};  // The different values of mtry to test.
		Double[] posFracLimitsToUse = {0.5, 0.6, 0.7, 0.75, 0.8};  // The different values of positiveFractionTerminalCutoff to use.
		Integer[] trainingObsToUse = {};  // The observations in the training set that will be used in growing the forests.

		// varyingClassWeightMapping can be used when the weights for a class should be varied, while constantClassWeightMapping can be used to assign a weight to
		// any class that will not have its weight varied.
		Map<String, Double[]> varyingClassWeightMapping = new HashMap<String, Double[]>();  // A mapping from class names to the weights that will be tested for the class.
		varyingClassWeightMapping.put("Unlabelled", new Double[]{1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0});
		Map<String, Double> constantClassWeightMapping = new HashMap<String, Double>();  // A mapping from class names to the weight that will be used for the class.
		constantClassWeightMapping.put("Positive", 1.0);

		// Default parameters for the tree growth and input dataset processing controller object.
		PUTreeGrowthControl ctrl = new PUTreeGrowthControl();
		ctrl.numberOfTreesToGrow = 1000;  // The number of trees in each forest.
		ctrl.minNodeSize = 1;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
		if (!resultsDirectory.exists())
		{
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The results directory could not be created.");
				System.exit(0);
			}
		}
		else if (!resultsDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
			System.exit(0);
		}

		// Process the input dataset.
		PUProcessDataForGrowing procData = new PUProcessDataForGrowing(inputFile, ctrl);

		// Determine the classes in the input dataset.
		List<String> classesInDataset = new ArrayList<String>(new HashSet<String>(procData.responseData));
		Collections.sort(classesInDataset);

		// Initialise the results, parameters and controller object record files.
		String resultsLocation = resultsDir + "/Results.txt";
		String parameterLocation = resultsDir + "/Parameters.txt";
		String controllerLocation = resultsDir + "/ControllerUsed.txt";
		try
		{
			// Setup the results file.
			FileWriter resultsOutputFile = new FileWriter(resultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			String seondHeader = "\t\t\t\t\t\t\t\t\t\t";
			for (String s : classesInDataset)
			{
				resultsOutputWriter.write(s + "Weight\t");
				seondHeader += "\t";
			}
			resultsOutputWriter.write("PositiveCutoff\tMtry\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tError\tTimeTakenPerRepetition(ms)\t");
			for (String s : classesInDataset)
			{
				resultsOutputWriter.write(s + "\t\t");
			}
			resultsOutputWriter.newLine();
			resultsOutputWriter.write(seondHeader);
			for (String s : classesInDataset)
			{
				resultsOutputWriter.write("True\tFalse\t");
			}
			resultsOutputWriter.newLine();
			resultsOutputWriter.close();

			// Record the parameters.
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Number of forests grown - " + Integer.toString(numberOfForestsToCreate));
			parameterOutputWriter.newLine();
			if (testFileLocation == null)
			{
				parameterOutputWriter.write("No test set used");
			}
			else
			{
				parameterOutputWriter.write("A set is used.");
			}
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used");
			parameterOutputWriter.newLine();
			for (String s : constantClassWeightMapping.keySet())
			{
				parameterOutputWriter.write("\t" + s + " - " + Double.toString(constantClassWeightMapping.get(s)));
				parameterOutputWriter.newLine();
			}
			for (String s : varyingClassWeightMapping.keySet())
			{
				parameterOutputWriter.write("\t" + s + " - " + Arrays.toString(varyingClassWeightMapping.get(s)));
				parameterOutputWriter.newLine();
			}
			parameterOutputWriter.write("Mtry used - " + Arrays.toString(mtryToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Positive fraction cutoffs used - " + Arrays.toString(posFracLimitsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Training observations used - " + Arrays.toString(trainingObsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();

			ctrl.save(controllerLocation);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Generate all the random seeds to use in growing the forests. The same numberOfForestsToCreate seeds will be used for every weight/mtry
		// combination. This ensures that the only difference in the results is due to the chosen weight/mtry combination.
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

		// Determine a fixed ordering for the classes that are going to have their weight varied.
		List<String> orderedClassesWithVaryingWeights = new ArrayList<String>(varyingClassWeightMapping.keySet());
		Collections.sort(orderedClassesWithVaryingWeights);

		// Initialise the maps for controlling the optimisation.
		Map<String, Integer> classWeightIndexMapping = new HashMap<String, Integer>();  // Maps each class s to the index of the weight in varyingClassWeightMapping.get(s) currently assigned to it.
		Map<String, Integer> finalWeightIndexMapping = new HashMap<String, Integer>();  // Maps each class to the maximum possible index it can take.
		for (String s : orderedClassesWithVaryingWeights)
		{
			classWeightIndexMapping.put(s, 0);
			finalWeightIndexMapping.put(s, varyingClassWeightMapping.get(s).length - 1);
		}

		Map<String, Double> weights = new HashMap<String, Double>();  // The weight mapping that will actually be used when growing the forest.
		for (String s : constantClassWeightMapping.keySet())
		{
			weights.put(s, constantClassWeightMapping.get(s));
		}

		for (Integer mtry : mtryToUse)
		{
			System.out.format("Now working on mtry - %d.\n", mtry);
			
			ctrl.mtry = mtry;

			for (double positiveCutoff : posFracLimitsToUse)
			{
				System.out.format("\tNow working on positive cutoff - %f.\n", positiveCutoff);

				ctrl.positiveFractionTerminalCutoff = positiveCutoff;
	
				boolean isJustStarted = true;  // Determines whether the analysis of this mtry has just begun.
											   // Used as the termination condition is the same as the starting condition (all values in classWeightIndexMapping == 0).
	
				while (!terminationReached(classWeightIndexMapping) || isJustStarted)
				{
					System.out.print("\t\tNow working on weights : ");
					for (String s : classWeightIndexMapping.keySet())
					{
						Double classWeight = varyingClassWeightMapping.get(s)[classWeightIndexMapping.get(s)];
						weights.put(s, classWeight);
						System.out.print(s + "-" + Double.toString(classWeight) + "\t");
					}
					DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				    Date startTime = new Date();
				    String strDate = sdfDate.format(startTime);
				    System.out.format("at %s.\n", strDate);
	
					isJustStarted = false;
	
					PUTestRunner runner = new PUTestRunner();
					runner.generateForests(weights, ctrl, inputFile, seeds, numberOfForestsToCreate, testFileLocation, discounts);
					long timeTaken = runner.getTimeTaken();
					timeTaken /= (double) numberOfForestsToCreate;
					Map<String, Double> statistics = runner.calculateAggregateStats();  // The record of the statistics of the predictions.
					Map<String, Map<String, Double>> confusionMatrix = runner.getAggregateConfMat();  // The aggregate confusion matrix of the predictions.
	
					// Write out the statistics for this mtry/weight combination.
					try
					{
						FileWriter resultsOutputFile = new FileWriter(resultsLocation, true);
						BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
						for (String s : classesInDataset)
						{
							resultsOutputWriter.write(String.format("%.5f", weights.get(s)));
							resultsOutputWriter.write("\t");
						}
						resultsOutputWriter.write(Double.toString(positiveCutoff));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(Integer.toString(ctrl.mtry));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(String.format("%.5f", statistics.get("GMean")));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(String.format("%.5f", statistics.get("MCC")));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(String.format("%.5f", statistics.get("F0.5")));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(String.format("%.5f", statistics.get("F1")));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(String.format("%.5f", statistics.get("F2")));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(String.format("%.5f", 1 - statistics.get("ErrorRate")));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(String.format("%.5f", statistics.get("ErrorRate")));
						resultsOutputWriter.write("\t");
						resultsOutputWriter.write(Long.toString(timeTaken));
						resultsOutputWriter.write("\t");
						for (String s : classesInDataset)
						{
							resultsOutputWriter.write(Double.toString(confusionMatrix.get(s).get("TruePositive")));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(Double.toString(confusionMatrix.get(s).get("FalsePositive")));
							resultsOutputWriter.write("\t");
						}
						resultsOutputWriter.newLine();
						resultsOutputWriter.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						System.exit(0);
					}
	
					classWeightIndexMapping = updateWeightIndices(classWeightIndexMapping, finalWeightIndexMapping, orderedClassesWithVaryingWeights);
				}
			}
		}
	}

	/**
	 * Checks whether all indices are equal to 0.
	 * 
	 * @param classWeightIndexMapping - Maps each class s to the index of the weight in varyingClassWeightMapping.get(s) currently assigned to it.
	 * @return - true if all weight combinations have been explored, else false.
	 */
	public static boolean terminationReached(Map<String, Integer> classWeightIndexMapping)
	{
		boolean isTerminate = true;
		for (String s : classWeightIndexMapping.keySet())
		{
			// Terminate only if all indices are 0.
			isTerminate = isTerminate && (classWeightIndexMapping.get(s) == 0);
		}
		return isTerminate;
	}

	/**
	 * Updates the mapping from the classes to the indices into their weight vectors.
	 * 
	 * Increments the indices as a binary register would increment itself. If you have three classes ordered A, B, C such that A has 3 possible weights,
	 * B has 2 possible weights and C has 4 possible weights, then a few example updates of the indices are as follows:
	 * 		start : A->2 (max 2)	A->0	A->2	A->2
	 * 				B->0 (max 1)	B->0	B->1	B->1
	 * 				C->1 (max 3)	C->0	C->0	C->3
	 * 
	 * 		end :	A->0			A->1	A->0	A->0
	 * 				B->1			B->0	B->0	B->0
	 * 				C->1			C->0	C->1	C->0
	 * The last update (A,B,C going from 2,1,3 to 0,0,0) indicates that all the possible index combinations have been used, and that the termination condition
	 * will be satisfied.
	 * 
	 * @param classWeightIndexMapping - Maps each class s to the index of the weight in varyingClassWeightMapping.get(s) currently assigned to it.
	 * @param finalWeightIndexMapping - Maps each class to the maximum possible index it can take.
	 * @return - A mapping from the classes to the indices into their weight vectors.
	 */
	public static Map<String, Integer> updateWeightIndices(Map<String, Integer> classWeightIndexMapping, Map<String, Integer> finalWeightIndexMapping,
			List<String> orderedClassesWithVaryingWeights)
	{
		for (String s : orderedClassesWithVaryingWeights)
		{
			if (classWeightIndexMapping.get(s) == finalWeightIndexMapping.get(s))
			{
				// If the 
				classWeightIndexMapping.put(s, 0);
			}
			else
			{
				classWeightIndexMapping.put(s, classWeightIndexMapping.get(s) + 1);
				break;
			}
		}
		return  classWeightIndexMapping;
	}

}