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
import tree.IndexedDoubleLongData;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class Controller
{

	/**
	 * The record of the top fitness found during the most recent run of the GA.
	 */
	public double currentBestFitness = 100.0;

	/**
	 * A list of the individuals that have the best fitness found.
	 */
	public List<Integer[]> bestMembersFound = new ArrayList<Integer[]>();

	/**
	 * The record of the seeds that produced the individuals with the best fitness.
	 */
	public List<Long> bestForestSeeds = new ArrayList<Long>();


	public Controller(String[] args)
	{
		// Initialise the controller for the dataset determination and forest growing.
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.numberOfTreesToGrow = 100;
		run(args, ctrl, new HashMap<String, Double>());
	}

	public Controller(String[] args, TreeGrowthControl ctrl)
	{
		run(args, ctrl, new HashMap<String, Double>());
	}

	public Controller(String[] args, TreeGrowthControl ctrl, Map<String, Double> weights)
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

		// Optional inputs.
		int populationSize = 50;  // The size of the population to use for the GA.
		int maxGenerations = 100;  // The number of generations to run the GA for.
		int maxEvaluations = 0;  // The maximum number of fitness evaluations to perform.
		double mutationRate = 0.35;  // The mutation rate to use.
		boolean verbose = false;  // Whether status updates should be displayed.
		long maxTimeAllowed = 0;  // What the maximum time allowed (in ms) for the run is. 0 indicates that timing is not used.

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
			case "-m":
				argIndex += 1;
				mutationRate = Double.parseDouble(args[argIndex]);
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
		    parameterOutputWriter.write("Mutation Rate:\t" + Double.toString(mutationRate));
		    parameterOutputWriter.newLine();
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

		// Setup the generation stats file.
		String genStatsOutputLocation = outputLocation + "/GenerationStatistics.txt";
		try
		{
			FileWriter genStatsOutputFile = new FileWriter(genStatsOutputLocation);
			BufferedWriter genStatsOutputWriter = new BufferedWriter(genStatsOutputFile);
			genStatsOutputWriter.write("Generation\tBestMCC\tMeanMCC\tMedianMCC\tStdDevMCC");
			genStatsOutputWriter.newLine();
			genStatsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Initialise the stopping criteria for the GA.
	    int currentGeneration = 1;
	    int numberEvaluations = 0;
	    if (maxGenerations == 0 && maxEvaluations == 0)
		{
	        // No stopping criteria given.
	        System.out.println("At least one of -g, -e or -s must be given, otherwise there are no stopping criteria.");
	        System.exit(0);
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
			e.printStackTrace();
			System.exit(0);
		}
		int threshold = numberGenes / 4;

		//----------------------
		// Begin the GA.
		//----------------------
		Date gaStartTime = new Date();

		// Initialise the random number generator, and the record of random seeds.
		Random random = new Random();
		List<Long> populationSeeds = new ArrayList<Long>();
		
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

		// Setup the negative and positive classes.
		String posClass = "Positive";
		String negClass = "Unlabelled";

	    // Initialise the number of generations since the fitness last changed.
	    this.currentBestFitness = 0.0;
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
    	// Write out the weights used for the forest.
		String weightOutputLocation = outputLocation + "/Weights.txt";
		try
		{
			FileWriter weightOutputFile = new FileWriter(weightOutputLocation);
			BufferedWriter weightOutputWriter = new BufferedWriter(weightOutputFile);
			for (String s : weights.keySet())
			{
				weightOutputWriter.write(s + "\t" + Double.toString(weights.get(s)));
				weightOutputWriter.newLine();
			}
			weightOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
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
	    	Forest forest = new Forest(inputLocation, ctrl, weights);
	    	Map<String, Map<String, Double>> oobConfusionMatrix = forest.oobConfusionMatrix;
	    	Double oobTP = oobConfusionMatrix.get(posClass).get("TruePositive");
			Double oobFP = oobConfusionMatrix.get(posClass).get("FalsePositive");
			Double oobTN = oobConfusionMatrix.get(negClass).get("TruePositive");
			Double oobFN = oobConfusionMatrix.get(negClass).get("FalsePositive");
			Double oobMCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
	    	fitness.add(oobMCC);
	    	populationSeeds.add(forest.seed);
	    	numberEvaluations += 1;
	    }

	    // Sort the initial population.
	    List<IndexedDoubleLongData> sortedInitialPopulation = new ArrayList<IndexedDoubleLongData>();
	    for (int j = 0; j < population.size(); j++)
	    {
	    	sortedInitialPopulation.add(new IndexedDoubleLongData(fitness.get(j), populationSeeds.get(j), j));
	    }
	    Collections.sort(sortedInitialPopulation, Collections.reverseOrder());  // Sort the indices of the list in descending order by OOB MCC.
	    List<Integer[]> newInitialPopulation = new ArrayList<Integer[]>();
	    List<Double> newInitialFitness = new ArrayList<Double>();
	    List<Long> newInitialSeeds = new ArrayList<Long>();
	    for (int j = 0; j < populationSize; j ++)
	    {
	    	// Add the first populationSize population members with the lowest error rates.
	    	int indexToAddFrom = sortedInitialPopulation.get(j).getIndex();
	    	newInitialPopulation.add(population.get(indexToAddFrom));
	    	newInitialFitness.add(fitness.get(indexToAddFrom));
	    	newInitialSeeds.add(populationSeeds.get(indexToAddFrom));
	    }
	    population = newInitialPopulation;
	    fitness = newInitialFitness;
	    populationSeeds = newInitialSeeds;

	    while (loopTermination(currentGeneration, maxGenerations, numberEvaluations, maxEvaluations, generationsOfStagnation, gaStartTime, maxTimeAllowed))
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
	    	boolean isOffspringCreated = false;
	    	for (int i = 0; i < populationSize / 2; i++)
	    	{
	    		// Select the parents (no preference given to fitter parents).
	    		Collections.shuffle(parentSelector);
	    		Integer[] parentOne = population.get(parentSelector.get(0));
	    		Integer[] parentTwo = population.get(parentSelector.get(1));

	    		// Determine if the selected parents can undergo combination.
	    		List<Integer> nonMatchingbits = hammingDistance(parentOne, parentTwo);
	    		int distanceBetweenParents = nonMatchingbits.size();
	    		if (distanceBetweenParents > threshold)
	    		{
	    			isOffspringCreated = true;
	    			Collections.shuffle(nonMatchingbits);
	    			List<Integer> toCrossover = new ArrayList<Integer>(nonMatchingbits.subList(0, distanceBetweenParents / 2));
	    			Integer childOne[] = new Integer[numberGenes];
	    			Integer childTwo[] = new Integer[numberGenes];
	    			for (int j = 0; j < numberGenes; j++)
	    			{
	    				if (toCrossover.contains(j))
	    				{
	    					childOne[j] = parentTwo[j];
	    					childTwo[j] = parentOne[j];
	    				}
	    				else
	    				{
	    					childOne[j] = parentOne[j];
	    					childTwo[j] = parentTwo[j];
	    				}
	    			}
	    			mutants.add(childOne);
	    			mutants.add(childTwo);
	    		}
	    	}

	    	System.out.format("Threshold : %d, Offspring created : %d.\n", threshold, mutants.size());

	    	if (isOffspringCreated)
	    	{
	    		// Calculate the fitness of the offspring.
		    	List<Double> offspringFitness = new ArrayList<Double>();
		    	List<Long> offspringSeeds = new ArrayList<Long>();
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
			    	Forest forest = new Forest(inputLocation, ctrl, weights);
			    	Map<String, Map<String, Double>> oobConfusionMatrix = forest.oobConfusionMatrix;
			    	Double oobTP = oobConfusionMatrix.get(posClass).get("TruePositive");
					Double oobFP = oobConfusionMatrix.get(posClass).get("FalsePositive");
					Double oobTN = oobConfusionMatrix.get(negClass).get("TruePositive");
					Double oobFN = oobConfusionMatrix.get(negClass).get("FalsePositive");
					Double oobMCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
			    	offspringFitness.add(oobMCC);
			    	offspringSeeds.add(forest.seed);
			    	numberEvaluations += 1;
			    }

			    // Update the population.
			    for (int j = 0; j < mutants.size(); j++)
			    {
			    	// Extend the population and the fitnesses to include the newly created offspring.
			    	population.add(mutants.get(j));
			    	fitness.add(offspringFitness.get(j));
			    	populationSeeds.add(offspringSeeds.get(j));
			    }
			    List<IndexedDoubleLongData> sortedPopulation = new ArrayList<IndexedDoubleLongData>();
			    for (int j = 0; j < population.size(); j++)
			    {
			    	sortedPopulation.add(new IndexedDoubleLongData(fitness.get(j), populationSeeds.get(j), j));
			    }
			    Collections.sort(sortedPopulation, Collections.reverseOrder());  // Sort the indices of the list in descending order by OOB MCC.
			    List<Integer[]> newPopulation = new ArrayList<Integer[]>();
			    List<Double> newFitness = new ArrayList<Double>();
			    List<Long> newSeeds = new ArrayList<Long>();
			    for (int j = 0; j < populationSize; j ++)
			    {
			    	// Add the first populationSize population members with the lowest error rates.
			    	int indexToAddFrom = sortedPopulation.get(j).getIndex();
			    	newPopulation.add(population.get(indexToAddFrom));
			    	newFitness.add(fitness.get(indexToAddFrom));
			    	newSeeds.add(populationSeeds.get(indexToAddFrom));
			    }
			    population = newPopulation;
			    fitness = newFitness;
			    populationSeeds = newSeeds;
	    	}
	    	else
	    	{
	    		threshold -= 1;
	    		if (threshold < 0)
	    		{
	    			// Generate the new population by copying over the best individuals found so far, and then randomly instantiating the rest of the population.
	    			population = new ArrayList<Integer[]>(new HashSet<Integer[]>(this.bestMembersFound));
	    			for (int i = 0; i < populationSize - population.size(); i++)
	    			{
	    				Integer newPopMember[] = new Integer[numberGenes];
	    				for (int j = 0; j < numberGenes; j++)
	    				{
	    					int currentGeneValue = random.nextInt(2); // Generate a 0 or 1.
	    					newPopMember[j] = currentGeneValue;
	    				}
	    				population.add(newPopMember);
	    			}
	    			// Calculate the fitness of the new population.
	    			populationSeeds = new ArrayList<Long>();
	    		    fitness = new ArrayList<Double>();
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
	    		    	Forest forest = new Forest(inputLocation, ctrl, weights);
	    		    	Map<String, Map<String, Double>> oobConfusionMatrix = forest.oobConfusionMatrix;
	    		    	Double oobTP = oobConfusionMatrix.get(posClass).get("TruePositive");
	    				Double oobFP = oobConfusionMatrix.get(posClass).get("FalsePositive");
	    				Double oobTN = oobConfusionMatrix.get(negClass).get("TruePositive");
	    				Double oobFN = oobConfusionMatrix.get(negClass).get("FalsePositive");
	    				Double oobMCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
	    		    	fitness.add(oobMCC);
	    		    	populationSeeds.add(forest.seed);
	    		    	numberEvaluations += 1;
	    		    }

	    		    // Sort the new population.
	    		    sortedInitialPopulation = new ArrayList<IndexedDoubleLongData>();
	    		    for (int j = 0; j < population.size(); j++)
	    		    {
	    		    	sortedInitialPopulation.add(new IndexedDoubleLongData(fitness.get(j), populationSeeds.get(j), j));
	    		    }
	    		    Collections.sort(sortedInitialPopulation, Collections.reverseOrder());  // Sort the indices of the list in descending order by OOB MCC.
	    		    newInitialPopulation = new ArrayList<Integer[]>();
	    		    newInitialFitness = new ArrayList<Double>();
	    		    newInitialSeeds = new ArrayList<Long>();
	    		    for (int j = 0; j < populationSize; j ++)
	    		    {
	    		    	// Add the first populationSize population members with the lowest error rates.
	    		    	int indexToAddFrom = sortedInitialPopulation.get(j).getIndex();
	    		    	newInitialPopulation.add(population.get(indexToAddFrom));
	    		    	newInitialFitness.add(fitness.get(indexToAddFrom));
	    		    	newInitialSeeds.add(populationSeeds.get(indexToAddFrom));
	    		    }
	    		    population = newInitialPopulation;
	    		    fitness = newInitialFitness;
	    		    populationSeeds = newInitialSeeds;
	    			threshold = numberGenes / 4;
	    		}
	    	}

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
	    		this.bestMembersFound = new ArrayList<Integer[]>();  // Clear out the list of the best individuals found as there is a new top fitness.
	    		this.bestForestSeeds = new ArrayList<Long>();  // Clear the list of the best individuals' seeds as there is a new top fitness.
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
	    			this.bestForestSeeds.add(populationSeeds.get(i));
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

	    // Write out the best member(s) of the population.
	    Set<Integer[]> recordedIndividuals = new HashSet<Integer[]>();
	    try
		{
	    	String bestIndivOutputLocation = outputLocation + "/BestIndividuals.txt";
			FileWriter bestIndivOutputFile = new FileWriter(bestIndivOutputLocation);
			BufferedWriter bestIndivOutputWriter = new BufferedWriter(bestIndivOutputFile);
			bestIndivOutputWriter.write("Fitness : ");
			bestIndivOutputWriter.write(Double.toString(this.currentBestFitness));
			bestIndivOutputWriter.newLine();
			for (int i = 0; i < this.bestMembersFound.size(); i++)
			{
				Integer[] currentMember = this.bestMembersFound.get(i);
				if (!recordedIndividuals.contains(currentMember))
				{
					recordedIndividuals.add(currentMember);
					bestIndivOutputWriter.write(Integer.toString(currentMember[0]));
					for (int j = 1; j < currentMember.length; j++)
					{
						bestIndivOutputWriter.write(",");
						bestIndivOutputWriter.write(Integer.toString(currentMember[j]));
					}
					bestIndivOutputWriter.write("\t" + Long.toString(this.bestForestSeeds.get(i)));
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

	List<Integer> hammingDistance(Integer[] parentOne, Integer[] parentTwo)
	{
		List<Integer> nonMatchingBits = new ArrayList<Integer>();
		for (int i = 0; i < parentOne.length; i++)
		{
			if (parentOne[i] != parentTwo[i])
			{
				nonMatchingBits.add(i);
			}
		}
		return nonMatchingBits;
	}

	boolean loopTermination(int currentGen, int maxGens, int currentEvals, int maxEvals, int generationsOfStagnation, Date startTime, long maxTimeAllowed)
	{
		boolean isGenNotStopping = false;
		boolean isEvalNotStopping = false;
		boolean isTimeNotStopping = false;
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

	    return isGenNotStopping && isEvalNotStopping && isTimeNotStopping;
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
			System.exit(0);
		}
	}

}
