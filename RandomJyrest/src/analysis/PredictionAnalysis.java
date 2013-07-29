package analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PredictionAnalysis
{
	
	/**
	 * @param observationClasses
	 * @param predictions
	 * @return
	 */
	public static final Map<String, Map<String, Integer>> calculateConfusionMatrix(List<String> observationClasses, Map<String, double[]> predictions)
	{
		// Set up the confusion matrix.
		Map<String, Map<String, Integer>> confusionMatrix = new HashMap<String, Map<String, Integer>>();
		for (String s : predictions.keySet())
		{
			Map<String, Integer> classPredictions = new HashMap<String, Integer>();
			classPredictions.put("Correct", 0);
			classPredictions.put("Incorrect", 0);
			confusionMatrix.put(s, classPredictions);
		}

		int numberOfObservations = observationClasses.size();
		for (int i = 0; i < numberOfObservations; i++)
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
				Integer oldCount = confusionMatrix.get(obsClass).get("Correct");
				confusionMatrix.get(obsClass).put("Correct", oldCount + 1);
			}
			else
			{
				// Incorrectly predicted the class of the observation.
				Integer oldCount = confusionMatrix.get(predictedClass).get("Incorrect");
				confusionMatrix.get(predictedClass).put("Incorrect", oldCount + 1);
			}
		}
		
		return confusionMatrix;
	}
	
	/**
	 * @param confusionMatrix
	 * @return
	 */
	public static final double calculateAccuracy(Map<String, Map<String, Integer>> confusionMatrix)
	{
		double correctPredictions = 0.0;
		double totalPredictions = 0.0;
		for (Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet())
		{
			Map<String, Integer> predictions = entry.getValue();
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
	public static final double calculateFMeasure(Map<String, Map<String, Integer>> confusionMatrix, List<String> observationClasses, double fType)
	{
		Map<String, Integer> countsOfEachClass = new HashMap<String, Integer>();
		for (String s : confusionMatrix.keySet())
		{
			countsOfEachClass.put(s, Collections.frequency(observationClasses, s));
		}
		
		double recall = 0.0;
		double precision = 0.0;
		for (Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet())
		{
			Map<String, Integer> predictions = entry.getValue();
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
	 * @param observationClasses
	 * @return
	 */
	public static final double calculateGMean(Map<String, Map<String, Integer>> confusionMatrix, List<String> observationClasses)
	{
		Map<String, Integer> countsOfEachClass = new HashMap<String, Integer>();
		for (String s : confusionMatrix.keySet())
		{
			countsOfEachClass.put(s, Collections.frequency(observationClasses, s));
		}
		
		double gMean = 1.0;
		for (Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet())
		{
			Map<String, Integer> predictions = entry.getValue();
			double correct = predictions.get("Correct");
			double recall = correct / countsOfEachClass.get(entry.getKey());
			gMean *= recall;
		}
		gMean = Math.pow(gMean, (1.0 / confusionMatrix.size()));
		return gMean;
	}
	
	/**
	 * @param confusionMatrix
	 * @return
	 */
	public static final double calculateMCC(Map<String, Map<String, Integer>> confusionMatrix)
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
	 * @param inputFile
	 * @return
	 */
	public static final List<String> determineClassOfObservations(String inputFile)
	{
		List<String> classOfObservations = new ArrayList<String>();
		Path dataPath = Paths.get(inputFile);
		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = null;

			// Determine the column that contains the class data.
			line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");
			String classFeatureColumnName = "Classification";
			int classIndex = -1;
			int featureIndex = 0;
			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					classIndex = featureIndex;
				}
				featureIndex++;
			}
			if (classIndex == -1)
			{
				// No class column was provided.
				System.out.println("No class column was provided. Please include a column headed Classification.");
				System.exit(0);
			}
			
			// Extract the class data.
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				
				String[] chunks = line.split("\t");				
				classOfObservations.add(chunks[classIndex]);
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while determining the class of each observation.");
			e.printStackTrace();
			System.exit(0);
		}
		
		return classOfObservations;
	}
	
}
