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
public class PUCARTTree
{

	/**
	 * The tree that has been grown.
	 */
	PUNode cartTree = null;

	/**
	 * The object recording the control parameters for the tree.
	 */
	PUTreeGrowthControl ctrl;

	/**
	 * 
	 */
	PUProcessDataForGrowing processedData;

	long seed;

	public PUCARTTree(PUProcessDataForGrowing processedData)
	{
		PUTreeGrowthControl ctrl = new PUTreeGrowthControl();
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
	}

	public PUCARTTree(PUProcessDataForGrowing processedData, PUTreeGrowthControl ctrl)
	{
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
	}

	public PUCARTTree(PUProcessDataForGrowing processedData, PUTreeGrowthControl ctrl, long seed)
	{
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = seed;
	}


	/**
	 * Controls the growth of the tree.
	 */
	public void growTree(Map<String, Double> classWeights, Map<String, Map<Integer, Double>> discounts, List<Integer> observationsUsed)
	{
		// Grow the tree.
		this.cartTree = controlTreeGrowth(observationsUsed, classWeights, discounts, 1);
	}


	/**
	 * Displays the tree.
	 */
	public String display()
	{
		return this.cartTree.display();
	}


	PUNode controlTreeGrowth(List<Integer> observationsInNode, Map<String, Double> classWeights, Map<String, Map<Integer, Double>> discounts,
			int currentDepth)
	{
		// Determine the weight of each class in the current node, and determine the classes present in the node.
		Map<String, Double> classWeightsInNode = new HashMap<String, Double>();
		Set<String> classesPresentInNode = new HashSet<String>();
		Map<String, Map<Integer, Double>> observationWeights = new HashMap<String, Map<Integer, Double>>();
		observationWeights.put("Positive", new HashMap<Integer, Double>());
		observationWeights.put("Unlabelled", new HashMap<Integer, Double>());
		double positiveWeight = 0.0;
		double unlabelledWeight = 0.0;
		for (Integer i : observationsInNode)
		{
			double obsPositiveDiscount = discounts.get("Positive").get(i);
			double obsUnlabelledDiscount = discounts.get("Unlabelled").get(i);
			if (obsPositiveDiscount > this.ctrl.positiveFractionTerminalCutoff)
			{
				classesPresentInNode.add("Positive");
			}
			else
			{
				classesPresentInNode.add("Unlabelled");
			}

			double obsPositiveWeight = obsPositiveDiscount * classWeights.get("Positive");
			double obseUnlabelledWeight = obsUnlabelledDiscount * classWeights.get("Unlabelled");
			observationWeights.get("Positive").put(i, obsPositiveWeight);
			observationWeights.get("Unlabelled").put(i, obseUnlabelledWeight);
			positiveWeight += (obsPositiveWeight);
			unlabelledWeight += (obseUnlabelledWeight);
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
			return new PUNodeTerminal(classWeightsInNode, currentDepth);
		}
		if (currentDepth >= ctrl.maxTreeDepth)
		{
			// The depth of the tree has reached the maximum permissible.
			// A terminal node must therefore be created.
			return new PUNodeTerminal(classWeightsInNode, currentDepth);
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

		PUDetermineSplit splitCalculator = new PUDetermineSplit();
		ImmutableThreeValues<Boolean, Double, String> splitResult = splitCalculator.findBestSplit(this.processedData.covariableData,
				this.processedData.responseData, observationsInNode, variablesToSplitOn, observationWeights, this.ctrl, classWeightsInNode);
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
			PUNode leftChild = controlTreeGrowth(leftObserations, classWeights, discounts, currentDepth + 1);
			PUNode rightChild = controlTreeGrowth(rightObservations, classWeights, discounts, currentDepth + 1);
			return new PUNodeNonTerminal(currentDepth, covarToSplitOn, splitValue, leftChild, rightChild);
		}
		else
		{
			// If there was no valid split found, then return a terminal node.
			return new PUNodeTerminal(classWeightsInNode, currentDepth);
		}

	}

	Map<Integer, Map<String, Double>> predict(PUProcessDataForGrowing predData)
	{
		List<Integer> observationsToPredict = new ArrayList<Integer>();
		for (int i = 0; i < predData.numberObservations; i++)
		{
			observationsToPredict.add(i);
		}
		return predict(predData, observationsToPredict);
	}

	Map<Integer, Map<String, Double>> predict(PUProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		if (this.cartTree == null)
		{
			System.out.println("The tree can not be used for prediction before it has been trained.");
			System.exit(0);
		}
		return this.cartTree.predict(predData, observationsToPredict);
	}

}
