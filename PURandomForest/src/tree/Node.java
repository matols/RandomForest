/**
 * 
 */
package tree;

import java.util.List;
import java.util.Map;

/**
 * @author Simon Bull
 *
 */
public abstract class Node
{

	int nodeDepth;
	Node[] children = new Node[2];

	String display()
	{
		return null;
	}

	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		return null;
	}

}
