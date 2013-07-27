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
	private String classPresent;
	private double weightInNode;

	public NodeTerminal(Set<String> classesPresent, Map<String, double[]> classData, int[] inBagObservations)
	{
		int numberOfObservations = inBagObservations.length;
		for (String s : classesPresent)
		{
			double[] classWeights = classData.get(s);
			double totalClassWeight = 0.0;
			for (int i = 0; i < numberOfObservations; i++)
			{
				totalClassWeight += (classWeights[i] * inBagObservations[i]);
			}
			this.classPresent = s;
			this.weightInNode = totalClassWeight;
		}
	}
	
	public final Map<Integer, Map<String, Double>> predict(Map<String, double[]> datasetToPredict, Set<Integer> obsToPredict)
	{
		Map<Integer, Map<String, Double>> predictions = new HashMap<Integer, Map<String, Double>>();
		for (Integer i : obsToPredict)
		{
//			predictions.put(i, this.weightInNode);
		}
		return predictions;
	}

}
