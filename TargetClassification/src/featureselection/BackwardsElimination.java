package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import datasetgeneration.BootstrapGeneration;
import datasetgeneration.CrossValidationFoldGenerationMultiClass;

import tree.Forest;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class BackwardsElimination
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Required inputs.
		String inputLocation = args[0];  // The location of the file containing the dataset to use in the feature selection.
		File inputFile = new File(inputLocation);
		if (!inputFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the dataset for feature selection.");
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
		int subsamplesToGenerate = 50;
		double fractionToReserveAsValidation = 0.8;
		int foldsToGenerate = 10;
		int finalSelectionRepetitions = 50;
//		double fractionToElim = 0.2;  // Eliminating a fraction allows you to remove lots of variables when there are lots remaining, and get better resolution when there are few remaining.
		int featuresToEliminate = 1;
		boolean continueRun = false;  // Whether or not you want to continue a run in progress or restart the whole process.

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 500;
		ctrl.mtry = 10;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.isCalculateOOB = false;

		TreeGrowthControl varImpCtrl = new TreeGrowthControl(ctrl);
		varImpCtrl.numberOfTreesToGrow = 5000;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.5);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

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

		// Determine the features that are used in growing the trees.
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

		// Determine whether bootstrap samples need to be generated.
		String resultsOutputLoc = outputLocation + "/Results.txt";
		String parameterLocation = outputLocation + "/Parameters.txt";
		if (!continueRun)
		{
			// Generate bootstraps and recreate the results file.
			removeDirectoryContent(outputDirectory);
			BootstrapGeneration.main(inputLocation, outputLocation, subsamplesToGenerate, false, fractionToReserveAsValidation);
			try
			{
				FileWriter featureFractionsOutputFile = new FileWriter(resultsOutputLoc);
				BufferedWriter featureFractionsOutputWriter = new BufferedWriter(featureFractionsOutputFile);
				for (String s : featuresUsed)
				{
					featureFractionsOutputWriter.write(s + "\t");
				}
				featureFractionsOutputWriter.write("ErrorRate");
				featureFractionsOutputWriter.newLine();
				featureFractionsOutputWriter.close();

				FileWriter parameterOutputFile = new FileWriter(parameterLocation);
				BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
				parameterOutputWriter.write("Subsamples generated - " + Integer.toString(subsamplesToGenerate));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Fraction to reserve as validation - " + Double.toString(fractionToReserveAsValidation));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Folds per subsample - " + Integer.toString(foldsToGenerate));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Final selection repetitions - " + Integer.toString(finalSelectionRepetitions));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Feature to eliminate each round - " + Integer.toString(featuresToEliminate));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Trees used in training - " + Integer.toString(ctrl.numberOfTreesToGrow));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Trees used for variable importance - " + Integer.toString(varImpCtrl.numberOfTreesToGrow));
				parameterOutputWriter.newLine();
				parameterOutputWriter.write("Weights used - " + weights.toString());
				parameterOutputWriter.newLine();
				parameterOutputWriter.close();

				ctrl.save(outputLocation + "/RegularCtrl.txt");
				varImpCtrl.save(outputLocation + "/VariableImportanceCtrl.txt");
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		for (int i = 0; i < subsamplesToGenerate; i++)
		{
			String subsampleDirectory = outputLocation + "/" + Integer.toString(i);
			if ((new File(subsampleDirectory + "/ErrorForThisSubsample.txt")).exists())
			{
				continue;
			}
			Date currentTime = new Date();
		    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String strDate = sdfDate.format(currentTime);
			System.out.format("Now working on subsample %d at %s.\n", i, strDate);

			String subsampleTrainingSet = subsampleDirectory + "/Train.txt";
			String subsampleTestingSet = subsampleDirectory + "/Test.txt";
			String internalFoldDirLoc = subsampleDirectory + "/Folds";
			ctrl.variablesToIgnore = new ArrayList<String>();
			CrossValidationFoldGenerationMultiClass.main(subsampleTrainingSet, internalFoldDirLoc, foldsToGenerate);

			//----------------------------------------------------------------------------//
			// Determine the order of the features by average feature importance ranking. //
			//----------------------------------------------------------------------------//
			currentTime = new Date();
		    sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    strDate = sdfDate.format(currentTime);
			System.out.format("\tNow determining average variable importance at %s.\n", strDate);
			Random seedGenerator = new Random();
			List<Long> seedsForFolds = new ArrayList<Long>();
			for (int j = 0; j < foldsToGenerate; j++)
			{
				long seedToUse = seedGenerator.nextLong();
				while (seedsForFolds.contains(seedToUse))
				{
					seedToUse = seedGenerator.nextLong();
				}
				seedsForFolds.add(seedToUse);
			}
			List<String> orderedFeaturesByImportance = new ArrayList<String>();
			Map<String, Double> averageVariableImportanceRanking = new HashMap<String, Double>();
			for (String s : featuresUsed)
			{
				averageVariableImportanceRanking.put(s, 0.0);
			}
			for (int j = 0; j < foldsToGenerate; j++)
			{
				String internalFoldTrainingSet = internalFoldDirLoc + "/" + Integer.toString(j) + "/Train.txt";
				// Calculate the variable importance.
				Forest forest = new Forest(internalFoldTrainingSet, varImpCtrl, weights, seedsForFolds.get(j));
				Map<String, Double> varImp = forest.variableImportance();
				// Rank the variables by importance.
				List<StringsSortedByDoubles> sortedVariables = new ArrayList<StringsSortedByDoubles>();
				for (String s : varImp.keySet())
				{
					sortedVariables.add(new StringsSortedByDoubles(varImp.get(s), s));
				}
				Collections.sort(sortedVariables, Collections.reverseOrder());  // Larger importance first.
				for (int k = 0; k < varImp.size(); k++)
				{
					String featureInQuestion = sortedVariables.get(k).getId();
					double newAverageRank = averageVariableImportanceRanking.get(featureInQuestion) + k + 1;
					averageVariableImportanceRanking.put(featureInQuestion, newAverageRank);
				}
			}
			for (String s : featuresUsed)
			{
				double averagedRank = averageVariableImportanceRanking.get(s) / foldsToGenerate;
				averageVariableImportanceRanking.put(s, averagedRank);
			}
			// Rank the variables by average importance.
			List<StringsSortedByDoubles> sortedVariables = new ArrayList<StringsSortedByDoubles>();
			for (String s : featuresUsed)
			{
				sortedVariables.add(new StringsSortedByDoubles(averageVariableImportanceRanking.get(s), s));
			}
			Collections.sort(sortedVariables);  // Lower ranking first.
			for (StringsSortedByDoubles ssbd : sortedVariables)
			{
				orderedFeaturesByImportance.add(ssbd.getId());
			}

			//--------------------------//
			// Perform the elimination. //
			//--------------------------//
			currentTime = new Date();
		    sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    strDate = sdfDate.format(currentTime);
			System.out.format("\tNow performing the elimination at %s.\n", strDate);
			List<String> bestFeatureSet = new ArrayList<String>();
			List<String> bestFeatureIgnoredFeatureSet = new ArrayList<String>();
			double bestErrorRate = 100.0;
			while (!orderedFeaturesByImportance.isEmpty())
			{
				List<String> variablesToIgnore = new ArrayList<String>(featuresUsed);
				variablesToIgnore.removeAll(orderedFeaturesByImportance);
				ctrl.variablesToIgnore = variablesToIgnore;
				// Calculate the predictive error rate.
				Map<String, Map<String, Double>> averagedConfusionMatrix = new HashMap<String, Map<String, Double>>();
				for (int j = 0; j < foldsToGenerate; j++)
				{
					String internalFoldTrainingSet = internalFoldDirLoc + "/" + Integer.toString(j) + "/Train.txt";
					String internalFoldTestingSet = internalFoldDirLoc + "/" + Integer.toString(j) + "/Test.txt";
					Forest forest = new Forest(internalFoldTrainingSet, ctrl, weights, seedsForFolds.get(j));
					Map<String, Map<String, Double>> confMat = forest.predict(new ProcessDataForGrowing(internalFoldTestingSet, ctrl)).second;
					if (averagedConfusionMatrix.isEmpty())
					{
						averagedConfusionMatrix = confMat;
					}
					else
					{
						for (String s : averagedConfusionMatrix.keySet())
						{
							for (String p : averagedConfusionMatrix.get(s).keySet())
							{
								double newValue = averagedConfusionMatrix.get(s).get(p) + confMat.get(s).get(p);
								averagedConfusionMatrix.get(s).put(p, newValue);
							}
						}
					}
				}
				double totalErrors = 0.0;
				double totalPredictions = 0.0;
				for (String s : averagedConfusionMatrix.keySet())
				{
					totalErrors += averagedConfusionMatrix.get(s).get("FalsePositive");
					totalPredictions += averagedConfusionMatrix.get(s).get("TruePositive") + averagedConfusionMatrix.get(s).get("FalsePositive");
				}
				double averagedError = totalErrors / totalPredictions;

				if (averagedError < bestErrorRate)
				{
					bestErrorRate = averagedError;
					bestFeatureSet = new ArrayList<String>(orderedFeaturesByImportance);
					bestFeatureIgnoredFeatureSet = new ArrayList<String>(ctrl.variablesToIgnore);
				}

				orderedFeaturesByImportance.remove(orderedFeaturesByImportance.size() - featuresToEliminate);
			}

			// Validate the error rate of the best subset found for this subsample.
			ctrl.variablesToIgnore = bestFeatureIgnoredFeatureSet;
			Forest forest = new Forest(subsampleTrainingSet, ctrl, weights);
			double validatedError = forest.predict(new ProcessDataForGrowing(subsampleTestingSet, ctrl)).first;

			// Write out the results for this subsample.
			try
			{
				FileWriter featureFractionsOutputFile = new FileWriter(resultsOutputLoc, true);
				BufferedWriter featureFractionsOutputWriter = new BufferedWriter(featureFractionsOutputFile);
				for (String s : featuresUsed)
				{
					if (bestFeatureSet.contains(s))
					{
						featureFractionsOutputWriter.write("1\t");
					}
					else
					{
						featureFractionsOutputWriter.write("0\t");
					}
				}
				featureFractionsOutputWriter.write(Double.toString(validatedError));
				featureFractionsOutputWriter.newLine();
				featureFractionsOutputWriter.close();

				FileWriter subsampleOutputFile = new FileWriter(subsampleDirectory + "/ErrorForThisSubsample.txt");
				BufferedWriter subsampleOutputWriter = new BufferedWriter(subsampleOutputFile);
				subsampleOutputWriter.write("ErrorRate = ");
				subsampleOutputWriter.write(Double.toString(validatedError));
				subsampleOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		//----------------------------------------------------------------------------//
		// Determine the order of the features by average feature importance ranking. //
		//----------------------------------------------------------------------------//
		System.out.println("Now performing the final selection.");
		Date currentTime = new Date();
	    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    String strDate = sdfDate.format(currentTime);
		System.out.format("\tNow determining average variable importance at %s.\n", strDate);
		Random seedGenerator = new Random();
		List<Long> seedsForFolds = new ArrayList<Long>();
		for (int j = 0; j < finalSelectionRepetitions; j++)
		{
			long seedToUse = seedGenerator.nextLong();
			while (seedsForFolds.contains(seedToUse))
			{
				seedToUse = seedGenerator.nextLong();
			}
			seedsForFolds.add(seedToUse);
		}
		List<String> orderedFeaturesByImportance = new ArrayList<String>();
		Map<String, Double> averageVariableImportanceRanking = new HashMap<String, Double>();
		for (String s : featuresUsed)
		{
			averageVariableImportanceRanking.put(s, 0.0);
		}
		for (int j = 0; j < finalSelectionRepetitions; j++)
		{
			// Calculate the variable importance.
			Forest forest = new Forest(inputLocation, varImpCtrl, weights, seedsForFolds.get(j));
			Map<String, Double> varImp = forest.variableImportance();
			// Rank the variables by importance.
			List<StringsSortedByDoubles> sortedVariables = new ArrayList<StringsSortedByDoubles>();
			for (String s : varImp.keySet())
			{
				sortedVariables.add(new StringsSortedByDoubles(varImp.get(s), s));
			}
			Collections.sort(sortedVariables, Collections.reverseOrder());  // Larger importance first.
			for (int k = 0; k < varImp.size(); k++)
			{
				String featureInQuestion = sortedVariables.get(k).getId();
				double newAverageRank = averageVariableImportanceRanking.get(featureInQuestion) + k + 1;
				averageVariableImportanceRanking.put(featureInQuestion, newAverageRank);
			}
		}
		for (String s : featuresUsed)
		{
			double averagedRank = averageVariableImportanceRanking.get(s) / finalSelectionRepetitions;
			averageVariableImportanceRanking.put(s, averagedRank);
		}
		// Rank the variables by average importance.
		List<StringsSortedByDoubles> sortedVariables = new ArrayList<StringsSortedByDoubles>();
		for (String s : featuresUsed)
		{
			sortedVariables.add(new StringsSortedByDoubles(averageVariableImportanceRanking.get(s), s));
		}
		Collections.sort(sortedVariables);  // Lower ranking first.
		for (StringsSortedByDoubles ssbd : sortedVariables)
		{
			orderedFeaturesByImportance.add(ssbd.getId());
		}

		//--------------------------//
		// Perform the elimination. //
		//--------------------------//
		currentTime = new Date();
	    sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    strDate = sdfDate.format(currentTime);
		System.out.format("\tNow performing the elimination at %s.\n", strDate);
		List<String> bestFeatureSet = new ArrayList<String>();
		double bestErrorRate = 100.0;
		ctrl.isCalculateOOB = true;
		while (!orderedFeaturesByImportance.isEmpty())
		{
			List<String> variablesToIgnore = new ArrayList<String>(featuresUsed);
			variablesToIgnore.removeAll(orderedFeaturesByImportance);
			ctrl.variablesToIgnore = variablesToIgnore;
			// Calculate the predictive error rate.
			Map<String, Map<String, Double>> averagedConfusionMatrix = new HashMap<String, Map<String, Double>>();
			for (int j = 0; j < finalSelectionRepetitions; j++)
			{
				Forest forest = new Forest(inputLocation, ctrl, weights, seedsForFolds.get(j));
				Map<String, Map<String, Double>> confMat = forest.oobConfusionMatrix;
				if (averagedConfusionMatrix.isEmpty())
				{
					averagedConfusionMatrix = confMat;
				}
				else
				{
					for (String s : averagedConfusionMatrix.keySet())
					{
						for (String p : averagedConfusionMatrix.get(s).keySet())
						{
							double newValue = averagedConfusionMatrix.get(s).get(p) + confMat.get(s).get(p);
							averagedConfusionMatrix.get(s).put(p, newValue);
						}
					}
				}
			}
			double totalErrors = 0.0;
			double totalPredictions = 0.0;
			for (String s : averagedConfusionMatrix.keySet())
			{
				totalErrors += averagedConfusionMatrix.get(s).get("FalsePositive");
				totalPredictions += averagedConfusionMatrix.get(s).get("TruePositive") + averagedConfusionMatrix.get(s).get("FalsePositive");
			}
			double averagedError = totalErrors / totalPredictions;

			if (averagedError < bestErrorRate)
			{
				bestErrorRate = averagedError;
				bestFeatureSet = new ArrayList<String>(orderedFeaturesByImportance);
			}
			orderedFeaturesByImportance.remove(orderedFeaturesByImportance.size() - featuresToEliminate);
		}

		// Write out the results for the entire dataset.
		try
		{
			FileWriter featureFractionsOutputFile = new FileWriter(resultsOutputLoc, true);
			BufferedWriter featureFractionsOutputWriter = new BufferedWriter(featureFractionsOutputFile);
			featureFractionsOutputWriter.newLine();
			for (String s : featuresUsed)
			{
				if (bestFeatureSet.contains(s))
				{
					featureFractionsOutputWriter.write("1\t");
				}
				else
				{
					featureFractionsOutputWriter.write("0\t");
				}
			}
			featureFractionsOutputWriter.write(Double.toString(bestErrorRate));
			featureFractionsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
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
