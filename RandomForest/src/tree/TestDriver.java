package tree;

import java.util.Map;

public class TestDriver
{

	public static void main(String[] args)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.numberOfTreesToGrow = 500;
		ctrl.isReplacementUsed = true;

		Forest forest = new Forest(args[0], ctrl);
		System.out.format("The OOB error estimate is : %f\n", forest.oobErrorEstimate);
		Map<String, Double> varImp = forest.variableImportance();
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.2);
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.2);
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.3);
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.4);
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.5);
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.6);
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.7);
		System.out.println(varImp.entrySet());
		varImp = forest.condVariableImportance(0.8);
		System.out.println(varImp.entrySet());
	}

}
