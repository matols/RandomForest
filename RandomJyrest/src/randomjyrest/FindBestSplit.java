package randomjyrest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import utilities.ImmutableTwoValues;

public class FindBestSplit
{

	public static final ImmutableTwoValues<String, Double> main(Map<String, double[]> dataset, Map<String, int[]> dataIndices,
			Map<String, double[]> classData, int[] inBagObservations, List<String> featuresToSplitOn, int numberOfUniqueObservations)
	{
		String bestFeatureForSplit = null;
		double splitValue = 0.0;
		double lowestImpurity = 1.0;
		
		List<String> allClasses = new ArrayList<String>(classData.keySet());
		int numberOfClasses = allClasses.size();
		int numberOfObservations = inBagObservations.length;
		
		for (String f : featuresToSplitOn)
		{
			double[] allFeatureData = dataset.get(f);
			double[] subsetFeatureData = new double[numberOfUniqueObservations];
			int[] featureIndices = dataIndices.get(f);
			int[] subsetFeatureIndices = new int[numberOfUniqueObservations];
			int curretInsertionIndex = 0;
			for (int i = 0; i < numberOfObservations; i++)
			{
				int originalIndex = featureIndices[i];
				if (inBagObservations[originalIndex] != 0)
				{
					// If the index is in the bag.
					subsetFeatureData[curretInsertionIndex] = allFeatureData[i];
					subsetFeatureIndices[curretInsertionIndex] = featureIndices[i];
					curretInsertionIndex++;
				}
			}
			
			double totalParentNodeWeight = 0.0;
			double[] parentNodeClassWeights = new double[numberOfClasses];
			double[][] cumulativeClassWeights = new double[numberOfClasses][numberOfUniqueObservations];
			
			for (int i = 0; i < numberOfClasses; i++)
			{
				String currentClass = allClasses.get(i);
				double[] classWeights = classData.get(currentClass);
				double[] classCumulativeWeights = new double[numberOfUniqueObservations];
				double totalClassWeight = 0.0;
				int currentInsertionIndex = 0;
				for (int j : subsetFeatureIndices)
				{
					double weightOfThisObs = classWeights[j] * inBagObservations[j];
					totalClassWeight += weightOfThisObs;
					classCumulativeWeights[currentInsertionIndex] = totalClassWeight;
					currentInsertionIndex++;
				}
				parentNodeClassWeights[i] = totalClassWeight;
				cumulativeClassWeights[i] = classCumulativeWeights;
				totalParentNodeWeight += totalClassWeight;
			}
			
			int observationsToCheck = numberOfUniqueObservations - 1;  // Check all except the last observation as at last one observation must go down the RHS.
			for (int i = 0; i < observationsToCheck; i++)
			{
				double currentFeatureValue = subsetFeatureData[i];
				double nextFeatureValue = subsetFeatureData[i + 1];
				if (currentFeatureValue - nextFeatureValue != 0)
				{
					// Determine the total weight in each child node across all classes.
					double totalLeftChildWeight = 0.0;
					double[] leftChildClassWeights = new double[numberOfClasses];
					double totalRightChildWeight = 0.0;
					double[] rightChildClassWeights = new double[numberOfClasses];
					for (int j = 0; j < numberOfClasses; j++)
					{
						double leftChildWeight = cumulativeClassWeights[j][i];
						leftChildClassWeights[j] = leftChildWeight;
						totalLeftChildWeight += leftChildWeight;
						
						double rightChildWeight = parentNodeClassWeights[j] - leftChildWeight;
						rightChildClassWeights[j] = rightChildWeight;
						totalRightChildWeight += rightChildWeight;
					}
					
					// Determine the Gini impurity of the child nodes.
					double leftChildImpurity = 1.0;
					double rightChildImpurity = 1.0;
					for (int j = 0; j < numberOfClasses; j++)
					{
						double fractionOfClassSInLeftChild = leftChildClassWeights[j] / totalLeftChildWeight;
						leftChildImpurity = leftChildImpurity - (fractionOfClassSInLeftChild * fractionOfClassSInLeftChild);

						double fractionOfClassSInRightChild = rightChildClassWeights[j] / totalRightChildWeight;
						rightChildImpurity = rightChildImpurity - (fractionOfClassSInRightChild * fractionOfClassSInRightChild);
					}
					
					// Determine the Gini impurity for the split.
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
