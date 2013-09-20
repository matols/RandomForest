package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import randomjyrest.Forest;
import randomjyrest.PredictionAnalysis;
import utilities.DetermineDatasetProperties;
import utilities.ImmutableFourValues;
import utilities.ImmutableTwoValues;
import utilities.IndexedDoubleData;

public class CHCGeneticAlgorithm
{

	/**
	 * Run the CHC genetic algorithm.
	 * 
	 * @param inputFile			The location of the dataset used to grow the forests.
	 * @param resultsDir		The location where the results of the optimisation will be written.
	 * @param populationSize	The size of the population to use for the GA.
	 * @param isVerboseOutput	Whether status updates should be displayed.
	 * @param mtry				The number of features to consider at each split in a tree.
	 * @param numberOfTrees		The number of trees to grow in each forest.
	 * @param numberOfThreads	The number of threads to use when growing the trees.
	 * @param weights			The weights of the individual observations.
	 * @param featuresToRemove	The features in the dataset that should be removed (not used in growing the forest).
	 */
	public static final void main(String inputFile, String resultsDir, int populationSize, boolean isVerboseOutput,
			int mtry, int numberOfTrees, int numberOfThreads, double[] weights, List<String> featuresToRemove,
			int generationsWithoutChange)
	{
		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
		boolean isRunContinued = false;
		if (!resultsDirectory.exists())
		{
			// The results directory does not exist.
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The results directory does not exist, but could not be created.");
				System.exit(0);
			}
		}
		else
		{
			// The results directory already exists.
			isRunContinued = true;
		}
		
		if (populationSize < 2)
		{
			// There are not enough individuals in the population.
			System.out.println("You must specify a population of at least two individuals.");
			System.exit(0);
		}
		
		// Determine the class of each observation.
		List<String> observationClasses = DetermineDatasetProperties.determineObservationClasses(inputFile);
		
		// Determine the features that are to be used in the growing of the forest.
		List<String> featuresInDataset = DetermineDatasetProperties.determineDatasetFeatures(inputFile, featuresToRemove);
		
		
		// Determine the threshold Hamming distance between two individual that must be met before the individuals can undergo
		// crossover. This is a form of incest prevention, and ensures that the individuals only undergo crossover with other
		// individuals that are at least a certain amount different than themselves.
		int threshold = featuresInDataset.size() / 4;
		
		// Generate the initial population.
		int generationsElapsed = 0;
		List<List<String>> population = null;
		List<Double> fitnessOfPopulation = null;
	    List<Long> seedsOfPopulation = null;
		if (isRunContinued)
		{
			if (isVerboseOutput)
		    {
		    	System.out.println("Now retrieving the population from the last generation of the previous run");
		    }
			ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Integer> lastGeneration = retrieveInitialPopulation(resultsDir);
			population = lastGeneration.first;
			fitnessOfPopulation = lastGeneration.second;
			seedsOfPopulation = lastGeneration.third;
			generationsElapsed = lastGeneration.fourth;
		}
		else
		{
		    if (isVerboseOutput)
		    {
		    	System.out.println("Now generating the initial population");
		    }
		    population = initialisePopulation(featuresInDataset, featuresToRemove, populationSize);
	    
		    // Calculate the fitness of the initial population.
		    ImmutableTwoValues<List<Double>, List<Long>> populationFitness = calculateFitness(population, inputFile, numberOfTrees, mtry, numberOfThreads, weights, observationClasses);
		    fitnessOfPopulation = populationFitness.first;
		    seedsOfPopulation = populationFitness.second;
		}
	    
	    // Generate generations until convergence is reached.
	    boolean isConvergenceReached = false;
	    while(!isConvergenceReached)
	    {
	    	if (isVerboseOutput)
	    	{
	    		DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    Date now = new Date();
			    String strDate = sdfDate.format(now);
	    		System.out.format("\tNow starting generation number : %d at %s.\n", generationsElapsed + 1, strDate);
	    	}
	    	
	    	// Attempt to generate an offspring that is better than at least one of the parents. Make generationsWithoutChange attempts,
	    	// and if no offspring that meets the criterion is generated, then decrease the threshold.
	    	int numberOfAttemptsToImprovePopulation = 0;
	    	boolean isPopulationUpdated = false;
	    	while (numberOfAttemptsToImprovePopulation < generationsWithoutChange & !isPopulationUpdated)
	    	{
	    		// Generate offspring for potential inclusion in the next generation. This list may be empty if there were no
		    	// offspring created.
		    	List<List<String>> offspring = generateOffspring(population, populationSize, threshold);
		    	
		    	if (!offspring.isEmpty())
		    	{
		    		// Some offspring were created.
		    		
			    	// Calculate the fitness of the offspring.
			    	ImmutableTwoValues<List<Double>, List<Long>> offspringFitness = calculateFitness(offspring, inputFile, numberOfTrees, mtry, numberOfThreads, weights, observationClasses);
			    	List<Double> fitnessOfOffspring = offspringFitness.first;
				    List<Long> seedsOfOffspring = offspringFitness.second;
			    	
			    	// Update the population.
				    population.addAll(offspring);
				    fitnessOfPopulation.addAll(fitnessOfOffspring);
				    seedsOfPopulation.addAll(seedsOfOffspring);
				    ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Boolean> updatedPopulation = updatePopulation(
				    		population, fitnessOfPopulation, seedsOfPopulation, populationSize);
				    population = updatedPopulation.first;
				    fitnessOfPopulation = updatedPopulation.second;
				    seedsOfPopulation = updatedPopulation.third;
				    isPopulationUpdated = updatedPopulation.fourth;
		    	}
		    	else
		    	{
		    		// No offspring were created.
		    		isPopulationUpdated = true; 
		    		threshold -= 1;
		    		if (threshold == 0)
		    		{
		    			isConvergenceReached = true;
		    		}
		    	}
		    	
	    		numberOfAttemptsToImprovePopulation++;
	    	}
	    	
	    	// If the population was not updated in the numberOfAttemptsToImprovePopulation attempts at generating offspring, then
	    	// decrease he threshold.
	    	if (!isPopulationUpdated)
	    	{
	    		threshold -= 1;
	    		if (threshold == 0)
	    		{
	    			isConvergenceReached = true;
	    		}
	    	}
	    	
	    	// Update the number of generations elapsed.
	    	generationsElapsed += 1;
	    	
	    	// Write out the population.
	    	recordPopulation(resultsDir, population, fitnessOfPopulation, seedsOfPopulation, generationsElapsed, populationSize,
	    			threshold, numberOfAttemptsToImprovePopulation);
	    }
	}
	
	
	/**
	 * Calculates the fitness for each member of a population.
	 * 
	 * To control the method of calculating the fitness alter the following line to calculate the measure you want:
	 * 		double individualFitness = PredictionAnalysis.calculate...
	 * In built measures include the G mean, MCC, F measure, accuracy and error.
	 * 
	 * @param population
	 * @param dataset
	 * @param numberOfTrees
	 * @param mtry
	 * @param numberOfThreads
	 * @param weights
	 * @param observationClasses
	 * @return
	 */
	private static final ImmutableTwoValues<List<Double>, List<Long>> calculateFitness(List<List<String>> population, String dataset, int numberOfTrees, int mtry,
			int numberOfThreads, double[] weights, List<String> observationClasses)
	{
		List<Double> fitness = new ArrayList<Double>();
		List<Long> seeds = new ArrayList<Long>();
	    for (List<String> p : population)
	    {
	    	Forest forest = new Forest();
	    	Map<String, double[]> predictions = forest.main(dataset, numberOfTrees, mtry, p, weights, numberOfThreads, true);
	    	Map<String, Map<String, Double>> confusionMatrix = PredictionAnalysis.calculateConfusionMatrix(observationClasses, predictions);

	    	double individualFitness = PredictionAnalysis.calculateGMean(confusionMatrix, observationClasses);
	    	
	    	fitness.add(individualFitness);
	    	seeds.add(forest.getSeed());
	    }
	    return new ImmutableTwoValues<List<Double>, List<Long>>(fitness, seeds);
	}
	
	
	/**
	 * @param population
	 * @param populationSize
	 * @param threshold
	 * @return
	 */
	private static final List<List<String>> generateOffspring(List<List<String>> population, int populationSize, int threshold)
	{
		List<List<String>> offspring = new ArrayList<List<String>>();
		
		List<List<String>> shuffleablePopulation = new ArrayList<List<String>>(population);
    	for (int i = 0; i < populationSize / 2; i++)
    	{
    		// Select the parents (no preference given to fitter parents).
    		Collections.shuffle(shuffleablePopulation);
    		List<String> parentOne = shuffleablePopulation.get(0);
    		List<String> parentTwo = shuffleablePopulation.get(1);

    		// Determine if the selected parents can undergo combination.
    		ImmutableTwoValues<Set<String>, Set<String>> uniqueParentFeatures = hammingDistance(parentOne, parentTwo);
    		Set<String> uniqueToParentOne = uniqueParentFeatures.first;
    		Set<String> uniqueToParentTwo = uniqueParentFeatures.second;
    		List<String> uniqueFeatures = new ArrayList<String>(uniqueToParentOne);
    		uniqueFeatures.addAll(uniqueToParentTwo);
    		int distanceBetweenParents = uniqueFeatures.size();
    		if (distanceBetweenParents > threshold)
    		{
    			Collections.shuffle(uniqueFeatures);
    			List<String> toCrossover = new ArrayList<String>(uniqueFeatures.subList(0, distanceBetweenParents / 2));
    			List<String> childOne = new ArrayList<String>(parentOne);
    			List<String> childTwo = new ArrayList<String>(parentTwo);
    			for (String s : toCrossover)
    			{
    				if (uniqueToParentOne.contains(s))
    				{
    					childOne.remove(s);
    					childTwo.add(s);
    				}
    				else
    				{
    					childOne.add(s);
    					childTwo.remove(s);
    				}
    			}
    			offspring.add(childOne);
    			offspring.add(childTwo);
    		}
    	}
    	
    	return offspring;
	}
	
	
	/**
	 * Calculate the Hamming distance between two individuals.
	 * 
	 * @param parentOne
	 * @param parentTwo
	 * @return
	 */
	private static final ImmutableTwoValues<Set<String>, Set<String>> hammingDistance(List<String> parentOne, List<String> parentTwo)
	{
		Set<String> uniqueToParentOne = new HashSet<String>();
		Set<String> uniqueToParentTwo = new HashSet<String>();
		for (String s : parentOne)
		{
			if (!parentTwo.contains(s))
			{
				uniqueToParentOne.add(s);
			}
		}
		for (String s : parentTwo)
		{
			if (!parentOne.contains(s))
			{
				uniqueToParentTwo.add(s);
			}
		}
		return new ImmutableTwoValues<Set<String>, Set<String>>(uniqueToParentOne, uniqueToParentTwo);
	}
	
	
	/**
	 * @param resultsDir
	 * @param population
	 * @param fitnesses
	 * @param seeds
	 * @param generationsElapsed
	 * @param populationSize
	 */
	private static final void recordPopulation(String resultsDir, List<List<String>> population, List<Double> fitnesses,
			List<Long> seeds, int generationsElapsed, int populationSize, int threshold, int attemptsAtImprovementMade)
	{
		String resultsLocation = resultsDir + "/" + String.format("%09d", generationsElapsed);
		try
		{
			FileWriter resultsOutputFile = new FileWriter(resultsLocation);
			BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
			resultsOutputWriter.write("Fitness\tSeedUsed\tIndividual\t");
			resultsOutputWriter.write("(Attempts=" + Integer.toString(attemptsAtImprovementMade) + ", Threshold=" + Integer.toString(threshold) + ")");
			resultsOutputWriter.newLine();
			for (int i = 0; i < populationSize; i++)
			{
				resultsOutputWriter.write(String.format("%.5f", fitnesses.get(i)));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(Long.toString(seeds.get(i)));
				resultsOutputWriter.write("\t");
				resultsOutputWriter.write(population.get(i).toString());
				resultsOutputWriter.newLine();
			}
			resultsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	
	/**
	 * Initialise the population.
	 * 
	 * Each population member will consist of a set of features. These will be the features that are not used in
	 * growing the particular forest.
	 * Example:
	 * 		The set of all features				[A, B, C, D, E, F, G, H]
	 * 		Individual i in the population		[A, D, F, G]
	 * 		The forest grown from individual i will not be grown using features A, D, F and G.
	 * 
	 * The population is initialised so that each individual has the same number of features (half the total number).
	 * Additionally, if the feature selected most often is selected n times, then the feature selected the fewest number
	 * of times will be selected no less than n - 1 times.
	 * 
	 * @param features			The list of the features in the dataset that are to be used for growing the forest.
	 * @param removedFeatures	The list of features that should never be considered for growing the forest.
	 * @param populationSize	The number of individuals in the population
	 * @return					A list of the chosen feature sets.
	 */
	private static final List<List<String>> initialisePopulation(List<String> features, List<String> removedFeatures, int populationSize)
	{
		List<List<String>> population = new ArrayList<List<String>>(populationSize);
		
		List<String> featuresAvailableForSelection = new ArrayList<String>(features);
		int numberOfFeatures = features.size();
		Random featureSelector = new Random();
		for (int i = 0; i < populationSize; i++)
		{
			List<String> newPopMember = new ArrayList<String>();
			for (int j = 0; j < (numberOfFeatures / 2.0); j++)
			{
				// Select a random available observation from class s.
				String chosenFeature = featuresAvailableForSelection.get(featureSelector.nextInt(featuresAvailableForSelection.size()));
				newPopMember.add(chosenFeature);
				featuresAvailableForSelection.remove(chosenFeature);
				if (featuresAvailableForSelection.isEmpty())
				{
					featuresAvailableForSelection = new ArrayList<String>(features);
				}
			}
			
			// Add the features that should never be used to the new population member.
			for (String s : removedFeatures)
			{
				newPopMember.add(s);
			}

			// Add the new population member to the population being generated.
			population.add(newPopMember);
		}
		
		return population;
	}
	
	
	/**
	 * Generate the new population from the parent population and the offspring.
	 * 
	 * If a fitness measure is used that determines the best fitness to be the smallest fitness (e.g. error rates), then the
	 * line
	 * 		Collections.sort(sortedByFitness, Collections.reverseOrder());
	 * must be changed to
	 * 		Collections.sort(sortedByFitness);
	 * 
	 * @param population
	 * @param fitness
	 * @param seeds
	 * @param populationSize
	 * @return
	 */
	private static final ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Boolean> updatePopulation(
			List<List<String>> population, List<Double> fitness, List<Long> seeds, int populationSize)
	{
		List<IndexedDoubleData> sortedByFitness = new ArrayList<IndexedDoubleData>();
	    for (int j = 0; j < population.size(); j++)
	    {
	    	sortedByFitness.add(new IndexedDoubleData(fitness.get(j), j));
	    }
	    Collections.sort(sortedByFitness, Collections.reverseOrder());  // Sort in descending order by fitness.
	    
	    List<List<String>> fittestIndividuals = new ArrayList<List<String>>();
	    List<Double> fittestIndividualsFitness = new ArrayList<Double>();
	    List<Long> fittestIndividualsSeeds = new ArrayList<Long>();
	    boolean isPopulationUpdated = false;
	    for (int j = 0; j < populationSize; j ++)
	    {
	    	// Add the first populationSize individuals as the are the fittest.
	    	int indexToAddFrom = sortedByFitness.get(j).getIndex();
	    	fittestIndividuals.add(population.get(indexToAddFrom));
	    	fittestIndividualsFitness.add(fitness.get(indexToAddFrom));
	    	fittestIndividualsSeeds.add(seeds.get(indexToAddFrom));
	    	
	    	if (indexToAddFrom >= populationSize)
	    	{
	    		// One of the offspring has been added to the population.
	    		isPopulationUpdated = true;
	    	}
	    }
	    
	    return new ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Boolean>(fittestIndividuals,
	    		fittestIndividualsFitness, fittestIndividualsSeeds, isPopulationUpdated);
	}
	
	/**
	 * @param populationDir
	 * @return
	 */
	private static final ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Integer> retrieveInitialPopulation(String populationDir)
	{
		List<List<String>> population = new ArrayList<List<String>>();
		List<Double> fitnesses = new ArrayList<Double>();
		List<Long> seeds =new ArrayList<Long>();
		int lastGeneration = 0;
		
		File populationDirectory = new File(populationDir);
		File[] generationRecords = populationDirectory.listFiles();

		// Determine the final generation from the previous run.
		String finalGenerationLocation = "";
		for (File f : generationRecords)
		{
			int currentGeneration = Integer.parseInt(f.getName());
			if (currentGeneration > lastGeneration)
			{
				lastGeneration = currentGeneration;
				finalGenerationLocation = f.getAbsolutePath();
			}
		}
		
		// Read in the population from the last generation record.
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(finalGenerationLocation));
			reader.readLine();  // Strip the header line.
			
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				String[] individualData = line.trim().split("\t");
				fitnesses.add(Double.parseDouble(individualData[0]));
				seeds.add(Long.parseLong(individualData[1]));
				String[] individualFeatureSet = individualData[2].substring(1, individualData[2].length() - 1).split(", ");
				population.add(Arrays.asList(individualFeatureSet));
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while extracting the population informaion located at: " + finalGenerationLocation);
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			try
			{
				if (reader != null)
				{
					reader.close();
				}
			}
			catch (IOException e)
			{
				// Caught an error while closing the file. Indicate this and exit.
				System.out.println("An error occurred while closing the file located at: " + finalGenerationLocation);
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		return new ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Integer>(population, fitnesses, seeds, lastGeneration);
	}
	
}
