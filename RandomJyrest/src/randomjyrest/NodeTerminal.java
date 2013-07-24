/**
 * 
 */
package randomjyrest;

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
	Map<String, Double> classWeightsInNode = new HashMap<String, Double>();

	NodeTerminal()
	{
		//TODO determine how to initialise the node.
	}
	
	Map<Integer, Map<String, Double>> predict(Map<String, List<Double>> datasetToPredict)
	{
		//TODO return the predictions
		return null;
	}

//	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
//	{
//		Map<Integer, Map<String, Double>> predictedValues = new HashMap<Integer, Map<String, Double>>();
//		for (Integer i : observationsToPredict)
//		{
//			Map<String, Double> weightedVotes = new HashMap<String, Double>();
//			for (String className : this.classWeightsInNode.keySet())
//			{
//				weightedVotes.put(className, this.classWeightsInNode.get(className));
//			}
//			predictedValues.put(i, weightedVotes);
//		}
//
//		return predictedValues;
//	}

}
