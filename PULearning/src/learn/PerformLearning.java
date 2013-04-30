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
	int numberOfLearningRounds = 20;  // The number of negative sets to generate.
	double fractionObsAppearsIn = 1.0;  // The fraction of negative sets that the observation must appear in to be included in the final negative set (1.0 indicates that it must appear in all negative sets).

	public PerformLearning(String dataForLearning)
	{
		this.dataForLearning = dataForLearning;
	}

	public PerformLearning(String dataForLearning, int numberOfLearningRounds)
	{
		this.dataForLearning = dataForLearning;
		this.numberOfLearningRounds = numberOfLearningRounds;
	}

	public PerformLearning(String dataForLearning, double fractionObsAppearsIn)
	{
		this.dataForLearning = dataForLearning;
		this.fractionObsAppearsIn = fractionObsAppearsIn;
	}

	public PerformLearning(String dataForLearning, int numberOfLearningRounds, double fractionObsAppearsIn)
	{
		this.dataForLearning = dataForLearning;
		this.numberOfLearningRounds = numberOfLearningRounds;
		this.fractionObsAppearsIn = fractionObsAppearsIn;
	}

	/**
	 * @return A list of the integer numbers of the observations that will appear in the final dataset,
	 * 		   and the number of observations in the original dataset.
	 */
	public ImmutableThreeValues<List<Integer>, List<Integer>, Integer> main()
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.minCriterion = -Double.MAX_VALUE;
		ctrl.isClassificationUsed = true;
		ctrl.isProbabilisticPrediction = true;
		ctrl.numberOfTreesToGrow = 100;
		return main(ctrl);
	}

	public ImmutableThreeValues<List<Integer>, List<Integer>, Integer> main(TreeGrowthControl ctrl)
	{

		CovarianceCalculator covCalc = new CovarianceCalculator(this.dataForLearning);
		List<List<Integer>> negativesFound = new ArrayList<List<Integer>>();
		for (int i = 0; i < this.numberOfLearningRounds; i++)
		{
			System.out.format("This is PU learning round %d.\n", i);
			Learner puLearner = new Learner(dataForLearning, ctrl);
			negativesFound.add(puLearner.learnNegativeSet(10, 100));
		}

		// Determine the number of negative sets that each observation appears in.
		Map<Integer, Integer> numNegativeOccurences = new HashMap<Integer, Integer>();
		for (List<Integer> l : negativesFound)
		{
			for (Integer i : l)
			{
				if (numNegativeOccurences.containsKey(i))
				{
					numNegativeOccurences.put(i, numNegativeOccurences.get(i) + 1);
				}
				else
				{
					numNegativeOccurences.put(i, 1);
				}
			}
		}

		List<Integer> negativeDataset = new ArrayList<Integer>();
		for (Integer i : numNegativeOccurences.keySet())
		{
			if (((double) numNegativeOccurences.get(i) / this.numberOfLearningRounds) >= this.fractionObsAppearsIn)
			{
				// If the observation appears in enough negative sets, then it is in the final negative set.
				negativeDataset.add(i);
			}
		}

		ImmutableThreeValues<List<Integer>, List<Integer>, Integer> returnValue = new ImmutableThreeValues<>(negativeDataset,
				covCalc.clusterObservations, covCalc.processedData.numberObservations);
		return returnValue;

	}

}
