package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import utilities.ImmutableFourValues;
import utilities.ImmutableTwoValues;

/**
 * Implements a CART decision tree.
 */
public class Tree
{
	
	private Node tree;  // The tree is represented as a linked list of nodes with pointers between them.
	
	/**
	 * Create the tree starting from the root node.
	 * 
	 * @param dataset						A mapping from the feature names to the data values sorted in ascending order.
	 * @param dataIndices					A mapping from the feature names to the original indices of the data values. For example, if
	 * 										the smallest observation value for feature F comes from the 3rd observation in the input file,
	 * 										then the 0th entry in the array mapped to by F will be 2.
	 * @param classData						A mapping from each class to an array containing the weight of each observation for the class.
	 * 										The observations are ordered by their original indices (dataIndices ordering).
	 * @param inBagObservations				An array recording which observations in the dataset have reached the node for which the split
	 * 										is being determined (in bag observations). Observations that are not in bag are given a value
	 * 										of 0.
	 * @param mtry							The number of features to evaluate for the cutpoint in each nonterminal node.
	 * @param treeRNG						The random number generator for the tree.
	 * @param numberOfUniqueObservations	The number of unique observations that have reached the node. Observations can be duplicates
	 * 										as sampling is performed with replacement.
	 */
	public final void main(Map<String, double[]> dataset, Map<String, int[]> dataIndices, Map<String, double[]> classData,
			int[] inBagObservations, int mtry, Random treeRNG, int numberOfUniqueObservations)
	{
		this.tree = this.growTree(dataset, dataIndices, classData, inBagObservations, mtry, treeRNG, numberOfUniqueObservations);
	}

	/**
	 * @param dataset						A mapping from the feature names to the data values sorted in ascending order.
	 * @param dataIndices					A mapping from the feature names to the original indices of the data values. For example, if
	 * 										the smallest observation value for feature F comes from the 3rd observation in the input file,
	 * 										then the 0th entry in the array mapped to by F will be 2.
	 * @param classData						A mapping from each class to an array containing the weight of each observation for the class.
	 * 										The observations are ordered by their original indices (dataIndices ordering).
	 * @param inBagObservations				An array recording which observations in the dataset have reached the node for which the split
	 * 										is being determined (in bag observations). Observations that are not in bag are given a value
	 * 										of 0.
	 * @param mtry							The number of features to evaluate for the cutpoint in each nonterminal node.
	 * @param treeRNG						The random number generator for the tree.
	 * @param numberOfUniqueObservations	The number of unique observations that have reached the node. Observations can be duplicates
	 * 										as sampling is performed with replacement.
	 * @return								The current node. Will either be a terminal node (with associated class composition
	 * 										information) or a nonterminal node with all its descendants calculated.
	 */
	private final Node growTree(Map<String, double[]> dataset, Map<String, int[]> dataIndices, Map<String, double[]> classData,
			int[] inBagObservations, int mtry, Random treeRNG, int numberOfUniqueObservations)
	{
		Set<String> classesPresent = this.classesPresent(classData, inBagObservations);  // The classes present in the in bag observations.
		
		// Create a terminal node if there are only observations of one class remaining.
		if (classesPresent.size() < 2)
		{
			Iterator<String> it = classesPresent.iterator();
			return new NodeTerminal(it.next(), classData, inBagObservations);
		}
		
		// Determine the best split that can be made.
		String featureUsedForSplit = null;
		double splitValue = 0.0;
		while (featureUsedForSplit == null)
		{
			//TODO Put in a value for the number of times this loop can go through before giving up and killing the search.
			List<String> datasetFeatures = new ArrayList<String>(dataset.keySet());
			Collections.shuffle(datasetFeatures, treeRNG);
			int numVarsToSelect = Math.min(datasetFeatures.size(), mtry);
			List<String> featuresToSplitOn = datasetFeatures.subList(0, numVarsToSelect);
			ImmutableTwoValues<String, Double> bestSplit = FindBestSplit.main(dataset, dataIndices, classData, inBagObservations,
					featuresToSplitOn, numberOfUniqueObservations);
			featureUsedForSplit = bestSplit.first;
			splitValue = bestSplit.second;
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
	
	
	/**
	 * Determine the names of the classes of the specified set of observations.
	 * 
	 * @param classData				A mapping from each class to an array containing the weight of each observation for the class.
	 * 								The observations are ordered by their original indices (dataIndices ordering).
	 * @param inBagObservations		An array recording which observations in the dataset have reached the node for which the split
	 * 								is being determined (in bag observations). Observations that are not in bag are given a value
	 * 								of 0.
	 * @return						A set containing the names of the different classes of the specified observations.
	 */
	private final Set<String> classesPresent(Map<String, double[]> classData, int[] inBagObservations)
	{
		Set<String> classesPresent = new HashSet<String>();
		Set<String> allClasses = new HashSet<String>(classData.keySet());
		
		// Add the classes that have non-zero weight for an in bag observation (and are therefore present).
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

		return classesPresent;
	}

	
	/**
	 * Predict the classes of a set of observations.
	 * 
	 * @param datasetToPredict	A mapping from each feature name to the values of the observations for it. The feature values are
	 * 							ordered in the same order as the observations appear in the file they were read from.
	 * @param obsToPredict		The indices of the observations to predict.
	 * @param predictions		A mapping from class names to observations. Used as an accumulator as it is passed down and then up
	 * 							the tree. Each class contains an entry for each observation in datasetToPredict. For an observation, i,
	 * 							the ith entry in the array for each class, c, will record the predicted weight given to class c for
	 * 							observation i. 
	 * @return					The updated predictions mapping.
	 */
	public final Map<String, double[]> predict(Map<String, double[]> datasetToPredict, Set<Integer> obsToPredict,
			Map<String, double[]> predictions)
	{
		return this.tree.predict(datasetToPredict, obsToPredict, predictions);
	}
	
	
	/**
	 * Split the dataset into the observations going to the left child and those going to the right child.
	 * 
	 * @param splitFeatureData		The observation values for the feature being split on.
	 * @param splitDataIndices		An array recording the original indices of the observations associated with the feature values in
	 * 								splitFeatureData.
	 * @param inBagObservations		An array recording the transformed indices of the observations that are still in bag. Observations
	 * 								that are not in bag are given a value of 0.
	 * @param splitValue			The value of the cutpoint for the feature being split on.
	 * @return						Four values.
	 * 								1) The indices of the observations that will be in bag for the left hand child.
	 * 								2) The number of unique observations going to the left hand child.
	 * 								3) The indices of the observations that will be in bag for the right hand child.
	 * 								4) The number of unique observations going to the right hand child.
	 */
	private final ImmutableFourValues<int[], Integer, int[], Integer> splitDataset(double[] splitFeatureData,
			int[] splitDataIndices, int[] inBagObservations, double splitValue)
	{
		// Initialise the arrays recording the observations going down the left and right hand branches.
		int numberOfObservations = inBagObservations.length;
		int[] leftChildInBag = new int[numberOfObservations];
		int numberOfUniqueLeftObservations = 0;
		int[] rightChildInBag = new int[numberOfObservations];
		int numberOfUniqueRightObservations = 0;

		// Determine where each observation is going.
		for (int i = 0; i < numberOfObservations; i++)
		{
			double dataValue = splitFeatureData[i];  // The observation's value for the feature being split on.
			int originalObsIndex = splitDataIndices[i];  // The original index of the observation.
			int inBagCount = inBagObservations[originalObsIndex];  // The number of times the observation is in bag.
			int inBagUnique = (inBagCount == 0 ? 0 : 1);  // 0 if the observation is not in bag and 1 otherwise.
			if (dataValue <= splitValue)
			{
				// The observation should go down the left hand branch.
				leftChildInBag[originalObsIndex] = inBagCount;
				numberOfUniqueLeftObservations += inBagUnique;
			}
			else
			{
				// The observation should go down the right hand branch.
				rightChildInBag[originalObsIndex] = inBagCount;
				numberOfUniqueRightObservations += inBagUnique;
			}
		}
		
		return new ImmutableFourValues<int[], Integer, int[], Integer>(leftChildInBag, numberOfUniqueLeftObservations,
				rightChildInBag, numberOfUniqueRightObservations);
	}
	
}
