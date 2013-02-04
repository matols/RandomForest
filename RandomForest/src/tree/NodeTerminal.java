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
public class NodeTerminal extends Node
{

	NodeTerminal(String loadString)
	{
		loadString = loadString.replaceAll("\n", "");
		String split[] = loadString.split("\t");
		this.classCountsInNode = new HashMap<String, Integer>();
		for (String s : split[0].split(","))
		{
			String sSplit[] = s.split(";");
			this.classCountsInNode.put(sSplit[0], Integer.parseInt(sSplit[1]));
		}
		this.nodeDepth = Integer.parseInt(split[1]);
		this.children[0] = null;
		this.children[1] = null;
	}

	NodeTerminal(Map<String, Integer> classCounts, int currentDepth, Map<String, Double> weights)
	{
		this.classCountsInNode = classCounts;
		this.nodeDepth = currentDepth;
		this.weights = weights;
		this.children[0] = null;
		this.children[1] = null;
	}

	void display()
	{
		for (int i = 0; i < this.nodeDepth; i++)
		{
			System.out.print("|  ");
		}
		System.out.println(this.classCountsInNode.entrySet());
	}

	ImmutableTwoValues<String, Double> predict(Map<String, Double> currentObservation)
	{
		String maxClass = "";
		double largestClassCount = 0.0;
		for (String className : this.classCountsInNode.keySet())
		{
			double thisClassVote = this.classCountsInNode.get(className) * this.weights.get(className);  // Weighted class vote.
			if (thisClassVote > largestClassCount)
			{
				maxClass = className;
				largestClassCount = thisClassVote;
			}
		}

		return new ImmutableTwoValues<String, Double>(maxClass, largestClassCount);
	}

	String save()
	{
		String returnValue = "";
		for (String s : this.classCountsInNode.keySet())
		{
			returnValue += s + ";" + Integer.toBinaryString(this.classCountsInNode.get(s)) + ",";
		}
		returnValue = returnValue.substring(0, returnValue.length() - 1);  // Chop off the last ','.
		returnValue += "\t";
		returnValue += Integer.toString(this.nodeDepth);
		return returnValue;
	}

}
