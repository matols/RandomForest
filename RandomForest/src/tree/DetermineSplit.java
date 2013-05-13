/**
 * 
 */
package tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Simon Bull
 *
 */
public class DetermineSplit
{

	ImmutableThreeValues<Boolean, Double, String> findBestSplit(Map<String, List<Double>> covariableData, List<String> responseData,
			List<Integer> observationsInNode, List<String> variablesToSplitOn, Map<String, Double> weights, TreeGrowthControl ctrl)
	{
		double maxSumDaughterNodeGini = -Double.MAX_VALUE;
		double splitValue = 0.0;
		String covariableToSplitOn = null;
		boolean isSplitFound = false;

		// Calculate the weighted number of observations of each class that are present in the parent node.
		// Also calculate the weighted number of occurrences of each observation in the parent node.
		Map<String, Double> parentClassCount = new HashMap<String, Double>();
		Map<Integer, Double> parentObservationCounts = new HashMap<Integer, Double>();
		for (String s : weights.keySet())
		{
			parentClassCount.put(s, 0.0);
		}
		for (Integer i : observationsInNode)
		{
			String response = responseData.get(i);
			parentClassCount.put(response, parentClassCount.get(response) + weights.get(response));
			if (parentObservationCounts.containsKey(i))
			{
				double oldObsCount = parentObservationCounts.get(i);
				parentObservationCounts.put(i, oldObsCount + weights.get(response));
			}
			else
			{
				parentObservationCounts.put(i, weights.get(response));
			}
		}

		// Calculate Gini index for parent node.
		double parentGiniNumerator = 0.0;
		double parentGiniDenominator = 0.0;
		for (String s : weights.keySet())
		{
			double classWeightedCount = parentClassCount.get(s);
			parentGiniNumerator += Math.pow(classWeightedCount, 2);
			parentGiniDenominator += classWeightedCount;
		}

		for (String s : variablesToSplitOn)
		{
			// Initialise the child nodes' Gini numerators and denominators.
			double rightChildNumerator = parentGiniNumerator;
			double rightChildDenominator = parentGiniDenominator;
			double leftChildNumerator = 0.0;
			double leftChildDenominator = 0.0;

			// Initialise the right and left child node weighted class counts.
			Map<String, Double> rightChildWeghtedCounts = new HashMap<String, Double>();
			Map<String, Double> leftChildWeghtedCounts = new HashMap<String, Double>();
			for (String i : parentClassCount.keySet())
			{
				rightChildWeghtedCounts.put(i, parentClassCount.get(i));
				leftChildWeghtedCounts.put(i, 0.0);
			}

			// Sort the observations in the node in ascending order by their value for covariable s.
			List<IndexedDoubleData> sortedCovariableValues = new ArrayList<IndexedDoubleData>();
			for (Integer i : observationsInNode)
			{
				sortedCovariableValues.add(new IndexedDoubleData(covariableData.get(s).get(i), i));
			}
			Collections.sort(sortedCovariableValues);

			// Loop through the ordered covariable values, and find the best split for the covariable.
			for (int i = 0; i < sortedCovariableValues.size() - 1; i++)
			{
				IndexedDoubleData currentCovariableInstance = sortedCovariableValues.get(i);
				double covariableValue = currentCovariableInstance.getData();
				int covariableIndex = currentCovariableInstance.getIndex();
//				double covariableWeightedOccurences = parentObservationCounts.get(covariableIndex);
				String covariableClass = responseData.get(covariableIndex);
				double covariableWeightedOccurences = weights.get(covariableClass);

				// Update the child node Gini numerators and denominators.
				rightChildNumerator = covariableWeightedOccurences * (-2 * rightChildWeghtedCounts.get(covariableClass) + covariableWeightedOccurences);
				rightChildDenominator -= covariableWeightedOccurences;
				leftChildNumerator = covariableWeightedOccurences * (2 * leftChildWeghtedCounts.get(covariableClass) + covariableWeightedOccurences);;
				leftChildDenominator += covariableWeightedOccurences;

				// Update the child node weighted observations counts.
				double oldRightChildCount = rightChildWeghtedCounts.get(covariableClass);
				rightChildWeghtedCounts.put(covariableClass, oldRightChildCount - covariableWeightedOccurences);
				double oldLeftChildCount = leftChildWeghtedCounts.get(covariableClass);
				leftChildWeghtedCounts.put(covariableClass, oldLeftChildCount + covariableWeightedOccurences);

				if ((i + 1) < ctrl.minNodeSize || (observationsInNode.size() - (i + 1)) < ctrl.minNodeSize)
				{
					// Can't split if the children would be too small.
					// (i + 1) observations down left branch and (observationsInNode.size() - (i + 1)) down right.
					continue;
				}

				double nextValue = sortedCovariableValues.get(i + 1).getData();
				if (covariableValue < nextValue)
				{
					// If the value of the covariable at position i in the sorted list is < (and therefore not equal to)
					// the value of the covariable at the next sorted position, then determine whether the split is worth keeping.
					double crit = (leftChildNumerator / leftChildDenominator) + (rightChildNumerator / rightChildDenominator);
					if (crit > maxSumDaughterNodeGini)
					{
						// If the sum of the Gini impurity for the children nodes is better than any found before, then
						// record this fact.
						maxSumDaughterNodeGini = crit;
						covariableToSplitOn = s;
						splitValue = (covariableValue + nextValue) / 2.0;
					}
				}
			}
		}

		if (maxSumDaughterNodeGini >= 0.1)
		{
			// Only return the best split if the sum of the children nodes Gini impurity is greater than a threshold value.
			isSplitFound = true;
		}
		return new ImmutableThreeValues<Boolean, Double, String>(isSplitFound, splitValue, covariableToSplitOn);
	}
}
