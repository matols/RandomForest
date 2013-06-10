package comparison;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
		int[] numberOfNeighbours = new int[]{1};

		int[] clusetersToGenerate = new int[]{10};
		int clusterRepetitionsToPerform = 1;

		double[] fractionOfPositiveObservationsToKeep = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
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

			// Get all the datasets in the directory.
			String datasetDirLoc = splitPositiveLocations.get(s);
			File datasetDir = new File(datasetDirLoc);
			File[] datasetsInDir = datasetDir.listFiles();
			for (File f : datasetsInDir)
			{
				String nameOfFile = f.getName().split("[.]")[0] + "." + f.getName().split("[.]")[1];
				ProcessDataForGrowing processedSampledDataset = new ProcessDataForGrowing(f.getAbsolutePath(), new TreeGrowthControl());

				// Setup the results files.
				String kNNNoRFResultsLoc = datasetDirLoc + "/" + nameOfFile + "-" + "kNNNoRF.txt";
				String kNNRFResultsLoc = datasetDirLoc + "/" + nameOfFile + "-" + "kNNRF.txt";
				String kMeansNoRFResultsLoc = datasetDirLoc + "/" + nameOfFile + "-" + "kMeansNoRF.txt";
				String kMeansRFResultsLoc = datasetDirLoc + "/" + nameOfFile + "-" + "kMeansRF.txt";
				try
				{
					FileWriter resultsFile = new FileWriter(kNNNoRFResultsLoc);
					BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
					resultsWriter.write("NumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
					resultsWriter.newLine();
					resultsWriter.close();
		
					resultsFile = new FileWriter(kNNRFResultsLoc);
					resultsWriter = new BufferedWriter(resultsFile);
					resultsWriter.write("NumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
					resultsWriter.newLine();
					resultsWriter.close();
		
					resultsFile = new FileWriter(kMeansNoRFResultsLoc);
					resultsWriter = new BufferedWriter(resultsFile);
					resultsWriter.write("NumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
					resultsWriter.newLine();
					resultsWriter.close();
		
					resultsFile = new FileWriter(kMeansRFResultsLoc);
					resultsWriter = new BufferedWriter(resultsFile);
					resultsWriter.write("NumberPositive\tNumberNegative\tk\tKnownPositives\tTruePositive\tFalsePositive\tTrueNegative\tFalseNegative\tFalsePosWeight\tFalseNegWeight");
					resultsWriter.newLine();
					resultsWriter.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.exit(0);
				}

				// Compute some statistics about the dataset.
				List<Integer> positiveObservationsInDataset = new ArrayList<Integer>();
				List<Integer> negativeObservationsInDataset = new ArrayList<Integer>();
				for (int i = 0; i < processedSampledDataset.numberObservations; i++)
				{
					if (processedSampledDataset.responseData.get(i).equals("Positive"))
					{
						positiveObservationsInDataset.add(i);
					}
					else
					{
						negativeObservationsInDataset.add(i);
					}
				}
				int numberOfPositivesInDataset = positiveObservationsInDataset.size();
				int numberOfNegativesInDataset = negativeObservationsInDataset.size();

				// Generate the results for the kNN methods.
				for (Integer k : numberOfNeighbours)
				{
					try
					{
						FileWriter resultsFile = new FileWriter(kNNNoRFResultsLoc, true);
						BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(processedEntireDataset.numberObservations - numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
						resultsWriter.newLine();
						resultsWriter.close();

						resultsFile = new FileWriter(kNNRFResultsLoc, true);
						resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(processedEntireDataset.numberObservations - numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
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
					try
					{
						FileWriter resultsFile = new FileWriter(kMeansNoRFResultsLoc, true);
						BufferedWriter resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(processedEntireDataset.numberObservations - numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
						resultsWriter.newLine();
						resultsWriter.close();

						resultsFile = new FileWriter(kMeansRFResultsLoc, true);
						resultsWriter = new BufferedWriter(resultsFile);
						resultsWriter.write(Integer.toString(numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(processedEntireDataset.numberObservations - numberOfObsOfClassS));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(k));
						resultsWriter.write("\t");
						resultsWriter.write(Integer.toString(numberOfPositivesInDataset));
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
