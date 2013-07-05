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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tree.Forest;
import tree.ImmutableTwoValues;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class VariableImportance {

	/**
	 * @param args
	 */
	public static void main(String[] args)
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

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int repetitions = 200;
		Integer[] trainingObsToUse = {};

		TreeGrowthControl ctrl = new TreeGrowthControl();;
		ctrl.isReplacementUsed = true;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.numberOfTreesToGrow = 5000;
		ctrl.mtry = 10;
		ctrl.minNodeSize = 1;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Write out the parameters.
		String parameterLocation = outputLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Repetitions - " + Integer.toString(repetitions));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Training observations used - " + Arrays.toString(trainingObsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used");
			parameterOutputWriter.newLine();
			for (String s : weights.keySet())
			{
				parameterOutputWriter.write("\t" + s + " - " + Double.toString(weights.get(s)));
				parameterOutputWriter.newLine();
			}
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

		// Write out the importance header
		String accVarImpLocation = outputLocation + "/AccuracyVariableImportances.txt";
		String gMeanVarImpLocation = outputLocation + "/QualityVariableImportances.txt";
		try
		{
			FileWriter accVarImpOutputFile = new FileWriter(accVarImpLocation, true);
			BufferedWriter accVarImpOutputWriter = new BufferedWriter(accVarImpOutputFile);
			FileWriter gMeanVarImpOutputFile = new FileWriter(gMeanVarImpLocation, true);
			BufferedWriter gMeanVarImpOutputWriter = new BufferedWriter(gMeanVarImpOutputFile);

			accVarImpOutputWriter.write(featuresUsed.get(0));
			gMeanVarImpOutputWriter.write(featuresUsed.get(0));
			for (String s : featuresUsed.subList(1, featuresUsed.size()))
			{
				accVarImpOutputWriter.write("\t" + s);
				gMeanVarImpOutputWriter.write("\t" + s);
			}
			accVarImpOutputWriter.newLine();
			gMeanVarImpOutputWriter.newLine();

			accVarImpOutputWriter.close();
			gMeanVarImpOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		String seedsLocation = outputLocation + "/SeedsUsed.txt";

		// Setup the record of the average importance ranking.
		Map<String, List<Integer>> importanceRanking = new HashMap<String, List<Integer>>();
		for (String s : featuresUsed)
		{
			importanceRanking.put(s, new ArrayList<Integer>());
		}

		// Determine the seeds that will be used.
		Random randGen = new Random();
		List<Long> seedsUsed = new ArrayList<Long>();

		for (int i = 0; i < repetitions; i++)
		{
			Date startTime = new Date();
		    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String strDate = sdfDate.format(startTime);
		    System.out.format("Now working on repetition %d at %s.\n", i, strDate);

		    // Determine a new unique seed to use.
			Long seedToUse = randGen.nextLong();
			while (seedsUsed.contains(seedToUse))
			{
				// Ensure a unique seed each time.
				seedToUse = randGen.nextLong();
			}
			seedsUsed.add(seedToUse);

			Forest forest = new Forest(inputLocation, ctrl, seedToUse);
			forest.setWeightsByClass(weights);
			forest.growForest();
			System.out.println("\tNow determining variable importances.");
			ImmutableTwoValues<Map<String,Double>, Map<String,Double>> varImp = forest.variableImportance();
			Map<String, Double> accuracyImportance = varImp.first;
			Map<String, Double> gMeanImportance = varImp.second;

			// Determine the importance ordering for the variables, largest importance first.
			List<StringsSortedByDoubles> accSortedVariables = new ArrayList<StringsSortedByDoubles>();
			List<StringsSortedByDoubles> gMeanSortedVariables = new ArrayList<StringsSortedByDoubles>();
			for (String s : accuracyImportance.keySet())
			{
				accSortedVariables.add(new StringsSortedByDoubles(accuracyImportance.get(s), s));
				gMeanSortedVariables.add(new StringsSortedByDoubles(gMeanImportance.get(s), s));
			}
			Collections.sort(accSortedVariables, Collections.reverseOrder());  // Larger importance first.
			Collections.sort(gMeanSortedVariables, Collections.reverseOrder());  // Larger importance first.

			Map<String, Integer> varToAccImpRank = new HashMap<String, Integer>();
			Map<String, Integer> varToGMeanImpRank = new HashMap<String, Integer>();
			for (int j = 0; j < accSortedVariables.size(); j++)
			{
				varToAccImpRank.put(accSortedVariables.get(j).getId(), j + 1);
				varToGMeanImpRank.put(gMeanSortedVariables.get(j).getId(), j + 1);
			}

			// Write out the results for this repetition.
			try
			{
				String accOutputString = "";
				String gMeanOutputString = "";
				for (String s : featuresUsed)
				{
					accOutputString += varToAccImpRank.get(s) + "\t";
					gMeanOutputString += varToGMeanImpRank.get(s) + "\t";
				}

				FileWriter accVarImpOutputFile = new FileWriter(accVarImpLocation, true);
				BufferedWriter accVarImpOutputWriter = new BufferedWriter(accVarImpOutputFile);
				accVarImpOutputWriter.write(accOutputString.substring(0, accOutputString.length() - 1));
				accVarImpOutputWriter.newLine();
				accVarImpOutputWriter.close();
				
				FileWriter gMeanVarImpOutputFile = new FileWriter(gMeanVarImpLocation, true);
				BufferedWriter gMeanImpOutputWriter = new BufferedWriter(gMeanVarImpOutputFile);
				gMeanImpOutputWriter.write(gMeanOutputString.substring(0, gMeanOutputString.length() - 1));
				gMeanImpOutputWriter.newLine();
				gMeanImpOutputWriter.close();

				FileWriter seedsOutputFile = new FileWriter(seedsLocation, true);
				BufferedWriter seedsOutputWriter = new BufferedWriter(seedsOutputFile);
				seedsOutputWriter.write(Long.toString(seedToUse));
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
