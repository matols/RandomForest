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

	NodeTerminal(Map<String, double[]> classData, int[] observationsInNode)
	{
		Set<String> classes = classData.keySet();
		for (String s : classes)
		{
			double classWeight = 0.0;
			double[] classWeights = classData.get(s);
			for (double d : classWeights)
			{
				classWeight += d;
			}
			this.classWeightsInNode.put(s, classWeight);
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
