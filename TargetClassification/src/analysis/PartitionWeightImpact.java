package analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.CARTTree;
import tree.Forest;
import tree.ImmutableTwoValues;
import tree.Node;
import tree.TreeGrowthControl;

public class PartitionWeightImpact
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		Double[] weightsToUse = {1.0, 2.0, 3.0};

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 1;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.minNodeSize = 1;
		ctrl.mtry = 10;
		ctrl.isReplacementUsed = false;
		ctrl.selectionFraction = 1.0;
		ctrl.isCalculateOOB = false;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Negative", 1.0);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse input parameters.
		String inputLoc = args[0];
		String outputLoc = args[1];
		File resultsDirectory = new File(outputLoc);
		if (!resultsDirectory.exists())
		{
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The output directory could not be created.");
				System.exit(0);
			}
		}

		for (Double d : weightsToUse)
		{
			weights.put("Positive", d);
			Forest forest = new Forest(inputLoc, ctrl);
			forest.setWeightsByClass(weights);
			forest.growForest();
			CARTTree tree = forest.forest.get(0);
			String thisWeightOutputLoc = outputLoc + "/" + Double.toString(d) + ".txt";
			try
			{
				FileWriter resultsOutputFile = new FileWriter(thisWeightOutputLoc);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write(tree.getTreeAsString());
				resultsOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}
