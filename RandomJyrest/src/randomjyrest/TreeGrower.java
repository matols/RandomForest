package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import utilities.ImmutableTwoValues;
import utilities.IndexedDoubleData;

public class TreeGrower implements Callable<ImmutableTwoValues<Set<Integer>, Tree>>
{
	
	private Map<String, double[]> dataset = new HashMap<String, double[]>();
	
	private Map<String, int[]> dataIndices = new HashMap<String, int[]>();
	
	private Random treeRNG;
	
	private Map<String, double[]> classData;
	
	private Set<Integer> oobOnThisTree = null;
	
	private int mtry;

	
	public TreeGrower(Map<String, List<Double>> featureData, Map<String, double[]> classData, List<Integer> inBagObservations, int mtry,
			long seed)
	{
		this.mtry = mtry;
		this.treeRNG = new Random(seed);
		this.classData = classData;
		
		int numberOfObservations = inBagObservations.size();  // Determine the number of observations being used to grow the tree.
		Set<String> featuresUsed = featureData.keySet();
		Map<String, List<Double>> trainingDatasetForTree = new HashMap<String, List<Double>>();  // The unsorted data for training the tree.
		for (String f : featuresUsed)
		{
			trainingDatasetForTree.put(f, new ArrayList<Double>(numberOfObservations));
		}
		
		// Determine the OOB observations.
		this.oobOnThisTree = new HashSet<Integer>();
		for (int i = 0; i < numberOfObservations; i++)
		{
			this.oobOnThisTree.add(i);
		}
		this.oobOnThisTree.removeAll(inBagObservations);
		
		// Determine the final dataset to use for trraining.
		for (String f : featuresUsed)
		{
			List<Double> dataForFeatureF = featureData.get(f);
			List<IndexedDoubleData> sortedData = new ArrayList<IndexedDoubleData>();
			for (Integer i : inBagObservations)
			{
				sortedData.add(new IndexedDoubleData(dataForFeatureF.get(i).doubleValue(), i));
			}
			Collections.sort(sortedData);
			
			if (sortedData.get(0).getData() == sortedData.get(numberOfObservations - 1).getData())
			{
				// If the first and last data value are equal, then the feature contains only one value and is useless.
				// Therefore, remove features where the first and last value are equal.
				continue;
			}
			
			double[] sortedFeatureData = new double[numberOfObservations];
			int[] sortedFeatureIndices = new int[numberOfObservations];
			for (int i = 0; i < numberOfObservations; i++)
			{
				sortedFeatureData[i] = sortedData.get(i).getData();
				sortedFeatureIndices[i] = sortedData.get(i).getIndex();
			}
			this.dataset.put(f, sortedFeatureData);
			this.dataIndices.put(f, sortedFeatureIndices);
		}
	}

	public ImmutableTwoValues<Set<Integer>, Tree> call()
	{
		Tree tree = new Tree();
		tree.main(this.dataset, this.dataIndices, this.classData, this.mtry, this.treeRNG);
		return new ImmutableTwoValues<Set<Integer>, Tree>(this.oobOnThisTree, tree);
	}
	
}
