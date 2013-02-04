package tree;

public class TestDriver
{

	public static void main(String[] args)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.numberOfTreesToGrow = 100;
		ctrl.isReplacementUsed = true;

//		ProcessDataForGrowing predData = new ProcessDataForGrowing(args[0], ctrl);
//		ConditionalInferenceTree ct = new ConditionalInferenceTree(predData, ctrl);
//		ct.display();

		Forest forest = new Forest(args[0], ctrl);
		System.out.format("The OOB error estimate is : %f\n", forest.oobErrorEstimate);

//		VariableImportance varImpCalculator = new VariableImportance();
//		Map<String, Double> varImp = varImpCalculator.conditionalVariableImportance(forest, ctrl, 0.2, 2, false);
//		System.out.println(varImp.entrySet());
	}

}
