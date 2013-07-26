/**
 * 
 */
package randomjyrest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Simon Bull
 *
 */
public class NodeTerminal extends Node
{
	Map<String, Double> classWeightsInNode = new HashMap<String, Double>();

	NodeTerminal(Map<String, double[]> classData, int[] inBagObservations)
	{				
		Set<String> allClasses = classData.keySet();
		
		int numberOfObservations = inBagObservations.length;
		for (String s : allClasses)
		{
			double[] classWeights = classData.get(s);
			double totalClassWeight = 0.0;
			for (int i = 0; i < numberOfObservations; i++)
			{
				totalClassWeight += (classWeights[i] * inBagObservations[i]);
			}
			this.classWeightsInNode.put(s, totalClassWeight);
		}
	}
	
	public Map<Integer, Map<String, Double>> predict(Map<Integer, Map<String, Double>> datasetToPredict)
	{
		Map<Integer, Map<String, Double>> predictions = new HashMap<Integer, Map<String, Double>>();
		for (Integer i : datasetToPredict.keySet())
		{
			predictions.put(i, this.classWeightsInNode);
		}
		return predictions;
	}

}
