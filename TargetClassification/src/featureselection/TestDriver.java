/**
 * 
 */
package featureselection;

import java.util.HashMap;
import java.util.Map;

import tree.Forest;
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
		boolean isXValUsed = false;
		Map<String, Double> weights = new HashMap<String, Double>();
		Forest forest = new Forest(args[0], ctrl, weights);
		forest.save("C:\\Users\\Simonial\\Documents\\PhD\\FeatureSelection\\TreeSave");
		Forest loadForest = new Forest("C:\\Users\\Simonial\\Documents\\PhD\\FeatureSelection\\TreeSave", true);
		System.out.println(forest.equals(loadForest));
		System.exit(0);
		new Controller(args, ctrl, gaRepetitions, isXValUsed, weights);
	}

}
