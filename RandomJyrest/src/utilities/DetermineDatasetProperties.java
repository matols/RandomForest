package utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DetermineDatasetProperties
{
	
	/**
	 * 
	 * returns the features ordered in the order they appear in the header line of the dataset file
	 * 
	 * @param dataset
	 * @param feauresToIgnore
	 * @return
	 */
	public static final List<String> determineDatasetFeatures(String dataset, List<String> feauresToIgnore)
	{
		String classFeatureColumnName = "Classification";
		return determineDatasetFeatures(dataset, feauresToIgnore, classFeatureColumnName);
	}
	
	/**
	 * 
	 * returns the features ordered in the order they appear in the header line of the dataset file
	 * 
	 * @param dataset
	 * @param feauresToIgnore
	 * @return
	 */
	public static final List<String> determineDatasetFeatures(String dataset, List<String> feauresToIgnore, String classFeatureColumnName)
	{
		List<String> featuresInDataset = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(dataset));
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");

			for (String feature : featureNames)
			{
				if (feature.equals(classFeatureColumnName))
				{
					// Ignore the class column.
					;
				}
				else if (!feauresToIgnore.contains(feature))
				{
					featuresInDataset.add(feature);
				}
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while determining the features to use.");
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
		
		return featuresInDataset;
	}
	

	/**
	 * @param dataset
	 * @param accessionColumnName
	 * @return
	 */
	public static final List<String> determineObservationAccessions(String dataset)
	{
		String accessionColumnName = "UPAccession";
		return determineObservationAccessions(dataset, accessionColumnName);
	}
	
	/**
	 * @param dataset
	 * @param accessionColumnName
	 * @return
	 */
	public static final List<String> determineObservationAccessions(String dataset, String accessionColumnName)
	{
		List<String> proteinAccessions = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(dataset));
			
			// Determine the class and accession column indices.
			String[] features = reader.readLine().trim().split("\t");
			int accessionColumnIndex = -1;
			for (int i = 0; i < features.length; i++)
			{
				String feature = features[i];
				if (feature.equals(accessionColumnName))
				{
					accessionColumnIndex = i;
				}
			}
			
			if (accessionColumnIndex == -1)
			{
				// No acession column was provided.
				System.out.format("The accession column %s could not be found in the dataset header line. Please include a column with" +
						" this name, or correct the name of the accession column\n", accessionColumnName);
				System.exit(0);
			}
			
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.trim();
				String[] splitLine = line.split("\t");
				proteinAccessions.add(splitLine[accessionColumnIndex]);
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while determining the accessions.");
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
				System.out.println("An error occurred while closing the dataset file.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		return proteinAccessions;
	}
	
	
	/**
	 * @param inputFile
	 * @return
	 */
	public static final List<String> determineObservationClasses(String dataset)
	{
		String classFeatureColumnName = "Classification";
		return determineObservationClasses(dataset, classFeatureColumnName);
	}
	
	/**
	 * @param inputFile
	 * @return
	 */
	public static final List<String> determineObservationClasses(String dataset, String classFeatureColumnName)
	{
		List<String> classOfObservations = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(dataset));
			String line = null;

			// Determine the column that contains the class data.
			line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] featureNames = line.split("\t");
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
	 * Calculate the vector of weights for the observations in a dataset.
	 * 
	 * The vector of weights will contain the weights of the observations in the same order that they appear in the dataset.
	 * The vector at index i therefore contains the weight of the ith observation.
	 * 
	 * @param dataset				The location of the file containing the dataset.
	 * @param classWeights			A mapping between class names and class weights.
	 * @return						An array of the observation weights ordered as the observationClasses are ordered.
	 */
	public static final double[] determineObservationWeights(String dataset, Map<String, Double> classWeights)
	{
		List<String> observationClasses = determineObservationClasses(dataset);
		int numberOfObservations = observationClasses.size();  // Determine the total number of observations.
		double[] weights = new double[numberOfObservations];  // Initialise the weight vector to contain one entry for each observation.
		
		// For each observation, determine its class, and then its weight.
		for (int i = 0; i < numberOfObservations; i++)
		{
			String classOfObs = observationClasses.get(i);  // The class of the ith observation is the ith entry in observationClasses.
			weights[i] = classWeights.get(classOfObs);  // Set the weight of the observation to the weight of its class.
		}
		
		return weights;
	}

}
