package randomjyrest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessPredictionData
{

	public static final Map<String, double[]> main(String dataset, List<String> featuresToRemove)
	{
		// Setup the mapping to hold the temporary and final processed data.
		Map<String, List<Double>> temporaryData = new HashMap<String, List<Double>>();
		Map<String, double[]> datasetToPredict = new HashMap<String, double[]>();
		
		int numberOfObservations = 0;
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(dataset));
			String line = null;
			
			// Generate a mapping from the index of the column in the dataset to the name of the feature that the column contains values of.
			line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");
			String classFeatureColumnName = "Classification";
			
			// Initialise the mapping that holds the temporary processing of the data.
			List<Integer> featureIndicesToUse = new ArrayList<Integer>();  // The indices of the columns in the file which are not to be removed.
			int featureIndex = 0;
			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					;  // Ignore the class column
				}
				else if (!featuresToRemove.contains(feature))
				{
					// If the feature is not one to be removed from the dataset.
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

		return datasetToPredict;
	}

}
