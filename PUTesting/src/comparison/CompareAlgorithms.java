package comparison;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.ImmutableTwoValues;
import algorithms.KMeansPULearning;
import algorithms.KMeansWithNegative;
import algorithms.KNNPULearning;

public class CompareAlgorithms
{

	public void runKNN(String inputFile, int[] numberOfNeighbours, boolean isReliableNegativeGenerated,
			String[] variablesToIgnore, List<Integer> observationsInPositiveClass, String outputLocation, String positiveFraction,
			int numberOfNegativeObservations, int numberOfPositivesInMaskedSet)
	{
		for (Integer k : numberOfNeighbours)
		{
			KNNPULearning kNNRunner = new KNNPULearning();
			ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> kNNResults = kNNRunner.main(inputFile, k, isReliableNegativeGenerated, variablesToIgnore);
	
			Set<Integer> positivesFoundNoRN = kNNResults.first;
			Map<Integer, Double> weightsNoRN = kNNResults.second;
			int truePositivesNoRN = 0;
			int falsePositivesNoRN = 0;
			int trueNegativesNoRN = 0;
			int falseNegativesNoRN = 0;
			double truePositiveWeightNoRN = 0.0;
			double falsePositiveWeightNoRN = 0.0;
			double trueNegativeWeightNoRN = 0.0;
			double falseNegativeWeightNoRN = 0.0;
			for (Integer i : weightsNoRN.keySet())
			{
				if (observationsInPositiveClass.contains(i))
				{
					if (positivesFoundNoRN.contains(i))
					{
						truePositivesNoRN++;
						truePositiveWeightNoRN += weightsNoRN.get(i);
					}
					else
					{
						falseNegativesNoRN++;
						falseNegativeWeightNoRN += weightsNoRN.get(i);
					}
				}
				else
				{
					if (positivesFoundNoRN.contains(i))
					{
						falsePositivesNoRN++;
						falsePositiveWeightNoRN += weightsNoRN.get(i);
					}
					else
					{
						trueNegativesNoRN++;
						trueNegativeWeightNoRN += weightsNoRN.get(i);
					}
				}
			}
	
			try
			{
				FileWriter resultsFile = new FileWriter(outputLocation, true);
				BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
				resultsWriter.write(positiveFraction);
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(observationsInPositiveClass.size()));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(numberOfNegativeObservations));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(k));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(numberOfPositivesInMaskedSet));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(truePositivesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(falsePositivesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(trueNegativesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(falseNegativesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(truePositiveWeightNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(falsePositiveWeightNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(trueNegativeWeightNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(falseNegativeWeightNoRN));
				resultsWriter.newLine();
				resultsWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public void runKMeans(String inputFile, int[] numberOfMeans, int numberClusteringReps, boolean isReliableNegativeGenerated,
			String[] variablesToIgnore, List<Integer> observationsInPositiveClass, String outputLocation, String positiveFraction,
			int numberOfNegativeObservations, int numberOfPositivesInMaskedSet)
	{
		for (Integer k : numberOfMeans)
		{
			ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> kMeansResults;
			if (isReliableNegativeGenerated)
			{
				KMeansPULearning kMeansRunner = new KMeansPULearning();
				kMeansResults = kMeansRunner.main(inputFile, k, numberClusteringReps, variablesToIgnore);
			}
			else
			{
				KMeansWithNegative kMeansRunner = new KMeansWithNegative();
				kMeansResults = kMeansRunner.main(inputFile, k, numberClusteringReps, variablesToIgnore);
			}

			Set<Integer> positivesFoundNoRN = kMeansResults.first;
			Map<Integer, Double> weightsNoRN = kMeansResults.second;
			int truePositivesNoRN = 0;
			int falsePositivesNoRN = 0;
			int trueNegativesNoRN = 0;
			int falseNegativesNoRN = 0;
			double truePositiveWeightNoRN = 0.0;
			double falsePositiveWeightNoRN = 0.0;
			double trueNegativeWeightNoRN = 0.0;
			double falseNegativeWeightNoRN = 0.0;
			for (Integer i : weightsNoRN.keySet())
			{
				if (observationsInPositiveClass.contains(i))
				{
					if (positivesFoundNoRN.contains(i))
					{
						truePositivesNoRN++;
						truePositiveWeightNoRN += weightsNoRN.get(i);
					}
					else
					{
						falseNegativesNoRN++;
						falseNegativeWeightNoRN += weightsNoRN.get(i);
					}
				}
				else
				{
					if (positivesFoundNoRN.contains(i))
					{
						falsePositivesNoRN++;
						falsePositiveWeightNoRN += weightsNoRN.get(i);
					}
					else
					{
						trueNegativesNoRN++;
						trueNegativeWeightNoRN += weightsNoRN.get(i);
					}
				}
			}
	
			try
			{
				FileWriter resultsFile = new FileWriter(outputLocation, true);
				BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
				resultsWriter.write(positiveFraction);
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(observationsInPositiveClass.size()));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(numberOfNegativeObservations));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(k));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(numberOfPositivesInMaskedSet));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(truePositivesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(falsePositivesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(trueNegativesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Integer.toString(falseNegativesNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(truePositiveWeightNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(falsePositiveWeightNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(trueNegativeWeightNoRN));
				resultsWriter.write("\t");
				resultsWriter.write(Double.toString(falseNegativeWeightNoRN));
				resultsWriter.newLine();
				resultsWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}
