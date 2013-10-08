package randomjyrest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PermuteData
{

	/**
	 * Generate a permutation of a feature's observation data.
	 * 
	 * Example:
	 * 		Original data is			[a, b, c, d, e] (featureValues)
	 * 		Indices to permute is		[1, 3, 4]		(observationsToPermute)
	 * 		Re-ordered indices are		[4, 3, 1]
	 * 		Permuted data is			[a, e, c, d, b]	(return)
	 * 
	 * @param observationsToPermute		The indices of the features values that should be permuted.
	 * @param featureValues				The feature values.
	 * @return							An array of the permuted observation data for the feature.
	 */
	public static final double[] main(Set<Integer> observationsToPermute, double[] featureValues)
	{
		// Permute the indices.
		List<Integer> originalIndices = new ArrayList<Integer>(observationsToPermute);
		List<Integer> permutedIndices = new ArrayList<Integer>(observationsToPermute);
		Collections.shuffle(permutedIndices);
		
		// Make a copy of the data that is to be permuted.
		double[] permutedFeatureData = new double[featureValues.length];
		for (int i = 0; i < featureValues.length; i++)
		{
			permutedFeatureData[i] = featureValues[i];
		}
		
		// Permute the data.
		int numberOfObservations = originalIndices.size();
		for (int i = 0; i < numberOfObservations; i++)
		{
			int observationIndex = originalIndices.get(i);
			double permutedValue = permutedFeatureData[permutedIndices.get(i)];
			permutedFeatureData[observationIndex] = permutedValue;
		}

		return permutedFeatureData;
	}

}
