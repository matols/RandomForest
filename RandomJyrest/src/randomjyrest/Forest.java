package randomjyrest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import utilities.DetermineObservationProperties;
import utilities.ImmutableThreeValues;
import utilities.ImmutableTwoValues;

public class Forest
{
	
	/**
	 * The trees that make up the forest.
	 */
	private List<Tree> forest;
	
	/**
	 * The location of the dataset that was used to train the forest.
	 */
	private String trainingDataset;
	
	/**
	 * The features that were removed from the training dataset.
	 */
	private List<String> featuresRemoved;
	
	/**
	 * A record of the classes that were used in training the tree.
	 */
	private List<String> classesInTrainingSet;
	
	/**
	 * The seed used to grow the forest.
	 */
	private long seedUsedForGrowing;
	
	/**
	 * A sorted list of the indices of the observations that are OOB on each tree. For example, this.oobObservations.get(i) contains
	 * the indices of the set of observations that are OOB on the ith tree.
	 */
	private List<Set<Integer>> oobObservations;


	public final Map<String, double[]> main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights,
			int numberOfThreads, boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		this.trainingDataset = dataset;
		Random seedGenerator = new Random();
		this.featuresRemoved = featuresToRemove;
		this.seedUsedForGrowing = seedGenerator.nextLong();
		return growForest(weights, numberOfTrees, mtry, numberOfThreads, isCalcualteOOB);
	}

	public final Map<String, double[]> main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights,
			long seed, int numberOfThreads, boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		this.trainingDataset = dataset;
		this.featuresRemoved = featuresToRemove;
		this.seedUsedForGrowing = seed;
		return growForest(weights, numberOfTrees, mtry, numberOfThreads, isCalcualteOOB);
	}


	private final Map<String, double[]> growForest(double[] weights, int numberOfTrees, int mtry, int numberOfThreads,
			boolean isCalcualteOOB)
	{
		// Initialise the random number generator used to grow the forest.
		Random forestRNG = new Random(this.seedUsedForGrowing);
		
		this.oobObservations = new ArrayList<Set<Integer>>(numberOfTrees);
		int numberOfObservations = 0;

		{	
			ImmutableThreeValues<Map<String, double[]>, Map<String, int[]>, Map<String, double[]>> processedData =
					ProcessDataset.main(this.trainingDataset, this.featuresRemoved, weights);
			Map<String, double[]> processedFeatureData = processedData.first;
			Map<String, int[]> processedIndexData = processedData.second;
			Map<String, double[]> processedClassData = processedData.third;
	
			// Determine the classes in the dataset, and the indices of the observations from each class.
			this.classesInTrainingSet = new ArrayList<String>(processedClassData.keySet());
			numberOfObservations = processedClassData.get(this.classesInTrainingSet.get(0)).length;
			Map<String, List<Integer>> observationsFromEachClass = new HashMap<String, List<Integer>>();
			for (String s : this.classesInTrainingSet)
			{
				double[] classWeights = processedClassData.get(s);
				List<Integer> observationsInClass = new ArrayList<Integer>();
				for (int i = 0; i < numberOfObservations; i++)
				{
					if (classWeights[i] != 0.0)
					{
						observationsInClass.add(i);
					}
				}
				observationsFromEachClass.put(s, observationsInClass);
			}
			
			// Grow trees.
			final ExecutorService treeGrowthPool = Executors.newFixedThreadPool(numberOfThreads);
			List<Future<ImmutableTwoValues<Set<Integer>, Tree>>> futureGrowers = new ArrayList<Future<ImmutableTwoValues<Set<Integer>, Tree>>>(numberOfTrees);
			for (int i = 0; i < numberOfTrees; i++)
			{
				futureGrowers.add(treeGrowthPool.submit(new TreeGrower(processedFeatureData, processedIndexData, processedClassData,
						mtry, forestRNG.nextLong(), observationsFromEachClass, numberOfObservations)));
			}
			
			// Get the results of growing the trees.
			try
			{
				for (Future<ImmutableTwoValues<Set<Integer>, Tree>> t : futureGrowers)
				{
					ImmutableTwoValues<Set<Integer>, Tree> growthReturn = t.get();
					t = null;
					this.oobObservations.add(growthReturn.first);
					this.forest.add(growthReturn.second);
				}
			}
			catch (ExecutionException e)
			{
				System.out.println("Error in a grower thread.");
				e.printStackTrace();
				System.exit(0);
			}
			catch (InterruptedException e)
			{
				// Interrupted the thread, so exit the program.
				System.out.println("Grower interruption received.");
				e.printStackTrace();
				System.exit(0);
			}
			finally
			{
				treeGrowthPool.shutdown();
			}
		}
		
		// Make OOB predictions if required.
		Map<String, double[]> predictions = new HashMap<String, double[]>();
		if (isCalcualteOOB)
		{
			// Generate the entire set of prediction data.
			Map<String, double[]> datasetToPredict = ProcessPredictionData.main(this.trainingDataset, this.featuresRemoved).first;

			// Setup the prediction output.
			for (String s : this.classesInTrainingSet)
			{
				predictions.put(s, new double[numberOfObservations]);
			}

			for (int i = 0; i < numberOfTrees; i++)
			{
				Tree treeToPredictOn = this.forest.get(i);
				predictions = treeToPredictOn.predict(datasetToPredict, this.oobObservations.get(i), predictions); 
			}
		}
		
		return predictions;
	}
	
	
	public final long getSeed()
	{
		return this.seedUsedForGrowing;
	}
	
	
	public final Map<String, double[]> predict(String dataset, List<String> featuresToRemove)
	{
		ImmutableTwoValues<Map<String, double[]>, Integer> predictionData = ProcessPredictionData.main(dataset, featuresToRemove);
		Map<String, double[]> datasetToPredict = predictionData.first;
		int numberOfObservations = predictionData.second;
		
		// Determine the observations being predicted.
		Set<Integer> observationsToPredict = new HashSet<Integer>();
		for (int i = 0; i < numberOfObservations; i++)
		{
			observationsToPredict.add(i);
		}

		// Setup the prediction output.
		Map<String, double[]> predictions = new HashMap<String, double[]>();
		for (String s : this.classesInTrainingSet)
		{
			predictions.put(s, new double[numberOfObservations]);
		}

		int numberOfTrees = this.forest.size();
		for (int i = 0; i < numberOfTrees; i++)
		{
			Tree treeToPredictOn = this.forest.get(i);
			predictions = treeToPredictOn.predict(datasetToPredict, observationsToPredict, predictions); 
		}
		
		return predictions;
	}
	
	
	/**
	 * Determine the importance of each feature in the training dataset.
	 * 
	 * Calculates the importance based on the change in a quality measure (here g mean is used). For each tree in the forest the
	 * quality measure is calculated by predicting only those observations that are OOB on the tree. Then for each feature, s, in the
	 * dataset the quality measure is evaluated again on every tree using the OOB observations. The difference is that the values for
	 * the feature in the dataset are permuted. Basically the values of s for the OOB observations on a tree, t, are shuffled between
	 * each other (see PermutedData for an example of how this works). Then the OOB observations with the permuted values are
	 * predicted on t. The quality measure is calculated, and the difference between the original quality measure of the predictions
	 * of the OOB observations on t and the prediction of the permuted data is calculated. For each feature the arithmetic average
	 * of the change in quality measure over all trees is determined, and this is the importance of that feature.
	 * 
	 * @return		A mapping from the feature names to their importance.
	 */
	public final Map<String, Double> variableImportance()
	{
		
		List<Double> baseOOBQualityMeasure = new ArrayList<Double>(this.forest.size());
		List<String> classOfObservations = DetermineObservationProperties.determineObservationClasses(this.trainingDataset);
		int numberOfObservations = classOfObservations.size();
		int numberOfTrees = this.forest.size();
		
		// Generate the original prediction data.
		Map<String, double[]> datasetToPredict = ProcessPredictionData.main(this.trainingDataset, this.featuresRemoved).first;

		// Determine the base quality measure for each tree in the forest.
		{
			for (int i = 0; i < numberOfTrees; i++)
			{
				// Setup the prediction output.
				Map<String, double[]> predictions = new HashMap<String, double[]>();
				for (String s : this.classesInTrainingSet)
				{
					predictions.put(s, new double[numberOfObservations]);
				}
				
				// Generate the predictions.
				Tree treeToPredictOn = this.forest.get(i);
				predictions = treeToPredictOn.predict(datasetToPredict, this.oobObservations.get(i), predictions);
				Map<String, Map<String, Double>> confusionMatrix = PredictionAnalysis.calculateConfusionMatrix(classOfObservations,
						predictions, this.oobObservations.get(i));
				baseOOBQualityMeasure.add(PredictionAnalysis.calculateGMean(confusionMatrix, classOfObservations));
			}
		}
		
		// Determine the features in the dataset.
		List<String> featuresInDataset = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(this.trainingDataset));
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");
			String classFeatureColumnName = "Classification";

			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					// Ignore the class column.
					;
				}
				else if (!this.featuresRemoved.contains(feature))
				{
					featuresInDataset.add(feature);
				}
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while determining the features to use.");
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
				System.out.println("An error occurred while closing the input data file during the determining of the features used.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		// Determine the variable importance for each feature.
		Map<String, Double> variableImportance = new HashMap<String, Double>();
		for (String s : featuresInDataset)
		{
			// Record a copy of the non-permuted data for feature s.
			double[] dataForFeatureS = datasetToPredict.get(s);
			double[] copyOfOriginalValuesForFeatureS = new double[numberOfObservations];
			for (int i = 0; i < numberOfObservations; i++)
			{
				copyOfOriginalValuesForFeatureS[i] = dataForFeatureS[i];
			}
			
			double cumulativeQualityMeasureChange = 0.0;
			for (int i = 0; i < numberOfTrees; i++)
			{
				// Permute the data.
				double[] permutedFeatureValues = PermuteData.main(this.trainingDataset, this.oobObservations.get(i),
						this.featuresRemoved, s, copyOfOriginalValuesForFeatureS);
				datasetToPredict.put(s, permutedFeatureValues);
				
				// Setup the prediction output.
				Map<String, double[]> predictions = new HashMap<String, double[]>();
				for (String p : this.classesInTrainingSet)
				{
					predictions.put(p, new double[numberOfObservations]);
				}
				
				// Make the predictions on the permuted data.
				predictions = this.forest.get(i).predict(datasetToPredict, this.oobObservations.get(i), predictions);
				
				// Determine the change in quality caused by permuting the data.
				Map<String, Map<String, Double>> confusionMatrix = PredictionAnalysis.calculateConfusionMatrix(classOfObservations,
						predictions, this.oobObservations.get(i));
				double permutedQualityMeasure = PredictionAnalysis.calculateGMean(confusionMatrix, classOfObservations);
				cumulativeQualityMeasureChange += (baseOOBQualityMeasure.get(i) - permutedQualityMeasure);
			}
			cumulativeQualityMeasureChange /= numberOfTrees;
			variableImportance.put(s, cumulativeQualityMeasureChange);
			
			// Reset the values for feature s back to their original values.
			datasetToPredict.put(s, copyOfOriginalValuesForFeatureS);
		}
		
		return variableImportance;
	}

}
