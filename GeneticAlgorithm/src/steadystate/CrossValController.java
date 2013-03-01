/**
 * 
 */
package steadystate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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
import java.util.Set;

import tree.Forest;
import tree.IndexedDoubleData;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class CrossValController
{

	/**
	 * The record of the top fitness found during the most recent run of the GA.
	 */
	public double currentBestFitness = 100.0;

	/**
	 * A set of the individuals that have the best fitness found.
	 */
	public Set<Integer[]> bestMembersFound = new HashSet<Integer[]>();

	public CrossValController(String[] args)
	{
		// Initialise the controller for the dataset determination and forest growing.
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.numberOfTreesToGrow = 100;
		run(args, ctrl, new HashMap<String, Double>());
	}

	public CrossValController(String[] args, TreeGrowthControl ctrl)
	{
		run(args, ctrl, new HashMap<String, Double>());
	}

	public CrossValController(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights)
	{
		run(args, ctrl, weights);
	}

	/**
	 * @param args
	 * @param negDataset
	 * @param posDataset
	 * @param numberOfObs
	 * @param ctrl
	 */
	void run(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights)
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
		int populationSize = 20;  // The size of the population to use for the GA.
		int maxGenerations = 100;  // The number of generations to run the GA for.
		int maxEvaluations = 0;  // The maximum number of fitness evaluations to perform.
		double replacementRate = 0.2;  // The fraction of the population to be replaced at each generation.
		int numberParents = 2;  // The number of parents for each child.
		double mutationRate = 0.005;  // The mutation rate to use.
		int maxStagnantGenerations = -1;  // The maximum number of generations to go without fitness change;
		boolean verbose = false;  // Whether status updates should be displayed.

		// Read in the user input.
		int argIndex = 3;
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
			case "-r":
				argIndex += 1;
				replacementRate = Double.parseDouble(args[argIndex]);
				argIndex += 1;
				break;
			case "-a":
				argIndex += 1;
				numberParents = Integer.parseInt(args[argIndex]);
				argIndex += 1;
				break;
			case "-m":
				argIndex += 1;
				mutationRate = Double.parseDouble(args[argIndex]);
				argIndex += 1;
				break;
			case "-s":
				argIndex += 1;
				maxStagnantGenerations = Integer.parseInt(args[argIndex]);
				argIndex += 1;
				break;
			case "-v":
				verbose = true;
				argIndex += 1;
				break;
			default:
				System.out.format("Unexpeted argument : %s.\n", currentArg);
				System.exit(0);
			}
		}

		// Write out the parameters used for the GA.
		String parameterOutputLocation = outputLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterOutputLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Replacement Rate:\t" + Double.toString(replacementRate));
			parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Mutation Rate:\t" + Double.toString(mutationRate));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Number of Parents:\t" + Integer.toString(numberParents));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Population Size:\t" + Integer.toString(populationSize));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Number of Generations:\t" + Integer.toString(maxGenerations));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Number of Evaluations:\t" + Integer.toString(maxEvaluations));
		    parameterOutputWriter.newLine();
		    parameterOutputWriter.write("Generations Permitted with no Fitness Change:\t" + Integer.toString(maxStagnantGenerations));
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
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

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

		// Setup the generation stats file.
		String genStatsOutputLocation = outputLocation + "/GenerationStatistics.txt";
		try
		{
			FileWriter genStatsOutputFile = new FileWriter(genStatsOutputLocation);
			BufferedWriter genStatsOutputWriter = new BufferedWriter(genStatsOutputFile);
			genStatsOutputWriter.write("Generation\tBestErrorRate\tMeanErrorRate\tMedianErrorRate\tStdDevErrorRate");
			genStatsOutputWriter.newLine();
			genStatsOutputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

		// Initialise the stopping criteria for the GA.
	    int currentGeneration = 1;
	    int numberEvaluations = 0;
	    if (maxGenerations == 0 && maxEvaluations == 0 && maxStagnantGenerations < 0)
		{
	        // No stopping criteria given.
	        System.out.println("At least one of -g, -e or -s must be given, otherwise there are no stopping criteria.");
	        System.exit(0);
		}
	    if (maxStagnantGenerations < 0)
	    {
	        // If stagnant generations are not chosen:
	        maxStagnantGenerations = maxGenerations;
	    }

		// Determine the number of genes/features in the dataset.
		int numberGenes = 0;
		String[] geneNames = null;
		try
		{
			BufferedReader geneReader = new BufferedReader(new FileReader(inputLocation));
			String header = geneReader.readLine();
			geneNames = header.split("\t");
			numberGenes = geneNames.length - 1;  // Subtract one for the class column in the dataset.
			geneReader.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

		//--------------------
		// Get the cross validation information
		//--------------------
		String subDirs[] = inputCrossValDirectory.list();
		String crossValDirLoc = inputCrossValDirectory.getAbsolutePath();
		List<List<Object>> crossValFiles = new ArrayList<List<Object>>();
		for (String s : subDirs)
		{
			List<Object> trainTestLocs = new ArrayList<Object>();
			trainTestLocs.add(crossValDirLoc + "/" + s + "/Train.txt");
			trainTestLocs.add(new ProcessDataForGrowing(crossValDirLoc + "/" + s + "/Test.txt", ctrl));
			crossValFiles.add(trainTestLocs);
		}

		//----------------------
		// Begin the GA.
		//----------------------

		// Initialise the random number generator.
		Random random = new Random();
		
		// Generate the initial population.
		List<Integer[]> population = new ArrayList<Integer[]>();
		List<Integer> parentSelector = new ArrayList<Integer>();
		for (int i = 0; i < populationSize; i++)
		{
			Integer newPopMember[] = new Integer[numberGenes];
			for (int j = 0; j < numberGenes; j++)
			{
				int currentGeneValue = random.nextInt(2); // Generate a 0 or 1.
				newPopMember[j] = currentGeneValue;
			}
			population.add(newPopMember);
			parentSelector.add(i);
		}

	    // Set the number of individuals to replace each round.
	    double numbertoReplace = Math.round(populationSize * replacementRate);

	    // Initialise the number of generations since the fitness last changed.
	    this.currentBestFitness = 100.0;
	    int generationsOfStagnation = 0;

	    // Select all the chromosomes that are made up entirely of 0s, i.e. no features used, and alter them to include a 1.
    	for (int i = 0; i < populationSize; i++)
    	{
    		boolean isFeatureUsed = false;
    		for (int j = 0; j < numberGenes; j++)
    		{
    			if (population.get(i)[j] == 1)
    			{
    				// There is a feature being used.
    				isFeatureUsed = true;
    				break;
    			}
    		}
    		if (!isFeatureUsed)
    		{
    			// If no feature are being used in this population member, then set a random feature to be used.
    			population.get(i)[random.nextInt(numberGenes + 1)] = 1;
    		}
    	}

    	// Determine the weights to use if none are specified.
    	if (weights.size() == 0)
    	{
    		weights = determineWeights(args[0], ctrl);
    	}

	    // Calculate the fitness of the initial population.
	    List<Double> fitness = new ArrayList<Double>();
	    for (Integer[] geneSet : population)
	    {
	    	List<String> variablesToIgnore = new ArrayList<String>();
	    	for (int i = 0; i < numberGenes; i++)
	    	{
	    		if (geneSet[i] == 0)
	    		{
	    			// If the gene is to be ignored for this member.
	    			variablesToIgnore.add(geneNames[i]);
	    		}
	    	}
	    	ctrl.variablesToIgnore = variablesToIgnore;
	    	double cumulativeError = 0.0;
	    	for (List<Object> l : crossValFiles)
	    	{
	    		Forest forest = new Forest((String) l.get(0), ctrl, weights);
	    		cumulativeError += forest.predict((ProcessDataForGrowing) l.get(1)).first;
	    	}
	    	fitness.add(cumulativeError / crossValFiles.size());
	    	numberEvaluations += 1;
	    }

	    // Sort the initial population.
	    List<IndexedDoubleData> sortedInitialPopulation = new ArrayList<IndexedDoubleData>();
	    for (int j = 0; j < population.size(); j++)
	    {
	    	sortedInitialPopulation.add(new IndexedDoubleData(fitness.get(j), j));
	    }
	    Collections.sort(sortedInitialPopulation);  // Sort the indices of the list in ascending order by error rate.
	    List<Integer[]> newInitialPopulation = new ArrayList<Integer[]>();
	    List<Double> newInitialFitness = new ArrayList<Double>();
	    for (int j = 0; j < populationSize; j ++)
	    {
	    	// Add the first populationSize population members with the lowest error rates.
	    	int indexToAddFrom = sortedInitialPopulation.get(j).getIndex();
	    	newInitialPopulation.add(population.get(indexToAddFrom));
	    	newInitialFitness.add(fitness.get(indexToAddFrom));
	    }
	    population = newInitialPopulation;
	    fitness = newInitialFitness;

	    while (loopTermination(currentGeneration, maxGenerations, numberEvaluations, maxEvaluations, generationsOfStagnation, maxStagnantGenerations))
	    {

	    	if (verbose)
	    	{
	    		DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    Date now = new Date();
			    String strDate = sdfDate.format(now);
	    		System.out.format("Now starting generation number : %d at %s.\n", currentGeneration, strDate);
	    	}

	    	// Write out the statistics of the population.
	    	writeOutStatus(fitnessDirectoryLocation, fitness, populationDirectoryLocation, population, currentGeneration,
	    			genStatsOutputLocation, populationSize);

	    	// Generate mutants for possible inclusion in the next generation.
	    	List<Integer[]> mutants = new ArrayList<Integer[]>();
	    	for (int i = 0; i < numbertoReplace; i++)
	    	{
	    		// Select the parents (no preference given to fitter parents).
	    		Collections.shuffle(parentSelector);
	    		List<Integer[]> parents = new ArrayList<Integer[]>();
	    		for (int j = 0; j < numberParents; j++)
	    		{
	    			parents.add(population.get(parentSelector.get(j)));
	    		}

	    		// Perform crossover (done by setting each of the offspring's genes n to gene n from a random parent).
	    		// Mutate the resultant offspring.
	    		Integer offspring[] = new Integer[numberGenes];
	    		for (int j = 0; j < numberGenes; j++)
	    		{
	    			// Select the gene from the random parent, and possibly mutate.
	    			int chosenParentValue = parents.get(random.nextInt(numberParents))[j];
	    			boolean isMutate = random.nextDouble() < mutationRate;
	    			if (isMutate)
	    			{
	    				// Perform bit flip mutation.
	    				offspring[j] = (chosenParentValue - 1) * -1;  // (0 - 1) * -1 == 1 and (1 - 1) * -1 == 0 so it flips bits without an if statement.
	    			}
	    			else
	    			{
	    				// No mutation.
	    				offspring[j] = chosenParentValue;
	    			}
	    		}

	    		mutants.add(offspring);
	    	}

	    	// Calculate the fitness of the offspring.
	    	List<Double> offspringFitness = new ArrayList<Double>();
		    for (Integer[] geneSet : mutants)
		    {
		    	List<String> variablesToIgnore = new ArrayList<String>();
		    	for (int i = 0; i < numberGenes; i++)
		    	{
		    		if (geneSet[i] == 0)
		    		{
		    			// If the gene is to be ignored for this member.
		    			variablesToIgnore.add(geneNames[i]);
		    		}
		    	}
		    	ctrl.variablesToIgnore = variablesToIgnore;
		    	double cumulativeError = 0.0;
		    	for (List<Object> l : crossValFiles)
		    	{
		    		Forest forest = new Forest((String) l.get(0), ctrl, weights);
		    		cumulativeError += forest.predict((ProcessDataForGrowing) l.get(1)).first;
		    	}
		    	offspringFitness.add(cumulativeError / crossValFiles.size());
		    	numberEvaluations += 1;
		    }

	    	// Update the population.
		    for (int j = 0; j < numbertoReplace; j++)
		    {
		    	// Extend the population and the fitnesses to include the newly created offspring.
		    	population.add(mutants.get(j));
		    	fitness.add(offspringFitness.get(j));
		    }
		    List<IndexedDoubleData> sortedPopulation = new ArrayList<IndexedDoubleData>();
		    for (int j = 0; j < population.size(); j++)
		    {
		    	sortedPopulation.add(new IndexedDoubleData(fitness.get(j), j));
		    }
		    Collections.sort(sortedPopulation);  // Sort the indices of the list in ascending order by error rate.
		    List<Integer[]> newPopulation = new ArrayList<Integer[]>();
		    List<Double> newFitness = new ArrayList<Double>();
		    for (int j = 0; j < populationSize; j ++)
		    {
		    	// Add the first populationSize population members with the lowest error rates.
		    	int indexToAddFrom = sortedPopulation.get(j).getIndex();
		    	newPopulation.add(population.get(indexToAddFrom));
		    	newFitness.add(fitness.get(indexToAddFrom));
		    }
		    population = newPopulation;
		    fitness = newFitness;

	    	if (fitness.get(0) == this.currentBestFitness)
	    	{
	    		// If there is no improvement in the fitness during this generation.
	    		generationsOfStagnation += 1;
	    	}
	    	else
	    	{
	    		// If the fitness has improved during this generation. The fitness of the most fit individual can not get worse, so if it
	    		// is not the same then it must have improved.
	    		generationsOfStagnation = 0;
	    		this.currentBestFitness = fitness.get(0);
	    		this.bestMembersFound= new HashSet<Integer[]>();  // Clear out the set of the best individuals found as there is a new top fitness.
	    	}
	    	// Add all the members with the best fitness to the set of best individuals found.
	    	for (int i = 0; i < populationSize; i++)
	    	{
	    		if (fitness.get(i) == this.currentBestFitness)
	    		{
	    			// If the individual in position i has the best fitness of any individual found.
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
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

	    // Write out the best member(s) of the population.
	    try
		{
	    	String bestIndivOutputLocation = outputLocation + "/BestIndividuals.txt";
			FileWriter bestIndivOutputFile = new FileWriter(bestIndivOutputLocation);
			BufferedWriter bestIndivOutputWriter = new BufferedWriter(bestIndivOutputFile);
			bestIndivOutputWriter.write("Fitness : ");
			bestIndivOutputWriter.write(Double.toString(this.currentBestFitness));
			bestIndivOutputWriter.newLine();
			for (Integer[] i : this.bestMembersFound)
			{
				bestIndivOutputWriter.write(Integer.toString(i[0]));
				for (int j = 1; j < i.length; j++)
				{
					bestIndivOutputWriter.write(",");
					bestIndivOutputWriter.write(Integer.toString(i[j]));
				}
			    bestIndivOutputWriter.newLine();
			}
		    bestIndivOutputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

	}

	/**
	 * Determine the weighting of each class as its proportion of the total number of observations.
	 * 
	 * @param inputLocation
	 * @return
	 */
	Map<String, Double> determineWeights(String inputLocation, TreeGrowthControl ctrl)
	{
		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputLocation, ctrl);

		// Determine how often each class occurs.
		Map<String, Double> classCounts = new HashMap<String, Double>();
		for (String s : procData.responseData)
		{
			if (!classCounts.containsKey(s))
			{
				classCounts.put(s, 1.0);
			}
			else
			{
				classCounts.put(s, classCounts.get(s) + 1.0);
			}
		}

		// Find the number of occurrences of the class that occurs most often.
		double maxClass = 0.0;
		for (String s : classCounts.keySet())
		{
			if (classCounts.get(s) > maxClass)
			{
				maxClass = classCounts.get(s);
			}
		}

		// Determine the weighting of each class in relation to the class that occurs most often.
		// Weights the most frequent class as 1.
		// Two classes, A occurs 10 times and B 5 times. A gets a weight of 1 / (10 / 10) == 1.
		// B gets a weight of 1 / (5 / 10) == 2.
		Map<String, Double> classWeights = new HashMap<String, Double>();
		for (String s : classCounts.keySet())
		{
			classWeights.put(s, 1.0 / (classCounts.get(s) / maxClass));
		}

		return classWeights;
	}

	boolean loopTermination(int currentGen, int maxGens, int currentEvals, int maxEvals,
			int generationsOfStagnation, int maxStagnantGenerations)
	{
		boolean isGenStopping = false;
		boolean isEvalStopping = false;
		boolean isStagnantStopping = false;
		if (maxGens != 0)
	    {
	        // Using the number of generations as a stopping criterion.
			isGenStopping = currentGen <= maxGens;
	        
	    }
	    if (maxEvals != 0)
	    {
	        // Using the number of fitness function evaluations as a stopping criterion.
	    	isEvalStopping = currentEvals < maxEvals;
	    }
	    if (maxStagnantGenerations >= 0)
	    {
	    	// Using the number of generations without an improvement in the best fitness as a stopping criterion.
	    	isStagnantStopping = generationsOfStagnation >= maxStagnantGenerations;
	    }

	    return isGenStopping || isEvalStopping || isStagnantStopping;
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

	void writeOutStatus(String fitnessDirectoryLocation, List<Double> fitness, String populationDirectoryLocation,
			List<Integer[]> population, int currentGeneration, String genStatsOutputLocation, int populationSize)
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
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

		// Write out the population information for the current generation.
		String populationOutputLocation = populationDirectoryLocation + "/" + Integer.toString(currentGeneration) + ".txt";
		try
		{
			FileWriter populationOutputFile = new FileWriter(populationOutputLocation);
			BufferedWriter populationOutputWriter = new BufferedWriter(populationOutputFile);
			for (Integer[] p : population)
			{
				populationOutputWriter.write(Arrays.toString(p));
				populationOutputWriter.newLine();
			}
			populationOutputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

		// Calculate the mean and median fitness for the current generation.
		double meanErrorRate = 0.0;
		double medianErrorRate = 0.0;
		double stdDevErrorRate = 0.0;
		for (double d : fitness)
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
		for (double d : fitness)
		{
			squaredDiffWithMean += Math.pow(d - meanErrorRate, 2);
		}
		stdDevErrorRate = Math.pow(squaredDiffWithMean / populationSize, 0.5);

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
			genStatsOutputWriter.newLine();
			genStatsOutputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}
	}

}