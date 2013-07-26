package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import utilities.ImmutableFourValues;
import utilities.ImmutableTwoValues;

public class Tree
{
	
	private Node tree;
	
	public final void main(Map<String, double[]> dataset, Map<String, int[]> dataIndices, Map<String, double[]> classData,
			int[] inBagObservations, int mtry, Random treeRNG, int numberOfUniqueObservations)
	{
		// Determine the features in the dataset.
		this.tree = this.growTree(dataset, dataIndices, classData, inBagObservations, mtry, treeRNG, numberOfUniqueObservations);
	}

	private final Node growTree(Map<String, double[]> dataset, Map<String, int[]> dataIndices, Map<String, double[]> classData,
			int[] inBagObservations, int mtry, Random treeRNG, int numberOfUniqueObservations)
	{
		boolean isOnlyOneClassInNode = this.oneClassPresent(classData, inBagObservations);
		
		// Create a terminal node if there are only observations of one class remaining.
		if (isOnlyOneClassInNode)
		{
			return new NodeTerminal(classData, inBagObservations);
		}
		
		// Determine the best split that can be made.
		List<String> datasetFeatures = new ArrayList<String>(dataset.keySet());
		Collections.shuffle(datasetFeatures, treeRNG);
		int numVarsToSelect = Math.min(datasetFeatures.size(), mtry);
		List<String> featuresToSplitOn = datasetFeatures.subList(0, numVarsToSelect);
		ImmutableTwoValues<String, Double> bestSplit = FindBestSplit.main(dataset, dataIndices, classData, inBagObservations,
				featuresToSplitOn, numberOfUniqueObservations);
		String featureUsedForSplit = bestSplit.first;
		double splitValue = bestSplit.second;
		
		// If no split was found, then generate a terminal node.
		if (featureUsedForSplit == null)
		{
			return new NodeTerminal(classData, inBagObservations);
		}
		
		// Split the dataset into observations going to the left child and those going to the right one based on the feature to
		// split on and split value.
		ImmutableFourValues<int[], Integer, int[], Integer> splitObservations = this.splitDataset(dataset.get(featureUsedForSplit),
				dataIndices.get(featureUsedForSplit), inBagObservations, splitValue);
		int[] leftChildInBagObservations = splitObservations.first;
		int leftChildNumberOfUniqueObservations = splitObservations.second.intValue();
		int[] rightChildInBagObservations = splitObservations.third;
		int rightChildNumberOfUniqueObservations = splitObservations.fourth.intValue();
		
		// Generate the children of this node.
		Node leftChild = growTree(dataset, dataIndices, classData, leftChildInBagObservations, mtry, treeRNG,
				leftChildNumberOfUniqueObservations);
		Node rightChild = growTree(dataset, dataIndices, classData, rightChildInBagObservations, mtry, treeRNG,
				rightChildNumberOfUniqueObservations);
		return new NodeNonTerminal(featureUsedForSplit, splitValue, leftChild, rightChild);
		
	}
	
	
	private final boolean oneClassPresent(Map<String, double[]> classData, int[] inBagObservations)
	{
		Set<String> classesPresent = new HashSet<String>();
		Set<String> allClasses = new HashSet<String>(classData.keySet());
		
		// Add the classes that have non-zero weight (and are therefore present).
		int numberOfObservations = inBagObservations.length;
		for (String s : allClasses)
		{
			double[] classWeights = classData.get(s);
			double classWeightSum = 0.0;
			for (int i = 0; i < numberOfObservations; i++)
			{
				classWeightSum += (classWeights[i] * inBagObservations[i]);
			}
			if (classWeightSum != 0.0)
			{
				classesPresent.add(s);
			}
		}

		return classesPresent.size() < 2;
	}

	
	public final Map<Integer, Map<String, Double>> predict(Map<Integer, Map<String, Double>> datasetToPredict)
	{
		return this.tree.predict(datasetToPredict);
	}
	
	
	private final ImmutableFourValues<int[], Integer, int[], Integer> splitDataset(double[] splitFeatureData,
			int[] splitDataIndices, int[] inBagObservations, double splitValue)
	{
		int numberOfObservations = inBagObservations.length;
		int[] leftChildInBag = new int[numberOfObservations];
		int numberOfUniqueLeftObservations = 0;
		int[] rightChildInBag = new int[numberOfObservations];
		int numberOfUniqueRightObservations = 0;

		for (int i = 0; i < numberOfObservations; i++)
		{
			double dataValue = splitFeatureData[i];
			int originalObsIndex = splitDataIndices[i];
			int inBagCount = inBagObservations[originalObsIndex];
			int inBagUnique = (inBagCount == 0 ? 0 : 1);
			if (dataValue <= splitValue)
			{
				leftChildInBag[originalObsIndex] = inBagCount;
				numberOfUniqueLeftObservations += inBagUnique;
			}
			else
			{
				rightChildInBag[i] = inBagCount;
				numberOfUniqueRightObservations += inBagUnique;
			}
		}
		
		return new ImmutableFourValues<int[], Integer, int[], Integer>(leftChildInBag, numberOfUniqueLeftObservations,
				rightChildInBag, numberOfUniqueRightObservations);
	}
	
}
