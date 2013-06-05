package SyntheticData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

import tree.ImmutableTwoValues;

public class MultivariateGaussian
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int repetitionsToPerform = 2;
		int clusterRepetitionsToPerform = 1;

		double class1Variance = 1.0;
		double[] class1Means = new double[]{0.0, 0.0};
		double[][] class1Covariances = new double[][]{{class1Variance, 0.0}, {0.0, class1Variance}};
		double class2Variance = 1.0;
		double[] class2Means = new double[]{1.5, 1.5};
		double[][] class2Covariances = new double[][]{{class2Variance, 0.0}, {0.0, class2Variance}};

		int[] numberOfPositiveObservationsToGenerate = new int[]{20, 40};
		int[] numberOfNegativeObservationsToGenerate = new int[]{40, 80};
		double[] fractionOfPositiveObservationsToKeep = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

		int[] numberOfNeighbours = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		int[] cluseterToGeneratre = new int[]{};
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse input.
		String outputLocation = args[0];

		// Sanity check on control parameters.
		boolean isErrorFound = false;
		if (numberOfPositiveObservationsToGenerate.length != numberOfNegativeObservationsToGenerate.length)
		{
			System.out.println("The number of positive set sizes must be the same as the number of negative set sizes.");
			isErrorFound = true;
		}
		if (class1Means.length != class2Means.length)
		{
			System.out.println("The mean vectors for the positive and negative observations are not the same shape.");
			isErrorFound = true;
		}
		if (class1Covariances.length != class2Covariances.length)
		{
			System.out.println("The covariance matrices for the positive and negative observations do not have the same number of rows.");
			isErrorFound = true;
		}
		else
		{
			for (int i = 0; i < class1Covariances.length; i++)
			{
				if (class1Covariances[i].length != class2Covariances[i].length)
				{
					System.out.format("Row %d of the covaraince matrices do not have the same number of columns.");
					isErrorFound = true;
				}
			}
		}
		if (isErrorFound)
		{
			System.exit(0);
		}

		// Write out the parameters used.
		String parameterLocation = outputLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Repetitions used - " + Integer.toString(repetitionsToPerform));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Cluster repetitions used - " + Integer.toString(clusterRepetitionsToPerform));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Class 1 variance - " + Double.toString(class1Variance));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Class 2 variance - " + Double.toString(class2Variance));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Positive class means - " + Arrays.toString(class1Means));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Negative class means - " + Arrays.toString(class2Means));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Positive observations in each dataset - " + Arrays.toString(numberOfPositiveObservationsToGenerate));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Negative observations in each dataset - " + Arrays.toString(numberOfNegativeObservationsToGenerate));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Fraction of positive observations to keep - " + Arrays.toString(fractionOfPositiveObservationsToKeep));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Number of neighbours - " + Arrays.toString(numberOfNeighbours));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Number of clusters - " + Arrays.toString(cluseterToGeneratre));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Setup the results files.
		String kNNNoRFResultsLoc = outputLocation + "/kNNNoRF.txt";
		String kNNRFResultsLoc = outputLocation + "/kNNRF.txt";
		String kMeansNoRFResultsLoc = outputLocation + "/kMeansNoRF.txt";
		String kMeansRFResultsLoc = outputLocation + "/kMeansRF.txt";
		try
		{
			FileWriter resultsFile = new FileWriter(kNNNoRFResultsLoc);
			BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
			resultsWriter.write("NumberPositive\tNumberNegative\tFractionPositive\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
			resultsWriter.newLine();
			resultsWriter.close();

			resultsFile = new FileWriter(kNNRFResultsLoc);
			resultsWriter = new BufferedWriter(resultsFile);
			resultsWriter.write("NumberPositive\tNumberNegative\tFractionPositive\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
			resultsWriter.newLine();
			resultsWriter.close();

			resultsFile = new FileWriter(kMeansNoRFResultsLoc);
			resultsWriter = new BufferedWriter(resultsFile);
			resultsWriter.write("NumberPositive\tNumberNegative\tFractionPositive\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
			resultsWriter.newLine();
			resultsWriter.close();

			resultsFile = new FileWriter(kMeansRFResultsLoc);
			resultsWriter = new BufferedWriter(resultsFile);
			resultsWriter.write("NumberPositive\tNumberNegative\tFractionPositive\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
			resultsWriter.newLine();
			resultsWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Find the maximum positive and negative observations in any dataset.
		int maxPosObs = 0;
		for (Integer i : numberOfPositiveObservationsToGenerate)
		{
			if (i > maxPosObs)
			{
				maxPosObs = i;
			}
		}
		int maxNegObs = 0;
		for (Integer i : numberOfNegativeObservationsToGenerate)
		{
			if (i > maxNegObs)
			{
				maxNegObs = i;
			}
		}

		// Generate the multivariate distributions.
		MultivariateNormalDistribution class1DataGenerator = new MultivariateNormalDistribution(class1Means, class1Covariances);
		MultivariateNormalDistribution class2DataGenerator = new MultivariateNormalDistribution(class2Means, class2Covariances);

		// Generate the datasets.
		List<List<List<Double>>> positiveDatasetsGenerated = new ArrayList<List<List<Double>>>();  // Contains a set of data from the positive generator for each repetition.
		List<List<List<Double>>> negativeDatasetsGenerated = new ArrayList<List<List<Double>>>();  // Contains a set of data from the negative generator for each repetition.
		for (int i = 0; i < repetitionsToPerform; i++)
		{
			List<List<Double>> positiveObservationsCreated = new ArrayList<List<Double>>();
			List<List<Double>> negativeObservationsCreated = new ArrayList<List<Double>>();
			for (int k = 0; k < maxPosObs; k++)
			{
				double[] observation = class1DataGenerator.sample();
				List<Double> observationValues = new ArrayList<Double>();
				for (double d : observation)
				{
					observationValues.add(d);
				}
				positiveObservationsCreated.add(observationValues);
			}
			for (int k = 0; k < maxNegObs; k++)
			{
				double[] observation = class2DataGenerator.sample();
				List<Double> observationValues = new ArrayList<Double>();
				for (double d : observation)
				{
					observationValues.add(d);
				}
				negativeObservationsCreated.add(observationValues);
			}
			positiveDatasetsGenerated.add(positiveObservationsCreated);
			negativeDatasetsGenerated.add(negativeObservationsCreated);
		}

		// Perform the learning for all combinations.
		for (int i = 0; i < numberOfPositiveObservationsToGenerate.length; i++)
		{
			// Write out status message.
			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date startTime = new Date();
		    String strDate = sdfDate.format(startTime);
			System.out.format("Now working on dataset with %d positive and %d negative observations at %s.\n",
					numberOfPositiveObservationsToGenerate[i], numberOfNegativeObservationsToGenerate[i], strDate);

			// Create the directory for this dataset size.
			String currentDatasetLoc = outputLocation + "/" + Integer.toString(numberOfPositiveObservationsToGenerate[i]) +
					"Pos" + Integer.toString(numberOfNegativeObservationsToGenerate[i]) + "Neg";
			File currentDatasetDir = new File(currentDatasetLoc);
			if (!currentDatasetDir.exists())
			{
				boolean isDirCreated = currentDatasetDir.mkdirs();
				if (!isDirCreated)
				{
					System.out.format("The current dataset directory (%s) does not exist, and could not be created.\n", currentDatasetLoc);
					System.exit(0);
				}
			}

			// Create the sampled datasets.
			for (double posFrac : fractionOfPositiveObservationsToKeep)
			{
				// Write out the status message.
				sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    startTime = new Date();
			    strDate = sdfDate.format(startTime);
				System.out.format("\tNow working on positive fraction %f at %s.\n", posFrac, strDate);

				// Create the directory for this positive fraction.
				String currentFracLoc = currentDatasetLoc + "/PositiveFraction" + Double.toString(posFrac);
				File currentFracDir = new File(currentFracLoc);
				if (!currentFracDir.exists())
				{
					boolean isDirCreated = currentFracDir.mkdirs();
					if (!isDirCreated)
					{
						System.out.format("The current positive fraction directory (%s) does not exist, and could not be created.\n", currentFracLoc);
						System.exit(0);
					}
				}

				int numberOfPosObsToUse = (int) Math.floor(posFrac * numberOfPositiveObservationsToGenerate[i]);

				for (Integer k : numberOfNeighbours)
				{
					// Write out the status message.
					sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				    startTime = new Date();
				    strDate = sdfDate.format(startTime);
					System.out.format("\t\tNow working on %d nearest neighbours at %s.\n", k, strDate);

					// Setup the record of the results for this positive fraction and k.
					Map<String, Map<String, Integer>> confusionMatrixNoRN = new HashMap<String, Map<String, Integer>>();
					Map<String, Map<String, Integer>> confusionMatrixRN = new HashMap<String, Map<String, Integer>>();
					Map<String, Integer> initialCMValues = new HashMap<String, Integer>();
					initialCMValues.put("True", 0);
					initialCMValues.put("False", 0);
					confusionMatrixNoRN.put("Positive", new HashMap<String, Integer>(initialCMValues));
					confusionMatrixNoRN.put("Negative", new HashMap<String, Integer>(initialCMValues));
					confusionMatrixRN.put("Positive", new HashMap<String, Integer>(initialCMValues));
					confusionMatrixRN.put("Negative", new HashMap<String, Integer>(initialCMValues));
					Map<String, Double> weightMatrixNoRN = new HashMap<String, Double>();
					Map<String, Double> weightMatrixRN = new HashMap<String, Double>();
					weightMatrixNoRN.put("FalsePositive", 0.0);
					weightMatrixNoRN.put("FalseNegative", 0.0);
					weightMatrixRN.put("FalsePositive", 0.0);
					weightMatrixRN.put("FalseNegative", 0.0);

					for (int l = 0; l < repetitionsToPerform; l++)
					{
						List<List<Double>> currentPositiveDataset = positiveDatasetsGenerated.get(l);
						List<List<Double>> currentNegativeDataset = negativeDatasetsGenerated.get(l);
						List<Integer> positivesForThisFraction = new ArrayList<Integer>();
						String currentRepLoc = currentFracLoc + "/Repetition" + Integer.toString(l) + ".txt";
						try
						{
							FileWriter currentRepFile = new FileWriter(currentRepLoc);
							BufferedWriter currentRepWriter = new BufferedWriter(currentRepFile);
							currentRepWriter.write("A\tB\tClassification");
							currentRepWriter.newLine();
							currentRepWriter.write("n\tn\tr");
							currentRepWriter.newLine();
							currentRepWriter.write("\t\t");
							currentRepWriter.newLine();
							for (int m = 0; m < numberOfPositiveObservationsToGenerate[i]; m++)
							{
								positivesForThisFraction.add(m);
								currentRepWriter.write(currentPositiveDataset.get(m).get(0) + "\t" + currentPositiveDataset.get(m).get(1) + "\t");
								if (m < numberOfPosObsToUse)
								{
									currentRepWriter.write("Positive");
								}
								else
								{
									currentRepWriter.write("Unlabelled");
								}
								currentRepWriter.newLine();
							}
							for (int m = 0; m < numberOfNegativeObservationsToGenerate[i]; m++)
							{
								currentRepWriter.write(currentNegativeDataset.get(m).get(0) + "\t" + currentNegativeDataset.get(m).get(1) + "\t");
								currentRepWriter.write("Unlabelled");
								currentRepWriter.newLine();
							}
							currentRepWriter.close();
						}
						catch (Exception e)
						{
							e.printStackTrace();
							System.exit(0);
						}

						// Test the kNN approaches
						KNNPULearning noRNLearner = new KNNPULearning();
						ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> resultsNoRN = noRNLearner.main(currentRepLoc, k, false);
						Set<Integer> positivesFound = resultsNoRN.first;
						Map<Integer, Double> weights = resultsNoRN.second;
						for (Integer p : positivesForThisFraction)
						{
							if (!positivesFound.contains(p))
							{
								confusionMatrixNoRN.get("Negative").put("False", confusionMatrixNoRN.get("Negative").get("False") + 1);
								weightMatrixNoRN.put("FalseNegative", weightMatrixNoRN.get("FalseNegative") + weights.get(p));
							}
						}
						for (Integer p : positivesFound)
						{
							if (positivesForThisFraction.contains(p))
							{
								confusionMatrixNoRN.get("Positive").put("True", confusionMatrixNoRN.get("Positive").get("True") + 1);
							}
							else
							{
								confusionMatrixNoRN.get("Positive").put("False", confusionMatrixNoRN.get("Positive").get("False") + 1);
								weightMatrixNoRN.put("FalsePositive", weightMatrixNoRN.get("FalsePositive") + weights.get(p));
							}
						}
						confusionMatrixNoRN.get("Negative").put("True", confusionMatrixNoRN.get("Negative").get("True") + (numberOfNegativeObservationsToGenerate[i] - confusionMatrixNoRN.get("Positive").get("False")));

						KNNPULearning RNLearner = new KNNPULearning();
						ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> resultsRN = RNLearner.main(currentRepLoc, k, true);
						positivesFound = resultsRN.first;
						weights = resultsRN.second;
						for (Integer p : positivesForThisFraction)
						{
							if (!positivesFound.contains(p))
							{
								confusionMatrixRN.get("Negative").put("False", confusionMatrixRN.get("Negative").get("False") + 1);
								weightMatrixRN.put("FalseNegative", weightMatrixRN.get("FalseNegative") + weights.get(p));
							}
						}
						for (Integer p : positivesFound)
						{
							if (positivesForThisFraction.contains(p))
							{
								confusionMatrixRN.get("Positive").put("True", confusionMatrixRN.get("Positive").get("True") + 1);
							}
							else
							{
								confusionMatrixRN.get("Positive").put("False", confusionMatrixRN.get("Positive").get("False") + 1);
								weightMatrixRN.put("FalsePositive", weightMatrixRN.get("FalsePositive") + weights.get(p));
							}
						}
						confusionMatrixRN.get("Negative").put("True", confusionMatrixRN.get("Negative").get("True") + (numberOfNegativeObservationsToGenerate[i] - confusionMatrixRN.get("Positive").get("False")));
					}

					try
					{
						FileWriter resultsFile = new FileWriter(kNNNoRFResultsLoc, true);
						BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(Integer.toString(numberOfPositiveObservationsToGenerate[i]));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfNegativeObservationsToGenerate[i]));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(posFrac));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPosObsToUse));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixNoRN.get("Positive").get("True")));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixNoRN.get("Positive").get("False")));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixNoRN.get("Negative").get("True")));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixNoRN.get("Negative").get("False")));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(weightMatrixNoRN.get("FalsePositive")));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(weightMatrixNoRN.get("FalseNegative")));
						resultsWriter.newLine();
						resultsWriter.close();

						resultsFile = new FileWriter(kNNRFResultsLoc, true);
						resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(Integer.toString(numberOfPositiveObservationsToGenerate[i]));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfNegativeObservationsToGenerate[i]));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(posFrac));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPosObsToUse));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixRN.get("Positive").get("True")));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixRN.get("Positive").get("False")));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixRN.get("Negative").get("True")));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(confusionMatrixRN.get("Negative").get("False")));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(weightMatrixRN.get("FalsePositive")));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(weightMatrixRN.get("FalseNegative")));
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
	}

}
