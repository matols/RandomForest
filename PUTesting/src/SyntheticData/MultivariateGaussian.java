package syntheticdata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;


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

		double class1Variance = 1.0;
		double[] class1Means = new double[]{0.0, 0.0};
		double[][] class1Covariances = new double[][]{{class1Variance, 0.0}, {0.0, class1Variance}};
		double class2Variance = 1.0;
		double[] class2Means = new double[]{1.5, 1.5};
		double[][] class2Covariances = new double[][]{{class2Variance, 0.0}, {0.0, class2Variance}};

		int[] numberOfPositiveObservationsToGenerate = new int[]{20, 40};
		int[] numberOfNegativeObservationsToGenerate = new int[]{40, 80};
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse input.
		String outputLocation = args[0];
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

			for (int l = 0; l < repetitionsToPerform; l++)
			{
				List<List<Double>> currentPositiveDataset = positiveDatasetsGenerated.get(l);
				List<List<Double>> currentNegativeDataset = negativeDatasetsGenerated.get(l);
				String currentRepLoc = currentDatasetLoc + "/Repetition" + Integer.toString(l) + ".txt";
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
						currentRepWriter.write(currentPositiveDataset.get(m).get(0) + "\t" + currentPositiveDataset.get(m).get(1) + "\t");
						currentRepWriter.write("Positive");
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
			}
		}
	}

}
