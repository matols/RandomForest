package similaritycomparison;

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
import java.util.Random;

import randomjyrest.DetermineObservationProperties;
import randomjyrest.Forest;
import randomjyrest.PredictionAnalysis;

public class WeightOptimisation
{

	public static final void main(String[] args)
	{
		String inputDir = args[0];  // The location of the datasets used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the optimisation will be written.
		
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfForestsToCreate = 100;  // The number of forests to create for each weight/mtry combination.
		int numberOfTreesPerForest = 1000;  // The number of trees to grow in each forest.
		int[] mtryToUse = {10};  // The different values of mtry to test.
		
		// Specify the features in the input dataset that should be ignored.
		String[] unwantedFeatures = new String[]{"UPAccession"};
		List<String> featuresToRemove = Arrays.asList(unwantedFeatures);
		
		int numberOfThreads = 3;  // The number of threads to use when growing the trees.
		
		double[] positiveWeightsToTest = new double[]{0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9,
				2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.0};
		double[] unlabelledWeightsToTest = new double[]{1.0};
		
		boolean isCalculateOOB = true;  // OOB error is being calculated.
		
		String[] cutoffsToUse = new String[]{"20", "30", "40", "50", "60", "70", "80", "90", "100"};
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		
		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
		if (!resultsDirectory.exists())
		{
			// The results directory does not exist.
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The results directory does not exist, but could not be created.");
				System.exit(0);
			}
		}
		else
		{
			// The results directory already exists.
			System.out.println("The results directory already exists. Please remove/rename the file before retrying");
			System.exit(0);
		}
		
		// Initialise the results, parameters and controller object record files.
		String parameterLocation = resultsDir + "/Parameters.txt";
		try
		{
			// Record the parameters.
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Number of forests grown - " + Integer.toString(numberOfForestsToCreate));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Number of trees in each forest - " + Integer.toString(numberOfTreesPerForest));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used");
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("\tPositive - " + Arrays.toString(positiveWeightsToTest));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("\tUnlabelled - " + Arrays.toString(unlabelledWeightsToTest));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Mtry used - " + Arrays.toString(mtryToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
		// Generate all the random seeds to use in growing the forests. The same numberOfForestsToCreate seeds will be used for every weight/mtry
		// combination. This ensures that the only difference in the results is due to the chosen weight/mtry combination.
		Random randGen = new Random();
		List<Long> seeds = new ArrayList<Long>();
		for (int i = 0; i < numberOfForestsToCreate; i++)
		{
			long seedToUse = randGen.nextLong();
			while (seeds.contains(seedToUse))
			{
				seedToUse = randGen.nextLong();
			}
			seeds.add(seedToUse);
		}
		
		for (String c : cutoffsToUse)
		{
			System.out.format("Now working on cutoff - %s ", c);
			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date currentTime = new Date();
		    String strDate = sdfDate.format(currentTime);
		    System.out.format("at %s.\n", strDate);
		    
		    // Set up the files needed for the input and output for this cutoff.
			String inputFile = inputDir + "/NonRedundant-" + c + ".txt";
			String resultsLocation = resultsDir + "/Results-" + c + ".txt";
			try
			{
				// Setup the results file.
				FileWriter resultsOutputFile = new FileWriter(resultsLocation);
				BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
				resultsOutputWriter.write("PositiveWeight\tUnlabelledWeight\tMtry\tGMean\tMCC\tF0.5\tF1\tF2\tAccuracy\tError\tTimeTakenPerRepetition(ms)\tPositives\t\tUnlabelleds\t");
				resultsOutputWriter.newLine();
				resultsOutputWriter.write("\t\t\t\t\t\t\t\t\t\t\tTrue\tFalse\tTrue\tFalse");
				resultsOutputWriter.newLine();
				resultsOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}

			// Determine the class of each observation and the number of each class.
			List<String> classOfObservations = DetermineObservationProperties.determineObservationClasses(inputFile);
			
			for (int mtry : mtryToUse)
			{
				System.out.format("\tNow working on mtry - %d ", mtry);
				sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    currentTime = new Date();
			    strDate = sdfDate.format(currentTime);
			    System.out.format("at %s.\n", strDate);
				
				for (double pWeight : positiveWeightsToTest)
				{
					
					for (double uWeight : unlabelledWeightsToTest)
					{
						System.out.format("\t\tNow working on positive weight - %f and unlabelled weight - %f ", pWeight, uWeight);
						sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					    currentTime = new Date();
					    strDate = sdfDate.format(currentTime);
					    System.out.format("at %s.\n", strDate);
						
						// Determine the weight vector for this positive/unlabelled weight combination.
						double[] weights = determineObservationWeights(classOfObservations, "Positive", pWeight, "Unlabelled", uWeight);
						
						// Setup the aggregate confusion matrix.
						Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
						Map<String, Double> emptyConfMat = new HashMap<String, Double>();
						emptyConfMat.put("Correct", 0.0);
						emptyConfMat.put("Incorrect", 0.0);
						confusionMatrix.put("Positive", new HashMap<String, Double>(emptyConfMat));
						confusionMatrix.put("Unlabelled", new HashMap<String, Double>(emptyConfMat));
						
						long timeTaken = 0l;
						for (int i = 0; i < numberOfForestsToCreate; i++)
						{
							// Grow the forest.
							Date startTime = new Date();
							Forest forest = new Forest();
							Map<String, double[]> predictionsFromForest = forest.main(inputFile, numberOfTreesPerForest, mtry, featuresToRemove,
									weights, seeds.get(i), numberOfThreads, isCalculateOOB);
							Date endTime = new Date();
							timeTaken += (endTime.getTime() - startTime.getTime());
							
							Map<String, Map<String, Double>> confMat = PredictionAnalysis.calculateConfusionMatrix(classOfObservations, predictionsFromForest);
							for (String s : confMat.keySet())
							{
								for (String p : confMat.get(s).keySet())
								{
									double oldPrediction = confusionMatrix.get(s).get(p);
									double newPrediction = confMat.get(s).get(p) + oldPrediction;
									confusionMatrix.get(s).put(p, newPrediction);
								}
							}
						}
						timeTaken /= numberOfForestsToCreate;
						
						// Record the results of this weight combination.
						try
						{
							FileWriter resultsOutputFile = new FileWriter(resultsLocation, true);
							BufferedWriter resultsOutputWriter = new BufferedWriter(resultsOutputFile);
							resultsOutputWriter.write(String.format("%.5f", pWeight));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(String.format("%.5f", uWeight));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(Integer.toString(mtry));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(String.format("%.5f", PredictionAnalysis.calculateGMean(confusionMatrix, classOfObservations)));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(String.format("%.5f", PredictionAnalysis.calculateMCC(confusionMatrix)));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(String.format("%.5f", PredictionAnalysis.calculateFMeasure(confusionMatrix, classOfObservations, 0.5)));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(String.format("%.5f", PredictionAnalysis.calculateFMeasure(confusionMatrix, classOfObservations, 1.0)));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(String.format("%.5f", PredictionAnalysis.calculateFMeasure(confusionMatrix, classOfObservations, 2.0)));
							resultsOutputWriter.write("\t");
							double accuracy = PredictionAnalysis.calculateAccuracy(confusionMatrix);
							resultsOutputWriter.write(String.format("%.5f", accuracy));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(String.format("%.5f", 1 - accuracy));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(Long.toString(timeTaken));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(Double.toString(confusionMatrix.get("Positive").get("Correct")));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(Double.toString(confusionMatrix.get("Positive").get("Incorrect")));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(Double.toString(confusionMatrix.get("Unlabelled").get("Correct")));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.write(Double.toString(confusionMatrix.get("Unlabelled").get("Incorrect")));
							resultsOutputWriter.write("\t");
							resultsOutputWriter.newLine();
							resultsOutputWriter.close();
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


	/**
	 * @param observationClasses
	 * @param positiveClass
	 * @param positiveWeight
	 * @param unlabelledClass
	 * @param unlabelledWeight
	 * @return
	 */
	private static final double[] determineObservationWeights(List<String> observationClasses, String positiveClass, double positiveWeight,
			String unlabelledClass, double unlabelledWeight)
	{
		int numberOfObservations = observationClasses.size();
		double[] weights = new double[numberOfObservations];
		
		for (int i = 0; i < numberOfObservations; i++)
		{
			String classOfObs = observationClasses.get(i);
			if (classOfObs.equals(positiveClass))
			{
				weights[i] = positiveWeight;
			}
			else
			{
				weights[i] = unlabelledWeight;
			}
		}
		
		return weights;
	}

}
