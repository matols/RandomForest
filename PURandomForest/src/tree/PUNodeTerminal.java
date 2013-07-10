/**
 * 
 */
package tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Simon Bull
 *
 */
public class PUNodeTerminal extends PUNode
{
	int numberOfObservationsInNode;
	Map<String, Double> classWeightsInNode = new HashMap<String, Double>();

	PUNodeTerminal(Map<String, Double> classWeightsInNode, int currentDepth)
	{
		this.classWeightsInNode = classWeightsInNode;
		this.nodeDepth = currentDepth;
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
		outputString += this.classWeightsInNode.entrySet().toString();
		outputString += "\n";
		return outputString;
	}

	Map<Integer, Map<String, Double>> predict(PUProcessDataForGrowing predData, List<Integer> observationsToPredict)
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

}
