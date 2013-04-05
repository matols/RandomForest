/**
 * 
 */
package featureselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 5000;
		ctrl.mtry = 10;
		ctrl.isStratifiedBootstrapUsed = true;
		int gaRepetitions = 20;
		boolean isXValUsed = false;
		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.4);


//		new Controller(args, ctrl, weights, true);
//		System.exit(0);


//		new Controller(args, ctrl, gaRepetitions, isXValUsed, weights);
//		System.exit(0);

//		Forest forest = new Forest(args[0], ctrl, weights);
//		forest.save("C:\\Users\\Simon\\Documents\\PhD\\FeatureSelection\\TreeSave");
//		Forest loadForest = new Forest("C:\\Users\\Simon\\Documents\\PhD\\FeatureSelection\\TreeSave", true);
//		boolean isSeedEqual = forest.seed == loadForest.seed;
//		boolean isOobEstEqual = forest.oobErrorEstimate == loadForest.oobErrorEstimate;
//		boolean isDataFileEqual = forest.dataFileGrownFrom.equals(loadForest.dataFileGrownFrom);
//		boolean isWeightsEqual = forest.weights.equals(loadForest.weights);
//		boolean isOobObsEqual = forest.oobObservations.equals(loadForest.oobObservations);
//		boolean isForestEqual = true;
//		for (int i = 0; i < forest.forest.size(); i++)
//		{
//			String forestDisplay = forest.forest.get(i).display();
//			String loadForestDisplay = loadForest.forest.get(i).display();
//			boolean isDisplayEqual = forestDisplay.equals(loadForestDisplay);
//			if (!isDisplayEqual)
//			{
//				isForestEqual = false;
//			}
//		}
//		System.out.println(isSeedEqual);
//		System.out.println(isOobEstEqual);
//		System.out.println(isDataFileEqual);
//		System.out.println(isWeightsEqual);
//		System.out.println(isOobObsEqual);
//		System.out.println(isForestEqual);
//		System.exit(0);
	}

}
