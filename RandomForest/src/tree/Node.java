/**
 * 
 */
package tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Simon Bull
 *
 */
public abstract class Node
{

	int nodeDepth;
	int numberOfObservationsInNode;
	Node[] children = new Node[2];
	Map<String, Integer> classCountsInNode = new HashMap<String, Integer>();
	Map<String, Double> weights = new HashMap<String, Double>();

	String display()
	{
		return null;
	}

	List<List<Integer>> getProximities(ProcessDataForGrowing processedData, List<Integer> observationIndices)
	{
		return null;
	}

	Map<String, Set<Double>> getSplitPoints()
	{
		return null;
	}

	ImmutableTwoValues<String, Double> predict(Map<String, Double> currentObservation)
	{
		return null;
	}

	ImmutableTwoValues<String, Integer> save(Integer nodeID, Integer parentID)
	{
		// Takes the ID of the node and its parent, and returns the next available ID.
		return null;
	}

}
