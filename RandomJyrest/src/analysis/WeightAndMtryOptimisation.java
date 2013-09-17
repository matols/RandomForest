package analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import randomjyrest.DetermineObservationProperties;
import randomjyrest.Forest;
import randomjyrest.PredictionAnalysis;

public class WeightAndMtryOptimisation
{

	/**
	 * Performs a grid search over multiple mtry and class weight values.
	 * 
	 * Used in the optimisation of the mtry parameter and the weights of the individual classes.
	 * 
	 * @param args		The file system locations of the files and directories used in the optimisation.
	 */
	public static final void main(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset used to grow the forests.
		String resultsDir = args[1];  // The location where the results of the optimisation will be written.
		String parameterFile = args[2];  // The location where the parameters for the optimisation are recorded.
		String discountsFile = null;  // The location where the discounts for the observations are recorded.
		if (args.length > 3)
		{
			discountsFile = args[3];
		}

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfForestsToCreate = 100;  // The number of forests to create for each weight/mtry combination.
		int numberOfTreesPerForest = 1000;  // The number of trees to grow in each forest.
		int[] mtryToUse = {5, 10, 15, 20, 25, 30};  // The different values of mtry to test.
		
		// Specify the features in the input dataset that should be ignored.
		List<String> featuresToRemove = new ArrayList<String>();
		
		int numberOfThreads = 1;  // The number of threads to use when growing the trees.
		
		double[] positiveWeightsToTest = new double[]{1.0};
		double[] unlabelledWeightsToTest = new double[]{1.0};
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse the parameters.
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(parameterFile));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				
				// Enter the feature values for this observation into the mapping of the temporary processing of the data.
				String[] chunks = line.split("\t");
				if (chunks[0].equals("Forests"))
				{
					numberOfForestsToCreate = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Trees"))
				{
					numberOfTreesPerForest = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Mtry"))
				{
					String[] mtrys = chunks[1].split(",");
					mtryToUse = new int[mtrys.length];
					for (int i = 0; i < mtrys.length; i++)
					{
						mtryToUse[i] = Integer.parseInt(mtrys[i]);
					}
				}
				else if (chunks[0].equals("Features"))
				{
					String[] features = chunks[1].split(",");
					featuresToRemove = Arrays.asList(features);
				}
				else if (chunks[0].equals("Threads"))
				{
					numberOfThreads = Integer.parseInt(chunks[1]);
				}
				else if (chunks[0].equals("Weight"))
				{
					if (chunks[1].equals("Positive"))
					{
						String[] positiveWeights = chunks[2].split(",");
						positiveWeightsToTest = new double[positiveWeights.length];
						for (int i = 0; i < positiveWeights.length; i++)
						{
							positiveWeightsToTest[i] = Double.parseDouble(positiveWeights[i]);
						}
					}
					else if (chunks[1].equals("Unlabelled"))
					{
						String[] unlabelledWeights = chunks[2].split(",");
						unlabelledWeightsToTest = new double[unlabelledWeights.length];
						for (int i = 0; i < unlabelledWeights.length; i++)
						{
							unlabelledWeightsToTest[i] = Double.parseDouble(unlabelledWeights[i]);
						}
					}
				}
				else
				{
					// Got an unexpected line in the parameter file.
					System.out.println("An unexpected argument was found in the file of the parameters:");
					System.out.println(line);
					System.exit(0);
				}
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while extracting the parameters.");
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			try
			{
				if (reader != null)
				{
					reader.close();
				}
			}
			catch (IOException e)
			{
				// Caught an error while closing the file. Indicate this and exit.
				System.out.println("An error occurred while closing the parameters file.");
				e.printStackTrace();
				System.exit(0);
			}
		}

		// Mandatory control parameters for growing the forests.
		boolean isCalculateOOB = true;  // OOB error is being calculated.

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
		
		// Determine the class of each observation and the number of each class.
		List<String> classOfObservations = DetermineObservationProperties.determineObservationClasses(inputFile);
		Map<String, Integer> countsOfEachClass = new HashMap<String, Integer>();
		countsOfEachClass.put("Positive", Collections.frequency(classOfObservations, "Positive") * numberOfForestsToCreate);
		countsOfEachClass.put("Unlabelled", Collections.frequency(classOfObservations, "Unlabelled") * numberOfForestsToCreate);
		
		// Determine the vector of discounts.
		double[] discounts = new double[classOfObservations.size()];
		Arrays.fill(discounts, 1.0);
		if (discountsFile != null)
		{
			discounts = determineDiscounts(discountsFile);
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
					double[] weights = determineObservationWeights(classOfObservations, "Positive", pWeight, "Unlabelled", uWeight, discounts);
					
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
						resultsOutputWriter.write(String.format("%.5f", PredictionAnalysis.calculateGMean(confusionMatrix, countsOfEachClass)));
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
			String unlabelledClass, double unlabelledWeight, double[] discounts)
	{
		int numberOfObservations = observationClasses.size();
		double[] weights = new double[numberOfObservations];
		
		for (int i = 0; i < numberOfObservations; i++)
		{
			String classOfObs = observationClasses.get(i);
			if (classOfObs.equals(positiveClass))
			{
				weights[i] = positiveWeight * discounts[i];
			}
			else
			{
				weights[i] = unlabelledWeight * discounts[i];
			}
		}
		
		return weights;
	}

	private static final double[] determineDiscounts(String discountLocation)
	{
		List<Double> discounts = new ArrayList<Double>();

		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(discountLocation));
			String line = null;
			
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				discounts.add(Double.parseDouble(line));
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while processing the input data file.");
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			try
			{
				if (reader != null)
				{
					reader.close();
				}
			}
			catch (IOException e)
			{
				// Caught an error while closing the file. Indicate this and exit.
				System.out.println("An error occurred while closing the input data file.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		double[] observationDiscounts  = new double[discounts.size()];
		for (int i = 0; i < discounts.size(); i++)
		{
			observationDiscounts[i] = discounts.get(i).doubleValue();
		}
		
		return observationDiscounts;
	}
}