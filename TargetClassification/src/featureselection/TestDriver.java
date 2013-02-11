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
		ctrl.isReplacementUsed = false;
		ctrl.numberOfTreesToGrow = 100;
		ctrl.mtry = 10;
		int gaRepetitions = 10;
		boolean isXValUsed = true;
		Map<String, Double> weights = new HashMap<String, Double>();
		new Controller(args, ctrl, gaRepetitions, isXValUsed, weights);
	}

}
