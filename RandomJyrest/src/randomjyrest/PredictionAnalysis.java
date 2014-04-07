package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for evaluating predictions.
 */
public final class PredictionAnalysis
{
	
	/**
	 * Generate a confusion matrix from the predicted classes of a set of observations.
	 * 
	 * @param observationClasses	The class of each observation ordered as the observations are in the dataset.
	 * @param predictions			Mapping from class names to the prediction weight for that class for each observation.
	 * @return						A confusion matrix. This is a mapping from class names to the number of observations of that class
	 * 								correctly and incorrectly predicted as being members. For example, for class C, the number of
	 * 								observations of class C correctly predicted as being members of class C will be recorded in
	 * 								return.get("C").get("Correct"), and the number of observations of a difference class that were
	 * 								predicted as being members of class C in return.get("C").get("Incorrect").
	 */
	public static final Map<String, Map<String, Double>> calculateConfusionMatrix(List<String> observationClasses,
			Map<String, double[]> predictions)
	{
		Set<Integer> observationsToUse = new HashSet<Integer>(observationClasses.size());
		int numberOfObservations = observationClasses.size();
		// Use all observations.
		for (int i = 0; i < numberOfObservations; i++)
		{
			observationsToUse.add(i);
		}
		return calculateConfusionMatrix(observationClasses, predictions, observationsToUse);
	}
	
	/**
	 * Generate a confusion matrix from the predicted classes of a set of observations.
	 * 
	 * @param observationClasses	The class of each observation ordered as the observations are in the dataset.
	 * @param predictions			Mapping from class names to the prediction weight for that class for each observation.
	 * @param observationsToUse		The indices of the observations to use when calculating the confusion matrix.
	 * @return						A confusion matrix. This is a mapping from class names to the number of observations of that class
	 * 								correctly and incorrectly predicted as being members. For example, for class C, the number of
	 * 								observations of class C correctly predicted as being members of class C will be recorded in
	 * 								return.get("C").get("Correct"), and the number of observations of a difference class that were
	 * 								predicted as being members of class C in return.get("C").get("Incorrect").
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

		// Get the predicted class of each observation of interest. The predicted class of an observation is the class with the
		// largest predicted weight for the observation.
		for (int i : observationsToUse)
		{
			String obsClass = observationClasses.get(i);  // The observation's true class.
			String predictedClass = "";  // The class that the observation is predicted to be a member of.
			double maxPredictedWeightForObs = 0.0;
			// Look at the observation's predicted weight for each class.
			for (Map.Entry<String, double[]> entry : predictions.entrySet())
			{
				String classOfPrediction = entry.getKey();  // The class.
				double predictedClassWeight = entry.getValue()[i];  // The observation's predicted weight for the class.
				if (predictedClassWeight > maxPredictedWeightForObs)
				{
					// The predicted weight for this class is the greatest weight seen so far.
					predictedClass = classOfPrediction;
					maxPredictedWeightForObs = predictedClassWeight;
				}
			}
			
			// Update the confusion matrix.
			if (obsClass.equals(predictedClass))
			{
				// Correctly predicted the class of the observation.
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
	 * Calculate the accuracy of the predictions in a confusion matrix.
	 * 
	 * @param confusionMatrix	A confusion matrix as calculated by calculateConfusionMatrix.
	 * @return					The accuracy of the predictions
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
	 * Calculate the F beta measure from a confusion matrix.
	 * 
	 * @param confusionMatrix		A confusion matrix as calculated by calculateConfusionMatrix.
	 * @param observationClasses	The indices of the observations to use when calculating the confusion matrix.
	 * @param fType					The value of beta in the F beta calculation.
	 * @return						The F beta measure.
	 */
	public static final double calculateFMeasure(Map<String, Map<String, Double>> confusionMatrix, List<String> observationClasses,
			double fType)
	{
		return calculateFMeasure(confusionMatrix, observationClasses, fType, 1);
	}
	
	/**
	 * Calculate the F beta measure from a confusion matrix.
	 * 
	 * @param confusionMatrix						A confusion matrix as calculated by calculateConfusionMatrix.
	 * @param observationClasses					The indices of the observations to use when calculating the confusion matrix.
	 * @param fType									The value of beta in the F beta calculation.
	 * @param numberOfTimesEachObsAppearsInConfMat	The number of times each observation appears in the confusion matrix. Should be equal
	 * 												to 1 unless the confusion matrix is an aggregate of predictions from multiple
	 * 												classifiers.
	 * @return										The F beta measure.
	 */
	public static final double calculateFMeasure(Map<String, Map<String, Double>> confusionMatrix, List<String> observationClasses,
			double fType, int numberOfTimesEachObsAppearsInConfMat)
	{
		Map<String, Integer> countsOfEachClass = determineCountsOfEachClass(confusionMatrix, observationClasses,
				numberOfTimesEachObsAppearsInConfMat);
		
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
	 * Calculate the G mean of the predictions in a confusion matrix.
	 * 
	 * @param confusionMatrix		A confusion matrix as calculated by calculateConfusionMatrix.
	 * @param observationClasses	The indices of the observations to use when calculating the confusion matrix.
	 * @return						The G mean of the predictions in the confusion matrix.
	 */
	public static final double calculateGMean(Map<String, Map<String, Double>> confusionMatrix, List<String> observationClasses)
	{
		return calculateGMean(confusionMatrix, observationClasses, 1);
	}
	
	/**
	 * Calculate the G mean of the predictions in a confusion matrix.
	 * 
	 * @param confusionMatrix						A confusion matrix as calculated by calculateConfusionMatrix.
	 * @param observationClasses					The indices of the observations to use when calculating the confusion matrix.
	 * @param numberOfTimesEachObsAppearsInConfMat	The number of times each observation appears in the confusion matrix. Should be equal
	 * 												to 1 unless the confusion matrix is an aggregate of predictions from multiple
	 * 												classifiers.
	 * @return										The G mean of the predictions in the confusion matrix.
	 */
	public static final double calculateGMean(Map<String, Map<String, Double>> confusionMatrix, List<String> observationClasses,
			int numberOfTimesEachObsAppearsInConfMat)
	{
		Map<String, Integer> countsOfEachClass = determineCountsOfEachClass(confusionMatrix, observationClasses,
				numberOfTimesEachObsAppearsInConfMat);
		
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
	 * Calculate the Matthews correlation coefficient.
	 * 
	 * @param confusionMatrix		A confusion matrix as calculated by calculateConfusionMatrix.
	 * @return						The Matthews correlation coefficient for the confusion matrix, or NaN if there are not two classes.
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
	
	
	/**
	 * Determine the number of observations of each class in the confusion matrix.
	 * 
	 * @param confusionMatrix						A confusion matrix as calculated by calculateConfusionMatrix.
	 * @param observationClasses					The indices of the observations to use when calculating the confusion matrix.
	 * @param numberOfTimesEachObsAppearsInConfMat	The number of times each observation appears in the confusion matrix. Should be equal
	 * 												to 1 unless the confusion matrix is an aggregate of predictions from multiple
	 * 												classifiers.
	 * @return										A mapping from the classes in the confusion matrix to the number of observations
	 * 												of that class that were predicted.
	 */
	private static Map<String, Integer> determineCountsOfEachClass(Map<String, Map<String, Double>> confusionMatrix,
			List<String> observationClasses, int numberOfTimesEachObsAppearsInConfMat)
	{
		Map<String, Integer> countsOfEachClass = new HashMap<String, Integer>();
		for (String s : confusionMatrix.keySet())
		{
			countsOfEachClass.put(s, Collections.frequency(observationClasses, s) * numberOfTimesEachObsAppearsInConfMat);
		}
		return countsOfEachClass;
	}
	
}
