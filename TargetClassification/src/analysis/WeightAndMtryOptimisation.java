package analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

import datasetgeneration.CrossValidationFoldGenerationMultiClass;

import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class WeightAndMtryOptimisation
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
		String testFileLocation = null;  // The location of a dataset to test on the forests grown, but not to use in their growing.
		if (args.length >= 3)
		{
			// Only record an actual location if there are at least three argument supplied.
			testFileLocation = args[2];
		}
		main(inputFile, resultsDir, testFileLocation);
	}

	/**
	 * @param inputFile - The location of the dataset used to grow the forests.
	 * @param resultsDir - The location where the results and records of the optimisation will go.
	 * @param testFileLocation - The location of a dataset to test on the forests grown, but not to use in their growing.
	 */
	public static void main(String inputFile, String resultsDir, String testFileLocation)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfForestsToCreate = 100;  // The number of forests to create for each weight/mtry combination.
		int cvFoldsToUse = 10;  // The number of cross validation folds to use if cross validation is being used.
		Integer[] mtryToUse = {5, 10, 15, 20, 25, 30};  // The different values of mtry to test.
		Integer[] trainingObsToUse = {};  // The observations in the training set that will be used in growing the forests.

		// varyingClassWeightMapping can be used when the weights for a class should be varied, while constantClassWeightMapping can be used to assign a weight to
		// any class that will not have its weight varied.
		Map<String, Double[]> varyingClassWeightMapping = new HashMap<String, Double[]>();  // A mapping from class names to the weights that will be tested for the class.
		varyingClassWeightMapping.put("Positive", new Double[]{1.0, 2.0, 3.0});
		varyingClassWeightMapping.put("PossiblePositive", new Double[]{1.0, 2.0, 3.0});
		Map<String, Double> constantClassWeightMapping = new HashMap<String, Double>();  // A mapping from class names to the weight that will be used for the class.
		constantClassWeightMapping.put("Unlabelled", 1.0);

		// Default parameters for the tree growth and input dataset processing controller object.
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 1000;  // The number of trees in each forest.
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.isCalculateOOB = true;  // Set this to false to use cross-validation, or true to use OOB observations.
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
		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputFile, ctrl);

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
			String seondHeader = "\t\t\t\t\t\t\t\t\t";
			for (String s : classesInDataset)
			{
				resultsOutputWriter.write(s + "Weight\t");
				seondHeader += "\t";
			}
			resultsOutputWriter.write("Mtry\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tError\tTimeTakenPerRepetition(ms)\t");
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
			if (ctrl.isCalculateOOB)
			{
				parameterOutputWriter.write("CV not used");
				parameterOutputWriter.newLine();
			}
			else
			{
				parameterOutputWriter.write("CV used with " + Integer.toString(cvFoldsToUse) + " folds");
				parameterOutputWriter.newLine();
			}
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

		// Generate the cross validation folds if required.
		String cvFoldLocation = resultsDir + "/CVFolds-Repetition";
		if (!ctrl.isCalculateOOB)
		{
			for (int i = 0; i < numberOfForestsToCreate; i++)
			{
				String repCvFoldLoc = cvFoldLocation + Integer.toString(i);
				File cvFoldDir = new File(repCvFoldLoc);
				if (!cvFoldDir.exists())
				{
					boolean isDirCreated = cvFoldDir.mkdirs();
					if (!isDirCreated)
					{
						System.out.println("The CV fold directory does not exist, and could not be created.");
						System.exit(0);
					}
				}
				CrossValidationFoldGenerationMultiClass.main(inputFile, repCvFoldLoc, cvFoldsToUse);
			}
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

			boolean isJustStarted = true;  // Determines whether the analysis of this mtry has just begun.
										   // Used as the termination condition is the same as the starting condition (all values in classWeightIndexMapping == 0).

			while (!terminationReached(classWeightIndexMapping) || isJustStarted)
			{
				System.out.print("\tNow working on weights : ");
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

				TestRunner runner = new TestRunner();
				if (ctrl.isCalculateOOB)
				{
					// If cross validation is not being used.
					runner.generateForestsNoCV(weights, ctrl, inputFile, seeds, numberOfForestsToCreate, testFileLocation);
				}
				else
				{
					// If cross validation is being used.
					runner.generateForestsWithCV(weights, ctrl, cvFoldLocation, inputFile, seeds, numberOfForestsToCreate, cvFoldsToUse);
				}
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
