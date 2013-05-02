/**
 * 
 */
package learn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.ImmutableThreeValues;
import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class PerformLearning
{

	String dataForLearning = null;  // The location of the file to use to determine the negative observations from.

	public PerformLearning(String dataForLearning)
	{
		this.dataForLearning = dataForLearning;
	}

	/**
	 * @return A list of the integer numbers of the observations that will appear in the final dataset,
	 * 		   and the number of observations in the original dataset.
	 */
	public ImmutableThreeValues<List<Integer>, List<Integer>, Integer> main(TreeGrowthControl ctrl)
	{
		CovarianceCalculator covCalc = new CovarianceCalculator(this.dataForLearning, ctrl);
		Learner puLearner = new Learner(dataForLearning, ctrl);
		List<Integer> negativesFound = puLearner.learnNegativeSet(10, 100);

		ImmutableThreeValues<List<Integer>, List<Integer>, Integer> returnValue = new ImmutableThreeValues<>(negativesFound,
				covCalc.clusterObservations, covCalc.processedData.numberObservations);
		return returnValue;
	}

}
