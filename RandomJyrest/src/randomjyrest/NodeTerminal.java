/**
 * 
 */
package randomjyrest;

import java.util.Map;
import java.util.Set;

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
	
	public final Map<String, double[]> predict(Map<String, double[]> datasetToPredict, Set<Integer> obsToPredict,
			Map<String, double[]> predictions)
	{
		double[] predictionsForNodesClass = predictions.get(this.classPresent);
		for (Integer i : obsToPredict)
		{
			predictionsForNodesClass[i] += this.weightInNode;
		}
		return predictions;
	}

}
