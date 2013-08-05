package randomjyrest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetermineObservationProperties
{
	
	/**
	 * @param inputFile
	 * @return
	 */
	public static final List<String> determineObservationClasses(String inputFile)
	{
		List<String> classOfObservations = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(inputFile));
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
				System.out.println("An error occurred while closing the file used to determine the class of each observation.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		return classOfObservations;
	}
	

	/**
	 * @param observationClasses
	 * @param positiveClass
	 * @param positiveWeight
	 * @param unlabelledClass
	 * @param unlabelledWeight
	 * @return
	 */
	public static final double[] determineObservationWeights(List<String> observationClasses, String positiveClass, double positiveWeight,
			String unlabelledClass, double unlabelledWeight)
	{
		int numberOfObservations = observationClasses.size();
		double[] weights = new double[numberOfObservations];
		
		for (int i = 0; i < numberOfObservations; i++)
		{
			String classOfObs = observationClasses.get(i);
			if (classOfObs.equals(positiveClass))
			{
				weights[i] = positiveWeight;
			}
			else
			{
				weights[i] = unlabelledWeight;
			}
		}
		
		return weights;
	}

}
