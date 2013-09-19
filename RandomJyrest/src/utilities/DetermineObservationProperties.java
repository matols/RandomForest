package utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class DetermineObservationProperties
{
	
	/**
	 * @param dataset
	 * @param accessionColumnName
	 * @param classFeatureColumnName
	 */
	public static final ImmutableTwoValues<List<String>, List<String>> determineAccessionsAndClasses(String dataset,
			String accessionColumnName, String classFeatureColumnName)
	{
		List<String> proteinAccessions = new ArrayList<String>();
		List<String> proteinClasses = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(dataset));
			
			// Determine the class and accession column indices.
			String[] features = reader.readLine().trim().split("\t");
			int classColumnIndex = -1;
			int accessionColumnIndex = -1;
			for (int i = 0; i < features.length; i++)
			{
				String feature = features[i];
				if (feature.equals(classFeatureColumnName))
				{
					classColumnIndex = i;
				}
				else if (feature.equals(accessionColumnName))
				{
					accessionColumnIndex = i;
				}
			}
			
			if (classColumnIndex == -1)
			{
				// No class column was provided.
				System.out.println("No class column was provided. Please include a column headed Classification.");
				System.exit(0);
			}
			else if (accessionColumnIndex == -1)
			{
				// No class column was provided.
				System.out.println("No UniProt accession column was provided. Please include a column headed UPAccession.");
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
				proteinClasses.add(splitLine[classColumnIndex]);
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("An error occurred while determining the accessions of the training dataset proteins.");
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
				System.out.println("An error occurred while closing the training dataset.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		return new ImmutableTwoValues<List<String>, List<String>>(proteinAccessions, proteinClasses);
	}
	
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
	 * Calculate the vector of weights for the observations in a dataset.
	 * 
	 * The vector of weights will contain the weights of the observations in the same order that they appear in the dataset.
	 * The vector at index i therefore contains the weight of the ith observation. The ordering is determined by the list of
	 * the classes of the observations that is supplied.
	 * 
	 * @param observationClasses	The classes of the observations in the dataset.
	 * @param positiveClass			The name of the positive class.
	 * @param positiveWeight		The weight of the positive class.
	 * @param unlabelledClass		The name of the unlabelled class.
	 * @param unlabelledWeight		The weight of the unlabelled class.
	 * @return						An array of the observation weights ordered as the observationClasses are ordered.
	 */
	public static final double[] determineObservationWeights(List<String> observationClasses, String positiveClass, double positiveWeight,
			String unlabelledClass, double unlabelledWeight)
	{
		int numberOfObservations = observationClasses.size();  // Determine the total number of observations.
		double[] weights = new double[numberOfObservations];  // Initialise the weight vector to contain one entry for each observation.
		
		// For each observation, determine its class, and then its weight.
		for (int i = 0; i < numberOfObservations; i++)
		{
			String classOfObs = observationClasses.get(i);  // The class of the ith observation is the ith entry in observationClasses.
			if (classOfObs.equals(positiveClass))
			{
				// If the class of the observation is positive, then the weight of the observation is the positive class weight.
				weights[i] = positiveWeight;
			}
			else
			{
				// If the class of the observation is unlabelled, then the weight of the observation is the unlabelled class weight.
				weights[i] = unlabelledWeight;
			}
		}
		
		return weights;
	}

}
