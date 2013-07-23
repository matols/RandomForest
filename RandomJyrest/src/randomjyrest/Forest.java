package randomjyrest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Forest
{
	
	/**
	 * The trees that make up the forest.
	 */
	public List<Tree> forest;


	public final void main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights, int numberOfProcesses,
			boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		Map<String, Map<String, double[]>> processedData = ProcessDataset.main(dataset, featuresToRemove, weights);
		growForest(processedData, numberOfTrees, mtry, new Random(), numberOfProcesses, isCalcualteOOB);
	}

	public final void main(String dataset, int numberOfTrees, int mtry, List<String> featuresToRemove, double[] weights, long seed,
			int numberOfProcesses, boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		Map<String, Map<String, double[]>> processedData = ProcessDataset.main(dataset, featuresToRemove, weights);
		growForest(processedData, numberOfTrees, mtry, new Random(seed), numberOfProcesses, isCalcualteOOB);
	}

	public final void main(Map<String, Map<String, double[]>> processedData, int numberOfTrees, int mtry, int numberOfProcesses,
			boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		growForest(processedData, numberOfTrees, mtry, new Random(), numberOfProcesses, isCalcualteOOB);
	}

	public final void main(Map<String, Map<String, double[]>> processedData, int numberOfTrees, int mtry, long seed, int numberOfProcesses,
			boolean isCalcualteOOB)
	{
		this.forest = new ArrayList<Tree>(numberOfTrees);
		growForest(processedData, numberOfTrees, mtry, new Random(seed), numberOfProcesses, isCalcualteOOB);
	}
	

	private final void growForest(Map<String, Map<String, double[]>> processedData, int numberOfTrees, int mtry,
			Random forestRNG, int numberOfProcesses, boolean isCalcualteOOB)
	{
		// Determine the classes in the dataset, and the indices of the observations from each class.
		List<String> classes = new ArrayList<String>(processedData.get("Classification").keySet());
		int numberOfObservations = processedData.get("Classification").get(classes.get(0)).length;
		Map<String, List<Integer>> observationsFromEachClass = new HashMap<String, List<Integer>>();
		for (String s : classes)
		{
			double[] classWeights = processedData.get("Classification").get(s);
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
		List<Future<Tree>> futureGrowers = new ArrayList<Future<Tree>>(numberOfTrees);
		for (int i = 0; i < numberOfTrees; i++)
		{
			futureGrowers.add(treeGrowthPool.submit(new TreeGrower(processedData, inBagObservations.get(i), forestRNG.nextLong()))); 
		}
		
		// Get the results of growing the trees.
		try
		{
			for (Future<Tree> t : futureGrowers)
			{
				this.forest.add(t.get());
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
			//TODO process the data into the prediction format
			final ExecutorService treePredictionPool = Executors.newFixedThreadPool(numberOfProcesses);
			List<Future<Map<Integer, Map<String, Double>>>> futurePredictions = new ArrayList<Future<Map<Integer, Map<String, Double>>>>(numberOfTrees);
			for (int i = 0; i < numberOfTrees; i++)
			{
				//TODO change the stuff so that it predicts from the right dataset (need to take only the oob observations).
				//TODO not the processedData
				futurePredictions.add(treePredictionPool.submit(new PredictionGenerator(processedData, this.forest.get(i)))); 
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
	
	
	public final void predict(String datasetToPredict, int numberOfProcesses)
	{
		//TODO process the prediction data and make a prediction
		int numberOfTrees = this.forest.size();
		final ExecutorService treePredictionPool = Executors.newFixedThreadPool(numberOfProcesses);
		List<Future<Map<Integer, Map<String, Double>>>> futurePredictions = new ArrayList<Future<Map<Integer, Map<String, Double>>>>(numberOfTrees);
		for (Tree t : this.forest)
		{
//			futurePredictions.add(treePredictionPool.submit(new PredictionGenerator(processedData, t))); 
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
