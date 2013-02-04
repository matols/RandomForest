/**
 * 
 */
package tree;

import java.util.Map;

/**
 * @author Simon Bull
 *
 */
public class NodeNonTerminal extends Node
{

	double splitValue;
	String covariable;

	public NodeNonTerminal(String loadString)
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

	String save()
	{
		String returnValue = "";
		returnValue += Integer.toString(this.nodeDepth) + "/t" + Integer.toString(this.numberOfObservationsInNode) + "\t";
		returnValue +=  Double.toString(this.splitValue) + "\t" + this.covariable + "\t";
		for (String s : this.classCountsInNode.keySet())
		{
			returnValue += s + ";" + Integer.toString(this.classCountsInNode.get(s)) + ",";
		}
		returnValue = returnValue.substring(0, returnValue.length() - 1);  // Chop off the last ','.
		return returnValue;
	}

}
