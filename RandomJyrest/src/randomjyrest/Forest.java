package randomjyrest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

import utilities.ImmutableThreeValues;
import utilities.ImmutableTwoValues;

public class Forest
{
	
	/**
	 * The trees that make up the forest.
	 */
	private List<Tree> forest;
	
	/**
	 * A record of the classes that were used in training the tree.
	 */
	private List<String> classesInTrainingSet;
	
	/**
	 * The seed used to grow the forest.
	 */
	private long seedUsedForGrowing;

	public final Map<String, double[]> main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights, int numberOfProcesses,
			boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		Random seedGenerator = new Random();
		this.seedUsedForGrowing = seedGenerator.nextLong();
		return growForest(dataset, featuresToRemove, weights, numberOfTrees, mtry, numberOfProcesses, isCalcualteOOB);
	}

	public final Map<String, double[]> main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights, long seed,
			int numberOfProcesses, boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		this.seedUsedForGrowing = seed;
		return growForest(dataset, featuresToRemove, weights, numberOfTrees, mtry, numberOfProcesses, isCalcualteOOB);
	}


	public final Map<String, double[]> growForest(String dataset, List<String> featuresToRemove, double[] weights, int numberOfTrees,
			int mtry, int numberOfProcesses, boolean isCalcualteOOB)
	{
		// Initialise the random number generator used to grow the forest.
		Random forestRNG = new Random(this.seedUsedForGrowing);
		
		List<Set<Integer>> oobObservations = new ArrayList<Set<Integer>>(numberOfTrees);
		int numberOfObservations = 0;

		{	
			ImmutableThreeValues<Map<String, double[]>, Map<String, int[]>, Map<String, double[]>> processedData =
					ProcessDataset.main(dataset, featuresToRemove, weights);
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
			final ExecutorService treeGrowthPool = Executors.newFixedThreadPool(numberOfProcesses);
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
					oobObservations.add(growthReturn.first);
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
			//TODO remove prediction timing
//			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		    Date startTime = new Date();
//		    String strDate = sdfDate.format(startTime);
//		    System.out.format("Start predicting at %s.\n", strDate);
			//TODO

			// Generate the entire set of prediction data.
			Map<String, double[]> datasetToPredict = ProcessPredictionData.main(dataset, featuresToRemove);

			// Setup the prediction output.
			for (String s : this.classesInTrainingSet)
			{
				predictions.put(s, new double[numberOfObservations]);
			}

			for (int i = 0; i < numberOfTrees; i++)
			{
				Tree treeToPredictOn = this.forest.get(i);
				predictions = treeToPredictOn.predict(datasetToPredict, oobObservations.get(i), predictions); 
			}
			
			for (String s : this.classesInTrainingSet)
			{
				System.out.println(Arrays.toString(predictions.get(s)));
			}
			
			//TODO remove prediction timing
//			sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		    startTime = new Date();
//		    strDate = sdfDate.format(startTime);
//		    System.out.format("End Predicting at %s.\n", strDate);
			//TODO
		}
		
		return predictions;
	}
	
	
	public final long getSeed()
	{
		return this.seedUsedForGrowing;
	}
	
	
	public final Map<String, double[]> predict(String dataset, List<String> featuresToRemove, double[] weights)
	{
		Map<String, double[]> datasetToPredict = ProcessPredictionData.main(dataset, featuresToRemove);
		
		// Determine the observations being predicted.
		Set<Integer> observationsToPredict = new HashSet<Integer>();
		int numberOfObservations = 0;
		for (int i = 0; i < datasetToPredict.get(this.classesInTrainingSet.get(0)).length; i++)
		{
			observationsToPredict.add(numberOfObservations);
			numberOfObservations++;
		}
		
		int numberOfTrees = this.forest.size();
		

		// Setup the prediction output.
		Map<String, double[]> predictions = new HashMap<String, double[]>();
		for (String s : this.classesInTrainingSet)
		{
			predictions.put(s, new double[numberOfObservations]);
		}

		for (int i = 0; i < numberOfTrees; i++)
		{
			Tree treeToPredictOn = this.forest.get(i);
			predictions = treeToPredictOn.predict(datasetToPredict, observationsToPredict, predictions); 
		}
		
		return predictions;
	}

}
