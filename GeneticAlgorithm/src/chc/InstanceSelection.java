/**
 * 
 */
package chc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import tree.Forest;
import tree.IndexedDoubleData;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class InstanceSelection
{

	/**
	 * The record of the top fitness found during the most recent run of the GA.
	 */
	public double currentBestFitness = 0.0;

	/**
	 * A list of the individuals that have the best fitness found.
	 */
	public List<List<Integer>> bestMembersFound = new ArrayList<List<Integer>>();


	public InstanceSelection(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights, Map<String, Integer> fractionsToSelect)
	{
		// Required inputs.
		String datasetLocation = args[0];  // The location of the file containing the entire dataset.
		File datasetFile = new File(datasetLocation);
		if (!datasetFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the entire dataset.");
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

		// Optional inputs.
		int populationSize = 50;  // The size of the population to use for the GA.
		int maxGenerations = 100;  // The number of generations to run the GA for.
		int maxEvaluations = 0;  // The maximum number of fitness evaluations to perform.
		boolean verbose = false;  // Whether status updates should be displayed.
		long maxTimeAllowed = 0;  // What the maximum time allowed (in ms) for the run is. 0 indicates that timing is not used.
		int maxConvergences = 0;  // The number of times the population is allowed to converge.
		int maxStagnant = 5;  // The number of consecutive generations that can occur without any offspring being added to the population.

		// Read in the user input.
		int argIndex = 2;
		while (argIndex < args.length)
		{
			String currentArg = args[argIndex];
			switch (currentArg)
			{
			case "-p":
				argIndex += 1;
				populationSize = Integer.parseInt(args[argIndex]);
				argIndex += 1;
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
			case "-t":
				argIndex += 1;
				maxTimeAllowed = Long.parseLong(args[argIndex]);
				argIndex += 1;
				break;
			case "-v":
				verbose = true;
				argIndex += 1;
				break;
			case "-c":
				argIndex += 1;
				maxConvergences = Integer.parseInt(args[argIndex]);
				argIndex += 1;
				break;
			case "-s":
				argIndex += 1;
				maxStagnant = Integer.parseInt(args[argIndex]);
				argIndex += 1;
				break;
			default:
				System.out.format("Unexpeted argument : %s.\n", currentArg);
				System.exit(0);
			}
		}
		if (maxGenerations <= 0 && maxEvaluations <= 0 && maxTimeAllowed <= 0 && maxConvergences <= 0)
		{
	        // No stopping criteria given.
	        System.out.println("At least one of -g, -e, -t or -c must be given, otherwise there are no stopping criteria.");
	        System.exit(0);
		}

		// Write out the parameters used for the GA.
		String parameterOutputLocation = outputLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterOutputLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
		    parameterOutputWriter.write("Population Size:\t" + Integer.toString(populationSize));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Number of Generations:\t" + Integer.toString(maxGenerations));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Number of Evaluations:\t" + Integer.toString(maxEvaluations));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Length of time allowed (ms):\t" + Long.toString(maxTimeAllowed));
		    parameterOutputWriter.newLine();
		    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date now = new Date();
		    String strDate = sdfDate.format(now);
		    parameterOutputWriter.write("Time Started:\t" + strDate);
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		ctrl.save(outputLocation + "/RandomForestCtrl.txt");

		// Initialise the fitness and population output directories.
		String fitnessDirectoryLocation = outputLocation + "/Fitnesses";
		File outputFitnessDirectory = new File(fitnessDirectoryLocation);
		boolean isFitDirCreated = outputFitnessDirectory.mkdirs();
		if (!isFitDirCreated)
		{
			System.out.println("The fitness directory could not be created.");
			System.exit(0);
		}
		String populationDirectoryLocation = outputLocation + "/Populations";
		File outputPopulationDirectory = new File(populationDirectoryLocation);
		boolean isPopDirCreated = outputPopulationDirectory.mkdirs();
		if (!isPopDirCreated)
		{
			System.out.println("The population directory could not be created.");
			System.exit(0);
		}

		// Setup the generation stats files and write out the class weights being used.
		String genStatsOutputLocation = outputLocation + "/GenerationStatistics.txt";
		String convergenOutputLocation = outputLocation + "/BestConvergenceIndividuals.txt";
		String weightOutputLocation = outputLocation + "/Weights.txt";
		Map<String, String> classFMeasureOutputLocations = new HashMap<String, String>();
		try
		{
			FileWriter genStatsOutputFile = new FileWriter(genStatsOutputLocation);
			BufferedWriter genStatsOutputWriter = new BufferedWriter(genStatsOutputFile);
			genStatsOutputWriter.write("Generation\tBestGMean\tMeanGMean\tMedianGMean\tStdDevGMean\tBestIndivSize\tMeanIndivSize\tThreshold\tEvaluationsPerformed");
			genStatsOutputWriter.newLine();
			genStatsOutputWriter.close();

			FileWriter convergenceOutputFile = new FileWriter(convergenOutputLocation);
			BufferedWriter convergenceOutputWriter = new BufferedWriter(convergenceOutputFile);
			convergenceOutputWriter.write("Generation\tFitness\tIndividual");
			convergenceOutputWriter.newLine();
			convergenceOutputWriter.close();

			FileWriter weightOutputFile = new FileWriter(weightOutputLocation);
			BufferedWriter weightOutputWriter = new BufferedWriter(weightOutputFile);
			weightOutputWriter.write("Tree Growth Weights");
			weightOutputWriter.newLine();
			for (String s : weights.keySet())
			{
				weightOutputWriter.write(s + "\t" + Double.toString(weights.get(s)));
				weightOutputWriter.newLine();
			}
			weightOutputWriter.newLine();
			weightOutputWriter.write("Number of Observations to Start With");
			weightOutputWriter.newLine();
			for (String s : fractionsToSelect.keySet())
			{
				weightOutputWriter.write(s + "\t" + Integer.toString(fractionsToSelect.get(s)));
				weightOutputWriter.newLine();
			}
			weightOutputWriter.close();

			for (String s : weights.keySet())
			{
				String classOutputLoc = outputLocation + "/" + s + "GMeans.txt";
				classFMeasureOutputLocations.put(s, classOutputLoc);
				FileWriter classStatsOutputFile = new FileWriter(classOutputLoc);
				BufferedWriter classStatsOutputWriter = new BufferedWriter(classStatsOutputFile);
				classStatsOutputWriter.write("Generation\tBestMemberGMean\tMeanGMean\tMedianGMean\tStdDevGMean");
				classStatsOutputWriter.newLine();
				classStatsOutputWriter.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the number of genes/features in the dataset.
		int numberOfObservations = 0;
		ProcessDataForGrowing processedInputFile = new ProcessDataForGrowing(datasetLocation, ctrl);
		List<Integer> observationIndices = new ArrayList<Integer>();
		Map<String, List<Integer>> observations = new HashMap<String, List<Integer>>();
		Map<Integer, String> observationsToClass = new HashMap<Integer, String>();
		try
		{
			BufferedReader geneReader = new BufferedReader(new FileReader(datasetFile));
			String header = geneReader.readLine();
			int indexOfClassification = header.split("\t").length - 1;
			header = geneReader.readLine();
			header = geneReader.readLine();
			String line;
			while ((line = geneReader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				String[] splitLine = (line.trim()).split("\t");
				String classification = splitLine[indexOfClassification];
				if (!observations.containsKey(classification))
				{
					observations.put(classification, new ArrayList<Integer>());
				}
				observations.get(classification).add(numberOfObservations);
				observationsToClass.put(numberOfObservations, classification);
				observationIndices.add(numberOfObservations);
				numberOfObservations += 1;
			}
			geneReader.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Determine the threshold used.
		if (!observations.keySet().containsAll(fractionsToSelect.keySet()))
		{
			System.out.println("You specified a class to select a certain number of observations from, but this class does not exist.");
			System.exit(0);
		}
		int initialSetSize = 0;
		for (String s : fractionsToSelect.keySet())
		{
			initialSetSize += fractionsToSelect.get(s);
		}
		int threshold = initialSetSize / 4;

		//----------------------
		// Begin the GA.
		//----------------------
		Date gaStartTime = new Date();
		Random observationSelector = new Random();

		// Initialise the stopping criteria for the GA.
	    int currentGeneration = 1;
	    int numberEvaluations = 0;
	    int convergencesOccurred = 0;

	    // Initialise stable population criteria.
	    int populationLastChanged = 0;
		
		// Generate the initial population.
	    if (verbose)
	    {
	    	System.out.println("Now generating the initial population");
	    }
		List<List<Integer>> population = new ArrayList<List<Integer>>();
		List<Integer> parentSelector = new ArrayList<Integer>();
		for (int i = 0; i < populationSize; i++)
		{
			List<Integer> newPopMember = new ArrayList<Integer>();
			for (String s : observations.keySet())
			{
				List<Integer> availableForSelection = new ArrayList<Integer>(observations.get(s));
				for (int j = 0; j < fractionsToSelect.get(s); j++)
				{
					// Select a random available observation from class s.
					Integer chosenObservation = availableForSelection.get(observationSelector.nextInt(availableForSelection.size()));
					newPopMember.add(chosenObservation);
					availableForSelection.remove(chosenObservation);
					if (availableForSelection.isEmpty())
					{
						availableForSelection = new ArrayList<Integer>(observations.get(s));
					}
				}
			}
			population.add(newPopMember);
			parentSelector.add(i);
		}

	    // Calculate the fitness of the initial population.
	    List<Double> fitness = new ArrayList<Double>();
	    Map<String, List<Double>> classGMeanResults = new HashMap<String, List<Double>>();
	    for (String s : observations.keySet())
	    {
	    	classGMeanResults.put(s, new ArrayList<Double>());
	    }
	    for (List<Integer> geneSet : population)
	    {
	    	// Train and test the subsasmples.
    		ctrl.trainingObservations = geneSet;
	    	Forest forest = new Forest(datasetLocation, ctrl, weights);
	    	List<Integer> testObs = new ArrayList<Integer>(observationIndices);
	    	testObs.removeAll(geneSet);
	    	Map<String, Map<String, Double>> predictedConfusionMatrix = forest.predict(processedInputFile, testObs).second;
	    	// Determine the number of training observations in each class.
	    	Map<String, Integer> trainingSetClasses = new HashMap<String, Integer>();
	    	for (String s : observations.keySet())
	    	{
	    		int numberInTrainingSet = 0;
	    		List<Integer> observationsInClass = observations.get(s);
	    		for (Integer i : geneSet)
	    		{
	    			if (observationsInClass.contains(i))
	    			{
	    				numberInTrainingSet++;
	    			}
	    		}
	    		trainingSetClasses.put(s, numberInTrainingSet);
	    	}
	    	// Determine the macro f measure.
	    	double macroGMean = 1.0;
	    	for (String s : predictedConfusionMatrix.keySet())
	    	{
	    		double TP = predictedConfusionMatrix.get(s).get("TruePositive");
	    		double FN = observations.get(s).size() - trainingSetClasses.get(s) - TP;  // The number of false positives is the number of observaitons from the class in the testing set - the number of true positives.
	    		double recall = TP / (TP + FN);
	    		macroGMean *= recall;
		    	classGMeanResults.get(s).add(recall);
	    	}
	    	macroGMean = Math.pow(macroGMean, (1.0 / observations.size()));
	    	numberEvaluations += 1;
	    	fitness.add(macroGMean);
	    }

	    while (loopTermination(currentGeneration, maxGenerations, numberEvaluations, maxEvaluations, gaStartTime, maxTimeAllowed, convergencesOccurred, maxConvergences))
	    {

	    	if (verbose)
	    	{
	    		DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    Date now = new Date();
			    String strDate = sdfDate.format(now);
	    		System.out.format("Now starting generation number : %d at %s.\n", currentGeneration, strDate);
	    	}

	    	// Generate mutants for possible inclusion in the next generation.
	    	List<List<Integer>> mutants = new ArrayList<List<Integer>>();
	    	boolean isOffspringCreated = false;
	    	for (int i = 0; i < populationSize / 2; i++)
	    	{
	    		// Select the parents (no preference given to fitter parents).
	    		Collections.shuffle(parentSelector);
	    		List<Integer> parentOne = population.get(parentSelector.get(0));
	    		List<Integer> parentTwo = population.get(parentSelector.get(1));

	    		// Determine if the selected parents can undergo combination.
	    		List<List<Integer>> nonMatchingObs = hammingDistance(parentOne, parentTwo);
	    		int distanceBetweenParents = nonMatchingObs.size();
	    		if (distanceBetweenParents > threshold)
	    		{
	    			isOffspringCreated = true;
	    			Collections.shuffle(nonMatchingObs);
	    			List<List<Integer>> toCrossover = new ArrayList<List<Integer>>(nonMatchingObs.subList(0, distanceBetweenParents / 2));
	    			List<Integer> childOne = new ArrayList<Integer>(parentOne);
	    			List<Integer> childTwo = new ArrayList<Integer>(parentTwo);
	    			for (List<Integer> l : toCrossover)
	    			{
	    				Integer obs = l.get(1);
	    				if (l.get(0) == 1)
	    				{
	    					childOne.remove(obs);
	    					childTwo.add(obs);
	    				}
	    				else
	    				{
	    					childTwo.remove(obs);
	    					childOne.add(obs);
	    				}
	    			}
	    			mutants.add(childOne);
	    			mutants.add(childTwo);
	    		}
	    	}

	    	if (isOffspringCreated)
	    	{
	    		// Calculate the fitness of the offspring.
		    	for (List<Integer> geneSet : mutants)
		 	    {
		    		population.add(geneSet);
		    		// Train and test the subsamples.
		    		ctrl.trainingObservations = geneSet;
			    	Forest forest = new Forest(datasetLocation, ctrl, weights);
			    	List<Integer> testObs = new ArrayList<Integer>(observationIndices);
			    	testObs.removeAll(geneSet);
			    	Map<String, Map<String, Double>> predictedConfusionMatrix = forest.predict(processedInputFile, testObs).second;
			    	// Determine the number of training observations in each class.
			    	Map<String, Integer> trainingSetClasses = new HashMap<String, Integer>();
			    	for (String s : observations.keySet())
			    	{
			    		int numberInTrainingSet = 0;
			    		List<Integer> observationsInClass = observations.get(s);
			    		for (Integer i : geneSet)
			    		{
			    			if (observationsInClass.contains(i))
			    			{
			    				numberInTrainingSet++;
			    			}
			    		}
			    		trainingSetClasses.put(s, numberInTrainingSet);
			    	}
			    	// Determine the macro f measure.
			    	double macroGMean = 1.0;
			    	for (String s : predictedConfusionMatrix.keySet())
			    	{
			    		double TP = predictedConfusionMatrix.get(s).get("TruePositive");
			    		double FN = observations.get(s).size() - trainingSetClasses.get(s) - TP;  // The number of false positives is the number of observaitons from the class in the testing set - the number of true positives.
			    		double recall = TP / (TP + FN);
			    		macroGMean *= recall;
				    	classGMeanResults.get(s).add(recall);
			    	}
			    	macroGMean = Math.pow(macroGMean, (1.0 / observations.size()));
			    	fitness.add(macroGMean);
		 	    	numberEvaluations += 1;
			    }
	    	}

	    	// Update the population.
		    List<IndexedDoubleData> sortedPopulation = new ArrayList<IndexedDoubleData>();
		    for (int j = 0; j < population.size(); j++)
		    {
		    	sortedPopulation.add(new IndexedDoubleData(fitness.get(j), j));
		    }
		    Collections.sort(sortedPopulation, Collections.reverseOrder());  // Sort the indices of the list in descending order by F score.
		    List<List<Integer>> newPopulation = new ArrayList<List<Integer>>();
		    List<Double> newFitness = new ArrayList<Double>();
		    Map<String, List<Double>> newClassFMeasures = new HashMap<String, List<Double>>();
		    for (String s : observations.keySet())
		    {
		    	newClassFMeasures.put(s, new ArrayList<Double>());
		    }
		    for (int j = 0; j < populationSize; j ++)
		    {
		    	// Add the first populationSize population members with the lowest error rates.
		    	int indexToAddFrom = sortedPopulation.get(j).getIndex();
		    	newPopulation.add(population.get(indexToAddFrom));
		    	newFitness.add(fitness.get(indexToAddFrom));
		    	for (String s : observations.keySet())
		    	{
		    		newClassFMeasures.get(s).add(classGMeanResults.get(s).get(indexToAddFrom));
		    	}
		    	if (indexToAddFrom > populationSize)
		    	{
		    		// If this is true, the an offspring has been added to the updated population.
		    		populationLastChanged++;
		    	}
		    }
		    population = newPopulation;
		    fitness = newFitness;
		    classGMeanResults = newClassFMeasures;

		    if (populationLastChanged == maxStagnant)
	    	{
	    		populationLastChanged = 0;
	    		threshold -= 1;

	    		if (threshold < 1)
	    		{
	    			// Write out best individual at convergence.
	    			try
	    			{
		    			FileWriter convergenceOutputFile = new FileWriter(convergenOutputLocation, true);
		    			BufferedWriter convergenceOutputWriter = new BufferedWriter(convergenceOutputFile);
		    			convergenceOutputWriter.write(Integer.toString(currentGeneration));
		    			convergenceOutputWriter.write("\t");
		    			convergenceOutputWriter.write(Double.toString(fitness.get(0)));
		    			convergenceOutputWriter.write("\t");
		    			convergenceOutputWriter.write(population.get(0).toString());
		    			convergenceOutputWriter.newLine();
		    			convergenceOutputWriter.close();
	    			}
		    		catch (Exception e)
		    		{
		    			e.printStackTrace();
		    			System.exit(0);
		    		}

	    			// Record the fact that a convergence has occurred.
	    			convergencesOccurred++;

	    			threshold = initialSetSize / 4;
	    			// Generate the new population by copying over the best individuals found so far, and then randomly instantiating the rest of the population.
	    			population = new ArrayList<List<Integer>>(this.bestMembersFound);
	    			for (int i = 0; i < populationSize; i++)
	    			{
	    				List<Integer> newPopMember = new ArrayList<Integer>();
	    				for (String s : observations.keySet())
	    				{
	    					List<Integer> availableForSelection = new ArrayList<Integer>(observations.get(s));
	    					for (int j = 0; j < fractionsToSelect.get(s); j++)
	    					{
	    						// Select a random available observation from class s.
	    						Integer chosenObservation = availableForSelection.get(observationSelector.nextInt(availableForSelection.size()));
	    						newPopMember.add(chosenObservation);
	    						availableForSelection.remove(chosenObservation);
	    						if (availableForSelection.isEmpty())
	    						{
	    							availableForSelection = new ArrayList<Integer>(observations.get(s));
	    						}
	    					}
	    				}
	    				population.add(newPopMember);
	    			}
	    			// Calculate the fitness of the new population.
	    		    fitness = new ArrayList<Double>();
	    		    for (String s : observations.keySet())
	    		    {
	    		    	classGMeanResults.put(s, new ArrayList<Double>());
	    		    }
	    		    for (List<Integer> geneSet : population)
	    		    {
	    		    	// Train and test the subsamples.
	    	    		ctrl.trainingObservations = geneSet;
	    		    	Forest forest = new Forest(datasetLocation, ctrl, weights);
	    		    	List<Integer> testObs = new ArrayList<Integer>(observationIndices);
	    		    	testObs.removeAll(geneSet);
	    		    	Map<String, Map<String, Double>> predictedConfusionMatrix = forest.predict(processedInputFile, testObs).second;
	    		    	// Determine the number of training observations in each class.
	    		    	Map<String, Integer> trainingSetClasses = new HashMap<String, Integer>();
	    		    	for (String s : observations.keySet())
	    		    	{
	    		    		int numberInTrainingSet = 0;
	    		    		List<Integer> observationsInClass = observations.get(s);
	    		    		for (Integer i : geneSet)
	    		    		{
	    		    			if (observationsInClass.contains(i))
	    		    			{
	    		    				numberInTrainingSet++;
	    		    			}
	    		    		}
	    		    		trainingSetClasses.put(s, numberInTrainingSet);
	    		    	}
	    		    	// Determine the macro f measure.
	    		    	double macroGMean = 1.0;
	    		    	for (String s : predictedConfusionMatrix.keySet())
	    		    	{
	    		    		double TP = predictedConfusionMatrix.get(s).get("TruePositive");
	    		    		double FN = observations.get(s).size() - trainingSetClasses.get(s) - TP;  // The number of false positives is the number of observaitons from the class in the testing set - the number of true positives.
	    		    		double recall = TP / (TP + FN);
	    		    		macroGMean *= recall;
	    			    	classGMeanResults.get(s).add(recall);
	    		    	}
	    		    	macroGMean = Math.pow(macroGMean, (1.0 / observations.size()));
				    	fitness.add(macroGMean);
	    		    	numberEvaluations += 1;
	    		    }
	    		}
	    	}

		    // Write out the statistics of the population.
	    	writeOutStatus(fitnessDirectoryLocation, fitness, populationDirectoryLocation, population, currentGeneration,
	    			genStatsOutputLocation, populationSize, threshold, numberEvaluations, classGMeanResults, classFMeasureOutputLocations);

	    	if (fitness.get(0) > this.currentBestFitness)
	    	{
	    		// If the fitness has improved during this generation. The fitness of the most fit individual can not get worse, so if it
	    		// is not the same then it must have improved.
	    		this.currentBestFitness = fitness.get(0);
	    		this.bestMembersFound = new ArrayList<List<Integer>>();  // Clear out the list of the best individuals found as there is a new top fitness.
	    	}
	    	// Add all the members with the best fitness to the set of best individuals found.
	    	for (int i = 0; i < populationSize; i++)
	    	{
	    		if ((fitness.get(i) == this.currentBestFitness) && (!this.bestMembersFound.contains(population.get(i))))
	    		{
	    			// If the individual in position i has the best fitness of any individual found, and
	    			// the individual is not already recorded as having the best fitness found (i.e. a new individual has been found
	    			// that has the same fitness as the most fit individual already found).
	    			this.bestMembersFound.add(population.get(i));
	    		}
	    	}
	    	currentGeneration += 1;
	    }

	    // Write out the final information about time taken/generation performed/fitness evaluations.
	    try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterOutputLocation, true);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date now = new Date();
		    String strDate = sdfDate.format(now);
		    parameterOutputWriter.write("Time Finished:\t" + strDate);
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Evaluations Performed:\t" + Integer.toString(numberEvaluations));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Generation Reached:\t" + Integer.toString(currentGeneration));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

	    // Write out the statistics of the final population.
    	writeOutStatus(fitnessDirectoryLocation, fitness, populationDirectoryLocation, population, currentGeneration,
    			genStatsOutputLocation, populationSize, threshold, numberEvaluations, classGMeanResults, classFMeasureOutputLocations);

	    // Write out the best member(s) of the population.
	    Set<List<Integer>> recordedIndividuals = new HashSet<List<Integer>>();
	    try
		{
	    	String bestIndivOutputLocation = outputLocation + "/BestIndividuals.txt";
			FileWriter bestIndivOutputFile = new FileWriter(bestIndivOutputLocation);
			BufferedWriter bestIndivOutputWriter = new BufferedWriter(bestIndivOutputFile);
			bestIndivOutputWriter.write("Fitness\t");
			bestIndivOutputWriter.write(Double.toString(this.currentBestFitness));
			bestIndivOutputWriter.newLine();
			for (int i = 0; i < this.bestMembersFound.size(); i++)
			{
				List<Integer> currentMember = this.bestMembersFound.get(i);
				if (!recordedIndividuals.contains(currentMember))
				{
					recordedIndividuals.add(currentMember);
					bestIndivOutputWriter.write(currentMember.toString());
				    bestIndivOutputWriter.newLine();
				}
			}
		    bestIndivOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

	}

	List<List<Integer>> hammingDistance(List<Integer> parentOne, List<Integer> parentTwo)
	{
		List<List<Integer>> nonMatchingObs = new ArrayList<List<Integer>>();
		for (Integer i : parentOne)
		{
			if (!parentTwo.contains(i))
			{
				List<Integer> nonMatch = new ArrayList<Integer>();
				nonMatch.add(1);
				nonMatch.add(i);
				nonMatchingObs.add(nonMatch);
			}
		}
		for (Integer i : parentTwo)
		{
			if (!parentOne.contains(i))
			{
				List<Integer> nonMatch = new ArrayList<Integer>();
				nonMatch.add(2);
				nonMatch.add(i);
				nonMatchingObs.add(nonMatch);
			}
		}
		return nonMatchingObs;
	}

	boolean loopTermination(int currentGen, int maxGens, int currentEvals, int maxEvals, Date startTime, long maxTimeAllowed,
			int convergencesOccurred, int maxConvergences)
	{
		boolean isGenNotStopping = false;
		boolean isEvalNotStopping = false;
		boolean isTimeNotStopping = false;
		boolean isConvergenceNotStopping = false;

		if (maxGens != 0)
	    {
	        // Using the number of generations as a stopping criterion.
			isGenNotStopping = currentGen <= maxGens;
	    }
		else
		{
			isGenNotStopping = true;
		}
	    if (maxEvals != 0)
	    {
	        // Using the number of fitness function evaluations as a stopping criterion.
	    	isEvalNotStopping = currentEvals < maxEvals;
	    }
	    else
	    {
	    	isEvalNotStopping = true;
	    }
	    if (maxTimeAllowed != 0)
	    {
	    	// Using a time limit.
	    	Date currentTime = new Date();
	    	long timeElapsed = currentTime.getTime() - startTime.getTime();
	    	isTimeNotStopping = timeElapsed < maxTimeAllowed;
	    }
	    else
	    {
	    	isTimeNotStopping = true;
	    }
	    if (maxConvergences != 0)
	    {
	    	// Using a limit to the number of convergences.
	    	isConvergenceNotStopping = convergencesOccurred < maxConvergences;
	    }
	    else
	    {
	    	isConvergenceNotStopping = true;
	    }

	    return isGenNotStopping && isEvalNotStopping && isTimeNotStopping && isConvergenceNotStopping;
	}

	void writeOutStatus(String fitnessDirectoryLocation, List<Double> fitness, String populationDirectoryLocation,
			List<List<Integer>> population, int currentGeneration, String genStatsOutputLocation, int populationSize,
			int threshold, int numberEvaluations, Map<String, List<Double>> classGMeanResults, Map<String, String> classFMeasureOutputLocations)
	{
		// Write out the fitness info for the current generation.
		String fitnessOutputLocation = fitnessDirectoryLocation + "/" + Integer.toString(currentGeneration) + ".txt";
		try
		{
			FileWriter fitnessOutputFile = new FileWriter(fitnessOutputLocation);
			BufferedWriter fitnessOutputWriter = new BufferedWriter(fitnessOutputFile);
			for (double d : fitness)
			{
				fitnessOutputWriter.write(Double.toString(d));
				fitnessOutputWriter.newLine();
			}
			fitnessOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Write out the population information for the current generation.
		String populationOutputLocation = populationDirectoryLocation + "/" + Integer.toString(currentGeneration) + ".txt";
		double meanPopulationSize = 0.0;
		try
		{
			FileWriter populationOutputFile = new FileWriter(populationOutputLocation);
			BufferedWriter populationOutputWriter = new BufferedWriter(populationOutputFile);
			for (List<Integer> p : population)
			{
				populationOutputWriter.write(p.toString());
				populationOutputWriter.newLine();
				meanPopulationSize += p.size();
			}
			populationOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		meanPopulationSize /= populationSize;

		// Calculate the mean and median fitness for the current generation.
		double meanErrorRate = 0.0;
		double medianErrorRate = 0.0;
		double stdDevErrorRate = 0.0;
		for (Double d : fitness)
		{
			meanErrorRate += d;
		}
		meanErrorRate /= populationSize;
		if (populationSize % 2 == 0)
		{
			// If the size of the population is even.
			int midPointOne = populationSize / 2;
			int midPointTwo = midPointOne - 1;
			medianErrorRate = (fitness.get(midPointOne) + fitness.get(midPointTwo)) / 2.0;
		}
		else
		{
			medianErrorRate = fitness.get(populationSize / 2);  // Works as integer division causes this to be rounded down.
		}
		double squaredDiffWithMean = 0.0;
		for (Double d : fitness)
		{
			squaredDiffWithMean += Math.pow(d - meanErrorRate, 2);
		}
		stdDevErrorRate = Math.pow(squaredDiffWithMean / populationSize, 0.5);

		Map<String, List<Double>> classResults = new HashMap<String, List<Double>>();
		for (String s : classGMeanResults.keySet())
		{
			// Calculate the mean and median fitness for the current generation.
			double meanClassFMeasure = 0.0;
			double medianClassFMeasure = 0.0;
			double stdDevClassFMeasure = 0.0;
			for (Double d : classGMeanResults.get(s))
			{
				meanClassFMeasure += d;
			}
			meanClassFMeasure /= populationSize;
			if (populationSize % 2 == 0)
			{
				// If the size of the population is even.
				int midPointOne = populationSize / 2;
				int midPointTwo = midPointOne - 1;
				medianClassFMeasure = (classGMeanResults.get(s).get(midPointOne) + classGMeanResults.get(s).get(midPointTwo)) / 2.0;
			}
			else
			{
				medianClassFMeasure = classGMeanResults.get(s).get(populationSize / 2);  // Works as integer division causes this to be rounded down.
			}
			squaredDiffWithMean = 0.0;
			for (Double d : classGMeanResults.get(s))
			{
				squaredDiffWithMean += Math.pow(d - meanClassFMeasure, 2);
			}
			stdDevClassFMeasure = Math.pow(squaredDiffWithMean / populationSize, 0.5);

			List<Double> resultsList = new ArrayList<Double>();
			resultsList.add(classGMeanResults.get(s).get(0));
			resultsList.add(meanClassFMeasure);
			resultsList.add(medianClassFMeasure);
			resultsList.add(stdDevClassFMeasure);
			classResults.put(s, resultsList);
		}

		// Write out the fitness statistics for the current generation.
		try
		{
			FileWriter genStatsOutputFile = new FileWriter(genStatsOutputLocation, true);
			BufferedWriter genStatsOutputWriter = new BufferedWriter(genStatsOutputFile);
			genStatsOutputWriter.write(Integer.toString(currentGeneration));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Double.toString(fitness.get(0)));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Double.toString(meanErrorRate));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Double.toString(medianErrorRate));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Double.toString(stdDevErrorRate));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Integer.toString(population.get(0).size()));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Double.toString(meanPopulationSize));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Integer.toString(threshold));
			genStatsOutputWriter.write("\t");
			genStatsOutputWriter.write(Integer.toString(numberEvaluations));
			genStatsOutputWriter.newLine();
			genStatsOutputWriter.close();

			for (String s : classGMeanResults.keySet())
			{
				FileWriter classStatsOutputFile = new FileWriter(classFMeasureOutputLocations.get(s), true);
				BufferedWriter classStatsOutputWriter = new BufferedWriter(classStatsOutputFile);
				classStatsOutputWriter.write(Integer.toString(currentGeneration));
				classStatsOutputWriter.write("\t");
				classStatsOutputWriter.write(Double.toString(classResults.get(s).get(0)));
				classStatsOutputWriter.write("\t");
				classStatsOutputWriter.write(Double.toString(classResults.get(s).get(1)));
				classStatsOutputWriter.write("\t");
				classStatsOutputWriter.write(Double.toString(classResults.get(s).get(2)));
				classStatsOutputWriter.write("\t");
				classStatsOutputWriter.write(Double.toString(classResults.get(s).get(3)));
				classStatsOutputWriter.newLine();
				classStatsOutputWriter.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
