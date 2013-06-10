package comparison;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.ImmutableTwoValues;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class ComparisonController
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int[] numberOfNeighbours = new int[]{3, 5};

		int[] clusetersToGenerate = new int[]{10, 15, 20};
		int clusterRepetitionsToPerform = 5;

		double[] fractionOfPositiveObservationsToKeep = new double[]{0.5, 0.75};

		String[] variablesToIgnore = new String[]{"OGlycosylation"};  // Make sure to ignore any variables that are constant. Otherwise the standardised value of the variable will be NaN.
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse input.
		String inputFile = args[0];
		ProcessDataForGrowing processedEntireDataset = new ProcessDataForGrowing(inputFile, new TreeGrowthControl());
		String outputLocation = args[1];
		File outputLDir = new File(outputLocation);
		if (!outputLDir.exists())
		{
			boolean isDirCreated = outputLDir.mkdirs();
			if (!isDirCreated)
			{
				System.out.format("The output directory (%s) does not exist, and could not be created.\n", outputLocation);
				System.exit(0);
			}
		}

		// Write out the parameters used.
		String parameterLocation = outputLocation + "/Parameters.txt";
		try
		{
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Cluster repetitions used - " + Integer.toString(clusterRepetitionsToPerform));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Fraction of positive observations to keep - " + Arrays.toString(fractionOfPositiveObservationsToKeep));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Number of neighbours - " + Arrays.toString(numberOfNeighbours));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Number of clusters - " + Arrays.toString(clusetersToGenerate));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Split the input file so that each class is treated as positive (and all others as negative)
		SplitDataset datasetSplitter = new SplitDataset();
		Map<String, String> splitPositiveLocations = datasetSplitter.main(inputFile, outputLocation + "/Datasets", fractionOfPositiveObservationsToKeep);

		// For each class being treated as the positive class, compare the algorithms on the different fractions of the 'positive'
		// class that is being kept.
		for (String s : splitPositiveLocations.keySet())
		{
			// Determine the observations of class s in the full dataset.
			List<Integer> observationsOfClassS = new ArrayList<Integer>();
			for (int i = 0; i < processedEntireDataset.numberObservations; i++)
			{
				if (processedEntireDataset.responseData.get(i).equals(s))
				{
					observationsOfClassS.add(i);
				}
			}
			int numberOfObsOfClassS = observationsOfClassS.size();
			int numberOfObsOfNotClassS = processedEntireDataset.numberObservations - numberOfObsOfClassS;

			// Get all the datasets in the directory.
			String datasetDirLoc = splitPositiveLocations.get(s);
			File datasetDir = new File(datasetDirLoc);
			File[] datasetsInDir = datasetDir.listFiles();

			// Create the results files for the subsamples of this dataset.
			String kNNNoRFResultsLoc = datasetDirLoc + "/Results-" + "kNNNoRF.txt";
			String kNNRFResultsLoc = datasetDirLoc + "/Results-" + "kNNRF.txt";
			String kMeansNoRFResultsLoc = datasetDirLoc + "/Results-" + "kMeansNoRF.txt";
			String kMeansRFResultsLoc = datasetDirLoc + "/Results-" + "kMeansRF.txt";
			try
			{
				FileWriter resultsFile = new FileWriter(kNNNoRFResultsLoc);
				BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
				resultsWriter.write("PositiveFraction\tNumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tTruePosWeight\tFalsePosWeight\tTrueNegWeight\tFalseNegWeight");
				resultsWriter.newLine();
				resultsWriter.close();
	
				resultsFile = new FileWriter(kNNRFResultsLoc);
				resultsWriter = new BufferedWriter(resultsFile);
				resultsWriter.write("PositiveFraction\tNumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tTruePosWeight\tFalsePosWeight\tTrueNegWeight\tFalseNegWeight");
				resultsWriter.newLine();
				resultsWriter.close();
	
				resultsFile = new FileWriter(kMeansNoRFResultsLoc);
				resultsWriter = new BufferedWriter(resultsFile);
				resultsWriter.write("PositiveFraction\tNumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tTruePosWeight\tFalsePosWeight\tTrueNegWeight\tFalseNegWeight");
				resultsWriter.newLine();
				resultsWriter.close();
	
				resultsFile = new FileWriter(kMeansRFResultsLoc);
				resultsWriter = new BufferedWriter(resultsFile);
				resultsWriter.write("PositiveFraction\tNumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tTruePosWeight\tFalsePosWeight\tTrueNegWeight\tFalseNegWeight");
				resultsWriter.newLine();
				resultsWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}

			// Run the algorithms for each subsampled dataset.
			for (File f : datasetsInDir)
			{
				ProcessDataForGrowing processedSampledDataset = new ProcessDataForGrowing(f.getAbsolutePath(), new TreeGrowthControl());

				String positiveFraction = f.getName().split("-")[1].substring(0, 3);

				// Determine the number of positive observations in this sample.
				int numberOfPositivesInDataset = 0;
				for (int i = 0; i < processedSampledDataset.numberObservations; i++)
				{
					if (processedSampledDataset.responseData.get(i).equals("Positive"))
					{
						numberOfPositivesInDataset++;
					}
				}

				CompareAlgorithms comparer = new CompareAlgorithms();

				// Generate the results for the kNN methods.
				for (Integer k : numberOfNeighbours)
				{
					ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> resultsNoRN = comparer.runKNN(f.getAbsolutePath(), k, false, variablesToIgnore);
					Set<Integer> positivesFoundNoRN = resultsNoRN.first;
					Map<Integer, Double> weightsNoRN = resultsNoRN.second;
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
						if (observationsOfClassS.contains(i))
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

					ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> resultsRN = comparer.runKNN(f.getAbsolutePath(), k, true, variablesToIgnore);
					Set<Integer> positivesFoundRN = resultsRN.first;
					Map<Integer, Double> weightsRN = resultsRN.second;
					int truePositivesRN = 0;
					int falsePositivesRN = 0;
					int trueNegativesRN = 0;
					int falseNegativesRN = 0;
					double truePositiveWeightRN = 0.0;
					double falsePositiveWeightRN = 0.0;
					double trueNegativeWeightRN = 0.0;
					double falseNegativeWeightRN = 0.0;
					for (Integer i : weightsRN.keySet())
					{
						if (observationsOfClassS.contains(i))
						{
							if (positivesFoundRN.contains(i))
							{
								truePositivesRN++;
								truePositiveWeightRN += weightsRN.get(i);
							}
							else
							{
								falseNegativesRN++;
								falseNegativeWeightRN += weightsRN.get(i);
							}
						}
						else
						{
							if (positivesFoundRN.contains(i))
							{
								falsePositivesRN++;
								falsePositiveWeightRN += weightsRN.get(i);
							}
							else
							{
								trueNegativesRN++;
								trueNegativeWeightRN += weightsRN.get(i);
							}
						}
					}

					try
					{
						FileWriter resultsFile = new FileWriter(kNNNoRFResultsLoc, true);
						BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(positiveFraction);
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfNotClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
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

						resultsFile = new FileWriter(kNNRFResultsLoc, true);
						resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(positiveFraction);
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfNotClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(truePositivesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(falsePositivesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(trueNegativesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(falseNegativesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(truePositiveWeightRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(falsePositiveWeightRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(trueNegativeWeightRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(falseNegativeWeightRN));
						resultsWriter.newLine();
						resultsWriter.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						System.exit(0);
					}
				}

				// Generate the results for the k means methods.
				for (Integer k : clusetersToGenerate)
				{
					ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> resultsNoRN = comparer.runKMeans(f.getAbsolutePath(), k, clusterRepetitionsToPerform, false, variablesToIgnore);
					Set<Integer> positivesFoundNoRN = resultsNoRN.first;
					Map<Integer, Double> weightsNoRN = resultsNoRN.second;
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
						if (observationsOfClassS.contains(i))
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

					ImmutableTwoValues<Set<Integer>, Map<Integer, Double>> resultsRN = comparer.runKMeans(f.getAbsolutePath(), k, clusterRepetitionsToPerform, true, variablesToIgnore);
					Set<Integer> positivesFoundRN = resultsRN.first;
					Map<Integer, Double> weightsRN = resultsRN.second;
					int truePositivesRN = 0;
					int falsePositivesRN = 0;
					int trueNegativesRN = 0;
					int falseNegativesRN = 0;
					double truePositiveWeightRN = 0.0;
					double falsePositiveWeightRN = 0.0;
					double trueNegativeWeightRN = 0.0;
					double falseNegativeWeightRN = 0.0;
					for (Integer i : weightsRN.keySet())
					{
						if (observationsOfClassS.contains(i))
						{
							if (positivesFoundRN.contains(i))
							{
								truePositivesRN++;
								truePositiveWeightRN += weightsRN.get(i);
							}
							else
							{
								falseNegativesRN++;
								falseNegativeWeightRN += weightsRN.get(i);
							}
						}
						else
						{
							if (positivesFoundRN.contains(i))
							{
								falsePositivesRN++;
								falsePositiveWeightRN += weightsRN.get(i);
							}
							else
							{
								trueNegativesRN++;
								trueNegativeWeightRN += weightsRN.get(i);
							}
						}
					}

					try
					{
						FileWriter resultsFile = new FileWriter(kMeansNoRFResultsLoc, true);
						BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(positiveFraction);
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfNotClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
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

						resultsFile = new FileWriter(kMeansRFResultsLoc, true);
						resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(positiveFraction);
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfObsOfNotClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(truePositivesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(falsePositivesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(trueNegativesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(falseNegativesRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(truePositiveWeightRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(falsePositiveWeightRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(trueNegativeWeightRN));
						resultsWriter.write("\t");
						resultsWriter.write(Double.toString(falseNegativeWeightRN));
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
