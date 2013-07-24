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
public class NodeNonTerminal extends Node
{

	Node[] children = new Node[2];
	double splitValue;
	String covariable;


	public NodeNonTerminal(int nodeDepth, String covariable, double splitValue, Node leftChild, Node rightChild)
	{
		this.splitValue = splitValue;
		this.children[0] = leftChild;
		this.children[1] = rightChild;
		this.covariable = covariable;
	}
	
	Map<Integer, Map<String, Double>> predict(Map<String, List<Double>> datasetToPredict)
	{
		//TODO fill in prediction method
		return null;
	}

//	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
//	{
//		Map<Integer, Map<String, Double>> predictedValues = new HashMap<Integer, Map<String, Double>>();
//		if (!observationsToPredict.isEmpty())
//		{
//			List<Integer> leftChildObservations = new ArrayList<Integer>();
//			List<Integer> rightChildObservations = new ArrayList<Integer>();
//			for (Integer i : observationsToPredict)
//			{
//				double valueOfObservedCovar = predData.covariableData.get(this.covariable).get(i);
//				if (valueOfObservedCovar <= this.splitValue)
//				{
//					leftChildObservations.add(i);
//				}
//				else
//				{
//					rightChildObservations.add(i);
//				}
//			}
//			predictedValues.putAll(this.children[0].predict(predData, leftChildObservations));
//			predictedValues.putAll(this.children[1].predict(predData, rightChildObservations));
//		}
//		return predictedValues;
//	}

}
