package randomjyrest;

import java.util.Map;
import java.util.Set;

/**
 * Implements a terminal node.
 */
public class NodeTerminal extends Node
{
	// The attributes of a terminal node.
	private String classPresent;  // The class that any observation reaching the node is predicted to be (terminal nodes are pure).
	private double weightInNode;  // The weight of the observations in the training set that reached this terminal node.

	/**
	 * Class constructor for a terminal node.
	 * 
	 * @param classPresent			The class of the observations from the training set that reached this node.
	 * @param classData				The information about the weight that each observation in the training set contributes to each class.
	 * @param inBagObservations		The observations that have reached the node.
	 */
	public NodeTerminal(String classPresent, Map<String, double[]> classData, int[] inBagObservations)
	{
		// Sum up the total weight of the observations that have reached the node.
		int numberOfObservations = inBagObservations.length;
		double[] classWeights = classData.get(classPresent);  // The weight that every observation contributes to the class classPresent.
		double totalClassWeight = 0.0;  // The total weight of all the observations.
		for (int i = 0; i < numberOfObservations; i++)
		{
			totalClassWeight += (classWeights[i] * inBagObservations[i]);
		}

		// Assign the values to the node's attributes.
		this.classPresent = classPresent;
		this.weightInNode = totalClassWeight;
	}
	
	/**
	 * Classify every observation that has reached this node.
	 * 
	 * @param datasetToPredict	The data for every observation in the entire set of data that is to be predicted (not just the data
	 * 							for the observations that have reached this node).
	 * @param obsToPredict		The indices of the observations that have reached this node.
	 * @param predictions		The predictions of the observations in the entire dataset.
	 * @return					The predictions of the observations in the entire dataset that were passed in with the addition of
	 * 							the observations that reached this node added to it with a classification of this.classPresent and
	 * 							a prediction weight of this.weightInNode.
	 */
	public final Map<String, double[]> predict(Map<String, double[]> datasetToPredict, Set<Integer> obsToPredict,
			Map<String, double[]> predictions)
	{
		// Add the classification of all the observation that reached this node to the prediction record.
		double[] predictionsForNodesClass = predictions.get(this.classPresent);
		for (Integer i : obsToPredict)
		{
			predictionsForNodesClass[i] += this.weightInNode;
		}
		return predictions;
	}

}
