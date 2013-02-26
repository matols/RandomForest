/**
 * 
 */
package tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Simon Bull
 *
 */
public class Forest
{

	/**
	 * A list of the trees in the forest.
	 */
	public List<CARTTree> forest = new ArrayList<CARTTree>();

	/**
	 * A list where the ith element corresponds to the ith tree. The ith elemet of the list records
	 * all observations that are oob on the ith tree.
	 */
	public List<List<Integer>> oobObservations = new ArrayList<List<Integer>>();

	/**
	 * The oob error estimate.
	 */
	public double oobErrorEstimate = 0.0;

	/**
	 * The file containing the data that the forest was grown from.
	 */
	public String dataFileGrownFrom = "";

	/**
	 * The object recording the control parameters for the forest and its trees.
	 */
	public TreeGrowthControl ctrl;

	/**
	 * 
	 */
	public ProcessDataForGrowing processedData;

	public Map<String, Double> weights;

	public long seed;

	public List<Long> treeSeeds = new ArrayList<Long>();


	public Forest(String dataForGrowing)
	{
		this.ctrl = new TreeGrowthControl();
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, new HashMap<String, Double>());
	}

	public Forest(String dataForGrowing, TreeGrowthControl ctrl)
	{
		this.ctrl = ctrl;
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, new HashMap<String, Double>());
	}

	public Forest(String dataForGrowing, Map<String, Double> weights)
	{
		this.ctrl = new TreeGrowthControl();
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, weights);
	}

	public Forest(String dataForGrowing, TreeGrowthControl ctrl, Map<String, Double> weights)
	{
		this.ctrl = ctrl;
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, weights);
	}

	void growForest(String dataForGrowing, Map<String, Double> potentialWeights)
	{
		// Determine whether the tree is being regrown from known seeds, or a new forest is being grown.
		boolean isNewGrowth = false;
		if (this.treeSeeds.isEmpty())
		{
			// If no seeds are recorded, then the tree is being grown from scratch.
			isNewGrowth = true;
		}

		this.dataFileGrownFrom = dataForGrowing;
		ProcessDataForGrowing procData = new ProcessDataForGrowing(dataForGrowing, this.ctrl);
		this.processedData = procData;

		// Generate the default weightings.
		for (String s : this.processedData.responseData)
		{
			if (!potentialWeights.containsKey(s))
			{
				// Any classes without a weight are assigned a weight of 1.
				potentialWeights.put(s, 1.0);
			}
		}
		this.weights = potentialWeights;

		// Setup the observation selection variables.
		List<Integer> observations = new ArrayList<Integer>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			observations.add(i);
		}
		int numberObservationsToSelect = 0;
		if (!this.ctrl.isReplacementUsed)
		{
			numberObservationsToSelect = (int) Math.floor(this.ctrl.selectionFraction * this.processedData.numberObservations);
		}
		else
		{
			numberObservationsToSelect = this.processedData.numberObservations;
		}

		for (int i = 0; i < ctrl.numberOfTreesToGrow; i++)
		{
			// Randomly determine the observations used for growing this tree.
			List<Integer> observationsForTheTree = new ArrayList<Integer>();
			if (!ctrl.isReplacementUsed)
			{
				Collections.shuffle(observations, new Random(this.seed));
				for (int j = 0; j < numberObservationsToSelect; j++)
				{
					observationsForTheTree.add(observations.get(j));
				}
			}
			else
			{
				Random randomObservation = new Random(this.seed);
				int selectedObservation;
				for (int j = 0; j < numberObservationsToSelect; j++)
				{
					selectedObservation = randomObservation.nextInt(this.processedData.numberObservations);
					observationsForTheTree.add(observations.get(selectedObservation));
				}
			}

			// Update the list of which observations are oob on this tree.
			List<Integer> oobOnThisTree = new ArrayList<Integer>();
			for (int j = 0; j < this.processedData.numberObservations; j++)
			{
				if (!observationsForTheTree.contains(j))
				{
					// If the observation is not in the observations to use when growing the tree, then it is oob for the tree.
					oobOnThisTree.add(j);
				}
			}
			this.oobObservations.add(oobOnThisTree);

			// Grow this tree from the chosen observations.
			if (isNewGrowth)
			{
				long seedForTree = System.currentTimeMillis();
				this.treeSeeds.add(seedForTree);
				this.forest.add(new CARTTree(this.processedData, this.ctrl, weights, observationsForTheTree, seedForTree));
			}
			else
			{
				this.forest.add(new CARTTree(this.processedData, this.ctrl, weights, observationsForTheTree, this.treeSeeds.get(i)));
			}
		}

		// Calculate the oob error. This is done by putting each observation down the trees where it is oob.
		double cumulativeErrorRate = 0.0;
		int numberOobObservations = 0;
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			boolean isIOob = false;
			List<Integer> obsToPredict = new ArrayList<Integer>();
			obsToPredict.add(i);
			// Gather the trees or which observation i is oob.
			List<Integer> treesToPredictFrom = new ArrayList<Integer>();
			for (int j = 0; j < this.ctrl.numberOfTreesToGrow; j++)
			{
				if (this.oobObservations.get(j).contains(i))
				{
					// If the jth tree contains the ith observation as an oob observation.
					treesToPredictFrom.add(j);
					isIOob = true;
				}
			}
			if (isIOob)
			{
				numberOobObservations += 1;
				cumulativeErrorRate += predict(this.processedData, obsToPredict, treesToPredictFrom);
			}			
		}
		this.oobErrorEstimate = cumulativeErrorRate / numberOobObservations;
	}

	public double predict(ProcessDataForGrowing predData)
	{
		List<Integer> observationsToPredict = new ArrayList<Integer>();
		for (int i = 0; i < predData.numberObservations; i++)
		{
			observationsToPredict.add(i);
		}
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predict(predData, observationsToPredict, treesToUseForPrediction);
	}

	public double predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predict(predData, observationsToPredict, treesToUseForPrediction);
	}

	public double predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict, List<Integer> treesToUseForPrediction)
	{
		double errorRate = 0.0;
		Map<Integer, String> observationToClassification = new HashMap<Integer, String>();

		// Set up the mapping from observation index to predictions.
		// One key for each observation being predicted. The list of objects contains one entry for each tree the
		// observation is being predicted on.
		Map<Integer, List<ImmutableTwoValues<String, Double>>> predictions = new HashMap<Integer, List<ImmutableTwoValues<String, Double>>>();
		for (int i : observationsToPredict)
		{
			predictions.put(i, new ArrayList<ImmutableTwoValues<String, Double>>());
		}

		// Get the raw predictions for each tree.
		for (int i : treesToUseForPrediction)
		{
			Map<Integer, ImmutableTwoValues<String, Double>> predictedValues = forest.get(i).predict(predData, observationsToPredict);
			for (int j : observationsToPredict)
			{
				predictions.get(j).add(predictedValues.get(j));
			}
		}

		// Make sense of the prediction for each observation.
		for (int i : predictions.keySet())
		{
			// Get the list of predictions for observation i. The predictions are ordered so that the jth value in the list
			// is the prediction for the jth value in the list of treesToUseForPrediction.
			List<ImmutableTwoValues<String, Double>> predictedValues = predictions.get(i);
			// Mapping from a class to the number of times that the class was selected as the classification for the observation.
			Map<String, Double> predictedClasses = new HashMap<String, Double>();

			for (ImmutableTwoValues<String, Double> s : predictedValues)
			{
				String classPrediction = s.first;
				double classPredictionWeight = s.second;
				if (!predictedClasses.containsKey(classPrediction))
				{
					// If the class has not been predicted for the observation before, then set the count of the
					// number of times the class has been predicted to 1.
					predictedClasses.put(classPrediction, classPredictionWeight);
				}
				else
				{
					// If the class has been predicted for the observation before, then increment the count of the
					// number of times the class has been predicted.
					predictedClasses.put(classPrediction, predictedClasses.get(classPrediction) + classPredictionWeight);
				}
			}

			// Determine the majority classification for the observation.
			String majorityClass = "";
			double largestNumberClassifications = 0.0;
			for (String s : predictedClasses.keySet())
			{
				if (predictedClasses.get(s) > largestNumberClassifications)
				{
					majorityClass = s;
					largestNumberClassifications = predictedClasses.get(s);
				}
			}

			// Record the majority classification for the observation.
			observationToClassification.put(i, majorityClass);
		}

		// Record the error rate for all observations.
		for (int i : observationToClassification.keySet())
		{
			String predictedClass = observationToClassification.get(i);
			if (!predData.responseData.get(i).equals(predictedClass))
			{
				// If the classification is not correct.
				errorRate += 1.0;
			}
		}
		// Divide the number of observations predicted incorrectly by the total number of observations in order to get the
		// overall error rate of the set of observations provided on the set of trees provided.
		errorRate = errorRate / observationToClassification.size();

		return errorRate;
	}

	public void regrowForest()
	{
		// Regrow using old seeds.
		this.forest = new ArrayList<CARTTree>();
		this.oobObservations = new ArrayList<List<Integer>>();
		this.oobErrorEstimate = 0.0;
		this.growForest(this.dataFileGrownFrom, this.weights);
	}

	public void regrowForest(long newSeed)
	{
		// Regrow using different seeds.
		this.seed = newSeed;
		this.treeSeeds = new ArrayList<Long>();
		this.regrowForest();
	}

	public void regrowForest(TreeGrowthControl newCtrl)
	{
		// Regrow with old seeds, but a different controller.
		// This allows you to change replacement/mtry while keeping the random seeds the same.
		this.ctrl = newCtrl;
		this.regrowForest();
	}

	public void regrowForest(long newSeed, TreeGrowthControl newCtrl)
	{
		// Regrow using different seeds and a new controller.
		// This allows you to change replacement/mtry while keeping the random seeds the same.
		this.seed = newSeed;
		this.treeSeeds = new ArrayList<Long>();
		this.ctrl = newCtrl;
		this.regrowForest();
	}

}
