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
			List<Integer> observationsInNode, List<String> variablesToSplitOn, Map<String, Map<Integer, Double>> weights, TreeGrowthControl ctrl,
			Map<String, Double> classWeightsInParentNode)
	{
		double maxSumDaughterNodeGini = 1.0;
		double splitValue = 0.0;
		String covariableToSplitOn = null;
		boolean isSplitFound = false;

		for (String s : variablesToSplitOn)
		{
			// Initialise the right and left child node weighted class counts.
			Map<String, Double> rightChildWeghtedCounts = new HashMap<String, Double>();
			Map<String, Double> leftChildWeghtedCounts = new HashMap<String, Double>();
			double totalParentWeight = 0.0;
			double totalLeftChildWeight = 0.0;
			double totalRightChildWeight = 0.0;
			for (String i : classWeightsInParentNode.keySet())
			{
				double classWeight = classWeightsInParentNode.get(i);
				totalParentWeight += classWeight;
				totalRightChildWeight += classWeight;
				rightChildWeghtedCounts.put(i, classWeight);
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
				int observationIndex = currentCovariableInstance.getIndex();
				String covariableClass = responseData.get(observationIndex);
				double positiveObsWeight = weights.get("Positive").get(observationIndex);
				double unlabelledObsWeight = weights.get("Unlabelled").get(observationIndex);

				// Update the child node weighted observations counts.
				double oldRightChildCount = rightChildWeghtedCounts.get(covariableClass);
				rightChildWeghtedCounts.put("Positive", oldRightChildCount - positiveObsWeight);
				rightChildWeghtedCounts.put("Unlabelled", oldRightChildCount - unlabelledObsWeight);
				totalRightChildWeight = totalRightChildWeight - positiveObsWeight - unlabelledObsWeight;
				double oldLeftChildCount = leftChildWeghtedCounts.get(covariableClass);
				leftChildWeghtedCounts.put("Positive", oldLeftChildCount + positiveObsWeight);
				leftChildWeghtedCounts.put("Unlabelled", oldLeftChildCount + unlabelledObsWeight);
				totalLeftChildWeight = totalLeftChildWeight + positiveObsWeight + unlabelledObsWeight;

				double leftGini = 1.0;
				for (String p : leftChildWeghtedCounts.keySet())
				{
					leftGini -= Math.pow(leftChildWeghtedCounts.get(p) / totalLeftChildWeight, 2);
				}
				double rightGini = 1.0;
				for (String p : leftChildWeghtedCounts.keySet())
				{
					rightGini -= Math.pow(rightChildWeghtedCounts.get(p) / totalRightChildWeight, 2);
				}

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
					double crit = ((totalLeftChildWeight / totalParentWeight) * leftGini) + ((totalRightChildWeight / totalParentWeight) * rightGini);
					if (crit < maxSumDaughterNodeGini)
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

		if (maxSumDaughterNodeGini < 1.0)
		{
			// Only return the best split if the sum of the children nodes Gini impurity is greater than a threshold value.
			isSplitFound = true;
		}
		return new ImmutableThreeValues<Boolean, Double, String>(isSplitFound, splitValue, covariableToSplitOn);
	}
}
