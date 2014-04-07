package randomjyrest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import utilities.ImmutableTwoValues;

/**
 * Implements a class to enable parallel growth of the trees in the forest.
 */
public class TreeGrower implements Callable<ImmutableTwoValues<Set<Integer>, Tree>>
{
	
	/**
	 * A mapping from the feature names to the data values sorted in ascending order.
	 */
	private Map<String, double[]> dataset;
	
	/**
	 * A mapping from the feature names to the original indices of the data values. For example, if the smallest observation value for
	 * feature F comes from the 3rd observation in the input file, then the 0th entry in the array mapped to by F will be 2.
	 */
	private Map<String, int[]> dataIndices;
	
	/**
	 * A mapping from each class to an array containing the weight of each observation for the class.
	 * The observations are ordered by their original indices (dataIndices ordering).
	 */
	private Map<String, double[]> classData;
	
	/**
	 * The number of features to test for each split.
	 */
	private int mtry;
	
	/**
	 * The random number generator for the tree. Enables reproducable of results.
	 */
	private Random treeRNG;
	
	/**
	 * The indices of the observations that are OOB for this tree.
	 */
	private Set<Integer> oobOnThisTree;
	
	/**
	 * Records which observations in the dataset are in bag for this tree. Observations that are not in bag are given a value of 0.
	 */
	private int[] inBagObservations;
	
	/**
	 * The number of observations that are in bag and unique. Equal to the number of observations in the dataset minus the number that
	 * are OOB for this tree. Facilitates speed up for the split determination by reducing the number of unnecessary evaluations.
	 */
	private int numberOfUniqueObservations = 0;

	
	/**
	 * Set up the information needed to grow a tree.
	 * 
	 * @param featureData					The same as this.dataset.
	 * @param indexData						The same as this.dataIndices.
	 * @param classData						The same as this.classData.
	 * @param mtry							The same as this.mtry.
	 * @param seed							The seed for this tree's random number generator.
	 * @param observationsFromEachClass		A mapping from class names to the indices of the observations that are members of the class.
	 * @param numberOfObservations			The total number of observations in the dataset.
	 */
	public TreeGrower(Map<String, double[]> featureData, Map<String, int[]> indexData, Map<String, double[]> classData,
			int mtry, long seed, Map<String, List<Integer>> observationsFromEachClass, int numberOfObservations)
	{
		this.dataset = featureData;
		this.dataIndices = indexData;
		this.classData = classData;
		this.mtry = mtry;
		this.treeRNG = new Random(seed);
		
		// Determine observations to use. Perform a stratified bootstrap sampling to get the in bag observations.
		this.inBagObservations = new int[numberOfObservations];
		Set<String> classes = observationsFromEachClass.keySet();
		for (String s : classes)
		{
			List<Integer> indicesOfObservationsInClass = observationsFromEachClass.get(s);
			int numberOfObservationsInClass = indicesOfObservationsInClass.size();
			for (int j = 0; j < numberOfObservationsInClass; j++)
			{
				int observationToSelect = treeRNG.nextInt(numberOfObservationsInClass);
				int observationIndex = indicesOfObservationsInClass.get(observationToSelect).intValue();
				int oldCount = this.inBagObservations[observationIndex];
				this.inBagObservations[observationIndex] = oldCount + 1;
			}
		}
		
		// Determine the number of unique observations and the OOB observations.
		this.oobOnThisTree = new HashSet<Integer>();
		for (int i = 0; i < numberOfObservations; i++)
		{
			if (this.inBagObservations[i] != 0)
			{
				this.numberOfUniqueObservations++;
			}
			else
			{
				this.oobOnThisTree.add(i);
			}
		}
	}

	public ImmutableTwoValues<Set<Integer>, Tree> call()
	{
		// Initialise, grow and return the tree.
		Tree tree = new Tree();
		tree.main(this.dataset, this.dataIndices, this.classData, this.inBagObservations, this.mtry, this.treeRNG,
				this.numberOfUniqueObservations);
		return new ImmutableTwoValues<Set<Integer>, Tree>(this.oobOnThisTree, tree);
	}
	
}
