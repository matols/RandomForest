/**
 * 
 */
package tree;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Simon Bull
 *
 */
public class NodeNonTerminal extends Node
{

	double splitValue;
	String covariable;

	public NodeNonTerminal(String loadString, Node leftChild, Node rightChild)
	{
		loadString = loadString.replaceAll("\n", "");
		String split[] = loadString.split("\t");
		this.nodeDepth = Integer.parseInt(split[0]);
		this.numberOfObservationsInNode = Integer.parseInt(split[1]);
		this.splitValue = Double.parseDouble(split[2]);
		this.covariable = split[3];
		for (String s : split[4].split(","))
		{
			String sSplit[] = s.split(";");
			this.classCountsInNode.put(sSplit[0], Integer.parseInt(sSplit[1]));
		}
		this.weights = new HashMap<String, Double>();
		for (String s : split[5].split(","))
		{
			String sSplit[] = s.split(";");
			this.weights.put(sSplit[0], Double.parseDouble(sSplit[1]));
		}
		this.children[0] = leftChild;
		this.children[1] = rightChild;
	}

	public NodeNonTerminal(int nodeDepth, String covariable, double splitValue, Node leftChild, Node rightChild,
			Map<String, Integer> classCountsInNode, Map<String, Double> weights)
	{
		this.nodeDepth = nodeDepth;
		this.splitValue = splitValue;
		this.children[0] = leftChild;
		this.children[1] = rightChild;
		this.classCountsInNode = classCountsInNode;
		this.covariable = covariable;
		for (String s : this.classCountsInNode.keySet())
		{
			this.numberOfObservationsInNode += this.classCountsInNode.get(s);
		}
		this.weights = weights;

	}

	void display()
	{
		for (int i = 0; i < nodeDepth; i++)
		{
			System.out.print("|  ");
		}
		System.out.format("Covariable : %s, split value : %f\n", covariable, splitValue);
		this.children[0].display();
		this.children[1].display();
	}

	ImmutableTwoValues<String, Double> predict(Map<String, Double> currentObservation)
	{
		double valueOfObservedCovar = currentObservation.get(this.covariable);
		if (valueOfObservedCovar <= this.splitValue)
		{
			return this.children[0].predict(currentObservation);
		}
		else
		{
			return this.children[1].predict(currentObservation);
		}
	}

	ImmutableTwoValues<String, Integer> save(Integer nodeID, Integer parentID)
	{
		String returnString = Integer.toString(nodeID) + "\t" + Integer.toString(parentID) + "\t";
		returnString += Integer.toString(this.nodeDepth) + "\t" + Integer.toString(this.numberOfObservationsInNode) + "\t";
		returnString +=  Double.toString(this.splitValue) + "\t" + this.covariable + "\t";
		for (String s : this.classCountsInNode.keySet())
		{
			returnString += s + ";" + Integer.toString(this.classCountsInNode.get(s)) + ",";
		}
		returnString = returnString.substring(0, returnString.length() - 1);  // Chop off the last ','.
		returnString += "\t";
		for (String s : this.weights.keySet())
		{
			returnString += s + ";" + Double.toString(this.weights.get(s)) + ",";
		}
		returnString = returnString.substring(0, returnString.length() - 1);  // Chop off the last ','.

		ImmutableTwoValues<String, Integer> leftChild = this.children[0].save(nodeID + 1, nodeID);
		Integer rightChildID = leftChild.second;
		ImmutableTwoValues<String, Integer> rightChild = this.children[1].save(rightChildID, nodeID);
		Integer nextID = rightChild.second;
		returnString += "\n" + leftChild.first + "\n" + rightChild.first;
		return new ImmutableTwoValues<String, Integer>(returnString, nextID);
	}

}
