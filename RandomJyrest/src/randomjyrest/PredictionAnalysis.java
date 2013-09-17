package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PredictionAnalysis
{
	
	/**
	 * @param observationClasses
	 * @param predictions
	 * @return
	 */
	public static final Map<String, Map<String, Double>> calculateConfusionMatrix(List<String> observationClasses,
			Map<String, double[]> predictions)
	{
		Set<Integer> observationsToUse = new HashSet<Integer>(observationClasses.size());
		int numberOfObservations = observationClasses.size();
		for (int i = 0; i < numberOfObservations; i++)
		{
			observationsToUse.add(i);
		}
		return calculateConfusionMatrix(observationClasses, predictions, observationsToUse);
	}
	
	/**
	 * @param observationClasses
	 * @param predictions
	 * @param observationsToUse
	 * @return
	 */
	public static final Map<String, Map<String, Double>> calculateConfusionMatrix(List<String> observationClasses,
			Map<String, double[]> predictions, Set<Integer> observationsToUse)
	{
		// Set up the confusion matrix.
		Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
		for (String s : predictions.keySet())
		{
			Map<String, Double> classPredictions = new HashMap<String, Double>();
			classPredictions.put("Correct", 0.0);
			classPredictions.put("Incorrect", 0.0);
			confusionMatrix.put(s, classPredictions);
		}

		for (int i : observationsToUse)
		{
			String obsClass = observationClasses.get(i);
			String predictedClass = "";
			double maxPredictedWeightForObs = 0.0;
			for (Map.Entry<String, double[]> entry : predictions.entrySet())
			{
				String classOfPrediction = entry.getKey();
				double predictedClassWeight = entry.getValue()[i];
				if (predictedClassWeight > maxPredictedWeightForObs)
				{
					predictedClass = classOfPrediction;
					maxPredictedWeightForObs = predictedClassWeight;
				}
			}
			
			// Update the confusion matrix.
			if (obsClass.equals(predictedClass))
			{
				// Correctly predicted the class of an observation.
				Double oldCount = confusionMatrix.get(obsClass).get("Correct");
				confusionMatrix.get(obsClass).put("Correct", oldCount + 1.0);
			}
			else
			{
				// Incorrectly predicted the class of the observation.
				Double oldCount = confusionMatrix.get(predictedClass).get("Incorrect");
				confusionMatrix.get(predictedClass).put("Incorrect", oldCount + 1.0);
			}
		}
		
		return confusionMatrix;
	}
	
	/**
	 * @param observationClasses
	 * @param predictions
	 * @param discounts
	 * @return
	 */
	public static final Map<String, Map<String, Double>> calculateWeightedConfusionMatrix(List<String> observationClasses,
			Map<String, double[]> predictions, double[] discounts)
	{
		// Set up the confusion matrix.
		Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
		for (String s : predictions.keySet())
		{
			Map<String, Double> classPredictions = new HashMap<String, Double>();
			classPredictions.put("Correct", 0.0);
			classPredictions.put("Incorrect", 0.0);
			confusionMatrix.put(s, classPredictions);
		}

		for (int i = 0; i < observationClasses.size(); i++)
		{
			String obsClass = observationClasses.get(i);
			String predictedClass = "";
			double maxPredictedWeightForObs = 0.0;
			for (Map.Entry<String, double[]> entry : predictions.entrySet())
			{
				String classOfPrediction = entry.getKey();
				double predictedClassWeight = entry.getValue()[i];
				if (predictedClassWeight > maxPredictedWeightForObs)
				{
					predictedClass = classOfPrediction;
					maxPredictedWeightForObs = predictedClassWeight;
				}
			}
			
			// Update the confusion matrix.
			if (obsClass.equals(predictedClass))
			{
				// Correctly predicted the class of an observation.
				Double oldCount = confusionMatrix.get(obsClass).get("Correct");
				confusionMatrix.get(obsClass).put("Correct", oldCount + discounts[i]);
			}
			else
			{
				// Incorrectly predicted the class of the observation.
				Double oldCount = confusionMatrix.get(predictedClass).get("Incorrect");
				confusionMatrix.get(predictedClass).put("Incorrect", oldCount + discounts[i]);
			}
		}
		
		return confusionMatrix;
	}
	
	/**
	 * @param confusionMatrix
	 * @return
	 */
	public static final double calculateAccuracy(Map<String, Map<String, Double>> confusionMatrix)
	{
		double correctPredictions = 0.0;
		double totalPredictions = 0.0;
		for (Map.Entry<String, Map<String, Double>> entry : confusionMatrix.entrySet())
		{
			Map<String, Double> predictions = entry.getValue();
			double correct = predictions.get("Correct");
			double incorrect = predictions.get("Incorrect");
			correctPredictions += correct;
			totalPredictions += correct + incorrect;
		}
		return correctPredictions / totalPredictions;
	}
	
	/**
	 * @param confusionMatrix
	 * @param observationClasses
	 * @param fType
	 * @return
	 */
	public static final double calculateFMeasure(Map<String, Map<String, Double>> confusionMatrix, List<String> observationClasses, double fType)
	{
		Map<String, Integer> countsOfEachClass = new HashMap<String, Integer>();
		for (String s : confusionMatrix.keySet())
		{
			countsOfEachClass.put(s, Collections.frequency(observationClasses, s));
		}
		
		double recall = 0.0;
		double precision = 0.0;
		for (Map.Entry<String, Map<String, Double>> entry : confusionMatrix.entrySet())
		{
			Map<String, Double> predictions = entry.getValue();
			double correct = predictions.get("Correct");
			double incorrect = predictions.get("Incorrect");
			recall += correct / countsOfEachClass.get(entry.getKey());
			precision += correct / (correct + incorrect);
		}
		recall /= countsOfEachClass.size();
		precision /= countsOfEachClass.size();
		double fMeasure = (1 + (fType * fType)) * ((precision * recall) / ((fType * fType * precision) + recall));
		return fMeasure;
	}

	/**
	 * @param confusionMatrix
	 * @param countsOfEachClass
	 * @return
	 */
	public static final double calculateGMean(Map<String, Map<String, Double>> confusionMatrix, Map<String, Integer> countsOfEachClass)
	{
		double gMean = 1.0;
		for (Map.Entry<String, Map<String, Double>> entry : confusionMatrix.entrySet())
		{
			Map<String, Double> predictions = entry.getValue();
			double correct = predictions.get("Correct");
			double recall = correct / countsOfEachClass.get(entry.getKey());
			gMean *= recall;
		}
		gMean = Math.pow(gMean, (1.0 / confusionMatrix.size()));
		return gMean;
	}
	
	/**
	 * @param confusionMatrix
	 * @param observationClasses
	 * @return
	 */
	public static final double calculateGMean(Map<String, Map<String, Double>> confusionMatrix, List<String> observationClasses)
	{
		Map<String, Integer> countsOfEachClass = new HashMap<String, Integer>();
		for (String s : confusionMatrix.keySet())
		{
			countsOfEachClass.put(s, Collections.frequency(observationClasses, s));
		}
		
		double gMean = 1.0;
		for (Map.Entry<String, Map<String, Double>> entry : confusionMatrix.entrySet())
		{
			Map<String, Double> predictions = entry.getValue();
			double correct = predictions.get("Correct");
			double recall = correct / countsOfEachClass.get(entry.getKey());
			gMean *= recall;
		}
		gMean = Math.pow(gMean, (1.0 / confusionMatrix.size()));
		return gMean;
	}
	
	/**
	 * @param observationClasses
	 * @param predictions
	 * @return
	 */
	public static final double calculateLogarithmicScore(List<String> observationClasses, Map<String, double[]> predictions)
	{
		double averageLogarithmicScore = 0.0;
		int numberOfObservations = observationClasses.size();
		
		for (int i = 0; i < numberOfObservations; i++)
		{
			String trueClass = observationClasses.get(i);
			double totalPredictiveWeight = 0.0;
			double weightOfTrueClass = 0.0;

			for (Map.Entry<String, double[]> entry : predictions.entrySet())
			{
				double classWeight = entry.getValue()[i];
				totalPredictiveWeight += classWeight;
				if (entry.getKey().equals(trueClass))
				{
					weightOfTrueClass = classWeight;
				}
			}
			
			double score = Math.log(weightOfTrueClass / totalPredictiveWeight);
			averageLogarithmicScore += score;
		}
		
		return averageLogarithmicScore / numberOfObservations;
	}
	
	/**
	 * @param confusionMatrix
	 * @return
	 */
	public static final double calculateMCC(Map<String, Map<String, Double>> confusionMatrix)
	{
		double MCC = Double.NaN;
		List<String> allClasses = new ArrayList<String>(confusionMatrix.keySet());
		if (allClasses.size() == 2)
		{
			String classOne = allClasses.get(0);
			String classTwo = allClasses.get(1);
			double TP = confusionMatrix.get(classOne).get("Correct");
			double FP = confusionMatrix.get(classOne).get("Incorrect");
			double TN = confusionMatrix.get(classTwo).get("Correct");
			double FN = confusionMatrix.get(classTwo).get("Incorrect");
			MCC = ((TP * TN) - (FP * FN)) / (Math.sqrt((TP + TN) * (TP + FN) * (TN + FP) * (TN + FN)));
		}
		return MCC;
	}
	
}
