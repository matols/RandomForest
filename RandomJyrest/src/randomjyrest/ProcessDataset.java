package randomjyrest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utilities.ImmutableThreeValues;
import utilities.IndexedDoubleData;

/**
 * Implements the processing of a dataset in order to use it to grow a random forest.
 */
public final class ProcessDataset
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
	 * the same value for every observation are not recorded in the final processed dataset.
	 * 
	 * The values in the weight vector are the values for the individual observations. The values should
	 * be specified so that weights[i] is the weight for the ith observation in the dataset (i.e. the ith observaton in the input file).
	 * If the length of the weight vector is less than the number of observations, then the weight vector is padded with 1.0s to make
	 * it have one value for each observation.
	 * 
	 * The data file is expected to be tab separated with the first line containing the names of the features/columns.
	 * The column in the file containing the class should be headed with Classification.
	 *
	 * The processed data has three components:
	 * 		1) A mapping from the name of each feature, F, to the values of the feature for each observation. The values of the observations
	 * 		   are sorted in ascending order.
	 * 		2) A mapping from the name of each feature, F, to an array containing the original indices in the input file of the sorted
	 * 		   observation values for the feature. For example, if the smallest observation value comes from the 3rd observation in
	 * 		   the input file, then the 0th entry in this component for F will be 2.
	 * 		3) A mapping from each class to an array containing the weight of each observation for the class. The weights are ordered
	 * 		   according to the original indices of the observations not the sorted order.
	 *  	Processing is done in this manner for speed and memory efficiencies.
	 * Example:
	 * 		The values of a feature, F, in the input file are		[4, 7, 2, 8, 3, 4]
	 * 		The indices of the values are							[0, 1, 2, 3, 4, 5]
	 * 		The classes of the observations are						[A, B, B, C, A, C]
	 * 		The weights for the observations are					[3, 2, 7, 4, 1, 1]
	 * 
	 * 		1) The array stored at F for component 1 is				[2, 3, 4, 4, 7, 8]	(sorted observation values)
	 * 		2) The array stored at F for component 2 is				[2, 4, 0, 5, 1, 3]	(the original indices of the sorted values)
	 * 		3) The array stored at
	 * 			A for component 3 is								[3, 0, 0, 0, 1, 0]	(each observation's weight for class A)
	 * 			B for component 3 is								[0, 2, 7, 0, 0, 0]	(each observation's weight for class B)
	 * 			C for component 3 is								[0, 0, 0, 4, 0, 1]	(each observation's weight for class C)
	 * 
	 * @param dataset			The location of the file containing the data to be processed.
	 * @param featuresToRemove	The features in the dataset that should be removed (not processed).
	 * @param weights			The weights of the individual observations.
	 * @return					Three mappings, one from feature names to sorted data values, one from feature names to original
	 * 							indices of the sorted data values and one from class names to the weight of each observation to the class. 
	 */
	public static final ImmutableThreeValues<Map<String, double[]>, Map<String, int[]>, Map<String, double[]>>
		main(String dataset, List<String> featuresToRemove, double[] weights)
	{
		// Setup the mapping to hold the temporary and final processed data.
		Map<String, List<Double>> temporaryData = new HashMap<String, List<Double>>();  // Mapping to hold the raw extracted data.
		Map<String, double[]> processedFeatureData = new HashMap<String, double[]>();  // Mapping to hold the sorted data values.
		Map<String, int[]> processedIndexData = new HashMap<String, int[]>();  // Mapping to hold the original indices of the data values.
		Map<String, double[]> processedClassData = new HashMap<String, double[]>();  // Mapping to hold the class weight of each observation.

		int numberOfObservations = 0;  // The number of observations in the input file.

		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(dataset));
			String line = null;

			// Generate a mapping from the index of the column in the dataset to the name of the feature that the column contains
			// values of.
			line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");
			String classFeatureColumnName = "Classification";
			
			// Initialise the mapping that holds the temporary processing of the data.
			List<Integer> featureIndicesToUse = new ArrayList<Integer>();  // The indices of the columns in the file which are not to be removed.
			List<String> classData = new ArrayList<String>();  // The class of each observation ordered as the observations are in the file.
			int classIndex = -1;  // The index of the column containing the class of the observations.
			int featureIndex = 0;  // The current index of the feature.
			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					// Record if the feature is actually the class column.
					classIndex = featureIndex;
				}
				else if (!featuresToRemove.contains(feature))
				{
					// If the feature is not one to be removed, then record the index of the column as one to extract data from.
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
			
			int currentObservationIndex = -1;  // The index of the current observation.
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
			numberOfObservations = weights.length;

			// Setup the class information.
			Set<String> classesInDataset = new HashSet<String>(classData);
			for (String s : classesInDataset)
			{
				double[] classWeights  = new double[numberOfObservations];
				for (int i = 0; i < numberOfObservations; i++)
				{
					String classOfObservation = classData.get(i);
					if (s.equals(classOfObservation))
					{
						classWeights[i] = weights[i];
					}
					else
					{
						classWeights[i] = 0.0;
					}
				}
				processedClassData.put(s, classWeights);
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while processing the input data file.");
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			try
			{
				if (reader != null)
				{
					reader.close();
				}
			}
			catch (IOException e)
			{
				// Caught an error while closing the file. Indicate this and exit.
				System.out.println("An error occurred while closing the input data file.");
				e.printStackTrace();
				System.exit(0);
			}
		}

		// Generate the final processed data.
		for (Map.Entry<String, List<Double>> entry : temporaryData.entrySet())
		{
			// Sort the values of each observaton separately for each feature.
			String feature = entry.getKey();
			List<Double> data = entry.getValue();
			List<IndexedDoubleData> sortedData = new ArrayList<IndexedDoubleData>();
			for (int i = 0; i < numberOfObservations; i++)
			{
				// Add all the observation values along with their original index.
				sortedData.add(new IndexedDoubleData(data.get(i).doubleValue(), i));
			}
			Collections.sort(sortedData);  // Sort the value-index pairs in ascending order by value.
			
			if (sortedData.get(0).getData() == sortedData.get(numberOfObservations - 1).getData())
			{
				// If the first and last data value are equal, then the feature contains only one value and is useless.
				// Therefore, remove features where the first and last value are equal.
				continue;
			}
			
			double[] sortedFeatureData = new double[numberOfObservations];  // Initialise the sorted data value.
			int[] sortedFeatureIndices = new int[numberOfObservations];  // Initialise the original indices.
			for (int i = 0; i < numberOfObservations; i++)
			{
				sortedFeatureData[i] = sortedData.get(i).getData();
				sortedFeatureIndices[i] = sortedData.get(i).getIndex();
			}
			
			// Put the data values and original indices into the return value mappings.
			processedFeatureData.put(feature, sortedFeatureData);
			processedIndexData.put(feature, sortedFeatureIndices);
		}
		
		return new ImmutableThreeValues<Map<String, double[]>, Map<String, int[]>, Map<String, double[]>>(processedFeatureData, processedIndexData, processedClassData);
	}

}