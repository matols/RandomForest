/**
 * 
 */
package randomjyrest;

import java.util.HashMap;
import java.util.Map;

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
	
	public Map<Integer, Map<String, Double>> predict(Map<Integer, Map<String, Double>> datasetToPredict)
	{
		Map<Integer, Map<String, Double>> leftChildDataset = new HashMap<Integer, Map<String, Double>>();
		Map<Integer, Map<String, Double>> rightChildDataset = new HashMap<Integer, Map<String, Double>>();
		for (Map.Entry<Integer, Map<String, Double>> entry : datasetToPredict.entrySet())
		{
			Integer index = entry.getKey();
			Map<String, Double> data = entry.getValue();
			if (data.get(this.featureSplitOn) <= this.splitValue)
			{
				// Observation should go to the left hand child.
				leftChildDataset.put(index, data);
			}
			else
			{
				// Observation should go to the right hand child.
				rightChildDataset.put(index, data);
			}
		}
		
		// Have the child nodes make predictions.
		Map<Integer, Map<String, Double>> leftHandChildPredictions = this.children[0].predict(leftChildDataset);
		Map<Integer, Map<String, Double>> rightHandChildPredictions = this.children[1].predict(rightChildDataset);
		
		// Combine the predictions of the children.
		Map<Integer, Map<String, Double>> thisNodePredictions = new HashMap<Integer, Map<String, Double>>();
		thisNodePredictions.putAll(leftHandChildPredictions);
		thisNodePredictions.putAll(rightHandChildPredictions);
		return thisNodePredictions;
	}

}
