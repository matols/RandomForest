package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PermuteData
{

	/**
	 * Generate a permutation of the prediction data.
	 * 
	 * The permutation is made for one set of observation indices and one feature.
	 * 
	 * Example:
	 * 		Original data is			[a, b, c, d, e]
	 * 		oobOnThisTree is			[0, 1, 3, 5, 8]
	 * 		permutedOobOnThisTree is	[5, 0, 3, 8, 1]
	 * 		Permuted data is			[d, a, c, e, b]
	 * 
	 * @param dataset
	 * @param observationsToPermute
	 * @param featuresRemoved
	 * @param featureToPermute
	 * @return
	 */
	public static final double[] main(String dataset, Set<Integer> observationsToPermute, List<String> featuresRemoved,
			String featureToPermute, double[] originalFeatureValues)
	{
		// Generate the permutation of the OOB observation indices. The permuting will take place so that the value of the observation
		// with index oobOnThisTree.get(i) is set to the value that was at permutedOobOnThisTree.get(i).
		List<Integer> oobOnThisTree = new ArrayList<Integer>(observationsToPermute);
		List<Integer> permutedOobOnThisTree = new ArrayList<Integer>(observationsToPermute);
		Collections.shuffle(permutedOobOnThisTree);
		int numberOfOOBObservations = oobOnThisTree.size();
		
		// Make a copy of the data for the feature that is to be permuted.
		double[] permutedFeatureData = new double[originalFeatureValues.length];
		for (int i = 0; i < originalFeatureValues.length; i++)
		{
			permutedFeatureData[i] = originalFeatureValues[i];
		}
		
		// Permute the data.
		for (int i = 0; i < numberOfOOBObservations; i++)
		{
			int observationIndex = oobOnThisTree.get(i);
			double permutedValue = permutedFeatureData[permutedOobOnThisTree.get(i)];
			permutedFeatureData[observationIndex] = permutedValue;
		}

		return permutedFeatureData;
	}

}
