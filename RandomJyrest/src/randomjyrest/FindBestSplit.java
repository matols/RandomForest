package randomjyrest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import utilities.ImmutableTwoValues;

/**
 * Implements the determination of the best feature split value.
 */
public class FindBestSplit
{

	/**
	 * Determine the feature and its value such that the binary split induced has the minimal impurity out of all possible splits.
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
	 * @param featuresToSplitOn				The features that are to be tested for a split value.
	 * @param numberOfUniqueObservations	The number of unique observations that have reached the node. Observations can be duplicates
	 * 										as sampling is performed with replacement.
	 * @return								The feature to split on, along with the value of the feature to use for the split.
	 */
	public static final ImmutableTwoValues<String, Double> main(Map<String, double[]> dataset, Map<String, int[]> dataIndices,
			Map<String, double[]> classData, int[] inBagObservations, List<String> featuresToSplitOn, int numberOfUniqueObservations)
	{
		// Initialise the values for the best split found.
		String bestFeatureForSplit = null;
		double splitValue = 0.0;
		double lowestImpurity = 1.0;
		
		// Determine the number of observations in the node and the different classes of these observations.
		List<String> allClasses = new ArrayList<String>(classData.keySet());
		int numberOfClasses = allClasses.size();
		int numberOfObservations = inBagObservations.length;
		
		// Evaluate all possible binary splits for each feature being analysed.
		for (String f : featuresToSplitOn)
		{
			double[] allFeatureData = dataset.get(f);  // Get the sorted observation values for feature f.
			double[] subsetFeatureData = new double[numberOfUniqueObservations];  // Set up the array for the values of the unique in bag observations.
			int[] featureIndices = dataIndices.get(f);  // Get the original indices of the in bag observations.
			int[] subsetFeatureIndices = new int[numberOfUniqueObservations];  // Set up the array for the indices of the unique in bag observations.
			int curretInsertionIndex = 0;
			// Go through all observations and record the data value and original index of the unique in bag observations.
			for (int i = 0; i < numberOfObservations; i++)
			{
				int originalIndex = featureIndices[i];  // The original index of the observation with the ith smallest value for feature f.
				if (inBagObservations[originalIndex] != 0)
				{
					// If the index is in bag and has not been encountered before, then record the value and original index of the
					// observation. Recording only unique observations reduces the number of split evaluations and logic that is needed.
					subsetFeatureData[curretInsertionIndex] = allFeatureData[i];
					subsetFeatureIndices[curretInsertionIndex] = featureIndices[i];
					curretInsertionIndex++;
				}
			}
			
			// Initialise weights for the classes.
			double totalParentNodeWeight = 0.0;  // The total weight of all observations in the parent node.
			double[] parentNodeClassWeights = new double[numberOfClasses];  // The individual weight for each class in the parent node.
			// The cumulative weight for each class, C, is an array where the value at index i+1 is equal to the value at index i plus the
			// weight for class C of observation i+1.
			double[][] cumulativeClassWeights = new double[numberOfClasses][numberOfUniqueObservations];
			
			// Determine the class weight for each class.
			for (int i = 0; i < numberOfClasses; i++)
			{
				String currentClass = allClasses.get(i);
				double[] classWeights = classData.get(currentClass);
				double[] classCumulativeWeights = new double[numberOfUniqueObservations];
				double totalClassWeight = 0.0;
				int currentInsertionIndex = 0;
				// For each unique observation in bag.
				for (int j : subsetFeatureIndices)
				{
					// The weight of the observation with original index j is its class weight multiplied by the number of times it is
					// in bag. The class weight for j can be 0, in which case j is of a different class. In this case there will be no
					// increase in the totalClassWeight.
					double weightOfThisObs = classWeights[j] * inBagObservations[j];
					totalClassWeight += weightOfThisObs;  // Increment the total weight for the class by the weight of observation j.
					classCumulativeWeights[currentInsertionIndex] = totalClassWeight;
					currentInsertionIndex++;
				}
				parentNodeClassWeights[i] = totalClassWeight;
				cumulativeClassWeights[i] = classCumulativeWeights;
				totalParentNodeWeight += totalClassWeight;
			}
			
			// Check for a split between all pairs of observations except for the second to last and last observations, as the last
			// observation (the one with largest value for feature f) must always go to the right child node.
			int observationsToCheck = numberOfUniqueObservations - 1;
			for (int i = 0; i < observationsToCheck; i++)
			{
				double currentFeatureValue = subsetFeatureData[i];  // Value of feature f for the observation with the ith smallest value for f.
				double nextFeatureValue = subsetFeatureData[i + 1];  // Value of feature f for the observation with the i+1 smallest value for feature f.
				// If the value for observation i == value for i+1, then there can be no split. Therefore, only split when there is a
				// difference between the values (and in this case nextFeatureValue > currentFeatureValue due to the ordering).
				if (currentFeatureValue - nextFeatureValue != 0)
				{
					// Determine the total weight (for all classes) in both the left and right child nodes that would be created by this potential split.
					// The weight for a child node is calculated as follows:
					//
					// w_c = sum_i (w_i * n_i)
					//
					// where w_c is the weight of child c, i is a class, w_i is the weight of all class i observations in c ad n_i is the
					// number of observations of class i in the node.
					double totalLeftChildWeight = 0.0;  // Total left child weight.
					double[] leftChildClassWeights = new double[numberOfClasses];  // Weight of each class in the left child.
					double totalRightChildWeight = 0.0;  // Total right child weight.
					double[] rightChildClassWeights = new double[numberOfClasses];  // Weight of each class in the right child.
					for (int j = 0; j < numberOfClasses; j++)
					{
						double leftChildWeight = cumulativeClassWeights[j][i];
						leftChildClassWeights[j] = leftChildWeight;
						totalLeftChildWeight += leftChildWeight;
						
						double rightChildWeight = parentNodeClassWeights[j] - leftChildWeight;
						rightChildClassWeights[j] = rightChildWeight;
						totalRightChildWeight += rightChildWeight;
					}
					
					// Determine the Gini impurity of the child nodes. This is calculated as follows:
					//
					// i_c = 1 - sum_i (w_i * n_i / w_c)^2
					//
					// where i_c is the impurity of child c, i is a class, w_i is the weight of all class i observations in c, n_i is the
					// number of observations of class i in the node and w_c is the total weight in child c.
					double leftChildImpurity = 1.0;
					double rightChildImpurity = 1.0;
					for (int j = 0; j < numberOfClasses; j++)
					{
						double fractionOfClassSInLeftChild = leftChildClassWeights[j] / totalLeftChildWeight;
						leftChildImpurity = leftChildImpurity - (fractionOfClassSInLeftChild * fractionOfClassSInLeftChild);

						double fractionOfClassSInRightChild = rightChildClassWeights[j] / totalRightChildWeight;
						rightChildImpurity = rightChildImpurity - (fractionOfClassSInRightChild * fractionOfClassSInRightChild);
					}
					
					// Determine the Gini impurity for the split. this is calculated as follows:
					//
					// sum_c (w_c / w_p) * i_c
					//
					// where c is a child node, w_c is the weight of all observations in node c, w_p is the weight of all observations in
					// the parent and i_c is the impurity of child c.
					double splitImpurity = ((totalLeftChildWeight / totalParentNodeWeight) * leftChildImpurity) +
							((totalRightChildWeight / totalParentNodeWeight) * rightChildImpurity);
					
					// Check whether this is the best split found.
					if (splitImpurity < lowestImpurity)
					{
						lowestImpurity = splitImpurity;
						bestFeatureForSplit = f;
						splitValue = (currentFeatureValue + nextFeatureValue) / 2.0;
					}
				}
			}
		}

		return new ImmutableTwoValues<String, Double>(bestFeatureForSplit, splitValue);
	}

}
