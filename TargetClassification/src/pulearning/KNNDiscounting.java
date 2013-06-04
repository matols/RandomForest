package pulearning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.IndexedDoubleData;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

public class KNNDiscounting
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isStandardised = true;

		String[] variablesToIgnore = new String[]{"OGlycosylation"};  // Make sure to ignore any variables that are constant. Otherwise the standardised value of the variable will be NaN.
		ctrl.variablesToIgnore = Arrays.asList(variablesToIgnore);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse inputs.
		String dataForLearning = args[0];
		String outputFolder = args[1];
		int numberOfNeighbours = Integer.parseInt(args[2]);
		boolean isReliableNegativesGenerated = Boolean.parseBoolean(args[3]);

		// Process the input data.
		ProcessDataForGrowing processedDataForLearning = new ProcessDataForGrowing(dataForLearning, ctrl);

		// Setup the results.
		Map<Integer, Double> weightModifiers = new HashMap<Integer, Double>();
		Set<Integer> finalPositiveSet = new HashSet<Integer>();
		Set<Integer> finalNegativeSet = new HashSet<Integer>();

		// Determine the indices for the positive, unlabelled and all observations.
		List<Integer> allObservations = new ArrayList<Integer>();
		List<Integer> positiveObservations = new ArrayList<Integer>();
		List<Integer> unlabelledObservations = new ArrayList<Integer>();
		List<Integer> reliableNegativeSet = new ArrayList<Integer>();
		for (int i = 0; i < processedDataForLearning.numberObservations; i++)
		{
			allObservations.add(i);
			if (processedDataForLearning.responseData.get(i).equals("Positive"))
			{
				// If the observation is in the 'Positive' class.
				positiveObservations.add(i);
				finalPositiveSet.add(i);
				weightModifiers.put(i, 1.0);
			}
			else
			{
				// If the observation is in the 'Unlabelled' class.
				unlabelledObservations.add(i);
			}
		}
		int numberAllObservations = allObservations.size();
		int numberPositiveObservations = positiveObservations.size();
		int numberUnlabelledObservations = unlabelledObservations.size();
		int numberNegativeObservations = 0;

		if (isReliableNegativesGenerated)
		{
			// Determine the mean vector for the positive observations.
			Map<String, Double> meanPositiveVector = new HashMap<String, Double>();
			for (String s : processedDataForLearning.covariableData.keySet())
			{
				double expectedValue = 0.0;
				for (Integer i : positiveObservations)
				{
					expectedValue += processedDataForLearning.covariableData.get(s).get(i);
				}
				expectedValue /= numberPositiveObservations;
				meanPositiveVector.put(s, expectedValue);
			}

			// Calculate the reliable negative set.
			reliableNegativeSet = determineReliableNegative(unlabelledObservations, meanPositiveVector,
					processedDataForLearning);
			numberNegativeObservations = reliableNegativeSet.size();
			for (Integer i : reliableNegativeSet)
			{
				finalNegativeSet.add(i);
				weightModifiers.put(i, 1.0);
			}
		}

		List<Integer> observationsToCheck = new ArrayList<Integer>(unlabelledObservations);
		List<Integer> observationsToCheckAgainst = new ArrayList<Integer>();
		if (isReliableNegativesGenerated)
		{
			observationsToCheck.removeAll(reliableNegativeSet);
			observationsToCheckAgainst.addAll(positiveObservations);
			observationsToCheckAgainst.addAll(reliableNegativeSet);
		}
		else
		{
			observationsToCheckAgainst.addAll(allObservations);
		}

		for (Integer i : observationsToCheck)
		{
			List<Integer> nonI = new ArrayList<Integer>(observationsToCheckAgainst);
			nonI.remove(i);
			Map<Integer, Double> distancesFromI = distanceBetweenObservations(processedDataForLearning, i, nonI);

			List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
			for (Integer j : distancesFromI.keySet())
			{
				sortedDistances.add(new IndexedDoubleData(distancesFromI.get(j), j));
			}
			Collections.sort(sortedDistances);

			double numberPositive = 0.0;
			double numberNonPositive = 0.0;
			for (int j = 0; j < numberOfNeighbours; j++)
			{
				Integer observationIndex = sortedDistances.get(j).getIndex();
				if (positiveObservations.contains(observationIndex))
				{
					numberPositive += 1;
				}
				else
				{
					numberNonPositive += 1;
				}
			}

			if (numberPositive > numberNonPositive)
			{
				finalPositiveSet.add(i);
				weightModifiers.put(i, numberPositive / numberOfNeighbours);
			}
			else
			{
				finalNegativeSet.add(i);
				weightModifiers.put(i, numberNonPositive / numberOfNeighbours);
			}
		}

		// Write out the new dataset and weight modifiers.
		outputNewDataset(dataForLearning, outputFolder + "/NewDataset.txt", finalPositiveSet, finalNegativeSet);
		String weightModifierLocation = outputFolder + "/WeightModifier.txt";
		try
		{
			FileWriter weightModifierFile = new FileWriter(weightModifierLocation);
			BufferedWriter weightModifierWriter = new BufferedWriter(weightModifierFile);
			for (Integer i : allObservations)
			{
				weightModifierWriter.write(Integer.toString(i));
				weightModifierWriter.write("\t");
				weightModifierWriter.write(Double.toString(weightModifiers.get(i)));
				weightModifierWriter.newLine();
			}
			weightModifierWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}


	static Map<Integer, Double> distanceBetweenObservations(ProcessDataForGrowing dataset, int observation, List<Integer> obsToCompareTo)
	{
		Map<Integer, Double> distances = new HashMap<Integer, Double>();
		for (Integer i : obsToCompareTo)
		{
			double obsDistance = 0.0;
			for (String s : dataset.covariableData.keySet())
			{
				obsDistance += Math.pow(dataset.covariableData.get(s).get(observation) - dataset.covariableData.get(s).get(i), 2);
			}
			obsDistance = Math.pow(obsDistance, 0.5);
			distances.put(i, obsDistance);
		}
		return distances;
	}

	static List<Integer> determineReliableNegative(List<Integer> unlabelledObservations, Map<String, Double> meanPositiveVector,
			ProcessDataForGrowing processedDataForLearning)
	{
		// Determine the distance of all unlabelled observations from the mean positive vector.
		List<Double> distsanceToPositiveCluster = distanceFromMean(unlabelledObservations, meanPositiveVector, processedDataForLearning);
		List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
		double meanDistanceToPositive = 0.0;
		for (int i = 0; i < unlabelledObservations.size(); i++)
		{
			meanDistanceToPositive += distsanceToPositiveCluster.get(i);
			sortedDistances.add(new IndexedDoubleData(distsanceToPositiveCluster.get(i), i));
		}
		Collections.sort(sortedDistances);
		meanDistanceToPositive /= unlabelledObservations.size();

		// The reliable negative set is all the observations with a distance to the positive cluster greater than the mean distance.
		List<Integer> reliableNegativeSet = new ArrayList<Integer>();
		for (int i = 0; i < sortedDistances.size(); i++)
		{
			if (sortedDistances.get(i).getData() > meanDistanceToPositive)
			{
				reliableNegativeSet.add(sortedDistances.get(i).getIndex());
			}
		}
		return reliableNegativeSet;
	}

	static List<Double> distanceFromMean(List<Integer> observationIndices, Map<String, Double> meanPositiveVector, ProcessDataForGrowing dataset)
	{
		List<Double> distances = new ArrayList<Double>();
		for (Integer i : observationIndices)
		{
			double obsDistance = 0.0;
			for (String s : dataset.covariableData.keySet())
			{
				obsDistance += Math.pow(meanPositiveVector.get(s) - dataset.covariableData.get(s).get(i), 2);
			}
			obsDistance = Math.pow(obsDistance, 0.5);
			distances.add(obsDistance);
		}
		return distances;
	}

	static void outputNewDataset(String originalDataset, String outputLocation, Set<Integer> positiveObservations,
			Set<Integer> negativeObservations)
	{
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(originalDataset), StandardCharsets.UTF_8))
		{
			// Record the header lines.
			String headerOne = reader.readLine();
			headerOne = headerOne.replaceAll("\n", "");
			String headerTwo = reader.readLine();
			headerTwo = headerTwo.replaceAll("\n", "");
			String headerThree = reader.readLine();
			headerThree = headerThree.replaceAll("\n", "");

			String[] variableTypes = headerTwo.split("\t");
			int responseColumn = 0;
			for (int i = 0; i < variableTypes.length; i++)
			{
				if (variableTypes[i].equals("r"))
				{
					responseColumn = i;
				}
			}

			try
			{
				FileWriter newDatasetFile = new FileWriter(outputLocation);
				BufferedWriter newDatasetWriter = new BufferedWriter(newDatasetFile);
				newDatasetWriter.write(headerOne);
				newDatasetWriter.newLine();
				newDatasetWriter.write(headerTwo);
				newDatasetWriter.newLine();
				newDatasetWriter.write(headerThree);
				newDatasetWriter.newLine();

				String line = null;
				int lineCount = 0;
				while ((line = reader.readLine()) != null)
				{
					if (line.trim().length() == 0)
					{
						// If the line is made up of all whitespace, then ignore the line.
						continue;
					}
					line = line.replaceAll("\n", "");
					String[] splitLine = line.split("\t");
					if (positiveObservations.contains(lineCount))
					{
						splitLine[responseColumn] = "Positive";
					}
					else
					{
						splitLine[responseColumn] = "Negative";
					}
					lineCount++;

					newDatasetWriter.write(splitLine[0]);
					for (int i = 1; i < splitLine.length; i++)
					{
						newDatasetWriter.write("\t");
						newDatasetWriter.write(splitLine[i]);
					}
					newDatasetWriter.newLine();
				}
				newDatasetWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
