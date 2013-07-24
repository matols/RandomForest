package randomjyrest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import utilities.ImmutableTwoValues;

public class Forest
{
	
	/**
	 * The trees that make up the forest.
	 */
	public List<Tree> forest;
	
	/**
	 * A list where the list at index i (this.oobObservations.get(i)) contains a list of the indices of the observations that
	 * are OOB on the ith tree.
	 */
	public List<Set<Integer>> oobObservations;


	public final void main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights, int numberOfProcesses,
			boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>> processedData = ProcessDataset.main(dataset, featuresToRemove, weights);
		growForest(processedData, numberOfTrees, mtry, new Random(), numberOfProcesses, isCalcualteOOB);
	}

	public final void main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights, long seed,
			int numberOfProcesses, boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>> processedData = ProcessDataset.main(dataset, featuresToRemove, weights);
		growForest(processedData, numberOfTrees, mtry, new Random(seed), numberOfProcesses, isCalcualteOOB);
	}

	public final void main(ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>> processedData, int numberOfTrees,
			int mtry, int numberOfProcesses, boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		growForest(processedData, numberOfTrees, mtry, new Random(), numberOfProcesses, isCalcualteOOB);
	}

	public final void main(ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>> processedData, int numberOfTrees,
			int mtry, long seed, int numberOfProcesses, boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		growForest(processedData, numberOfTrees, mtry, new Random(seed), numberOfProcesses, isCalcualteOOB);
	}
	

	private final void growForest(ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>> processedData, int numberOfTrees,
			int mtry, Random forestRNG, int numberOfProcesses, boolean isCalcualteOOB)
	{
		Map<String, List<Double>> processedFeatureData = processedData.first;
		Map<String, double[]> processedClassData = processedData.second;

		// Determine the classes in the dataset, and the indices of the observations from each class.
		List<String> classes = new ArrayList<String>(processedClassData.keySet());
		int numberOfObservations = processedClassData.get(classes.get(0)).length;
		Map<String, List<Integer>> observationsFromEachClass = new HashMap<String, List<Integer>>();
		for (String s : classes)
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

		// Determine observations to use. Perform a stratified bootstrap sampling to get the in bag observations.
		Map<Integer, List<Integer>> inBagObservations = new HashMap<Integer, List<Integer>>();  // A mapping from each tree index to its in bag observations.
		for (int i = 0; i < numberOfTrees; i++)
		{
			List<Integer> inBagForThisTree = new ArrayList<Integer>(numberOfObservations);
			for (String s : classes)
			{
				List<Integer> indicesOfObservationsInClass = observationsFromEachClass.get(s);
				int numberOfObservationsInClass = indicesOfObservationsInClass.size();
				for (int j = 0; j < numberOfObservationsInClass; j++)
				{
					int observationToSelect = forestRNG.nextInt(numberOfObservationsInClass);
					inBagForThisTree.add(indicesOfObservationsInClass.get(observationToSelect).intValue());
				}
			}
			inBagObservations.put(i, inBagForThisTree);
		}
		
		// Grow trees.
		final ExecutorService treeGrowthPool = Executors.newFixedThreadPool(numberOfProcesses);
		List<Future<ImmutableTwoValues<Set<Integer>, Tree>>> futureGrowers = new ArrayList<Future<ImmutableTwoValues<Set<Integer>, Tree>>>(numberOfTrees);
		for (int i = 0; i < numberOfTrees; i++)
		{
			futureGrowers.add(treeGrowthPool.submit(new TreeGrower(processedFeatureData, processedClassData, inBagObservations.get(i),
					mtry, forestRNG.nextLong())));
		}
		inBagObservations = null;  // Deallocate the reference to the in bag observations.
		
		// Get the results of growing the trees.
		this.oobObservations = new ArrayList<Set<Integer>>(numberOfTrees);
		try
		{
			for (Future<ImmutableTwoValues<Set<Integer>, Tree>> t : futureGrowers)
			{
				ImmutableTwoValues<Set<Integer>, Tree> growthReturn = t.get();
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
		
		// Make OOB predictions if required.
		if (isCalcualteOOB)
		{
			final ExecutorService treePredictionPool = Executors.newFixedThreadPool(numberOfProcesses);
			List<Future<Map<Integer, Map<String, Double>>>> futurePredictions = new ArrayList<Future<Map<Integer, Map<String, Double>>>>(numberOfTrees);
			for (int i = 0; i < numberOfTrees; i++)
			{
				//TODO change the stuff so that it predicts from the right dataset (need to take only the oob observations).
				//TODO not the processedFeatureData
				futurePredictions.add(treePredictionPool.submit(new PredictionGenerator(processedFeatureData, this.forest.get(i)))); 
			}
			
			try
			{
				for (Future<Map<Integer, Map<String, Double>>> f : futurePredictions)
				{
					//TODO combine the predictions.
					f.get();
				}
			}
			catch (ExecutionException e)
			{
				System.out.println("Error in a predictor thread.");
				e.printStackTrace();
				System.exit(0);
			}
			catch (InterruptedException e)
			{
				// Interrupted the thread, so exit the program.
				System.out.println("Predictor interruption received.");
				e.printStackTrace();
				System.exit(0);
			}
			finally
			{
				treePredictionPool.shutdown();
			}
		}
	}
	
	
	public final void predict(String datasetToPredict, List<String> featuresToRemove, double[] weights, int numberOfProcesses)
	{
		ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>> processedData = ProcessDataset.main(datasetToPredict,
				featuresToRemove, weights);
		Map<String, List<Double>> featureData = processedData.first;
		Map<String, double[]> classData = processedData.second;
		
		int numberOfTrees = this.forest.size();
		final ExecutorService treePredictionPool = Executors.newFixedThreadPool(numberOfProcesses);
		List<Future<Map<Integer, Map<String, Double>>>> futurePredictions = new ArrayList<Future<Map<Integer, Map<String, Double>>>>(numberOfTrees);
		for (Tree t : this.forest)
		{
			futurePredictions.add(treePredictionPool.submit(new PredictionGenerator(featureData, t))); 
		}
		
		try
		{
			for (Future<Map<Integer, Map<String, Double>>> f : futurePredictions)
			{
				//TODO combine the predictions.
				f.get();
			}
		}
		catch (ExecutionException e)
		{
			System.out.println("Error in a predictor thread.");
			e.printStackTrace();
			System.exit(0);
		}
		catch (InterruptedException e)
		{
			// Interrupted the thread, so exit the program.
			System.out.println("Predictor interruption received.");
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			treePredictionPool.shutdown();
		}
	}

}
