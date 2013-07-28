/**
 * 
 */
package randomjyrest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Simon Bull
 *
 */
public class NodeNonTerminal extends Node
{

	Node[] children = new Node[2];
	double splitValue;
	String featureSplitOn;


	public NodeNonTerminal(String featureSplitOn, double splitValue, Node leftChild, Node rightChild)
	{
		this.splitValue = splitValue;
		this.children[0] = leftChild;
		this.children[1] = rightChild;
		this.featureSplitOn = featureSplitOn;
	}
	
	public final Map<String, double[]> predict(Map<String, double[]> datasetToPredict, Set<Integer> obsToPredict,
			Map<String, double[]> predictions)
	{
		double[] dataForSplitFeature = datasetToPredict.get(this.featureSplitOn);

		Set<Integer> leftChildObs = new HashSet<Integer>();
		Set<Integer> rightChildObs = new HashSet<Integer>();
		for (Integer i : obsToPredict)
		{
			if (dataForSplitFeature[i] <= this.splitValue)
			{
				// Observation should go to the left hand child.
				leftChildObs.add(i);
			}
			else
			{
				// Observation should go to the right hand child.
				rightChildObs.add(i);
			}
		}
		
		// Have the child nodes make predictions.
		predictions = this.children[0].predict(datasetToPredict, leftChildObs, predictions);
		predictions = this.children[1].predict(datasetToPredict, rightChildObs, predictions);
		
		return predictions;
	}

}
