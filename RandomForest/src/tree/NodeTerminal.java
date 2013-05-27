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
	int numberOfObservationsInNode;
	Map<String, Double> classWeightsInNode = new HashMap<String, Double>();

	NodeTerminal(Map<String, Map<String, String>> treeSkeleton, String nodeID)
	{
		String loadString = treeSkeleton.get(nodeID).get("Data");
		String split[] = loadString.split("\t");
		this.nodeDepth = Integer.parseInt(split[0]);
		this.classWeightsInNode = new HashMap<String, Double>();
		for (String s : split[1].split(","))
		{
			String sSplit[] = s.split(";");
			this.classWeightsInNode.put(sSplit[0], Double.parseDouble(sSplit[1]));
		}
		this.children[0] = null;
		this.children[1] = null;
	}

	NodeTerminal(Map<String, Double> classWeightsInNode, int currentDepth)
	{
		this.classWeightsInNode = classWeightsInNode;
		this.nodeDepth = currentDepth;
		this.children[0] = null;
		this.children[1] = null;
	}

	int countTerminalNodes()
	{
		return 1;
	}

	String display()
	{
		String outputString = "";
		for (int i = 0; i < this.nodeDepth; i++)
		{
			outputString += "|  ";
		}
		outputString += this.classWeightsInNode.entrySet().toString();
		outputString += "\n";
		return outputString;
	}

	List<List<Integer>> getProximities(ProcessDataForGrowing processedData, List<Integer> observationIndices)
	{
		List<List<Integer>> proximities = new ArrayList<List<Integer>>();
		proximities.add(observationIndices);
		return proximities;
	}

	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		Map<Integer, Map<String, Double>> predictedValues = new HashMap<Integer, Map<String, Double>>();
		for (Integer i : observationsToPredict)
		{
			Map<String, Double> weightedVotes = new HashMap<String, Double>();
			for (String className : this.classWeightsInNode.keySet())
			{
				weightedVotes.put(className, this.classWeightsInNode.get(className));
			}
			predictedValues.put(i, weightedVotes);
		}

		return predictedValues;
	}

	ImmutableTwoValues<String, Integer> save(Integer nodeID, Integer parentID)
	{
		String returnString = Integer.toString(nodeID) + "\t" + Integer.toString(parentID) + "\tTerminal\t";
		returnString += Integer.toString(this.nodeDepth);
		returnString += "\t";
		for (String s : this.classWeightsInNode.keySet())
		{
			returnString += s + ";" + Double.toString(this.classWeightsInNode.get(s)) + ",";
		}
		returnString = returnString.substring(0, returnString.length() - 1);  // Chop off the last ','.
		return new ImmutableTwoValues<String, Integer>(returnString, nodeID + 1);
	}

}
