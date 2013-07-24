package randomjyrest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utilities.ArrayManipulation;
import utilities.ImmutableTwoValues;

public class FindBestSplit
{

	public static final ImmutableTwoValues<String, Double> main(Map<String, double[]> dataset, Map<String, int[]> dataIndices,
			Map<String, double[]> classData, List<String> featuresToSplitOn)
	{
		String bestFeatureForSplit = null;
		double splitValue = 0.0;
		double lowestImpurity = 1.0;

		// Determine the original indices of all the observations present in the node.
		int[] originalIndicesOfObservationsPresent = dataIndices.get(featuresToSplitOn.get(0));
		
		// Determine the total weight of the observations of each class in the parent node along with the total weight of all observations
		// in the parent node.
		Map<String, Double> parentNodeClassWeights = new HashMap<String, Double>();
		double totalParentNodeWeight = 0.0;
		for (Map.Entry<String, double[]> entry : classData.entrySet())
		{
			String className = entry.getKey();
			double[] allClassWeights = entry.getValue();
			double classWeight = ArrayManipulation.sumArray(ArrayManipulation.selectSubset(allClassWeights, originalIndicesOfObservationsPresent));
			parentNodeClassWeights.put(className, classWeight);
			totalParentNodeWeight += classWeight;
		}
		
		for (String f : featuresToSplitOn)
		{
			double[] allFeatureData = dataset.get(f);
			int[] featureIndices = dataIndices.get(f);
			
			// Determine the cumulative and total class weight when the observations are sorted in ascending ordering on feature f.
			Map<String, double[]> leftChildCumulativeClassWeights = new HashMap<String, double[]>();
			Map<String, double[]> rightChildCumulativeClassWeights = new HashMap<String, double[]>();
			for (Map.Entry<String, double[]> entry : classData.entrySet())
			{
				String className = entry.getKey();
				double[] allClassWeights = entry.getValue();
				double[] leftChildWeights = ArrayManipulation.cumulativeArray(allClassWeights, featureIndices);
				leftChildCumulativeClassWeights.put(className, leftChildWeights);
				
				double parentWeightForClass = parentNodeClassWeights.get(className);
				double[] rightChildWeights = new double[leftChildWeights.length];
				int currentInsertionIndex = 0;
				for (double d : leftChildWeights)
				{
					rightChildWeights[currentInsertionIndex] = parentWeightForClass - d;
					currentInsertionIndex++;
				}
				rightChildCumulativeClassWeights.put(className, rightChildWeights);
			}
			
			// Determine the unique values for this feature amongst the observation along with the last index in the array of sorted
			// data values where the value occurs.
			Map<Double, Integer> uniqueFeatureData = ArrayManipulation.unique(allFeatureData);
			
			// Determine the Gini impurity for each unique data value.
			for (Map.Entry<Double, Integer> entry : uniqueFeatureData.entrySet())
			{
				Integer dataIndex = entry.getValue();

				// Determine the total weight in each child node across all classes.
				double totalLeftChildWeight = 0.0;
				Map<String, Double> leftChildClassWeights = new HashMap<String, Double>();
				Map<String, Double> rightChildClassWeights = new HashMap<String, Double>();
				double totalRightChildWeight = 0.0;
				for (String s : classData.keySet())
				{
					double leftChildWeight = leftChildCumulativeClassWeights.get(s)[dataIndex];
					leftChildClassWeights.put(s, leftChildWeight);
					totalLeftChildWeight += leftChildWeight;
					
					double rightChildWeight = rightChildCumulativeClassWeights.get(s)[dataIndex];
					rightChildClassWeights.put(s, rightChildWeight);
					totalRightChildWeight += rightChildWeight;
				}
				
				// Determine the Gini impurity of the child nodes.
				double leftChildImpurity = 1.0;
				double rightChildImpurity = 1.0;
				for (String s : classData.keySet())
				{
					double fractionOfClassSInLeftChild = leftChildClassWeights.get(s) / totalLeftChildWeight;
					leftChildImpurity = leftChildImpurity - (fractionOfClassSInLeftChild * fractionOfClassSInLeftChild);

					double fractionOfClassSInRightChild = rightChildClassWeights.get(s) / totalRightChildWeight;
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
					splitValue = (entry.getKey() + allFeatureData[dataIndex + 1]) / 2.0;
				}
			}
		}

		return new ImmutableTwoValues<String, Double>(bestFeatureForSplit, splitValue);
	}

}
