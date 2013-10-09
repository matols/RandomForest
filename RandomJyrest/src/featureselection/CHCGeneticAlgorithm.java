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

/**
 * Implements a CHC genetic algorithm.
 */
public class CHCGeneticAlgorithm
{

	/**
	 * Run the CHC genetic algorithm.
	 * 
	 * @param inputFile			The location of the dataset used to grow the forests.
	 * @param resultsDir		The location where the results of the feature selection will be written.
	 * @param populationSize	The size of the population to use for the GA.
	 * @param isVerboseOutput	Whether status updates should be displayed.
	 * @param mtry				The number of features to consider at each split in a tree.
	 * @param numberOfTrees		The number of trees to grow in each forest.
	 * @param numberOfThreads	The number of threads to use when growing a forest.
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
		// individuals that are not too similar to themselves.
		int threshold = featuresInDataset.size() / 4;
		
		// Generate the initial population.
		int generationsElapsed = 0;
		List<List<String>> population = null;
		List<Double> fitnessOfPopulation = null;
	    List<Long> seedsOfPopulation = null;
		if (isRunContinued)
		{
			// Get the state of the GA when it was stopped previously.
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
			// Generate an initial population and evaluate its fitness.
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
	    	// and decrease the threshold if no offspring that meets the criterion is generated.
	    	int numberOfAttemptsMadeToImprovePopulation = 0;
	    	boolean isPopulationChangedOrThresholdDropped = false;
	    	while (numberOfAttemptsMadeToImprovePopulation < generationsWithoutChange & !isPopulationChangedOrThresholdDropped)
	    	{
	    		// Generate offspring for potential inclusion in the next generation. This list may be empty if there were no
		    	// offspring created.
		    	List<List<String>> offspring = generateOffspring(population, threshold);
		    	
		    	if (!offspring.isEmpty())
		    	{
		    		// Some offspring were created.
		    		
			    	// Calculate the fitness of the offspring.
			    	ImmutableTwoValues<List<Double>, List<Long>> offspringFitness = calculateFitness(offspring, inputFile, numberOfTrees, mtry, numberOfThreads, weights, observationClasses);
			    	List<Double> fitnessOfOffspring = offspringFitness.first;
				    List<Long> seedsOfOffspring = offspringFitness.second;
			    	
			    	// Update the population. The population is updated by first pooling the parents and the offspring, and then selecting
				    // the populationSize most fit individuals from the pooled population. The population is only considered to have been
				    // changed if one of the offspring is fitter than a parent, and therefore displaces a parent in the set of
				    // populationSize most fit individuals.
				    population.addAll(offspring);
				    fitnessOfPopulation.addAll(fitnessOfOffspring);
				    seedsOfPopulation.addAll(seedsOfOffspring);
				    ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Boolean> updatedPopulation = updatePopulation(
				    		population, fitnessOfPopulation, seedsOfPopulation, populationSize);
				    population = updatedPopulation.first;
				    fitnessOfPopulation = updatedPopulation.second;
				    seedsOfPopulation = updatedPopulation.third;
				    isPopulationChangedOrThresholdDropped = updatedPopulation.fourth;  // True if an offspring is fitter than a parent.
		    	}
		    	else
		    	{
		    		// No offspring were created.
		    		isPopulationChangedOrThresholdDropped = true;  // he threshold is being dropped
		    		threshold -= 1;
		    		if (threshold == 0)
		    		{
		    			// Convergence occurs when the threshold reaches 0.
		    			isConvergenceReached = true;
		    		}
		    	}
		    	
	    		numberOfAttemptsMadeToImprovePopulation++;
	    	}
	    	
	    	// If the population did not change in the numberOfAttemptsToImprovePopulation attempts at generating offspring, and
	    	// offspring were generated in each of the generationsWithoutChange attempts, then decrease the threshold.
	    	if (!isPopulationChangedOrThresholdDropped)
	    	{
	    		threshold -= 1;
	    		if (threshold == 0)
	    		{
	    			// Convergence occurs when the threshold reaches 0.
	    			isConvergenceReached = true;
	    		}
	    	}
	    	
	    	// Update the number of generations elapsed.
	    	generationsElapsed += 1;
	    	
	    	// Write out the population.
	    	recordPopulation(resultsDir, population, fitnessOfPopulation, seedsOfPopulation, generationsElapsed, populationSize,
	    			threshold, numberOfAttemptsMadeToImprovePopulation);
	    }
	}
	
	
	/**
	 * Calculates the fitness for each member of a population.
	 * 
	 * To control the method of calculating the fitness alter the following line to calculate the measure you want:
	 * 		double individualFitness = PredictionAnalysis.calculate...
	 * In built measures include the G mean, MCC, F measure, accuracy and error.
	 * 
	 * The return values are ordered as the population is, so the ith individual in the population list will have their fitness
	 * and seed used be the ith values in the fitness and seed lists returned.
	 * 
	 * @param population			The population of individuals that will have their fitness evaluated.
	 * @param dataset				The dataset that each individual's fitness will be evaluated on.
	 * @param numberOfTrees			The number of trees to grow in each forest.
	 * @param mtry					The number of features to evaluate at each split in a tree.
	 * @param numberOfThreads		The number of threads to use when growing a forest.
	 * @param weights				The vector of observation weights.
	 * @param observationClasses	The classes of each observation.
	 * @return						The fitness of each individual and the seed used to grow each forest.
	 */
	private static final ImmutableTwoValues<List<Double>, List<Long>> calculateFitness(List<List<String>> population, String dataset, int numberOfTrees, int mtry,
			int numberOfThreads, double[] weights, List<String> observationClasses)
	{
		List<Double> fitness = new ArrayList<Double>();  // The fitnesses of the individuals.
		List<Long> seeds = new ArrayList<Long>();  // The seeds used to grow the forests evaluating each individual.
		
		// Grow a forest to determine the fitnes of each individual in the population.
	    for (List<String> p : population)
	    {
	    	// Grow the forest and generate the OB predictions.
	    	Forest forest = new Forest();
	    	Map<String, double[]> predictions = forest.main(dataset, numberOfTrees, mtry, p, weights, numberOfThreads, true);
	    	Map<String, Map<String, Double>> confusionMatrix = PredictionAnalysis.calculateConfusionMatrix(observationClasses, predictions);

	    	// Evaluate the fitness.
	    	double individualFitness = PredictionAnalysis.calculateGMean(confusionMatrix, observationClasses);
	    	
	    	fitness.add(individualFitness);
	    	seeds.add(forest.getSeed());
	    }
	    
	    return new ImmutableTwoValues<List<Double>, List<Long>>(fitness, seeds);
	}
	
	
	/**
	 * Generates offspring of a population.
	 * 
	 * If all pairs of parents chosen are too similar, then the list of offspring returned will be empty.
	 * 
	 * @param population	The population from which the parents should be taken.
	 * @param threshold		The dissimilarity (in Hamming distance) required between pairs of parents before they can produce offspring.
	 * @return				The offspring generated.
	 */
	private static final List<List<String>> generateOffspring(List<List<String>> population, int threshold)
	{
		List<List<String>> offspring = new ArrayList<List<String>>();  // The generated offspring.
		int populationSize = population.size();  // he number of indivudals in the parent population.
		int parentPairsToGenerate = populationSize / 2;  // The number of pairs of parents to select.  
		
		Random parentPicker = new Random();
    	for (int i = 0; i < parentPairsToGenerate; i++)
    	{
    		// Select the parents (no preference given to fitter parents).
    		int indexParentOne = parentPicker.nextInt(populationSize);
    		int indexParentTwo = parentPicker.nextInt(populationSize);
    		while (indexParentOne == indexParentTwo)
    		{
    			indexParentTwo = parentPicker.nextInt(populationSize);
    		}
    		List<String> parentOne = population.get(indexParentOne);
    		List<String> parentTwo = population.get(indexParentTwo);

    		// Determine the features unique to each parent, and the set of features unqie to one parent or the other (e.g. if the
    		// parents are A and B, determine A-B, B-A and ((A-B) union (B-A))).
    		ImmutableTwoValues<Set<String>, Set<String>> uniqueParentFeatures = uniqueFeatures(parentOne, parentTwo);
    		Set<String> uniqueToParentOne = uniqueParentFeatures.first;
    		Set<String> uniqueToParentTwo = uniqueParentFeatures.second;
    		List<String> uniqueFeatures = new ArrayList<String>(uniqueToParentOne);
    		uniqueFeatures.addAll(uniqueToParentTwo);
    		
    		// Determine whether the parents are dissimilar enough to undergo crossover.
    		int hammingDistanceBetweenParents = uniqueFeatures.size();
    		if (hammingDistanceBetweenParents > threshold)
    		{
    			// The parents are dissimilar enough to undergo crossover. Peform half uniform crossover (HUX). Select a random subset
    			// of half the features that are unique to one parent or the other, and cross them over. This may result in one parent
    			// having many features and the other only a few, and therefore can provide a fluctuation in the size of individuals.
    			Collections.shuffle(uniqueFeatures);
    			List<String> toCrossover = new ArrayList<String>(uniqueFeatures.subList(0, hammingDistanceBetweenParents / 2));
    			
    			// Initialise both children to be the same as their corresponding parent.
    			List<String> childOne = new ArrayList<String>(parentOne);
    			List<String> childTwo = new ArrayList<String>(parentTwo);
    			for (String s : toCrossover)
    			{
    				// For each feature that should be crossed over, determine which parent, Pp, it is in and which, Pq, is isn't.
    				// Set child Cp to not have the feature and child Cq to have it.
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
	 * Calculate the features unique to each of two individuals.
	 * 
	 * This method can be used to determine the Hamming distance between two individuals. Given two individuals A and B, this method
	 * determines Ua = A - B and Ub = B - A. The Hamming distance between the two is then Ua + Ub.
	 * 
	 * @param parentOne		One of the parents.
	 * @param parentTwo		The other parent.
	 * @return				Two sets of features. The first set being the features unique to parentOne, and the second unique to parentTwo.
	 */
	private static final ImmutableTwoValues<Set<String>, Set<String>> uniqueFeatures(List<String> parentOne, List<String> parentTwo)
	{
		// Determine the features unique to parentOne.
		Set<String> uniqueToParentOne = new HashSet<String>();
		for (String s : parentOne)
		{
			if (!parentTwo.contains(s))
			{
				// If parentTwo does not contain feature s, then add the feature to the set of features unique to parentOne.
				uniqueToParentOne.add(s);
			}
		}
		
		// Determine the features unique to parentTwo.
		Set<String> uniqueToParentTwo = new HashSet<String>();
		for (String s : parentTwo)
		{
			if (!parentOne.contains(s))
			{
				// If parentOne does not contain feature s, then add the feature to the set of features unique to parentTwo.
				uniqueToParentTwo.add(s);
			}
		}
		
		return new ImmutableTwoValues<Set<String>, Set<String>>(uniqueToParentOne, uniqueToParentTwo);
	}
	
	
	/**
	 * Write out the information about a generation.
	 * 
	 * @param resultsDir					The directory where the generational information will be written.
	 * @param population					The population that is being recorded.
	 * @param fitnesses						The fitness of the individuals in the population.
	 * @param seeds							The seeds used to evaluate the individuals in the population.
	 * @param generationsElapsed			The number of this generation.
	 * @param populationSize				The size of the population.
	 * @param threshold						The dissimilarity threshold that must be met before crossover can be performed.
	 * @param attemptsAtImprovementMade		The number of attempts at crossover made before an offspring was fitter than a parent, or you gave up.
	 */
	private static final void recordPopulation(String resultsDir, List<List<String>> population, List<Double> fitnesses,
			List<Long> seeds, int generationsElapsed, int populationSize, int threshold, int attemptsAtImprovementMade)
	{
		String resultsLocation = resultsDir + "/" + String.format("%09d", generationsElapsed);  // The file where the population data will be recorded.
		
		// Write out the individuals in the population, along with their fitness and the seed used to evaluate their fitness.
		// Also record the threshold and number of attempts made at generating an offspring that is more fit than a parent.
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
	 * @param features			The list of all the features in the dataset that can be used for growing the forest.
	 * @param removedFeatures	The list of features that should never be considered for growing the forest.
	 * @param populationSize	The number of individuals in the population
	 * @return					A list of the chosen feature sets (the individuals in the initial population).
	 */
	private static final List<List<String>> initialisePopulation(List<String> features, List<String> removedFeatures, int populationSize)
	{
		List<List<String>> population = new ArrayList<List<String>>(populationSize);  // The initial population
		List<String> featuresAvailableForSelection = new ArrayList<String>(features);  // All features are initially available for selection.
		int numberOfFeatures = features.size();  // The total number of features available for selection.
		
		// Initialise the object that will be used to randomly select from among the features.
		Random featureSelector = new Random();
		
		// For each individual that should be created, generate a random set of features.
		for (int i = 0; i < populationSize; i++)
		{
			List<String> newPopMember = new ArrayList<String>();  // The individual.
			
			// Generate an individual consisting of half the total number of features.
			while (newPopMember.size() < (numberOfFeatures / 2.0))
			{
				// Select a random feature from the available features.
				String chosenFeature = featuresAvailableForSelection.get(featureSelector.nextInt(featuresAvailableForSelection.size()));
				while (newPopMember.contains(chosenFeature))
				{
					// If the chosen feature is already in the individual, then select another. This can only happened if the list of
					// available features had to be refilled while an individual was being generated.
					chosenFeature = featuresAvailableForSelection.get(featureSelector.nextInt(featuresAvailableForSelection.size()));
				}
				
				// Add the feature to the individual, and remove the feature from the set of available features.
				newPopMember.add(chosenFeature);
				featuresAvailableForSelection.remove(chosenFeature);
				
				// If there are no available features left, then refill the list of available features with all potential features. 
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
	 * The fitness and seed for the ith individual in the population can be found at fitness.get(i) and seeds.get(i) respectively.
	 * The returned lists are also ordered in this manner.
	 * 
	 * The population, fitness and seeds lists are all ordered so that the parents values have indices less than the indices of the
	 * offspring, and therefore the parents values are the first populationSize values and the offspring's values occur with
	 * and index >= populationSize.
	 * 
	 * @param population		The pooled list of parents and offspring.
	 * @param fitness			The pooled list of fitnesses of the parents and offspring.
	 * @param seeds				The pooled list of seeds used for evaluating the fitnesses of the parents and offspring.
	 * @param populationSize	The number of individuals that should be selected from the pooled set of parents and offspring.
	 * @return					The populationSize fittest individuals, along with their fitnesses and seeds.
	 */
	private static final ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Boolean> updatePopulation(
			List<List<String>> population, List<Double> fitness, List<Long> seeds, int populationSize)
	{
		// Sorted the fitnesses in such a way that the original index of the individual can be retrieved along with its rank according
		// to its fitness.
		List<IndexedDoubleData> sortedByFitness = new ArrayList<IndexedDoubleData>();
	    for (int j = 0; j < population.size(); j++)
	    {
	    	sortedByFitness.add(new IndexedDoubleData(fitness.get(j), j));
	    }
	    Collections.sort(sortedByFitness, Collections.reverseOrder());  // Sort in descending order by fitness.
	    
	    // Determine the populationSize most fit individuals, and record their fitnesses and seeds along with them.
	    // Add the first populationSize individuals in the sortedByFitness list, as they are the fittest.
	    List<List<String>> fittestIndividuals = new ArrayList<List<String>>();
	    List<Double> fittestIndividualsFitness = new ArrayList<Double>();
	    List<Long> fittestIndividualsSeeds = new ArrayList<Long>();
	    boolean isPopulationUpdated = false;
	    for (int j = 0; j < populationSize; j ++)
	    {
	    	int indexToAddFrom = sortedByFitness.get(j).getIndex();  // Determine the original index of the jth most fit individual.
	    	fittestIndividuals.add(population.get(indexToAddFrom));  // Get the jth most fit individual.
	    	fittestIndividualsFitness.add(fitness.get(indexToAddFrom));  // Get the fitness of the jth most fit individual.
	    	fittestIndividualsSeeds.add(seeds.get(indexToAddFrom));  // Get the seed of the jth most fit indivudal.
	    	
	    	// If the original index of the jth most fit individual is >= populationSize, then the indivual being added is one of the
	    	// offspring (as the offspring are at the end of te population list after the parents, and therefore have original indices
	    	// >= populationSize.
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
	 * Used to retrieve the initial population for a run continuation.
	 * 
	 * The threshold that had been reached before the run was stopped previously is not extracted. This does not make much difference
	 * as the homogeneity of the population ensures that the threshold from the stopped run is reached quickly (in only a few generations).
	 * 
	 * @param populationDir		The directory where the population is saved.
	 * @return					The population to use as a starting point for the run continuation (along with the fitnesses and seeds
	 * 							of the population and the generation in the run where the run was stopped previously).
	 */
	private static final ImmutableFourValues<List<List<String>>, List<Double>, List<Long>, Integer> retrieveInitialPopulation(
			String populationDir)
	{
		List<List<String>> population = new ArrayList<List<String>>();  // The population to use as the starting point for the continuation.
		List<Double> fitnesses = new ArrayList<Double>();  // The fitness of each starting population individual.
		List<Long> seeds =new ArrayList<Long>();  // The seed of each starting population individual.
		int lastGeneration = 0;  // The number of the generation that will be used as the starting generation for the continuation.
		
		// Get the files recording the population at all the generations performed so far.
		File populationDirectory = new File(populationDir);
		File[] generationRecords = populationDirectory.listFiles();

		// Determine the final generation from the previous run (this will be the starting generation for the current run).
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
		
		// Read in the population from the record of the last generation.
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
