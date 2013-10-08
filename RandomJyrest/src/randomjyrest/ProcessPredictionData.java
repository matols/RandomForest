package randomjyrest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utilities.ImmutableTwoValues;

public class ProcessPredictionData
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
	 * The features in featuresToRemove are not recorded in the final processed dataset.
	 * 
	 * The data file is expected to be tab separated with the first line containing the names of the features/columns.
	 * If a column headed Classification is present, then that column is ignored (as it is reserved for recording the class of observations).
	 * 
	 * @param dataset			The location of the file containing the data to be processed.
	 * @param featuresToRemove	The features in the dataset that should be removed (not processed).
	 * @return					A mapping from each feature name to the values of the observations for it, along with the number
	 * 							of observations.
	 */
	public static final ImmutableTwoValues<Map<String, double[]>, Integer> main(String dataset, List<String> featuresToRemove)
	{
		// Setup the mapping to hold the temporary and final processed data.
		Map<String, List<Double>> temporaryData = new HashMap<String, List<Double>>();  // Mapping to hold the raw extracted data.
		Map<String, double[]> datasetToPredict = new HashMap<String, double[]>();  // Mapping to hold the processed data.
		
		int numberOfObservations = 0;  // The number of observations in the dataset.

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
			int featureIndex = 0;  // The current index of the feature.
			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					;  // Ignore the class column if there is one.
				}
				else if (!featuresToRemove.contains(feature))
				{
					// If the feature is not one to be removed, then record the index of the column as one to extract data from.
					featureIndicesToUse.add(featureIndex);
					temporaryData.put(feature, new ArrayList<Double>());
				}
				featureIndex += 1;
			}

			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				
				// Enter the feature values for this observation into the mapping of the temporary processing of the data.
				String[] chunks = line.split("\t");
				for (Integer i : featureIndicesToUse)
				{
					String feature = featureNames[i];
					double value = Double.parseDouble(chunks[i]);
					temporaryData.get(feature).add(value);
				}
				numberOfObservations++;
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while generating the processed prediction data.");
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
				System.out.println("An error occurred while closing the prediction data file.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		// Generate the final processed data.
		for (Map.Entry<String, List<Double>> entry : temporaryData.entrySet())
		{
			double[] featureDataArray = new double[numberOfObservations];
			List<Double> parsedData = entry.getValue();
			for (int i = 0; i < numberOfObservations; i++)
			{
				featureDataArray[i] = parsedData.get(i).doubleValue();
			}
			datasetToPredict.put(entry.getKey(), featureDataArray);
		}

		return new ImmutableTwoValues<Map<String, double[]>, Integer>(datasetToPredict, numberOfObservations);
	}

}
