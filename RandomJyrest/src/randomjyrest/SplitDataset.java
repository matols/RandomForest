package randomjyrest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import utilities.ImmutableFourValues;

public class SplitDataset
{

	public static final ImmutableFourValues<Map<String, double[]>, Map<String, int[]>, Map<String, double[]>, Map<String, int[]>>
		main(Map<String, double[]> dataset, Map<String, int[]> dataIndices, String featureToSplitOn, double splitValue)
	{
		// Determine the indices of the observations that are going to the left hand and right hand children.
		// Use a set and counter rather than a list (which would permit the potential duplicates) as hashsets allow
		// for much faster checking for presence in the collection.
		double[] dataOfSplitFeature = dataset.get(featureToSplitOn);
		int[] indicesOfSplitFeature = dataIndices.get(featureToSplitOn);
		int numberOfObservationsInNode = dataOfSplitFeature.length;
		Set<Integer> leftChildObservationIndices = new HashSet<Integer>();
		int numberOfObservationsLeftChild = 0;
		for (int i = 0; i < numberOfObservationsInNode; i++)
		{
			int observationIndex = indicesOfSplitFeature[i];
			if (dataOfSplitFeature[i] <= splitValue)
			{
				leftChildObservationIndices.add(observationIndex);
				numberOfObservationsLeftChild++;
			}
		}
		int numberOfObservationsRightChild = numberOfObservationsInNode - numberOfObservationsLeftChild;
		
		// Setup the dataset for the left and right child.
		Map<String, double[]> datasetLeftChild = new HashMap<String, double[]>();
		Map<String, int[]> dataIndicesLeftChild = new HashMap<String, int[]>();
		Map<String, double[]> datasetRightChild = new HashMap<String, double[]>();
		Map<String, int[]> dataIndicesRightChild = new HashMap<String, int[]>();
		
		// Generate the datasets.
		Set<String> featuresInDataset = new HashSet<String>(dataset.keySet());
		for (String f : featuresInDataset)
		{
			double[] leftChildData = new double[numberOfObservationsLeftChild];
			int[] leftChildIndices = new int[numberOfObservationsLeftChild];
			double[] rightChildData = new double[numberOfObservationsRightChild];
			int[] rightChildIndices = new int[numberOfObservationsRightChild];
			
			double[] currentNodeData = dataset.get(f);
			int[] currentNodeIndices = dataIndices.get(f);
			
			int currentInsertionIndexLeftChild = 0;
			int currentInsertionIndexRightChild = 0;
			for (int i = 0; i < numberOfObservationsInNode; i++)
			{
				double featureValue = currentNodeData[i];
				int observationIndex = currentNodeIndices[i];
				if (leftChildObservationIndices.contains(observationIndex))
				{
					leftChildData[currentInsertionIndexLeftChild] = featureValue;
					leftChildIndices[currentInsertionIndexLeftChild] = observationIndex;
					currentInsertionIndexLeftChild++;
				}
				else
				{
					rightChildData[currentInsertionIndexRightChild] = featureValue;
					rightChildIndices[currentInsertionIndexRightChild] = observationIndex;
					currentInsertionIndexRightChild++;
				}
			}
			
			// Check if the values of the feature f in either child are all identical. If this is the case then don't add the
			// data for the feature to the child's dataset.
			// However, this may cause there to be no data in the dataset for a child (for example if there is only one observation
			// going to one of the children). To prevent this crashing the tree growth process, always record the indices of the
			// observations going to a node. This is acceptable as all the manipulation in determining splits and termination
			// conditions relies on the data of the observations not the indices, and as long as a check is made for the number
			// of features in the data value map before each iteration of growth.
			dataIndicesLeftChild.put(f, leftChildIndices);
			dataIndicesRightChild.put(f, rightChildIndices);
			if (leftChildData[0] != leftChildData[numberOfObservationsLeftChild - 1])
			{
				datasetLeftChild.put(f, leftChildData);
			}
			if (rightChildData[0] != rightChildData[numberOfObservationsRightChild - 1])
			{
				datasetRightChild.put(f, rightChildData);
			}
		}
		
		return new ImmutableFourValues<Map<String, double[]>, Map<String, int[]>, Map<String, double[]>, Map<String, int[]>>
			(datasetLeftChild, dataIndicesLeftChild, datasetRightChild, dataIndicesRightChild);
	}

}
