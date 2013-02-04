/**
 * 
 */
package tree;

import java.util.Map;

/**
 * @author Simon Bull
 *
 */
public abstract class Node
{

	int nodeDepth;
	int numberOfObservationsInNode;
	Node[] children = new Node[2];
	Map<String, Integer> classCountsInNode;
	Map<String, Double> weights;

	void display()
	{
	}

	ImmutableTwoValues<String, Double> predict(Map<String, Double> currentObservation)
	{
		return null;
	}

	String save()
	{
		return null;
	}

}
