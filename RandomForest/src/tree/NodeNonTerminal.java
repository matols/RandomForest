/**
 * 
 */
package tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Simon Bull
 *
 */
public class NodeNonTerminal extends Node
{

	double splitValue;
	String covariable;

	public NodeNonTerminal(Map<String, Map<String, String>> treeSkeleton, String nodeID)
	{
		// Create this node's left child.
		Node leftChild;
		String leftChildID = treeSkeleton.get(nodeID).get("LeftChild");
		Map<String, String> leftChildRecord = treeSkeleton.get(leftChildID);
		if (leftChildRecord.get("Type").equals("Terminal"))
		{
			// If the left child is a terminal node.
			leftChild = new NodeTerminal(treeSkeleton, leftChildID);
		}
		else
		{
			// If the left child is a non-terminal node.
			leftChild = new NodeNonTerminal(treeSkeleton, leftChildID);
		}

		// Create this node's right child.
		Node rightChild;
		String rightChildID  = treeSkeleton.get(nodeID).get("RightChild");
		Map<String, String> rightChildRecord = treeSkeleton.get(rightChildID);
		if (rightChildRecord.get("Type").equals("Terminal"))
		{
			// If the right child is a non-terminal node.
			rightChild = new NodeTerminal(treeSkeleton, rightChildID);
		}
		else
		{
			// If the right child is a non-terminal node.
			rightChild = new NodeNonTerminal(treeSkeleton, rightChildID);
		}

		// Create this node.
		String loadString = treeSkeleton.get(nodeID).get("Data");
		String split[] = loadString.split("\t");
		this.nodeDepth = Integer.parseInt(split[0]);
		this.splitValue = Double.parseDouble(split[1]);
		this.covariable = split[2];
		this.children[0] = leftChild;
		this.children[1] = rightChild;
	}

	public NodeNonTerminal(int nodeDepth, String covariable, double splitValue, Node leftChild, Node rightChild)
	{
		this.nodeDepth = nodeDepth;
		this.splitValue = splitValue;
		this.children[0] = leftChild;
		this.children[1] = rightChild;
		this.covariable = covariable;
	}

	int countTerminalNodes()
	{
		return this.children[0].countTerminalNodes() + this.children[1].countTerminalNodes();
	}

	String display()
	{
		String outputString = "";
		for (int i = 0; i < nodeDepth; i++)
		{
			outputString += "|  ";
		}
		outputString += "Covariable : " + this.covariable + ", split value : " + Double.toString(this.splitValue) + "\n";
		outputString += this.children[0].display();
		outputString += this.children[1].display();
		return outputString;
	}

	List<List<Integer>> getProximities(ProcessDataForGrowing processedData, List<Integer> observationIndices)
	{
		// Determine which observations go to which child.
		List<Integer> leftChildObs = new ArrayList<Integer>();
		List<Integer> rightChildObs = new ArrayList<Integer>();
		for (Integer i : observationIndices)
		{
			if (processedData.covariableData.get(this.covariable).get(i) <= this.splitValue)
			{
				leftChildObs.add(i);
			}
			else
			{
				rightChildObs.add(i);
			}
		}
		List<List<Integer>> leftChildProximities = this.children[0].getProximities(processedData, leftChildObs);
		List<List<Integer>> rightChildProximities = this.children[1].getProximities(processedData, rightChildObs);
		List<List<Integer>> proximities = new ArrayList<List<Integer>>();
		proximities.addAll(leftChildProximities);
		proximities.addAll(rightChildProximities);
		return proximities;
	}

	List<List<Integer>> getConditionalGrid(ProcessDataForGrowing processedData, List<List<Integer>> currentGrid, List<String> covToConditionOn)
	{
		List<List<Integer>> newGrid = new ArrayList<List<Integer>>();

		if (covToConditionOn.contains(this.covariable))
		{
			// If the covariable splitting this node is to be conditioned on.
			for (List<Integer> l : currentGrid)
			{
				// Bisect each grid element along the lines specified by this node's covariable and split point.
				List<Integer> leftSplitList = new ArrayList<Integer>();
				boolean isLeftListEmpty = true;
				List<Integer> rightSplitList = new ArrayList<Integer>();
				boolean isRightListEmpty = true;
				for (Integer i : l)
				{
					if (processedData.covariableData.get(this.covariable).get(i) <= this.splitValue)
					{
						leftSplitList.add(i);
						isLeftListEmpty = false;
					}
					else
					{
						rightSplitList.add(i);
						isRightListEmpty = false;
					}
				}
	
				if (!isLeftListEmpty)
				{
					// Only add the grid element for the left portion of the bisecting if it is not empty.
					newGrid.add(leftSplitList);
				}
				if (!isRightListEmpty)
				{
					// Only add the grid element for the right portion of the bisecting if it is not empty.
					newGrid.add(rightSplitList);
				}
			}
		}
		else
		{
			// If the covariable splitting this node is not to be conditioned on, then don't perform any bisecting for this node's split point.
			newGrid.addAll(currentGrid);
		}

		List<List<Integer>> leftChildGrid = this.children[0].getConditionalGrid(processedData, newGrid, covToConditionOn);
		List<List<Integer>> rightChildGrid = this.children[1].getConditionalGrid(processedData, leftChildGrid, covToConditionOn);
		return rightChildGrid;
	}

	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		Map<Integer, Map<String, Double>> predictedValues = new HashMap<Integer, Map<String, Double>>();
		if (!observationsToPredict.isEmpty())
		{
			List<Integer> leftChildObservations = new ArrayList<Integer>();
			List<Integer> rightChildObservations = new ArrayList<Integer>();
			for (Integer i : observationsToPredict)
			{
				double valueOfObservedCovar = predData.covariableData.get(this.covariable).get(i);
				if (valueOfObservedCovar <= this.splitValue)
				{
					leftChildObservations.add(i);
				}
				else
				{
					rightChildObservations.add(i);
				}
			}
			predictedValues.putAll(this.children[0].predict(predData, leftChildObservations));
			predictedValues.putAll(this.children[1].predict(predData, rightChildObservations));
		}
		return predictedValues;
	}

	ImmutableTwoValues<String, Integer> save(Integer nodeID, Integer parentID)
	{
		String returnString = Integer.toString(nodeID) + "\t" + Integer.toString(parentID) + "\tNonTerminal\t";
		returnString += Integer.toString(this.nodeDepth) + "\t" + Double.toString(this.splitValue) + "\t" + this.covariable + "\t";

		ImmutableTwoValues<String, Integer> leftChild = this.children[0].save(nodeID + 1, nodeID);
		Integer rightChildID = leftChild.second;
		ImmutableTwoValues<String, Integer> rightChild = this.children[1].save(rightChildID, nodeID);
		Integer nextID = rightChild.second;
		returnString += "\n" + leftChild.first + "\n" + rightChild.first;
		return new ImmutableTwoValues<String, Integer>(returnString, nextID);
	}

}
