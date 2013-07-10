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
public abstract class PUNode
{

	int nodeDepth;
	PUNode[] children = new PUNode[2];

	String display()
	{
		return null;
	}

	Map<Integer, Map<String, Double>> predict(PUProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		return null;
	}

}
