package analysis;

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

import randomjyrest.Forest;

public class WeightAndMtryOptimisation
{

	/**
	 * Used in the optimisation of the mtry parameter and the weights of the individual classes.
	 * 
	 * @param args		The file system locations of the files and directories used in the optimisation.
	 */
	public static final void main(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the optimisation will be written.
		main(inputFile, resultsDir);
	}

	/**
	 * @param inputFile		The location of the dataset used to grow the forests.
	 * @param resultsDir	The location where the results of the optimisation will be written.
	 */
	private static final void main(String inputFile, String resultsDir)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfForestsToCreate = 100;  // The number of forests to create for each weight/mtry combination.
		int numberOfTreesToGrow = 1000;  // The number of trees to grow in each forest.
		boolean isCalculateOOB = true;  // OOB error is being calculated.
		int[] mtryToUse = {5, 10, 15, 20, 25, 30};  // The different values of mtry to test.
		
		// Specify the features in the input dataset that should be ignored.
		String[] unusedFeatures = new String[]{"UPAccession"};
		List<String> featuresToRemove = Arrays.asList(unusedFeatures);
		
		int numberOfThreads = 4;  // The number of threads to use when growing the trees.
		
		double[] positiveWeightsToTest = new double[]{1.0};
		double[] unlabelledWeightsToTest = new double[]{1.0};
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
		String resultsLocation = resultsDir + "/Results.txt";
		String parameterLocation = resultsDir + "/Parameters.txt";
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

			// Record the parameters.
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Number of forests grown - " + Integer.toString(numberOfForestsToCreate));
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
		
		// Determine the class of each observation.
		List<String> classOfObservations = PredictionAnalysis.determineClassOfObservations(inputFile);
		int numberOfObservations = classOfObservations.size();

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

		for (int mtry : mtryToUse)
		{
			System.out.format("Now working on mtry - %d ", mtry);
			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date currentTime = new Date();
		    String strDate = sdfDate.format(currentTime);
		    System.out.format("at %s.\n", strDate);
			
			for (double pWeight : positiveWeightsToTest)
			{
				System.out.format("\tNow working on positive weight - %f ", pWeight);
				sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    currentTime = new Date();
			    strDate = sdfDate.format(currentTime);
			    System.out.format("at %s.\n", strDate);
				
				for (double uWeight : unlabelledWeightsToTest)
				{
					System.out.format("\t\tNow working on unlabelled weight - %f ", uWeight);
					sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				    currentTime = new Date();
				    strDate = sdfDate.format(currentTime);
				    System.out.format("at %s.\n", strDate);
					
					// Determine the weight vector for this positive/unlabelled weight combination.
					double[] weights = determineObservationWeights(classOfObservations, "Positive", pWeight, "Unlabelled", uWeight);
					
					// Setup the prediction output.
					Map<String, double[]> predictions = new HashMap<String, double[]>();
					predictions.put("Positive", new double[classOfObservations.size()]);
					predictions.put("Unlabelled", new double[classOfObservations.size()]);
					
					long timeTaken = 0l;
					for (int i = 0; i < numberOfForestsToCreate; i++)
					{
						// Grow the forest.
						Date startTime = new Date();
						Forest forest = new Forest();
						Map<String, double[]> predictionsFromForest = forest.main(inputFile, numberOfTreesToGrow, mtry, featuresToRemove,
								weights, seeds.get(i), numberOfThreads, isCalculateOOB);
						Date endTime = new Date();
						timeTaken += (endTime.getTime() - startTime.getTime());
						
						// Add the predictions from this forest to the aggregate results.
						for (Map.Entry<String, double[]> entry : predictionsFromForest.entrySet())
						{
							String classOfPredictions = entry.getKey();
							double[] oldPredictedWeights = predictions.get(classOfPredictions);
							double[] newPredictedWeights = entry.getValue();
							for (int j = 0; j < numberOfObservations; j++)
							{
								oldPredictedWeights[j] += newPredictedWeights[j];
							}
						}
					}
					
					// Evaluate the aggregate predictions.
					Map<String, Map<String, Integer>> confusionMatrix = PredictionAnalysis.calculateConfusionMatrix(classOfObservations, predictions);
					
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