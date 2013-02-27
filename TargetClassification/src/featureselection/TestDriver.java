/**
 * 
 */
package featureselection;

import java.util.HashMap;
import java.util.Map;

import tree.CARTTree;
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
		boolean isSeedEqual = forest.seed == loadForest.seed;
		boolean isOobEstEqual = forest.oobErrorEstimate == loadForest.oobErrorEstimate;
		boolean isDataFileEqual = forest.dataFileGrownFrom.equals(loadForest.dataFileGrownFrom);
		boolean isWeightsEqual = forest.weights.equals(loadForest.weights);
		boolean isOobObsEqual = forest.oobObservations.equals(loadForest.oobObservations);
		boolean isForestEqual = true;
		for (int i = 0; i < forest.forest.size(); i++)
		{
			String forestDisplay = forest.forest.get(i).display();
			String loadForestDisplay = loadForest.forest.get(i).display();
			boolean isDisplayEqual = forestDisplay.equals(loadForestDisplay);
			if (!isDisplayEqual)
			{
				isForestEqual = false;
			}
		}
		System.out.println(isSeedEqual);
		System.out.println(isOobEstEqual);
		System.out.println(isDataFileEqual);
		System.out.println(isWeightsEqual);
		System.out.println(isOobObsEqual);
		System.out.println(isForestEqual);
		System.exit(0);
		new Controller(args, ctrl, gaRepetitions, isXValUsed, weights);
	}

}
