/**
 * 
 */
package learn;

import java.util.List;

import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class PerformLearning
{
	public static void main(String[] args)
	{
		String dataForLearning = args[0];

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 500;
		ctrl.mtry = 10;
		ctrl.isStratifiedBootstrapUsed = true;

		CovarianceCalculator covCalc = new CovarianceCalculator(dataForLearning, ctrl);
		Learner puLearner = new Learner(dataForLearning, ctrl);
		System.out.println(puLearner.examineDistances());

		System.out.println(covCalc.clusterObservations.size());
		System.out.println();

		List<Integer> negativesFound = puLearner.learnNegativeSet(10, 100, 0);
		System.out.println(negativesFound.size());
		System.out.println();

		negativesFound = puLearner.learnNegativeSet(10, 100, 1);
		System.out.println(negativesFound.size());
		System.out.println();

		negativesFound = puLearner.learnNegativeSet(10, 100, 2);
		System.out.println(negativesFound.size());
		System.out.println();
	}

}
