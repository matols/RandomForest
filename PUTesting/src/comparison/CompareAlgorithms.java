package comparison;

import java.util.Map;
import java.util.Set;

import tree.ImmutableTwoValues;
import algorithms.KMeansPULearning;
import algorithms.KMeansWithNegative;
import algorithms.KNNPULearning;

public class CompareAlgorithms
{

	public ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> runKNN(String inputFile, int numberNeighbours, boolean isReliableNegativeGenerated, String[] variablesToIgnore)
	{
		KNNPULearning kNNRunner = new KNNPULearning();
		ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> kNNResults = kNNRunner.main(inputFile, numberNeighbours, isReliableNegativeGenerated, variablesToIgnore);
		return kNNResults;
	}

	public ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> runKMeans(String inputFile, int numberMeans, int numberClusteringReps, boolean isReliableNegativeGenerated, String[] variablesToIgnore)
	{
		ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> kMeansResuts;
		if (isReliableNegativeGenerated)
		{
			KMeansPULearning kMeansRunner = new KMeansPULearning();
			kMeansResuts = kMeansRunner.main(inputFile, numberMeans, numberClusteringReps, variablesToIgnore);
		}
		else
		{
			KMeansWithNegative kMeansRunner = new KMeansWithNegative();
			kMeansResuts = kMeansRunner.main(inputFile, numberMeans, numberClusteringReps, variablesToIgnore);
		}
		return kMeansResuts;
	}

}
