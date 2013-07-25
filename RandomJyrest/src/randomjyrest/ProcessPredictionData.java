package randomjyrest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessPredictionData
{

	public static final Map<Integer, Map<String, Double>> main(String dataset, List<String> featuresToRemove)
	{
		Map<Integer, Map<String, Double>> datasetToPredict = new HashMap<Integer, Map<String, Double>>();
		
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
				}
				featureIndex += 1;
			}
			
			int observationIndex = 0;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				
				// Enter the feature values for this observation into the mapping of the temporary processing of the data.
				Map<String, Double> observationMapping = new HashMap<String, Double>();
				String[] chunks = line.split("\t");
				for (Integer i : featureIndicesToUse)
				{
					String feature = featureNames[i];
					double value = Double.parseDouble(chunks[i]);
					observationMapping.put(feature, value);
				}
				datasetToPredict.put(observationIndex, observationMapping);
				observationIndex++;
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while generating the processed prediction data.");
			e.printStackTrace();
			System.exit(0);
		}
		
		return datasetToPredict;
	}

}
