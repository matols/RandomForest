package tree;

public class TestDriver
{

	public static void main(String[] args)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.numberOfTreesToGrow = 1;
		ctrl.isReplacementUsed = true;

		Forest forest = new Forest(args[0], ctrl);
		System.out.format("The OOB error estimate is : %f\n", forest.oobErrorEstimate);
		forest.variableImportance();
//		System.out.println(forest.forest.get(0).display());
	}

}
