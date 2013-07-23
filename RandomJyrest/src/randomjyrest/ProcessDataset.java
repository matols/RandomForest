package randomjyrest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utilities.IndexedDoubleData;

public class ProcessDataset
{

	/**
	 * Processes a file containing a dataset of observations.
	 * 
	 * Processes a tsv format file, while ensuring that only the desired features in the dataset are in the processed dataset.
	 * 
	 * The values for the features in the dataset are all assumed to be numeric (e.g. integers or reals) rather than strings
	 * representing categories. Any categorical data should therefore be mapped to a set of integers (or reals), and will then
	 * be treated as if it was numeric not categorical (i.e. binary splits will always be performed).
	 * 
	 * The features in featuresToRemove are not recorded in the final processed dataset. Additionally, any features that have
	 * the same value for every obaservation are not recorded in the final processed dataset.
	 * 
	 * The values in the weight vector are the values for the individual observations. The values int he weight vector should
	 * be specified so that weights[i] is the weight for the ith observation in the dataset. If the length of the weight vector
	 * is less than the number of observations, then the weight vector is padded with 1.0s to make it have one value for each
	 * observation.
	 * 
	 * The data file is expected to be tab separated with the first line containing the names of the features/columns.
	 * The restrictions on the naming of the features/columns are:
	 *	1) The column containing the class should be headed with Classification.
	 *	2) There should not be a feature/column named Weight.
	 *
	 * The processed data has the form:
	 * 		featureName ->
	 * 						"Data" 	- The array containing the values for the feature.
	 * 						"Index" - The array containing the indices of the observations.
	 * returnValue.get(featureName).get("Data") will contain the values of the feature sorted in ascending order. The values in
	 * returnValue.get(featureName).get("Index") are also sorted, but the sorting is done based on the data values.
	 * Example:
	 * 		The values of a feature are		[4, 7, 2, 8, 3, 4]
	 * 		The indices are					[0, 1, 2, 3, 4, 5]
	 * 		The sorted values are			[2, 3, 4, 4, 7, 8]
	 * 		The sorted indices are			[2, 4, 0, 5, 1, 3]
	 * 		The value 8 is therefore the greatest data value, and comes from the observation with index 3 in the original data file.
	 * 
	 * @param dataset			The location of the file containing the data to be processed.
	 * @param featuresToRemove	The features in the dataset that should be removed (not processed).
	 * @param weights			The weights of the individual observations.
	 * @return					A mapping from the feature names to two arrays. One is the "Data" array. This contains the sorted
	 * 							values for the feature. The other is the "Index" array. This contains the indices of the observations
	 * 							sorted by the ordering in the "Data" array.
	 */
	public static final Map<String, Map<String, double[]>> main(String dataset, List<String> featuresToRemove, double[] weights)
	{
		// Setup the mapping to hold the temporary and final processed data.
		Map<String, List<Double>> temporaryData = new HashMap<String, List<Double>>();
		Map<String, Map<String, double[]>> processedData = new HashMap<String, Map<String, double[]>>();

		Path dataPath = Paths.get(dataset);
		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = null;

			// Generate a mapping from the index of the column in the dataset to the name of the feature that the column contains values of.
			line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");
			String classFeatureColumnName = "Classification";
			
			// Initialise the mapping that holds the temporary processing of the data.
			List<Integer> featureIndicesToUse = new ArrayList<Integer>();  // The indices of the columns in the file which are not to be removed.
			List<String> classData = new ArrayList<String>();
			int classIndex = -1;
			int featureIndex = 0;
			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					classIndex = featureIndex;
				}
				else if (!featuresToRemove.contains(feature))
				{
					featureIndicesToUse.add(featureIndex);
					temporaryData.put(feature, new ArrayList<Double>());
				}
				featureIndex += 1;
			}
			
			if (classIndex == -1)
			{
				// No class column was provided.
				System.out.println("No class column was provided. Please include a column headed Classification.");
				System.exit(0);
			}
			
			int currentObservationIndex = -1;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				
				currentObservationIndex += 1;

				// Enter the feature values for this observation into the mapping of the temporary processing of the data.
				String[] chunks = line.split("\t");
				for (Integer i : featureIndicesToUse)
				{
					String feature = featureNames[i];
					double value = Double.parseDouble(chunks[i]);
					temporaryData.get(feature).add(value);
				}
				
				// Enter the class information for this observation.
				classData.add(chunks[classIndex]);
			}

			// Pad the weight vector with 1.0s if needed.
			int numberOfWeightsSupplied = weights.length;
			if (numberOfWeightsSupplied < (currentObservationIndex + 1))
			{
				// Not enough weights were supplied.
				double[] newWeightVector = new double[(int) currentObservationIndex + 1];
				Arrays.fill(newWeightVector, 1.0);
				for (int i = 0; i < numberOfWeightsSupplied; i++)
				{
					newWeightVector[i] = weights[i];
				}
				weights = newWeightVector;
			}

			// Setup the class information.
			processedData.put(classFeatureColumnName, new HashMap<String, double[]>());
			Map<String, double[]> classDMap = processedData.get(classFeatureColumnName);
			Set<String> classesInDataset = new HashSet<String>(classData);
			for (String s : classesInDataset)
			{
				classDMap.put(s, new double[currentObservationIndex + 1]);
			}
			for (int i = 0; i < currentObservationIndex + 1; i++)
			{
				String classOfObservation = classData.get(i);
				for (String s : classesInDataset)
				{
					if (s.equals(classOfObservation))
					{
						classDMap.get(s)[i] = weights[i];
					}
					else
					{
						classDMap.get(s)[i] = 0.0;
					}
				}
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while processing the input data file.");
			e.printStackTrace();
			System.exit(0);
		}

		// Generate the final processed data.
		int numberOfObservations = weights.length;
		for (Map.Entry<String, List<Double>> entry : temporaryData.entrySet())
		{
			String feature = entry.getKey();
			List<Double> data = entry.getValue();
			List<IndexedDoubleData> sortedData = new ArrayList<IndexedDoubleData>();
			for (int i = 0; i < numberOfObservations; i++)
			{
				sortedData.add(new IndexedDoubleData(data.get(i).doubleValue(), i));
			}
			Collections.sort(sortedData);
			
			if (sortedData.get(0).getData() == sortedData.get(numberOfObservations - 1).getData())
			{
				// If the first and last data value are equal, then the feature contains only one value and is useless.
				// Therefore, remove features where the first and last value are equal.
				continue;
			}
			
			double[] sortedFeatureData = new double[numberOfObservations];
			double[] sortedFeatureIndices = new double[numberOfObservations];
			for (int i = 0; i < numberOfObservations; i++)
			{
				sortedFeatureData[i] = sortedData.get(i).getData();
				sortedFeatureIndices[i] = sortedData.get(i).getIndex();
			}
			
			Map<String, double[]> featureMap = new HashMap<String, double[]>();
			featureMap.put("Data", sortedFeatureData);
			featureMap.put("Index", sortedFeatureIndices);
			processedData.put(feature, featureMap);
		}
		
		return processedData;
	}

}
