package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import utilities.ArrayManipulation;
import utilities.ImmutableTwoValues;

public class Tree
{
	
	private Node tree;
	
	public final void main(Map<String, double[]> dataset, Map<String, int[]> dataIndices, Map<String, double[]> classData, int mtry,
			Random treeRNG)
	{
		// Determine the features in the dataset.
		List<String> features = new ArrayList<String>(dataset.keySet());
		List<String> classes = new ArrayList<String>(classData.keySet());
		this.tree = this.growTree(dataset, dataIndices, classData, mtry, treeRNG, features, classes);
	}

	private final Node growTree(Map<String, double[]> dataset, Map<String, int[]> dataIndices, Map<String, double[]> classData, int mtry,
			Random treeRNG, List<String> datasetFeatures, List<String> classes)
	{
		// Determine the number of classes that remain. The basic principle is to determine if all the weights for one class are the
		// same. For example, if class A only contains weights of 0.1 (or really any non-zero value) then there are not observations
		// of class B in the node. This can be seen as the only way for both class A and B to have observations in the node is for
		// there to be at least two observations i and j, where i has non-zero weight for class A (and zero weight for class B) and j
		// has non-zero weight for class B (and zero weight for class A).
		//
		// First select the weights for one class of only those observations that have reached this node, and then determine the number
		// of different weights that the class contains. 
		double[] classWeights = ArrayManipulation.selectSubset(classData.get(classes.get(0)), dataIndices.get(datasetFeatures.get(0)));
		Set<Double> uniqueClassWeights = new HashSet<Double>();
		for (double d : classWeights)
		{
			uniqueClassWeights.add(d);
		}
		boolean isOnlyOneClassInNode = uniqueClassWeights.size() == 1;
		
		// Create a terminal node if there are only observations of one class remaining.
		if (isOnlyOneClassInNode)
		{
			return new NodeTerminal();
		}
		
		// Determine the best split that can be made.
		Collections.shuffle(datasetFeatures, treeRNG);
		int numVarsToSelect = Math.min(datasetFeatures.size(), mtry);
		List<String> featuresToSplitOn = datasetFeatures.subList(0, numVarsToSelect);
		ImmutableTwoValues<String, Double> bestSplit = FindBestSplit.main(dataset, dataIndices, classData, featuresToSplitOn);
		String featureToSplitOn = bestSplit.first;
		double splitValue = bestSplit.second;
		
		//TODO Split the dataset based on the feature to split on, and the value chosen for the split.
		
		//TODO Generate the children of this node.
		
	}

	
	public final Map<Integer, Map<String, Double>> predict(Map<String, List<Double>> datasetToPredict)
	{
		//TODO generate the predictions.
		return null;
	}
	
}
