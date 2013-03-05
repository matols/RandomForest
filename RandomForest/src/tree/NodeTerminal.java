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
public class NodeTerminal extends Node
{

	NodeTerminal(Map<String, Map<String, String>> treeSkeleton, String nodeID)
	{
		String loadString = treeSkeleton.get(nodeID).get("Data");
		String split[] = loadString.split("\t");
		this.nodeDepth = Integer.parseInt(split[0]);
		this.classCountsInNode = new HashMap<String, Integer>();
		for (String s : split[1].split(","))
		{
			String sSplit[] = s.split(";");
			this.classCountsInNode.put(sSplit[0], Integer.parseInt(sSplit[1]));
		}
		this.weights = new HashMap<String, Double>();
		for (String s : split[2].split(","))
		{
			String sSplit[] = s.split(";");
			this.weights.put(sSplit[0], Double.parseDouble(sSplit[1]));
		}
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

	String display()
	{
		String outputString = "";
		for (int i = 0; i < this.nodeDepth; i++)
		{
			outputString += "|  ";
		}
		outputString += this.classCountsInNode.entrySet().toString();
		outputString += "\n";
		return outputString;
	}

	List<List<Integer>> getProximities(ProcessDataForGrowing processedData, List<Integer> observationIndices)
	{
		List<List<Integer>> proximities = new ArrayList<List<Integer>>();
		proximities.add(observationIndices);
		return proximities;
	}

	List<List<Integer>> getConditionalGrid(ProcessDataForGrowing processedData, List<List<Integer>> currentGrid, String covToTest)
	{
		return currentGrid;
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

	ImmutableTwoValues<String, Integer> save(Integer nodeID, Integer parentID)
	{
		String returnString = Integer.toString(nodeID) + "\t" + Integer.toString(parentID) + "\tTerminal\t";
		returnString += Integer.toString(this.nodeDepth);
		returnString += "\t";
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
		return new ImmutableTwoValues<String, Integer>(returnString, nodeID + 1);
	}

}
