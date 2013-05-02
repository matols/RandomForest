/**
 * 
 */
package learn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * @author Simon Bull
 *
 */
public class CovarianceCalculator
{

	List<Integer> clusterObservations;  // A list of the integer indices of the observations in the cluster for which the mean vector and covariance matrix are to be learned.
	List<Integer> nonClusterObservations;  // A list of the integer indices of the observations not in the cluster.
	RealMatrix covarianceMatrix;  // The covariance matrix of the observations in the cluster.
	RealMatrix meanVector;  // The mean vector of the observations in the cluster.
	TreeGrowthControl ctrl;  // The controller object for processing the input dataset.
	ProcessDataForGrowing processedData;  // The object containing the information about the processed dataset.
	int numberClusterObservations;  // The number of observations in the cluster.
	List<String> covariableOrdering;
	int numberOfCovariables;  // The number of covariables in the dataset used in the covariablesUsed array.

	/**
	 * The cluster observations default to the positive observations.
	 * 
	 * @param dataForLearning
	 */
	CovarianceCalculator(String dataForLearning, TreeGrowthControl ctrl)
	{

		this.ctrl = ctrl;
		this.processedData = new ProcessDataForGrowing(dataForLearning, this.ctrl);
		this.covariableOrdering = new ArrayList<String>(this.processedData.covariableData.keySet());
		this.clusterObservations = new ArrayList<Integer>();
		this.nonClusterObservations = new ArrayList<Integer>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			if (this.processedData.responseData.get(i).equals("Positive"))
			{
				// If the observation is in the 'Positive' class.
				this.clusterObservations.add(i);
			}
			else
			{
				// If the observation is in the 'Unlabelled' class.
				this.nonClusterObservations.add(i);
			}
		}
		this.numberClusterObservations = this.clusterObservations.size();
		calculateCovariance();

	}

	CovarianceCalculator(String dataForLearning, TreeGrowthControl ctrl, List<Integer> clusterObservations)
	{

		this.ctrl = ctrl;
		this.processedData = new ProcessDataForGrowing(dataForLearning, this.ctrl);
		this.covariableOrdering = new ArrayList<String>(this.processedData.covariableData.keySet());
		this.clusterObservations = clusterObservations;
		this.nonClusterObservations = new ArrayList<Integer>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			if (!this.clusterObservations.contains(i))
			{
				// If the observation is not a member of the cluster.
				this.nonClusterObservations.add(i);
			}
		}
		this.numberClusterObservations = this.clusterObservations.size();
		calculateCovariance();

	}

	/**
	 * Calculate the covariance matrix for the observations in the cluster.
	 */
	void calculateCovariance()
	{
		this.numberOfCovariables = this.processedData.covariableData.size();
		double expectedCovariableValues[] = new double[this.numberOfCovariables];
		// Go through each covariable, and calculate the expected value of the variable on the
		// 'Positive' observations.
		for (int i = 0; i < this.numberOfCovariables; i++)
		{
			String currentCovar = covariableOrdering.get(i);
			double expectedValue = 0.0;
			for (Integer j : this.clusterObservations)
			{
				expectedValue += this.processedData.covariableData.get(currentCovar).get(j);
			}
			expectedValue /= this.numberClusterObservations;
			expectedCovariableValues[i] = expectedValue;
		}
		this.meanVector = new Array2DRowRealMatrix(expectedCovariableValues);

		// Calculate the variance of the variables.
		double calculatedCovariances[][] = new double[this.numberOfCovariables][this.numberOfCovariables];
		for (int i = 0; i < this.numberOfCovariables; i++)
		{
			String iCovar = this.covariableOrdering.get(i);
				
			double runningSummation = 0;
			for (Integer k : this.clusterObservations)
			{
				double iValue = (double) this.processedData.covariableData.get(iCovar).get(k);
				runningSummation += Math.pow(iValue - this.meanVector.getEntry(i, 0), 2);
			}
			runningSummation /= numberClusterObservations;
			calculatedCovariances[i][i] = runningSummation;
		}
		this.covarianceMatrix = new Array2DRowRealMatrix(calculatedCovariances);

		// Correct the list of covariables used, number used, mean vector and covariance matrix
		// in order to remove covariables which have a variance of 0.
		List<String> tempCovUsed = new ArrayList<String>();
		List<Integer> nonZeroVarIndices = new ArrayList<Integer>();
		for (int i = 0; i < this.numberOfCovariables; i++)
		{
			if (this.covarianceMatrix.getEntry(i, i) != 0.0)
			{
				// If the variance of the covariable is not 0.
				nonZeroVarIndices.add(i);
				tempCovUsed.add(this.covariableOrdering.get(i));
			}
		}
		double tempExp[] = new double[nonZeroVarIndices.size()];
		for (int i = 0; i < nonZeroVarIndices.size(); i++)
		{
			int indexOfValue = nonZeroVarIndices.get(i);
			tempExp[i] = expectedCovariableValues[indexOfValue];
		}
		this.covariableOrdering = tempCovUsed;
		this.numberOfCovariables = this.covariableOrdering.size();
		this.meanVector = new Array2DRowRealMatrix(tempExp);
		int indicesToTake[] = new int[nonZeroVarIndices.size()];
		for (int i = 0; i < nonZeroVarIndices.size(); i++)
		{
			indicesToTake[i] = nonZeroVarIndices.get(i);
		}
		this.covarianceMatrix = this.covarianceMatrix.getSubMatrix(indicesToTake, indicesToTake);

	}

	/**
	 * Determine the Mahalanobis distance of the points from the cluster represented by the
	 * covariance matrix.
	 * 
	 * distance(x) = sqrt((x - mu)T * cov^-1 * (x - mu))
	 * where mu is the mean vector and cov is the covariance matrix.
	 * 
	 * @param dataPoints A list of the indices of the observations for which the Mahalanobis distance should be calculated.
	 * @return A mapping from the data points (as integer index) to the Mahalanobis distance that the data point is from the cluster centre.
	 */
	Map<Integer, Double> distanceMahalanobis()
	{
		return distanceMahalanobis(this.nonClusterObservations);
	}

	Map<Integer, Double> distanceMahalanobis(List<Integer> dataPoints)
	{
		Map<Integer, Double> returnValue = new HashMap<Integer, Double>();

		// Determine the inverse covariance matrix.
		RealMatrix inverseCovarMatrix = new LUDecomposition(this.covarianceMatrix).getSolver().getInverse();

		for (Integer i : dataPoints)
		{
			// Get the data vector for the current observation index. Have to loop through all the covariables in covariablesUsed as some covariables
			// may have been removed from the mean vector and covariance matrix, and are therefore not used to describe the cluster.
			// Covariables would have been removed if their variance was 0, as this would make the covariance matrix singular
			// and not invertible.
			double obsData[] = new double[this.numberOfCovariables];
			for (int j = 0; j < this.numberOfCovariables; j++)
			{
				String currentCovar = this.covariableOrdering.get(j);
				// If the variable is numeric.
				obsData[j] = this.processedData.covariableData.get(currentCovar).get(i);
			}
			RealMatrix dataVector = new Array2DRowRealMatrix(obsData);
			RealMatrix xMinusMu = dataVector.subtract(this.meanVector);
			RealMatrix squaredMahalanobisDistance = ((xMinusMu.transpose()).multiply(inverseCovarMatrix)).multiply(xMinusMu);
			returnValue.put(i, squaredMahalanobisDistance.getEntry(0, 0));
		}
		return returnValue;
	}

	/**
	 * Determine the Mahalanobis distance of the data points provided from each other.
	 * 
	 * This method assumes that all the data points provided come from the cluster (represented by the mean vector and covariance
	 * matrix) that was learnt previously. If the data points do not come from the cluster, this distance measure has no meaning.
	 * 
	 * distance(x) = sqrt((x - y)T * cov^-1 * (x - y))
	 * where cov is the covariance matrix and x and y are two data points from the cluster
	 * 
	 * @param dataPoints A list of the indices of the observations between whih the Mahalanobis distances should be calculated.
	 * @return A Map representation of a matrix such that the value at [i, j] is the distance from observation i to j.
	 */
	Map<Integer, Map<Integer, Double>> subsetMahalanobisDistance(List<Integer> dataPoints)
	{
		Map<Integer, Map<Integer, Double>> returnValue = new HashMap<Integer, Map<Integer, Double>>();

		// Determine the inverse covariance matrix.
		RealMatrix inverseCovarMatrix = new LUDecomposition(this.covarianceMatrix).getSolver().getInverse();

		for (Integer i : dataPoints)
		{
			returnValue.put(i, new HashMap<Integer, Double>());
			// Get the data vector for observation i. Have to loop through all the covariables in covariablesUsed as some covariables
			// may have been removed from the mean vector and covariance matrix, and are therefore not used to describe the cluster.
			// Covariables would have been removed if their variance was 0, as this would make the covariance matrix singular
			// and not invertible.
			double obsIData[] = new double[this.numberOfCovariables];
			for (int j = 0; j < this.numberOfCovariables; j++)
			{
				String currentCovar = this.covariableOrdering.get(j);
				// If the variable is numeric.
				obsIData[j] = (double) this.processedData.covariableData.get(currentCovar).get(i);
			}

			// Loop through all the data points provided and determine the distance between data point i and all others.
			for (Integer j : dataPoints)
			{
				if (i == j)
				{
					// If the data points are the same, then you can set the distance to 0;
					returnValue.get(i).put(i, 0.0);
				}
				else
				{
					// Get the data vector for observation j. Have to loop through all the covariables in covariablesUsed as some covariables
					// may have been removed from the mean vector and covariance matrix, and are therefore not used to describe the cluster.
					// Covariables would have been removed if their variance was 0, as this would make the covariance matrix singular
					// and not invertible.
					double obsJData[] = new double[this.numberOfCovariables];
					for (int k = 0; k < this.numberOfCovariables; k++)
					{
						String currentCovar = this.covariableOrdering.get(k);
						// If the variable is numeric.
						obsJData[k] = this.processedData.covariableData.get(currentCovar).get(j);
					}

					// Calculate the distance between observations i and j.
					RealMatrix dataVectorI = new Array2DRowRealMatrix(obsIData);
					RealMatrix dataVectorJ = new Array2DRowRealMatrix(obsJData);
					RealMatrix iMinusJ = dataVectorI.subtract(dataVectorJ);
					RealMatrix squaredMahalanobisDistance = ((iMinusJ.transpose()).multiply(inverseCovarMatrix)).multiply(iMinusJ);
					returnValue.get(i).put(j, squaredMahalanobisDistance.getEntry(0, 0));
				}
			}
		}
		return returnValue;
	}
}
