/**
 * 
 */
package tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author		Simon Bull
 * @version		1.0
 * @since		2013-01-31
 */
public class CARTTree
{

	/**
	 * The tree that has been grown.
	 */
	Node cartTree = null;

	/**
	 * The object recording the control parameters for the tree.
	 */
	TreeGrowthControl ctrl;

	/**
	 * 
	 */
	ProcessDataForGrowing processedData;

	long seed;

	public CARTTree(ProcessDataForGrowing processedData)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
	}

	public CARTTree(ProcessDataForGrowing processedData, TreeGrowthControl ctrl)
	{
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
	}

	public CARTTree(ProcessDataForGrowing processedData, TreeGrowthControl ctrl, long seed)
	{
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = seed;
	}


	/**
	 * Controls the growth of the tree.
	 */
	public void growTree(Map<String, Map<Integer, Double>> weights, List<Integer> observationsUsed)
	{
		// Grow the tree.
		this.cartTree = controlTreeGrowth(observationsUsed, weights, 1);
	}


	/**
	 * Displays the tree.
	 */
	public String display()
	{
		return this.cartTree.display();
	}


	Node controlTreeGrowth(List<Integer> observationsInNode, Map<String, Map<Integer, Double>> weights, int currentDepth)
	{
		// Determine the weight of each class in the current node, and determine the classes present in the node.
		Map<String, Double> classWeightsInNode = new HashMap<String, Double>();
		Set<String> classesPresentInNode = new HashSet<String>();
		double positiveWeight = 0.0;
		double unlabelledWeight = 0.0;
		for (Integer i : observationsInNode)
		{
			double obsPosWeight = weights.get("Positive").get(i);
			double obsUnlabelledWeight = weights.get("Unlabelled").get(i);
			if (obsPosWeight > obsUnlabelledWeight)
			{
				classesPresentInNode.add("Positive");
			}
			else
			{
				classesPresentInNode.add("Unlabelled");
			}
			positiveWeight += obsPosWeight;
			unlabelledWeight += obsUnlabelledWeight;
		}
		classWeightsInNode.put("Positive", positiveWeight);
		classWeightsInNode.put("Unlabelled", unlabelledWeight);

		//**********************************************
		// Check whether growth should stop.
		//**********************************************
		if (!(classesPresentInNode.size() > 1))
		{
			// There are too few classes present in the observations in the node to warrant a split.
			// A terminal node must therefore be created.
			return new NodeTerminal(classWeightsInNode, currentDepth);
		}
		if (currentDepth >= ctrl.maxTreeDepth)
		{
			// The depth of the tree has reached the maximum permissible.
			// A terminal node must therefore be created.
			return new NodeTerminal(classWeightsInNode, currentDepth);
		}
		

		//**********************************************
		// Determine the variables to split on.
		//**********************************************
		Set<String> covariablesAvailable = this.processedData.covariableData.keySet();
		List<String> shuffledCovariables = new ArrayList<String>(covariablesAvailable);
		Collections.shuffle(shuffledCovariables, new Random(this.seed));
		int numVarsToSelect = Math.min(covariablesAvailable.size(), ctrl.mtry);
		List<String> variablesToSplitOn = shuffledCovariables.subList(0, numVarsToSelect);

		//**********************************************
		// Try to find a split.
		//**********************************************
		boolean isSplitFound = false;
		double splitValue;
		String covarToSplitOn;

		DetermineSplit splitCalculator = new DetermineSplit();
		ImmutableThreeValues<Boolean, Double, String> splitResult = splitCalculator.findBestSplit(this.processedData.covariableData,
				this.processedData.responseData, observationsInNode, variablesToSplitOn, weights, this.ctrl, classWeightsInNode);
		isSplitFound = splitResult.first;
		splitValue = splitResult.second;
		covarToSplitOn = splitResult.third;

		// Return the Node and continue building the tree.
		if (isSplitFound)
		{
			List<Integer> rightObservations = new ArrayList<Integer>();
			List<Integer> leftObserations = new ArrayList<Integer>();
			// If a valid split was found, then generate a non-terminal node and recurse through its children.
			for (Integer i : observationsInNode)
			{
				// Sort out which observations will go into the right child, and which will go into the left child.
				if (this.processedData.covariableData.get(covarToSplitOn).get(i) > splitValue)
				{
					rightObservations.add(i);
				}
				else
				{
					leftObserations.add(i);
				}
			}
			Node leftChild = controlTreeGrowth(leftObserations, weights, currentDepth + 1);
			Node rightChild = controlTreeGrowth(rightObservations, weights, currentDepth + 1);
			return new NodeNonTerminal(currentDepth, covarToSplitOn, splitValue, leftChild, rightChild);
		}
		else
		{
			// If there was no valid split found, then return a terminal node.
			return new NodeTerminal(classWeightsInNode, currentDepth);
		}

	}

	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData)
	{
		List<Integer> observationsToPredict = new ArrayList<Integer>();
		for (int i = 0; i < predData.numberObservations; i++)
		{
			observationsToPredict.add(i);
		}
		return predict(predData, observationsToPredict);
	}

	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		if (this.cartTree == null)
		{
			System.out.println("The tree can not be used for prediction before it has been trained.");
			System.exit(0);
		}
		return this.cartTree.predict(predData, observationsToPredict);
	}

}
