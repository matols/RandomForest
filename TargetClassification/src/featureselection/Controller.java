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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public Controller(String[] args)
	{
		// Initialise the controller for the dataset determination and forest growing.
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = false;
		ctrl.numberOfTreesToGrow = 100;
		gaSelection(args, ctrl, 10, false, new HashMap<String, Double>());
	}

	public Controller(String[] args, TreeGrowthControl ctrl, int gaRepetitions, boolean isXValUsed, Map<String, Double> weights)
	{
		gaSelection(args, ctrl, gaRepetitions, isXValUsed, weights);
	}

//	void backwardSelection(String[] args, TreeGrowthControl ctrl)
//	{
//
//		// Required inputs.
//		String inputLocation = args[0];  // The location of the file containing the data to use in the feature selection.
//		File inputFile = new File(inputLocation);
//		if (!inputFile.isFile())
//		{
//			System.out.println("The first argument must be a valid file location, and must contain the data for feature selection.");
//			System.exit(0);
//		}
//		String outputLocation = args[1];  // The location to store any and all results.
//		File outputDirectory = new File(outputLocation);
//		if (outputDirectory.isDirectory())
//		{
//			removeDirectoryContent(outputDirectory);
//		}
//		boolean isDirCreated = outputDirectory.mkdirs();
//		if (!isDirCreated)
//		{
//			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
//			System.exit(0);
//		}
//
//		ProcessDataForGrowing inpData = new ProcessDataForGrowing(inputLocation, ctrl);
//		List<String> availableCovars = new ArrayList<String>(inpData.covariablesGrownFrom);
//		List<String> covarsRemoved = new ArrayList<String>();
//		double bestOverallOobError = 100.0;
//		List<String> bestFeatureSet = null;
//		while (!availableCovars.isEmpty())
//		{
//			double bestOobError = 100.0;
//			String worstCovar = null;
//			Double weightVector[] = new Double[inpData.numberObservations];
//			Arrays.fill(weightVector, 1.0);
//			for (String s : availableCovars)
//			{
//				// Knock out each remaining variable, and determine which knock out gives the best oob error.
//				// Remove the feature that when knocked out gave the best error.
//				ctrl.variablesToIgnore = new ArrayList<String>(covarsRemoved);
//				ctrl.variablesToIgnore.add(s);
//				Forest forest = new Forest(inputLocation, ctrl, weightVector);
//				if (forest.oobErrorEstimate < bestOobError)
//				{
//					bestOobError = forest.oobErrorEstimate;
//					worstCovar = s;
//				}
//			}
//			covarsRemoved.add(worstCovar);
//			availableCovars.remove(worstCovar);
//			if (bestOobError < bestOverallOobError)
//			{
//				bestFeatureSet = new ArrayList<String>(availableCovars);
//				bestOverallOobError = bestOobError;
//			}
//			System.out.print(bestFeatureSet);
//			System.out.println(bestOverallOobError);
//		}
//	}

//	void forwardSelection(String[] args, TreeGrowthControl ctrl)
//	{
//
//		// Required inputs.
//		String inputLocation = args[0];  // The location of the file containing the data to use in the feature selection.
//		File inputFile = new File(inputLocation);
//		if (!inputFile.isFile())
//		{
//			System.out.println("The first argument must be a valid file location, and must contain the data for feature selection.");
//			System.exit(0);
//		}
//		String outputLocation = args[1];  // The location to store any and all results.
//		File outputDirectory = new File(outputLocation);
//		if (outputDirectory.isDirectory())
//		{
//			removeDirectoryContent(outputDirectory);
//		}
//		boolean isDirCreated = outputDirectory.mkdirs();
//		if (!isDirCreated)
//		{
//			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
//			System.exit(0);
//		}
//
//		ProcessDataForGrowing inpData = new ProcessDataForGrowing(inputLocation, ctrl);
//		List<String> availableCovars = new ArrayList<String>(inpData.covariablesGrownFrom);
//		List<String> covarsUsed = new ArrayList<String>();
//		double bestOverallOobError = 100.0;
//		List<String> bestFeatureSet = null;
//		while (!availableCovars.isEmpty())
//		{
//			double bestOobError = 100.0;
//			String bestCovar = null;
//			Double weightVector[] = new Double[inpData.numberObservations];
//			Arrays.fill(weightVector, 1.0);
//			for (String s : availableCovars)
//			{
//				ctrl.variablesToIgnore = new ArrayList<String>(availableCovars);
//				ctrl.variablesToIgnore.remove(s);
//				Forest forest = new Forest(inputLocation, ctrl, weightVector);
//				if (forest.oobErrorEstimate < bestOobError)
//				{
//					bestOobError = forest.oobErrorEstimate;
//					bestCovar = s;
//				}
//			}
//			covarsUsed.add(bestCovar);
//			availableCovars.remove(bestCovar);
//			if (bestOobError < bestOverallOobError)
//			{
//				bestFeatureSet = new ArrayList<String>(covarsUsed);
//				bestOverallOobError = bestOobError;
//			}
//			System.out.print(bestFeatureSet);
//			System.out.println(bestOverallOobError);
//		}
//	}

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

//	void recursiveFeatureElimination(String[] args, TreeGrowthControl ctrl)
//	{
//		// Required inputs.
//		String inputLocation = args[0];  // The location of the file containing the data to use in the feature selection.
//		File inputFile = new File(inputLocation);
//		if (!inputFile.isFile())
//		{
//			System.out.println("The first argument must be a valid file location, and must contain the data for feature selection.");
//			System.exit(0);
//		}
//		String outputLocation = args[1];  // The location to store any and all results.
//		File outputDirectory = new File(outputLocation);
//		if (outputDirectory.isDirectory())
//		{
//			removeDirectoryContent(outputDirectory);
//		}
//		boolean isDirCreated = outputDirectory.mkdirs();
//		if (!isDirCreated)
//		{
//			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
//			System.exit(0);
//		}
//
//		ProcessDataForGrowing inpData = new ProcessDataForGrowing(inputLocation, ctrl);
//		List<String> availableCovars = new ArrayList<String>(inpData.covariablesGrownFrom);
//		double bestOverallOobError = 100.0;
//		List<String> bestFeatureSet = null;
//		while (!availableCovars.isEmpty())
//		{
//			Forest forest = new Forest(inputLocation, ctrl);
//			System.out.format("Forest OOB error rate : %f\n", forest.oobErrorEstimate);
//			System.out.println(availableCovars);
//			if (forest.oobErrorEstimate < bestOverallOobError)
//			{
//				bestOverallOobError = forest.oobErrorEstimate;
//				bestFeatureSet = new ArrayList<String>(availableCovars);
//			}
//			VariableImportance varImpCalculator = new VariableImportance();
//			Map<String, Double> varImp = varImpCalculator.conditionalVariableImportance(forest, ctrl, 0.2, 2, false);
//			double lowestImportance = Double.MAX_VALUE;
//			double maxImportance = -Double.MAX_VALUE;
//			String leastImportantCovariable = null;
//			String mostImportantCovar = null;
//			for (String s : varImp.keySet())
//			{
//				if (varImp.get(s) < lowestImportance)
//				{
//					lowestImportance = varImp.get(s);
//					leastImportantCovariable = s;
//				}
//				if (varImp.get(s) > maxImportance)
//				{
//					maxImportance = varImp.get(s);
//					mostImportantCovar = s;
//				}
//			}
//			System.out.format("%s - %f\n", leastImportantCovariable, lowestImportance);
//			System.out.format("%s - %f\n", mostImportantCovar, maxImportance);
//			ctrl.variablesToIgnore.add(leastImportantCovariable);
//			availableCovars.remove(leastImportantCovariable);
//		}
//		System.out.println("\n=======================\n");
//		System.out.println(bestOverallOobError);
//		System.out.println(bestFeatureSet);
//	}

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
				matrixOutputWriter.write(Integer.toString(featureOccurreces));
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
