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
		weights.put("Positive", 1.1);
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
		String varImpLocation = outputLocation + "/VariableImportances.txt";
		try
		{
			FileWriter varImpOutputFile = new FileWriter(varImpLocation, true);
			BufferedWriter varImpOutputWriter = new BufferedWriter(varImpOutputFile);
			varImpOutputWriter.write(featuresUsed.get(0));
			for (String s : featuresUsed.subList(1, featuresUsed.size()))
			{
				varImpOutputWriter.write("\t" + s);
			}
			varImpOutputWriter.newLine();
			varImpOutputWriter.close();
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

		// Setup the proximity mapping.
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

			Forest forest = new Forest(inputLocation, ctrl, weights, seedToUse);
			System.out.println("\tNow determining variable importances.");
			Map<String, Double> varImp = forest.variableImportance();
		    System.out.println("\tNow determining proximities.");
//			Map<Integer, Map<Integer, Double>> prox = forest.calculatProximities(outputLocation + "/TempProximities.txt");
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
			Collections.sort(sortedVariables, Collections.reverseOrder());  // Larger importance first.
			Map<String, Integer> varToImpRank = new HashMap<String, Integer>();
			for (int j = 0; j < varImp.size(); j++)
			{
				varToImpRank.put(sortedVariables.get(j).getId(), j + 1);
			}

			// Write out the results for this repetition.
			try
			{
				FileWriter varImpOutputFile = new FileWriter(varImpLocation, true);
				BufferedWriter varImpOutputWriter = new BufferedWriter(varImpOutputFile);
				String outputString = "";
				for (String s : featuresUsed)
				{
					outputString += varToImpRank.get(s) + "\t";
				}
				varImpOutputWriter.write(outputString.substring(0, outputString.length() - 1));
				varImpOutputWriter.newLine();
				varImpOutputWriter.close();

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

		// Normalise and write out the proximities.
		for (int j : proximities.keySet())
		{
			Map<Integer, Double> currentProx = proximities.get(j);
			for (int k : currentProx.keySet())
			{
				currentProx.put(k, currentProx.get(k) / repetitions);
			}
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

//		ProcessDataForGrowing procInputData = new ProcessDataForGrowing(inputLocation, ctrl);
//		int numberOfObservations = procInputData.numberObservations;
//		int numberOfTrees = 0;
//		Path dataPath = Paths.get(outputLocation + "/TempProximities.txt");
//		try
//		{
//			FileWriter proxOutputFile = new FileWriter(outputLocation + "/Proximities.txt");
//			BufferedWriter proxOutputWriter = new BufferedWriter(proxOutputFile);
//			for (int i = 0; i < numberOfObservations; i++)
//			{
//				proxOutputWriter.write("\t" + Integer.toString(i));
//			}
//			proxOutputWriter.newLine();
//			for (int i = 0; i < numberOfObservations; i++)
//			{
//				// For each observation
//				String currentObs = Integer.toString(i);
//				Map<String, Double> sameTermNodeWithI = new HashMap<String, Double>();
//				for (int j = 0; j < numberOfObservations; j++)
//				{
//					// Set up coocurences where obs appears in same terminal node with other obs
//					sameTermNodeWithI.put(Integer.toString(j), 0.0);
//				}
//				try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
//				{
//					String line;
//					numberOfTrees = 0;
//					while ((line = reader.readLine()) != null)
//					{
//						numberOfTrees += 1;
//						String[] proxims = line.trim().split("\t");
//						for (String s : proxims)
//						{
//							List<String> termNodeObs = Arrays.asList(s.split(","));
//							if (termNodeObs.contains(currentObs))
//							{
//								for (String p : termNodeObs)
//								{
//									sameTermNodeWithI.put(p, sameTermNodeWithI.get(p) + 1.0);
//								}
//							}
//						}
//					}
//				}
//				catch (Exception e)
//				{
//					e.printStackTrace();
//					System.exit(0);
//				}
//				for (String s : sameTermNodeWithI.keySet())
//				{
//					sameTermNodeWithI.put(s, sameTermNodeWithI.get(s) / numberOfTrees);
//				}
//				proxOutputWriter.write(currentObs);
//				for (int j = 0; j < numberOfObservations; j++)
//				{
//					proxOutputWriter.write("\t" + Double.toString(sameTermNodeWithI.get(Integer.toString(j))));
//				}
//				proxOutputWriter.newLine();
//			}
//			proxOutputWriter.close();
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			System.exit(0);
//		}
	}

}