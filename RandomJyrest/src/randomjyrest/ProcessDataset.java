package randomjyrest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utilities.ImmutableTwoValues;

public class ProcessDataset
{

	/**
	 * Processes a file containing a dataset of observations.
	 * 
	 * Processes a tsv format file, while ensuring that only the desired features in the dataset are in the processed dataset.
	 * 
	 * The data file is expected to be tab separated with the first line containing the names of the features/columns.
	 * Additionally, it is expected that the column containing the class is headed with Classification.
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
	 * @param dataset			The location of the file containing the data to be processed.
	 * @param featuresToRemove	The features in the dataset that should be removed (not processed).
	 * @param weights			The weights of the individual observations.
	 * @return					A mapping from the feature names to the values of the feature for each observation (in the order that
	 * 							the observations appear in the file), and a mapping from each class to an array of the weight for that
	 * 							class for each observation.
	 */
	public static final ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>> main(String dataset, List<String> featuresToRemove, double[] weights)
	{
		// Setup the mappings to hold the final processed feature data along with the class data.
		Map<String, List<Double>> processedFeatureData = new HashMap<String, List<Double>>();
		Map<String, double[]> processedClassData = new HashMap<String, double[]>();
		
		int numberOfObservations = 0;  // The number of observations in the dataset.

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
					processedFeatureData.put(feature, new ArrayList<Double>());
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
					processedFeatureData.get(feature).add(value);
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
		
		return new ImmutableTwoValues<Map<String, List<Double>>, Map<String, double[]>>(processedFeatureData, processedClassData);
	}

}
