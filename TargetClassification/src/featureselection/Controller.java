/**
 * 
 */
package featureselection;

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
import java.util.Random;

import datasetgeneration.CrossValidationFoldGeneration;

import tree.Forest;
import tree.ImmutableTwoValues;
import tree.IndexedDoubleData;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class Controller
{

	public Controller()
	{
	}

	public Controller(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights)
	{
		recursiveFeatureElimination(args, ctrl, weights);
	}

	public Controller(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights, boolean isVarImpOnlyUsed)
	{
		if (isVarImpOnlyUsed)
		{
			varImpOnlySelection(args, ctrl, weights);
		}
		else
		{
			recursiveFeatureElimination(args, ctrl, weights);
		}
	}

	public Controller(String[] args, TreeGrowthControl ctrl, int gaRepetitions, boolean isXValUsed, Map<String, Double> weights)
	{
		gaSelection(args, ctrl, gaRepetitions, isXValUsed, weights);
	}

	void gaSelection(String[] args, TreeGrowthControl ctrl, int gaRepetitions, boolean isXValUsed, Map<String, Double> weights)
	{

		// Required inputs.
		String inputLocation = args[0];  // The location of the file containing the data to use in the feature selection.
		File inputFile = new File(inputLocation);
		if (!inputFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the data for feature selection.");
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
		String inputCrossValLocation = args[2];
		File inputCrossValDirectory = new File(inputCrossValLocation);
		if (!inputCrossValDirectory.isDirectory())
		{
			System.out.println("The third argument must be the location of the directory containing the cross validation files.");
			System.exit(0);
		}

		// Optional inputs.
		int maxGenerations = 100;  // The number of generations to run the GA for.
		int maxEvaluations = 0;  // The maximum number of fitness evaluations to perform.
		int maxStagnantGenerations = -1;  // The maximum number of generations to go without fitness change;

		// Read in the user input.
		int argIndex = 3;
		while (argIndex < args.length)
		{
			String currentArg = args[argIndex];
			switch (currentArg)
			{
			case "-p":
				argIndex += 2;
				break;
			case "-g":
				argIndex += 1;
				 maxGenerations = Integer.parseInt(args[argIndex]);
				 argIndex += 1;
				break;
			case "-e":
				argIndex += 1;
				maxEvaluations = Integer.parseInt(args[argIndex]);
				argIndex += 1;
				break;
			case "-r":
				argIndex += 2;
				break;
			case "-a":
				argIndex += 2;
				break;
			case "-m":
				argIndex += 2;
				break;
			case "-s":
				argIndex += 1;
				maxStagnantGenerations = Integer.parseInt(args[argIndex]);
				argIndex += 1;
				break;
			case "-v":
				argIndex += 1;
				break;
			default:
				System.out.format("Unexpeted argument : %s.\n", currentArg);
				System.exit(0);
			}
		}

		if (maxGenerations == 0 && maxEvaluations == 0 && maxStagnantGenerations < 0)
		{
	        // No stopping criteria given.
	        System.out.println("At least one of -g, -e or -s must be given, otherwise there are no stopping criteria.");
	        System.exit(0);
		}

		// Run the GA multiple times.
		for (int i = 0; i < gaRepetitions; i++)
		{
			System.out.format("GA round : %d.\n", i);
			String thisGAArgs[] = args.clone();
			thisGAArgs[1] += "/" + Integer.toString(i);
			TreeGrowthControl thisGAControl = new TreeGrowthControl(ctrl);
			if (isXValUsed)
			{
				new steadystate.CrossValController(thisGAArgs, thisGAControl, weights);
			}
			else
			{
				new steadystate.Controller(thisGAArgs, thisGAControl, weights);
			}
		}

		gaAnalysis(args[0], args[1], ctrl);

	}

	void recursiveFeatureElimination(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights)
	{
		String inputLocation = args[0];  // The location of the file containing the data to use in the feature selection.
		File inputFile = new File(inputLocation);
		if (!inputFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the data for feature selection.");
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
		String externalCVDir = outputLocation + "/ExternalCV";
		File externalCVDirectory = new File(externalCVDir);
		if (!externalCVDirectory.exists())
		{
			boolean isDirCreated = externalCVDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The external cross validation directory could not be created.");
				System.exit(0);
			}
		}
		else if (!externalCVDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("ERROR: The output directory contains a non-directory called ExternalCV.");
			System.exit(0);
		}
		String internalCVDir = outputLocation + "/InternalCV";
		File internalCVDirectory = new File(internalCVDir);
		if (!internalCVDirectory.exists())
		{
			boolean isDirCreated = internalCVDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The internal cross validation directory could not be created.");
				System.exit(0);
			}
		}
		else if (!internalCVDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("ERROR: The output directory contains a non-directory called InternalCV.");
			System.exit(0);
		}

		ProcessDataForGrowing fullDataset = new ProcessDataForGrowing(inputLocation, ctrl);
		String negClass = "Unlabelled";
		String posClass = "Positive";

		int externalRepetitions = 10;
		int externalFolds = 10;
		int internalRepetitions = 50;
		int internalFolds = 10;
		// Write out the parameters.
		String parameterLocation = outputLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("External Repetitions - " + Integer.toString(externalRepetitions));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("External Folds - " + Integer.toString(externalFolds));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Internal Repetitions - " + Integer.toString(internalRepetitions));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Internal Folds - " + Integer.toString(internalFolds));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		ctrl.save(outputLocation + "/RandomForestCtrl.txt");

		List<Double> overallMCC = new ArrayList<Double>();
		List<Double> overallErrorRate = new ArrayList<Double>();
		List<List<String>> bestSubsets = new ArrayList<List<String>>();
		// Determine the seeds that will be used.
		Random randGen = new Random();
		List<Long> seedsUsed = new ArrayList<Long>();

		for (int exRep = 0; exRep < externalRepetitions; exRep++)
		{
			// Generate external cross validation folds.
			CrossValidationFoldGeneration.main(inputLocation, externalCVDir, externalFolds);

			for (int exFold = 0; exFold < externalFolds; exFold++)
			{
				Map<Integer, Double> externalMCC = new HashMap<Integer, Double>();
				for (int i = 1; i <= fullDataset.covariableData.size(); i++)
				{
					externalMCC.put(i, 0.0);
				}

				for (int inRep = 0; inRep < internalRepetitions; inRep++)
				{
					// Generate the internal cross validation folds.
					CrossValidationFoldGeneration.main(externalCVDir + "\\" + Integer.toString(exFold) + "\\Train.txt", internalCVDir, internalFolds);

					for (int inFold = 0; inFold < internalFolds; inFold++)
					{
						Long seedToUse = randGen.nextLong();
						while (seedsUsed.contains(seedToUse))
						{
							// Ensure a unique seed each time.
							seedToUse = randGen.nextLong();
						}
						seedsUsed.add(seedToUse);

						TreeGrowthControl tempCtrl = new TreeGrowthControl(ctrl);
						ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predictionResults;
						Forest forest;
						ProcessDataForGrowing testData = new ProcessDataForGrowing(internalCVDir + "\\" + Integer.toString(inFold) + "\\Test.txt", tempCtrl);

						forest = new Forest(internalCVDir + "\\" + Integer.toString(inFold) + "\\Train.txt", tempCtrl, weights, seedToUse);
						predictionResults = forest.predict(testData);
						double MCC = calcMCC(posClass, negClass, predictionResults.second, fullDataset.responseData);  // Calculate the MCC.
						double oldMCC = externalMCC.get(fullDataset.covariableData.size());
						externalMCC.put(fullDataset.covariableData.size(), oldMCC + Math.abs(MCC));

						// Determine the importance ordering for the variables.
						Map<String, Double> varImp = forest.variableImportance();
						List<StringsSortedByDoubles> sortedVariables = new ArrayList<StringsSortedByDoubles>();
						for (String s : varImp.keySet())
						{
							sortedVariables.add(new StringsSortedByDoubles(varImp.get(s), s));
						}
						Collections.sort(sortedVariables);

						for (int i = 0; i < varImp.size() - 1; i++)
						{
							tempCtrl.variablesToIgnore.add(sortedVariables.get(i).getId());
							forest.regrowForest(tempCtrl);
							predictionResults = forest.predict(testData);
							MCC = calcMCC(posClass, negClass, predictionResults.second, fullDataset.responseData);  // Calculate the MCC.
							oldMCC = externalMCC.get(fullDataset.covariableData.size() - (i + 1));
							externalMCC.put(fullDataset.covariableData.size() - (i + 1), oldMCC + Math.abs(MCC));
						}
					}
				}

				// Find subset size F that gives the greatest absolute MCC.
				List<IndexedDoubleData> sortedSubsetSizes = new ArrayList<IndexedDoubleData>();
				for (Integer i : externalMCC.keySet())
				{
					sortedSubsetSizes.add(new IndexedDoubleData(externalMCC.get(i) / (internalFolds * internalRepetitions), i));
				}
				Collections.sort(sortedSubsetSizes);
				Collections.reverse(sortedSubsetSizes);
				int bestSubsetSize = sortedSubsetSizes.get(0).getIndex();

				// Train a forest on all the training data, and select the F most important features to return as the feature subset for these repetitions.
				Forest forest = new Forest(externalCVDir + "\\" + Integer.toString(exFold) + "\\Train.txt", ctrl, weights);
				Map<String, Double> varImp = forest.variableImportance();
				List<StringsSortedByDoubles> sortedVariables = new ArrayList<StringsSortedByDoubles>();
				for (String s : varImp.keySet())
				{
					sortedVariables.add(new StringsSortedByDoubles(varImp.get(s), s));
				}
				Collections.sort(sortedVariables);

				List<String> chosenSubset = new ArrayList<String>();
				for (int i = 0; i < bestSubsetSize; i++)
				{
					chosenSubset.add(sortedVariables.get(i).getId());
				}
				TreeGrowthControl tempCtrl = new TreeGrowthControl(ctrl);
				for (int i = bestSubsetSize; i < varImp.size(); i++)
				{
					tempCtrl.variablesToIgnore.add(sortedVariables.get(i).getId());
				}
				forest.regrowForest(tempCtrl);

				ProcessDataForGrowing testData = new ProcessDataForGrowing(externalCVDir + "\\" + Integer.toString(exFold) + "\\Test.txt", ctrl);
				ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predictionResults = forest.predict(testData);
				double MCC = calcMCC(posClass, negClass, predictionResults.second, fullDataset.responseData);  // Calculate the MCC.

				overallErrorRate.add(predictionResults.first);
				overallMCC.add(MCC);
				bestSubsets.add(chosenSubset);
			}
		}

		// Get the names and types of the features (the types are so that you know which features are the response and which aren't used).
		String featureNames[] = null;
		String featureTypes[] = null;
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputLocation), StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			featureNames = line.split("\t");

			line = reader.readLine();
			line = line.toLowerCase();
			line = line.replaceAll("\n", "");
			featureTypes = line.split("\t");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the features that are used in the GAs.
		List<String> featuresUsed = new ArrayList<String>();
		for (int i = 0; i < featureNames.length; i++)
		{
			if (featureTypes[i].equals("x") || featureTypes[i].equals("r"))
			{
				// If the feature is a response variable or is to be skipped.
				continue;
			}
			featuresUsed.add(featureNames[i]);
		}

		// Write out the results.
		String errorRatesLocation = outputLocation + "/ErrorRates.txt";
		try
		{
			FileWriter errorRatesOutputFile = new FileWriter(errorRatesLocation);
			BufferedWriter errorRatesOutputWriter = new BufferedWriter(errorRatesOutputFile);
			for (Double d : overallErrorRate)
			{
				errorRatesOutputWriter.write(Double.toString(d));
				errorRatesOutputWriter.newLine();
			}
			errorRatesOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		String MCCsLocation = outputLocation + "/MCCs.txt";
		try
		{
			FileWriter MCCsOutputFile = new FileWriter(MCCsLocation);
			BufferedWriter MCCsOutputWriter = new BufferedWriter(MCCsOutputFile);
			for (Double d : overallMCC)
			{
				MCCsOutputWriter.write(Double.toString(d));
				MCCsOutputWriter.newLine();
			}
			MCCsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		String featureFractionsLocation = outputLocation + "/FeatureSubsets.txt";
		try
		{
			FileWriter featureFractionsOutputFile = new FileWriter(featureFractionsLocation);
			BufferedWriter featureFractionsOutputWriter = new BufferedWriter(featureFractionsOutputFile);
			for (String s : featuresUsed)
			{
				featureFractionsOutputWriter.write(s);
				for (List<String> subset : bestSubsets)
				{
					if (subset.contains(s))
					{
						featureFractionsOutputWriter.write("\t1");
					}
					else
					{
						featureFractionsOutputWriter.write("\t0");
					}
				}
				featureFractionsOutputWriter.newLine();
			}
			featureFractionsOutputWriter.newLine();
			featureFractionsOutputWriter.write("MCC");
			for (Double mcc : overallMCC)
			{
				featureFractionsOutputWriter.write("\t" + Double.toString(mcc));
			}
			featureFractionsOutputWriter.newLine();
			featureFractionsOutputWriter.write("ErrorRate");
			for (Double error : overallErrorRate)
			{
				featureFractionsOutputWriter.write("\t" + Double.toString(error));
			}
			featureFractionsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	void varImpOnlySelection(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights)
	{
		String inputLocation = args[0];  // The location of the file containing the data to use in the feature selection.
		File inputFile = new File(inputLocation);
		if (!inputFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the data for feature selection.");
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

		int repetitions = 200;
		// Write out the parameters.
		String parameterLocation = outputLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Repetitions - " + Integer.toString(repetitions));
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		ctrl.save(outputLocation + "/RandomForestCtrl.txt");

		// Get the names and types of the features (the types are so that you know which features are the response and which aren't used).
		String featureNames[] = null;
		String featureTypes[] = null;
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputLocation), StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			featureNames = line.split("\t");

			line = reader.readLine();
			line = line.toLowerCase();
			line = line.replaceAll("\n", "");
			featureTypes = line.split("\t");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the features that are used in the GAs.
		List<String> featuresUsed = new ArrayList<String>();
		for (int i = 0; i < featureNames.length; i++)
		{
			if (featureTypes[i].equals("x") || featureTypes[i].equals("r"))
			{
				// If the feature is a response variable or is to be skipped.
				continue;
			}
			featuresUsed.add(featureNames[i]);
		}

		// Setup the record of the average importance ranking.
		Map<String, List<Integer>> importanceRanking = new HashMap<String, List<Integer>>();
		for (String s : featuresUsed)
		{
			importanceRanking.put(s, new ArrayList<Integer>());
		}
		ProcessDataForGrowing inputData = new ProcessDataForGrowing(inputLocation, ctrl);
		Map<Integer, Map<Integer, Double>> proximities = new HashMap<Integer, Map<Integer, Double>>();
		for (int i = 0; i < inputData.numberObservations; i++)
		{
			Map<Integer, Double> proxims = new HashMap<Integer, Double>();
			for (int j = 0; j < inputData.numberObservations; j++)
			{
				proxims.put(j, 0.0);
			}
			proximities.put(i, proxims);
		}

		// Determine the seeds that will be used.
		Random randGen = new Random();
		List<Long> seedsUsed = new ArrayList<Long>();

		for (int i = 0; i < repetitions; i++)
		{
			System.out.format("Now working on repetition - %d\n", i);

			Long seedToUse = randGen.nextLong();
			while (seedsUsed.contains(seedToUse))
			{
				// Ensure a unique seed each time.
				seedToUse = randGen.nextLong();
			}
			seedsUsed.add(seedToUse);
			Forest forest = new Forest(inputLocation, ctrl, weights, seedToUse);
			Map<String, Double> varImp = forest.variableImportance();
			Map<Integer, Map<Integer, Double>> prox = forest.calculatProximities();
			for (int j : prox.keySet())
			{
				Map<Integer, Double> currentProx = proximities.get(j);
				Map<Integer, Double> newProx = prox.get(j);
				for (int k : prox.get(j).keySet())
				{
					currentProx.put(k, currentProx.get(k) + newProx.get(k));
				}
			}

			// Determine the importance ordering for the variables, largest importance first.
			List<StringsSortedByDoubles> sortedVariables = new ArrayList<StringsSortedByDoubles>();
			for (String s : varImp.keySet())
			{
				sortedVariables.add(new StringsSortedByDoubles(varImp.get(s), s));
			}
			Collections.sort(sortedVariables);
			Collections.reverse(sortedVariables);

			for (int j = 0; j < varImp.size(); j++)
			{
				String featureImp = sortedVariables.get(j).getId();
				importanceRanking.get(featureImp).add(j + 1);
			}
		}

		// Normalise the proximities.
		for (int j : proximities.keySet())
		{
			Map<Integer, Double> currentProx = proximities.get(j);
			for (int k : currentProx.keySet())
			{
				currentProx.put(k, currentProx.get(k) / repetitions);
			}
		}

		// Write out the results.
		String varImpLocation = outputLocation + "/VariableImportances.txt";
		try
		{
			FileWriter varImpOutputFile = new FileWriter(varImpLocation);
			BufferedWriter varImpOutputWriter = new BufferedWriter(varImpOutputFile);
			for (String s : featuresUsed)
			{
				varImpOutputWriter.write(s);
				for (Integer i : importanceRanking.get(s))
				{
					varImpOutputWriter.write("\t" + Integer.toString(i));
				}
				varImpOutputWriter.newLine();
			}
			varImpOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		String proximitiesLocation = outputLocation + "/Proximities.txt";
		try
		{
			FileWriter proximitiesOutputFile = new FileWriter(proximitiesLocation);
			BufferedWriter proximitiesOutputWriter = new BufferedWriter(proximitiesOutputFile);
			for (int i = 0; i < inputData.numberObservations; i++)
			{
				proximitiesOutputWriter.write("\t" + Integer.toString(i));
			}
			proximitiesOutputWriter.newLine();
			for (int i = 0; i < inputData.numberObservations; i++)
			{
				proximitiesOutputWriter.write(Integer.toString(i));
				for (int j = 0; j < inputData.numberObservations; j++)
				{
					proximitiesOutputWriter.write("\t" + Double.toString(proximities.get(i).get(j)));
				}
				proximitiesOutputWriter.newLine();
			}
			proximitiesOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		String seedsLocation = outputLocation + "/SeedsUsed.txt";
		try
		{
			FileWriter seedsOutputFile = new FileWriter(seedsLocation);
			BufferedWriter seedsOutputWriter = new BufferedWriter(seedsOutputFile);
			for (Long l : seedsUsed)
			{
				seedsOutputWriter.write(Long.toString(l));
				seedsOutputWriter.newLine();
			}
			seedsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	double calcMCC(String posClass, String negClass, Map<String, Map<String, Double>> confMatrix, List<String> responseData)
	{
		Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
		for (String s : new HashSet<String>(responseData))
		{
			confusionMatrix.put(s, new HashMap<String, Double>());
			confusionMatrix.get(s).put("TruePositive", 0.0);
			confusionMatrix.get(s).put("FalsePositive", 0.0);
		}
		for (String s : confMatrix.keySet())
		{
			Double oldTruePos = confusionMatrix.get(s).get("TruePositive");
			Double newTruePos = oldTruePos + confMatrix.get(s).get("TruePositive");
			confusionMatrix.get(s).put("TruePositive", newTruePos);
			Double oldFalsePos = confusionMatrix.get(s).get("FalsePositive");
			Double newFalsePos = oldFalsePos + confMatrix.get(s).get("FalsePositive");
			confusionMatrix.get(s).put("FalsePositive", newFalsePos);
		}
		Double TP = confusionMatrix.get(posClass).get("TruePositive");
		Double FP = confusionMatrix.get(posClass).get("FalsePositive");
		Double TN = confusionMatrix.get(negClass).get("TruePositive");
		Double FN = confusionMatrix.get(negClass).get("FalsePositive");
		Double MCC = (((TP * TN)  - (FP * FN)) / Math.sqrt((TP + FP) * (TP + FN) * (TN + FP) * (TN + FN)));

		return MCC;
	}

	/**
	 * @param inputLocation
	 * @param resultDirLoc - directory containing results of the multiple GA runs for one dataset - must only contain the directories to use, no extra files or directories
	 * @param ctrl - the TreeGrowthCotrol object used to run the GA (or an equivalent one)
	 */
	void gaAnalysis(String inputLocation, String resultsDirLoc, TreeGrowthControl ctrl)
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

		// Get the best individuals from the GA runs.
		List<Integer[]> bestIndividuals = new ArrayList<Integer[]>();
		for (String s : outputDirectory.list())
		{
			String bestIndivLocation = resultsDirLoc + "/" + s + "/BestIndividuals.txt";
			try (BufferedReader reader = Files.newBufferedReader(Paths.get(bestIndivLocation), StandardCharsets.UTF_8))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					line = line.replaceAll("\n", "");
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
					List<Integer> individual = new ArrayList<Integer>();
					String[] splitLine = line.split("\t");
					for (String p : splitLine[0].split(","))
					{
						individual.add(Integer.parseInt(p));
					}
					bestIndividuals.add(individual.toArray(new Integer[individual.size()]));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		// Get the names and types of the features (the types are so that you know which features are the response and which aren't used).
		String featureNames[] = null;
		String featureTypes[] = null;
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputLocation), StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			featureNames = line.split("\t");

			line = reader.readLine();
			line = line.toLowerCase();
			line = line.replaceAll("\n", "");
			featureTypes = line.split("\t");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the features that are used in the GAs.
		List<String> featuresUsed = new ArrayList<String>();
		for (int i = 0; i < featureNames.length; i++)
		{
			if (featureTypes[i].equals("x") || featureTypes[i].equals("r"))
			{
				// If the feature is a response variable or is to be skipped.
				continue;
			}
			featuresUsed.add(featureNames[i]);
		}

		// Generate the output matrix.
		List<Integer> featureSums = new ArrayList<Integer>();
		try
		{
			String matrixOutputLocation = resultsDirLoc + "/MatrixOutput.txt";
			FileWriter matrixOutputFile = new FileWriter(matrixOutputLocation);
			BufferedWriter matrixOutputWriter = new BufferedWriter(matrixOutputFile);
			for (int i = 0; i < featuresUsed.size(); i++)
			{
				// Write out the feature name.
				matrixOutputWriter.write(featuresUsed.get(i));
				matrixOutputWriter.write("\t");
				// Write out the presence (1)/absence (0) of the feature for the given run, and sum up the occurrences
				// of the feature in the runs.
				int featureOccurreces = 0;
				for (int j = 0; j < bestIndividuals.size(); j++)
				{
					int individualsValue = bestIndividuals.get(j)[i];
					featureOccurreces += individualsValue;
					matrixOutputWriter.write(Integer.toString(individualsValue));
					matrixOutputWriter.write("\t");
				}
				double featureFractions = ((double) featureOccurreces) / bestIndividuals.size();
				matrixOutputWriter.write(Double.toString(featureFractions));
				matrixOutputWriter.newLine();
				featureSums.add(featureOccurreces);
			}
			matrixOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Generate the feature subsets for features that occur in 10%, 20%, ..., 100% of the best individuals.
		for (double d = 1; d < 11; d++)
		{
			double featureFraction = d / 10.0;
			int numberOfOccurences = (int) Math.floor(featureFraction * bestIndividuals.size());  // Determine the number of times a feature must occur for it to be included in the feature set.
			List<String> featureSet = new ArrayList<String>();
			for (int i = 0; i < featuresUsed.size(); i++)
			{
				if (featureSums.get(i) >= numberOfOccurences)
				{
					// If the ith feature occurred enough times.
					featureSet.add(featuresUsed.get(i));
				}
			}
			// Write out the feature set.
			try
			{
				String featureSetOutputLocation = resultsDirLoc + "/" + Integer.toString((int) (featureFraction * 100)) + "%FeatureSet.txt";
				FileWriter featureSetOutputFile = new FileWriter(featureSetOutputLocation);
				BufferedWriter featureSetOutputWriter = new BufferedWriter(featureSetOutputFile);
				for (String s : featureSet)
				{
					featureSetOutputWriter.write(s);
					featureSetOutputWriter.newLine();
				}
				featureSetOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

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
