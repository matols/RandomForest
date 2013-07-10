/**
 * 
 */
package tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Simon Bull
 *
 */
public class PUForest
{

	/**
	 * A list of the trees in the forest.
	 */
	public List<PUCARTTree> forest = new ArrayList<PUCARTTree>();

	/**
	 * A list where the ith element corresponds to the ith tree. The ith element of the list records
	 * all observations that are oob on the ith tree.
	 */
	public List<List<Integer>> oobObservations = new ArrayList<List<Integer>>();

	/**
	 * A mapping from each integer to a list of the trees that it is oob on.
	 */
	public Map<Integer, List<Integer>> oobOnTree = new HashMap<Integer, List<Integer>>();

	/**
	 * The oob error estimate.
	 */
	public double oobErrorEstimate = 0.0;

	/**
	 * The oob confusion matrix.
	 */
	public Map<String, Map<String, Double>> oobConfusionMatrix = new HashMap<String, Map<String, Double>>();

	/**
	 * The file containing the data that the forest was grown from.
	 */
	public String dataFileGrownFrom = "";

	/**
	 * The object recording the control parameters for the forest and its trees.
	 */
	public PUTreeGrowthControl ctrl;

	/**
	 * 
	 */
	public PUProcessDataForGrowing processedData;

	public Map<String, Map<Integer, Double>> discounts = new HashMap<String, Map<Integer, Double>>();

	public Map<String, Double> classWeights = new HashMap<String, Double>();

	public long seed;


	public PUForest(String dataForGrowing)
	{
		this.ctrl = new PUTreeGrowthControl();
		this.seed = System.currentTimeMillis();
		this.dataFileGrownFrom = dataForGrowing;
		this.processedData = new PUProcessDataForGrowing(dataForGrowing, this.ctrl);
	}

	public PUForest(String dataForGrowing, PUTreeGrowthControl ctrl)
	{
		this.ctrl = new PUTreeGrowthControl(ctrl);
		this.seed = System.currentTimeMillis();
		this.dataFileGrownFrom = dataForGrowing;
		this.processedData = new PUProcessDataForGrowing(dataForGrowing, this.ctrl);
	}

	public PUForest(String dataForGrowing, PUTreeGrowthControl ctrl, Long seed)
	{
		this.ctrl = new PUTreeGrowthControl(ctrl);
		this.seed = seed;
		this.dataFileGrownFrom = dataForGrowing;
		this.processedData = new PUProcessDataForGrowing(dataForGrowing, this.ctrl);
	}

	public PUForest(PUProcessDataForGrowing procData)
	{
		this.ctrl = new PUTreeGrowthControl();
		this.seed = System.currentTimeMillis();
		this.processedData = procData;
		this.dataFileGrownFrom = procData.dataFileGrownFrom;
	}

	public PUForest(PUProcessDataForGrowing procData, PUTreeGrowthControl ctrl)
	{
		this.ctrl = new PUTreeGrowthControl(ctrl);
		this.seed = System.currentTimeMillis();
		this.processedData = procData;
		this.dataFileGrownFrom = procData.dataFileGrownFrom;
	}

	public PUForest(PUProcessDataForGrowing procData, PUTreeGrowthControl ctrl, Long seed)
	{
		this.ctrl = new PUTreeGrowthControl(ctrl);
		this.seed = seed;
		this.processedData = procData;
		this.dataFileGrownFrom = procData.dataFileGrownFrom;
	}


	public void setClassWeights(Map<String, Double> weights)
	{
		for (String s : new HashSet<String>(this.processedData.responseData))
		{
			if (!weights.containsKey(s))
			{
				this.classWeights.put(s, 1.0);
			}
			else
			{
				this.classWeights.put(s, weights.get(s));
			}
		}
	}

	public void setPositiveDiscounts(Map<Integer, Double> discounts)
	{
		Map<Integer, Double> newWeightings = new HashMap<Integer, Double>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			if (!discounts.containsKey(i))
			{
				// If there is no weight for the observation, then set it to 1.0.
				newWeightings.put(i, 1.0);
			}
			else
			{
				newWeightings.put(i, discounts.get(i));
			}
		}

		this.discounts.put("Positive", newWeightings);
	}

	public void setUnlabelledDiscounts(Map<Integer, Double> discounts)
	{
		Map<Integer, Double> newWeightings = new HashMap<Integer, Double>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			if (!discounts.containsKey(i))
			{
				// If there is no weight for the observation, then set it to 0.0.
				newWeightings.put(i, 0.0);
			}
			else
			{
				newWeightings.put(i, discounts.get(i));
			}
		}

		this.discounts.put("Unlabelled", newWeightings);
	}


	public void growForest()
	{
		// Seed the random generator used to control all the randomness in the algorithm,
		Random randGenerator = new Random(this.seed);

		// Determine the possible classes of the observations.
		Set<String> responseClasses = new HashSet<String>(this.processedData.responseData);

		// Ensure all classes are weighted.
		setClassWeights(this.classWeights);
		setPositiveDiscounts(this.discounts.get("Positive"));
		setUnlabelledDiscounts(this.discounts.get("Unlabelled"));

		// Set up the mapping of trees that each observation is OOB on.
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			oobOnTree.put(i, new ArrayList<Integer>());
		}

		// Record the observations in each class.
		Map<String, List<Integer>> observationsInEachClass = new HashMap<String, List<Integer>>();
		for (String s : responseClasses)
		{
			observationsInEachClass.put(s, new ArrayList<Integer>());
		}
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			observationsInEachClass.get(this.processedData.responseData.get(i)).add(i);
		}

		// Determine the total discount for each class.
		double totalPositiveDiscount = 0.0;
		double totalUnlabelledDiscount = 0.0;
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			totalPositiveDiscount += this.discounts.get("Positive").get(i);
			totalUnlabelledDiscount += this.discounts.get("Unlabelled").get(i);
		}

		// Setup the observation selection variables.
		List<Integer> observations = new ArrayList<Integer>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			observations.add(i);
		}

		// Grow the trees in the forest.
		for (int i = 0; i < ctrl.numberOfTreesToGrow; i++)
		{
			// Randomly determine the observations used for growing this tree.
			List<Integer> observationsForTheTree = new ArrayList<Integer>();
			double totalPositiveDiscountSelected = 0.0;
			double totalUnlabelledDiscountSelected = 0.0;
			List<Integer> unlabelledObservations = new ArrayList<Integer>(observationsInEachClass.get("Unlabelled"));
			int numberUnlabelledObservations = unlabelledObservations.size();
			while (totalUnlabelledDiscountSelected < totalUnlabelledDiscount)
			{
				// Keep selecting unlabelled observations.
				int selectedObservation = unlabelledObservations.get(randGenerator.nextInt(numberUnlabelledObservations));
				observationsForTheTree.add(selectedObservation);
				totalPositiveDiscountSelected += this.discounts.get("Positive").get(selectedObservation);
				totalUnlabelledDiscountSelected += this.discounts.get("Unlabelled").get(selectedObservation);
			}
			List<Integer> positiveObservations = new ArrayList<Integer>(observationsInEachClass.get("Positive"));
			int numberPositiveObservations = positiveObservations.size();
			while (totalPositiveDiscountSelected < totalPositiveDiscount)
			{
				// Keep selecting positive observations.
				int selectedObservation = positiveObservations.get(randGenerator.nextInt(numberPositiveObservations));
				observationsForTheTree.add(selectedObservation);
				totalPositiveDiscountSelected += this.discounts.get("Positive").get(selectedObservation);
				totalUnlabelledDiscountSelected += this.discounts.get("Unlabelled").get(selectedObservation);
			}

			// Update the list of which observations are OOB on this tree.
			List<Integer> oobOnThisTree = new ArrayList<Integer>(observations);
			oobOnThisTree.removeAll(observationsForTheTree);
			this.oobObservations.add(oobOnThisTree);
			for (Integer j : oobOnThisTree)
			{
				this.oobOnTree.get(j).add(i);
			}

			// Grow this tree from the chosen observations.
			long seedForTree = randGenerator.nextLong();
			PUCARTTree newTree = new PUCARTTree(this.processedData, this.ctrl, seedForTree);
			newTree.growTree(this.classWeights, this.discounts, observationsForTheTree);
			this.forest.add(newTree);
		}

		// Calculate the OOB error and confusion matrix. This is done by putting each observation down the trees where it is OOB.
		double cumulativeErrorRate = 0.0;
		Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
		Set<String> responsePossibilities = new HashSet<String>(this.processedData.responseData);
		for (String s : responsePossibilities)
		{
			Map<String, Double> classEntry = new HashMap<String, Double>();
			classEntry.put("TruePositive", 0.0);
			classEntry.put("FalsePositive", 0.0);
			confusionMatrix.put(s, classEntry);
		}
		int numberOobObservations = 0;
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			boolean isIOob = !oobOnTree.get(i).isEmpty();
			List<Integer> obsToPredict = new ArrayList<Integer>();
			obsToPredict.add(i);
			if (isIOob)
			{
				numberOobObservations += 1;
				ImmutableTwoValues<Double, Map<String, Map<String, Double>>> oobPrediction = predict(this.processedData, obsToPredict, oobOnTree.get(i));
				cumulativeErrorRate += oobPrediction.first;
				for (String s : oobPrediction.second.keySet())
				{
					for (String p : oobPrediction.second.get(s).keySet())
					{
						Double oldValue = confusionMatrix.get(s).get(p);
						confusionMatrix.get(s).put(p, oldValue + oobPrediction.second.get(s).get(p));
					}
				}
			}			
		}
		this.oobErrorEstimate = cumulativeErrorRate / numberOobObservations;
		this.oobConfusionMatrix = confusionMatrix;
	}


	public ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predict(PUProcessDataForGrowing predData)
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

	public ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predict(PUProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predict(predData, observationsToPredict, treesToUseForPrediction);
	}

	public ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predict(PUProcessDataForGrowing predData, List<Integer> observationsToPredict, List<Integer> treesToUseForPrediction)
	{
		Double errorRate = 0.0;

		Set<String> classNames = new HashSet<String>(this.processedData.responseData);  // A set containing the names of all the classes in the dataset used for training.

		// Set up the mapping from observation index to predictions. The key is the index of the observation in the dataset, the Map contains
		// a mapping from each class to the weighted vote for it from the forest.
		Map<Integer, Map<String, Double>> predictions = new HashMap<Integer,Map<String, Double>>();
		Map<String, Double> possiblePredictions = new HashMap<String, Double>();
		for (String s : classNames)
		{
			possiblePredictions.put(s, 0.0);
		}
		for (int i : observationsToPredict)
		{
			predictions.put(i, new HashMap<String, Double>(possiblePredictions));
		
		}

		// Get the raw predictions for each tree.
		for (int i : treesToUseForPrediction)
		{
			Map<Integer, Map<String, Double>> predictedValues = forest.get(i).predict(predData, observationsToPredict);
			for (int j : predictedValues.keySet())
			{
				for (String s : predictedValues.get(j).keySet())
				{
					Double oldPrediction = predictions.get(j).get(s);
					Double newPrediction = predictedValues.get(j).get(s);
					predictions.get(j).put(s, oldPrediction + newPrediction);
				}
			}
		}

		// Set up the confusion matrix.
		Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
		for (String s : classNames)
		{
			Map<String, Double> classEntry = new HashMap<String, Double>();
			classEntry.put("TruePositive", 0.0);
			classEntry.put("FalsePositive", 0.0);
			confusionMatrix.put(s, classEntry);
		}

		// Make sense of the prediction for each observation.
		for (Integer i : predictions.keySet())
		{
			// Determine the majority classification for the observation.
			Map.Entry<String, Double> maxEntry = null;

			for (Map.Entry<String, Double> entry : predictions.get(i).entrySet())
			{
			    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
			    {
			        maxEntry = entry;
			    }
			}
			String predictedClass = maxEntry.getKey();

			if (!predictedClass.equals(predData.responseData.get(i)))
			{
				// If the classification is not correct.
				errorRate += 1.0; // Indicate that an incorrect prediction has been encountered.
				// Increment the number of false positives for the predicted class.
				Double currentFalsePos = confusionMatrix.get(predictedClass).get("FalsePositive");
				confusionMatrix.get(predictedClass).put("FalsePositive", currentFalsePos + 1);
			}
			else
			{
				// Increment the number of true positives for the predicted class.
				Double currentTruePos = confusionMatrix.get(predictedClass).get("TruePositive");
				confusionMatrix.get(predictedClass).put("TruePositive", currentTruePos + 1);
			}
		}

		// Divide the number of observations predicted incorrectly by the total number of observations predicted in order to get the
		// overall error rate of the set of observations provided on the set of trees provided.
		errorRate = errorRate / predictions.size();

		return new ImmutableTwoValues<Double, Map<String,Map<String,Double>>>(errorRate, confusionMatrix);
	}


	public Map<Integer, Map<String, Double>> predictRaw(PUProcessDataForGrowing predData)
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
		return predictRaw(predData, observationsToPredict, treesToUseForPrediction);
	}

	public Map<Integer, Map<String, Double>> predictRaw(PUProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predictRaw(predData, observationsToPredict, treesToUseForPrediction);
	}

	public Map<Integer, Map<String, Double>> predictRaw(PUProcessDataForGrowing predData, List<Integer> observationsToPredict, List<Integer> treesToUseForPrediction)
	{
		Set<String> classNames = new HashSet<String>(this.processedData.responseData);  // A set containing the names of all the classes in the dataset used for training.

		// Set up the mapping from observation index to predictions. The key is the index of the observation in the dataset, the Map contains
		// a mapping from each class to the weighted vote for it from the forest.
		Map<Integer, Map<String, Double>> predictions = new HashMap<Integer,Map<String, Double>>();
		Map<String, Double> possiblePredictions = new HashMap<String, Double>();
		for (String s : classNames)
		{
			possiblePredictions.put(s, 0.0);
		}
		for (int i : observationsToPredict)
		{
			predictions.put(i, new HashMap<String, Double>(possiblePredictions));
		}

		// Get the raw predictions for each tree.
		for (Integer i : treesToUseForPrediction)
		{
			Map<Integer, Map<String, Double>> predictedValues = forest.get(i).predict(predData, observationsToPredict);
			for (Integer j : predictedValues.keySet())
			{
				for (String s : predictedValues.get(j).keySet())
				{
					Double oldPrediction = predictions.get(j).get(s);
					Double newPrediction = predictedValues.get(j).get(s);
					predictions.get(j).put(s, oldPrediction + newPrediction);
				}
			}
		}

		return predictions;
	}


	/**
	 * Calculate the importance for each variable in the dataset.
	 * 
	 * The first Map returned is the variable importance calculated based on the change in accuracy of the forest.
	 * The second Map returned is the variable importance calculated based on the change in g mean of the forest.
	 * 
	 * @return
	 */
	public ImmutableTwoValues<Map<String, Double>, Map<String, Double>> variableImportance()
	{
		// Determine the counts for each class in the dataset.
		Map<String, Integer> countsOfClass = new HashMap<String, Integer>();
		for (String s : this.processedData.responseData)
		{
			countsOfClass.put(s, 0);
		}
		for (String s : this.processedData.responseData)
		{
			countsOfClass.put(s, countsOfClass.get(s) + 1);
		}
		int numberOfClasses = countsOfClass.size();

		// Determine base accuracy for each tree.
		List<Double> baseOOBAccuracy = new ArrayList<Double>();
		List<Double> baseOOBGMean = new ArrayList<Double>();
		for (int i = 0; i < this.forest.size(); i++)
		{
			List<Integer> oobOnThisTree = this.oobObservations.get(i);
			List<Integer> treesToUse = new ArrayList<Integer>();
			treesToUse.add(i);
			ImmutableTwoValues<Double, Map<String, Map<String, Double>>> originalPredictions = predict(this.processedData, oobOnThisTree, treesToUse);
			Double originalAccuracy = 1 - originalPredictions.first;
			baseOOBAccuracy.add(originalAccuracy);
			Map<String, Map<String, Double>> confusionMatrix = originalPredictions.second;
			double macroGMean = 1.0;
			for (String s : confusionMatrix.keySet())
			{
				double TP = confusionMatrix.get(s).get("TruePositive");
	    		double FN = countsOfClass.get(s) - TP;  // The number of false positives is the number of observations from the class  - the number of true positives.
	    		double recall = (TP / (TP + FN));
	    		macroGMean *= recall;
			}
			macroGMean = Math.pow(macroGMean, (1.0 / numberOfClasses));
			baseOOBGMean.add(macroGMean);
		}

		// Determine permuted importance.
		Map<String, Double> accVariableImportance = new HashMap<String, Double>();
		Map<String, Double> gMeanVariableImportance = new HashMap<String, Double>();
		for (String s : this.processedData.covariableData.keySet())
		{
			double cumulativeAccChange = 0.0;
			double cumulativeGMeanChange = 0.0;
			for (int i = 0; i < this.forest.size(); i++)
			{
				List<Integer> oobOnThisTree = this.oobObservations.get(i);
				List<Integer> permutedOobOnThisTree = new ArrayList<Integer>(this.oobObservations.get(i));
				Collections.shuffle(permutedOobOnThisTree);

				// Create the permuted copy of the data.
				PUProcessDataForGrowing permData = new PUProcessDataForGrowing(this.processedData);
				for (int j = 0; j < permutedOobOnThisTree.size(); j++)
				{
					int obsIndex = oobOnThisTree.get(j);  // Index of the observation that is being changed to a different value for the covariable s.
					int permObsIndex = permutedOobOnThisTree.get(j);  // Index of the observation that is having its value placed in the obsIndex index.
					double permValue = this.processedData.covariableData.get(s).get(permObsIndex);
					permData.covariableData.get(s).set(obsIndex, permValue);
				}

				List<Integer> treesToUse = new ArrayList<Integer>();
				treesToUse.add(i);
				ImmutableTwoValues<Double, Map<String, Map<String, Double>>> permutedPredictions = predict(permData, oobOnThisTree, treesToUse);
				Double permutedAccuracy = 1 - permutedPredictions.first;  // Determine the predictive accuracy for the permuted observations.
				cumulativeAccChange += (baseOOBAccuracy.get(i) - permutedAccuracy);
				Map<String, Map<String, Double>> confusionMatrix = permutedPredictions.second;
				double permutedMacroGMean = 1.0;
				for (String p : confusionMatrix.keySet())
				{
					double TP = confusionMatrix.get(p).get("TruePositive");
		    		double FN = countsOfClass.get(p) - TP;  // The number of false positives is the number of observations from the class  - the number of true positives.
		    		double recall = (TP / (TP + FN));
		    		permutedMacroGMean *= recall;
				}
				permutedMacroGMean = Math.pow(permutedMacroGMean, (1.0 / numberOfClasses));
				cumulativeGMeanChange += (baseOOBGMean.get(i) - permutedMacroGMean);
			}
			cumulativeAccChange /= this.forest.size();  // Get the mean change in the accuracy. This is the importance for the variable.
			cumulativeGMeanChange /= this.forest.size();  // Get the mean change in the accuracy. This is the importance for the variable.
			accVariableImportance.put(s, cumulativeAccChange);
			gMeanVariableImportance.put(s, cumulativeGMeanChange);
		}

		return new ImmutableTwoValues<Map<String,Double>, Map<String,Double>>(accVariableImportance, gMeanVariableImportance);
	}

}
