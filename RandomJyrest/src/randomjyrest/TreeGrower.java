package randomjyrest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import utilities.ImmutableTwoValues;

public class TreeGrower implements Callable<ImmutableTwoValues<Set<Integer>, Tree>>
{
	
	private Map<String, double[]> dataset;
	
	private Map<String, int[]> dataIndices;
	
	private Map<String, double[]> classData;
	
	private int mtry;
	
	private Random treeRNG;
	
	private Set<Integer> oobOnThisTree;
	
	private int[] inBagObservations;
	
	private int numberOfUniqueObservations = 0;

	
	public TreeGrower(Map<String, double[]> featureData, Map<String, int[]> indexData, Map<String, double[]> classData,
			int mtry, long seed, Map<String, List<Integer>> observationsFromEachClass, int observationsToSelect)
	{
		this.dataset = featureData;
		this.dataIndices = indexData;
		this.classData = classData;
		this.mtry = mtry;
		this.treeRNG = new Random(seed);
		
		// Determine observations to use. Perform a stratified bootstrap sampling to get the in bag observations.
		this.inBagObservations = new int[observationsToSelect];
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
		for (int i = 0; i < observationsToSelect; i++)
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
		Tree tree = new Tree();
		tree.main(this.dataset, this.dataIndices, this.classData, this.inBagObservations, this.mtry, this.treeRNG,
				this.numberOfUniqueObservations);
		return new ImmutableTwoValues<Set<Integer>, Tree>(this.oobOnThisTree, tree);
	}
	
}
