package tree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDriver
{

	public static void main(String[] args)
	{
		String inputFileLocation = args[0];
		String crossValidationLocation = args[1];
		// A file containing the chosen subset of features as one line of 0s and 1s separated by commas. A 0 indicates that the feature is
		// absent, a 1 that it is in use.
		// Now it is a file with the name of a feature that is being used on each line.
		String chosenFeatureSubset = args[2];

		// Determine the number of features in the dataset.
		int numberFeatures = 0;
		String[] featureNames = null;
		try
		{
			BufferedReader featureReader = new BufferedReader(new FileReader(inputFileLocation));
			String header = featureReader.readLine();
			featureNames = header.split("\t");
			numberFeatures = featureNames.length - 1;  // Subtract one for the class column in the dataset.
			featureReader.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

		// Determine the number of features, and which ones are deemed to be unused.
		List<String> featuresIgnored = new ArrayList<String>();
		try
		{
			BufferedReader featureSubSetReader = new BufferedReader(new FileReader(chosenFeatureSubset));
			String featureLine = featureSubSetReader.readLine();
			featureLine.trim();
			String[] featureSubSet = featureLine.split(",");
			for (int i = 0; i < numberFeatures; i++)
			{
				int presence = Integer.parseInt(featureSubSet[i]);
				if (presence == 0)
				{
					featuresIgnored.add(featureNames[i]);
				}
			}
			featureSubSetReader.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.numberOfTreesToGrow = 500;
		ctrl.isReplacementUsed = true;
		ctrl.variablesToIgnore = featuresIgnored;

		// Get the cross validation information
		File inputCrossValDirectory = new File(crossValidationLocation);
		if (!inputCrossValDirectory.isDirectory())
		{
			System.out.println("The second argument must be the location of the directory containing the cross validation files.");
			System.exit(0);
		}
		String subDirs[] = inputCrossValDirectory.list();
		String crossValDirLoc = inputCrossValDirectory.getAbsolutePath();
		List<List<Object>> subsetFeaturCrossValFiles = new ArrayList<List<Object>>();
		for (String s : subDirs)
		{
			List<Object> subsetFeatureTrainTestLocs = new ArrayList<Object>();
			subsetFeatureTrainTestLocs.add(crossValDirLoc + "/" + s + "/Train.txt");
			subsetFeatureTrainTestLocs.add(new ProcessDataForGrowing(crossValDirLoc + "/" + s + "/Test.txt", ctrl));
			subsetFeaturCrossValFiles.add(subsetFeatureTrainTestLocs);
		}

		System.out.println(featuresIgnored);
		Map<String, Double> weights = determineWeights(inputFileLocation, ctrl);
		long seed = 6262139609547888975L;
		Forest forest;
		double cumulativeError = 0.0;
    	for (List<Object> l : subsetFeaturCrossValFiles)
    	{
    		forest = new Forest((String) l.get(0), ctrl, weights, seed);
    		cumulativeError += forest.predict((ProcessDataForGrowing) l.get(1)).first;
    	}
    	cumulativeError /= subsetFeaturCrossValFiles.size();
    	System.out.println(cumulativeError);
		forest = new Forest(args[0], ctrl, weights, seed);
		System.out.format("The OOB error estimate is : %f\n", forest.oobErrorEstimate);
	}

	/**
	 * Determine the weighting of each class as its proportion of the total number of observations.
	 * 
	 * @param inputLocation
	 * @return
	 */
	static Map<String, Double> determineWeights(String inputLocation, TreeGrowthControl ctrl)
	{
		ProcessDataForGrowing procData = new ProcessDataForGrowing(inputLocation, ctrl);

		// Determine how often each class occurs.
		Map<String, Double> classCounts = new HashMap<String, Double>();
		for (String s : procData.responseData)
		{
			if (!classCounts.containsKey(s))
			{
				classCounts.put(s, 1.0);
			}
			else
			{
				classCounts.put(s, classCounts.get(s) + 1.0);
			}
		}

		// Find the number of occurrences of the class that occurs most often.
		double maxClass = 0.0;
		for (String s : classCounts.keySet())
		{
			if (classCounts.get(s) > maxClass)
			{
				maxClass = classCounts.get(s);
			}
		}

		// Determine the weighting of each class in relation to the class that occurs most often.
		// Weights the most frequent class as 1.
		// Two classes, A occurs 10 times and B 5 times. A gets a weight of 1 / (10 / 10) == 1.
		// B gets a weight of 1 / (5 / 10) == 2.
		Map<String, Double> classWeights = new HashMap<String, Double>();
		for (String s : classCounts.keySet())
		{
			classWeights.put(s, 1.0 / (classCounts.get(s) / maxClass));
		}

		return classWeights;
	}

}
