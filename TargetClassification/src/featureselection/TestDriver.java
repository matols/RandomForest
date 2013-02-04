/**
 * 
 */
package featureselection;

import java.util.HashMap;
import java.util.Map;

import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class TestDriver
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 100;
		int gaRepetitions = 10;
		boolean isXValUsed = false;
		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Positive", 1.0);
		new Controller(args, ctrl, gaRepetitions, isXValUsed, weights);
	}

}
