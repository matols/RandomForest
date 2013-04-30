/**
 * 
 */
package learn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.Forest;
import tree.IndexedDoubleData;
import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class Learner
{

	String dataForLearning;  // The location of the file that contains the dataset of positive and unlabelled data.
	TreeGrowthControl ctrl;  // The controller object for growing the trees/forest.

	Learner(String dataForLearning)
	{
		this.dataForLearning = dataForLearning;
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.minCriterion = -Double.MAX_VALUE;
		ctrl.isClassificationUsed = true;
		ctrl.isProbabilisticPrediction = true;
		this.ctrl = ctrl;
	}

	Learner(String dataForLearning, TreeGrowthControl ctrl)
	{
		this.dataForLearning = dataForLearning;
		this.ctrl = ctrl;
	}

	/**
	 * @param maxNumberNeighbours The maximum number of neighbours to find for each observation.
	 * @param numberOfIterationsToPerform The number of times to iterate the graph probability update method.
	 * @return
	 */
	List<Integer> learnNegativeSet(int maxNumberNeighbours, int numberOfIterationsToPerform)
	{

		// Calculate the Mahalanobis distance of every non-positive observation from the positive cluster, and sort them in ascending order.
		CovarianceCalculator positiveCovCalc = new CovarianceCalculator(this.dataForLearning);
		Map<Integer, Double> positiveMahalanobisDistance = positiveCovCalc.distanceMahalanobis();
		List<IndexedDoubleData> sortedPositiveDistances = new ArrayList<IndexedDoubleData>();
		double averageDistance = 0.0;
		for (Integer i : positiveMahalanobisDistance.keySet())
		{
			averageDistance += positiveMahalanobisDistance.get(i);
			sortedPositiveDistances.add(new IndexedDoubleData(positiveMahalanobisDistance.get(i), i));
		}
		averageDistance /= positiveMahalanobisDistance.size();
		Collections.sort(sortedPositiveDistances);

		// The reliable negative set is all the observations with a Mahalanobis distance greater than the mean distance.
		List<Integer> reliableNegativeSet = new ArrayList<Integer>();
		for (int i = 0; i < sortedPositiveDistances.size(); i++)
		{
			if (sortedPositiveDistances.get(i).getData() > averageDistance)
			{
				reliableNegativeSet.add(sortedPositiveDistances.get(i).getIndex());
			}
		}

		// Calculate the Mahalanobis distance of every non-negative observation from the reliable negative cluster, and sort them in ascending order.
		CovarianceCalculator negativeCovCalc = new CovarianceCalculator(this.dataForLearning, reliableNegativeSet);
		Map<Integer, Double> negativeMahalanobisDistance = negativeCovCalc.distanceMahalanobis();
		List<IndexedDoubleData> sortedNegativeDistances = new ArrayList<IndexedDoubleData>();
		for (Integer i : negativeMahalanobisDistance.keySet())
		{
			sortedNegativeDistances.add(new IndexedDoubleData(negativeMahalanobisDistance.get(i), i));
		}

		// Create subsets of observations for use in the distance calculations.
		List<Integer> positiveSet = positiveCovCalc.clusterObservations;  // The positive observations in the dataset.
		List<Integer> unlabelledObservations = positiveCovCalc.nonClusterObservations;  // The unlalled observations in the dataset.
		List<Integer> unionPAndU = new ArrayList<Integer>();  // The union of all positive and unlabelled observation (i.e. every observation in the dataset).
		unionPAndU.addAll(positiveSet);
		unionPAndU.addAll(unlabelledObservations);
		List<Integer> unionPAndUMinusRN = new ArrayList<Integer>(unionPAndU);  // All observation in the dataset minus the reliable negative ones.
		unionPAndUMinusRN.removeAll(reliableNegativeSet);

		//*****************************************
		// Calculate distances.
		//*****************************************
		// Determine the distance between each observation in unionPAndU and all the others.
		// This calculation assumes that all observations are really from the positive cluster, just that the unlabelled ones have not been
		// marked as being so (and that the reliably negative ones are marked incorrectly).
		Map<Integer, Map<Integer, Double>> positiveClusterAllDistances = positiveCovCalc.subsetMahalanobisDistance(unionPAndU);

		// Determine the distance between each observation in unionPAndU and all the others.
		// This calculation assumes that all observations are really from the negative cluster, just that the unlabelled ones have not been
		// marked as being so (and that the positive ones are marked incorrectly).
		Map<Integer, Map<Integer, Double>> negativeClusterAllDistances = negativeCovCalc.subsetMahalanobisDistance(unionPAndU);

		// Determine the distance between each observation in unionPAndUMinusRN and all the others.
		// This calculation assumes that all observations that are not reliably negative are really from the positive cluster,
		// just that the non-reliably negative unlabelled ones have not been marked as being so.
		Map<Integer, Map<Integer, Double>> positiveClusterMinusRNDistances = positiveCovCalc.subsetMahalanobisDistance(unionPAndUMinusRN);

		// Determine the distance between each unlabelled observation and all the others.
		// This calculation assumes that all observations are really from the negative cluster, just that the non-reliably negative ones
		// have not been marked as being so.
		Map<Integer, Map<Integer, Double>> negativeClusterUDistances = negativeCovCalc.subsetMahalanobisDistance(unlabelledObservations);

		//*****************************************
		// Calculate graph structures.
		//*****************************************
		// Determine the directed graph structures for the distance matrices for each of the observation subsets. The graph is represented
		// as a mapping of an observation index to its neighbours, along with the weight on the edge. For each observation the
		// maxNumberNeighbours nearest observations will be taken to be the neighbours of the observation.

		// Calculate the graph for the distances of all the observations from the positive cluster.
		Map<Integer, Map<Integer, Double>> positiveClusterAllGraph = initialiseGraph(unionPAndU, positiveClusterAllDistances, maxNumberNeighbours);

		// Calculate the graph for the distances of all the observations from the negative cluster.
		Map<Integer, Map<Integer, Double>> negativeClusterAllGraph = initialiseGraph(unionPAndU, negativeClusterAllDistances, maxNumberNeighbours);

		// Calculate the graph for the distances of the non-reliably negative observations from the positive cluster.
		Map<Integer, Map<Integer, Double>> positiveClusterMinusRNGraph = initialiseGraph(unionPAndUMinusRN, positiveClusterMinusRNDistances, maxNumberNeighbours);

		// Calculate the graph for the distances of the unlabelled observations from the negative cluster.
		Map<Integer, Map<Integer, Double>> negativeClusterUGraph = initialiseGraph(unlabelledObservations, negativeClusterUDistances, maxNumberNeighbours);

		//*****************************************************************
		// Calculate the initial probabilities for the observations.
		//*****************************************************************
		// Initialise the array of probabilities for the graph created from the distances of all the observations
		// from the positive cluster. For observations from the set of positive observations the probability of being
		// from the positive cluster is set to 1. For observations in the reliable negative set the probability of being
		// from the positive cluster is set to 0. For all other observations the probability is set to 0.5 (no information
		// about the positive or negative nature of the observation).
		Map<Integer, Double> positiveClusterAllProbabilities = initiliaseProbabilities(unionPAndU, positiveSet, reliableNegativeSet);

		// Initialise the array of probabilities for the graph created from the distances of all the observations
		// from the negative cluster. For observations from the set of reliable negative observations, the probability of being
		// from the reliable negative cluster is set to 1. For observations in the set of positive observations the probability of
		// being from the reliable negative cluster is set to 0. For all other observations the probability is set to 0.5 (no information
		// about the positive or negative nature of the observation).
		Map<Integer, Double> negativeClusterAllProbabilities = initiliaseProbabilities(unionPAndU, reliableNegativeSet, positiveSet);

		// Initialise the array of probabilities for the graph created from the distances of all non-reliably negative
		// observations from the positive cluster. For observations from the set of positive observations, the probability of being
		// from the positive cluster is set to 1. For all other observations the probability is set to 0.5 (no information
		// about the positive or negative nature of the observation).
		Map<Integer, Double> positiveClusterMinusRNProbabilities = initiliaseProbabilities(unionPAndUMinusRN, positiveSet);

		// Initialise the array of probabilities for the graph created from the distances of all of the unlabelled
		// observations from the negative cluster. For observations from the set of reliable negative observations, the probability of being
		// from the reliable negative cluster is set to 1. For all other observations the probability is set to 0.5 (no information
		// about the positive or negative nature of the observation).
		Map<Integer, Double> negativeClusterUProbabilities = initiliaseProbabilities(unlabelledObservations, reliableNegativeSet);

		//*****************************************************************
		// Calculate the final probabilities for the observations.
		//*****************************************************************
		for (int i = 0; i < numberOfIterationsToPerform; i++)
		{
			positiveClusterAllProbabilities = updateProbabilities(positiveClusterAllProbabilities, positiveClusterAllGraph);
			negativeClusterAllProbabilities = updateProbabilities(negativeClusterAllProbabilities, negativeClusterAllGraph);
			positiveClusterMinusRNProbabilities = updateProbabilities(positiveClusterMinusRNProbabilities, positiveClusterMinusRNGraph);
			negativeClusterUProbabilities = updateProbabilities(negativeClusterUProbabilities, negativeClusterUGraph);
		}

		return reliableNegativeSet;

	}

	/**
	 * Creates a graph using the Mahalanobis distance between the observations.
	 * 
	 * @param dataset The indices of the observations in the dataset.
	 * @param distanceMapping The distances between all the pairs of observations in the dataset.
	 * @param maxNumberNeighbours The maximum number of neighbours to find for each observation.
	 * @return A structure that contains, for every observation index, a mapping of the observation's neighbours and the weight on the edge
	 * 		   going to that neighbour.
	 */
	Map<Integer, Map<Integer, Double>> initialiseGraph(List<Integer> dataset, Map<Integer, Map<Integer, Double>> distanceMapping,
			int maxNumberNeighbours)
	{
		Map<Integer, Map<Integer, Double>> graph = new HashMap<Integer, Map<Integer, Double>>();

		// Determine the mean distance for the maxNumberNeighbours nearest observations for all the observations in the dataset.
		double meanDistance = 0.0;
		for (Integer i : dataset)
		{
			List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
			for (Integer j : dataset)
			{
				if (i == j)
				{
					// Ignore the case where i == j, as this means the observation's distance from itself would be used.
					continue;
				}
				sortedDistances.add(new IndexedDoubleData(distanceMapping.get(i).get(j), j));
			}
			Collections.sort(sortedDistances);
			// Select the maxNumberNeighbours closet neighbours, and record them as observation i's neighbours. The distance to the
			// individual observations is treated as the preliminary weight for the edge.
			for (int j = 0; j < maxNumberNeighbours; j++)
			{
				double distance = sortedDistances.get(j).getData();
				meanDistance += distance;
			}
		}
		meanDistance /= maxNumberNeighbours * dataset.size();

		// Determine the neighbours for each observation, along with the global maximum distance between any two observations chosen to be neighbours.
		double maximumDistance = 0.0;
		for (Integer i : dataset)
		{
			Map<Integer, Double> observationINeighbours = new HashMap<Integer, Double>();
			List<IndexedDoubleData> sortedDistances = new ArrayList<IndexedDoubleData>();
			for (Integer j : dataset)
			{
				if (i == j)
				{
					// Ignore the case where i == j, as this means the observation's distance from itself would be used.
					continue;
				}
				sortedDistances.add(new IndexedDoubleData(distanceMapping.get(i).get(j), j));
			}
			Collections.sort(sortedDistances);
			// Select the maxNumberNeighbours closet neighbours, and record them as observation i's neighbours. The distance to the
			// individual observations is treated as the preliminary weight for the edge.
			for (int j = 0; j < maxNumberNeighbours; j++)
			{
				double distance = sortedDistances.get(j).getData();
				if (distance > meanDistance)
				{
					// If the distance is too large for the observations to be neighbours.
					break;
				}
				if (distance > maximumDistance)
				{
					maximumDistance = distance;
				}
				observationINeighbours.put(sortedDistances.get(j).getIndex(), distance);
			}
			graph.put(i, observationINeighbours);
		}

		// Modify the weights of the graph so that they are all scaled to fall between 0 and 1.
		for (Integer i : graph.keySet())
		{
			for (Integer j : graph.get(i).keySet())
			{
				graph.get(i).put(j, 1 - graph.get(i).get(j) / maximumDistance);
			}
		}

		return graph;
	}

	/**
	 * Initialise the probabilities for a graph of observations.
	 * 
	 * @param dataset The set of observations in the graph.
	 * @param fromCluster The observations that should have a probability of 1.
	 * @param notFromCluster The observations that should have a probability of 0.
	 * @return A mapping from the observations in the dataset to their probabilities.
	 */
	Map<Integer, Double> initiliaseProbabilities(List<Integer> dataset)
	{
		return initiliaseProbabilities(dataset, new ArrayList<Integer>(), new ArrayList<Integer>());
	}

	Map<Integer, Double> initiliaseProbabilities(List<Integer> dataset, List<Integer> fromCluster)
	{
		return initiliaseProbabilities(dataset, fromCluster, new ArrayList<Integer>());
	}

	Map<Integer, Double> initiliaseProbabilities(List<Integer> dataset, List<Integer> fromCluster, List<Integer> notFromCluster)
	{
		Map<Integer, Double> datasetProbabilities = new HashMap<Integer, Double>();
		double notFromClusterWeight =  -((double) fromCluster.size() / notFromCluster.size());
		for (Integer i : dataset)
		{
			if (fromCluster.contains(i))
			{
				datasetProbabilities.put(i, 1.0);
			}
			else if (notFromCluster.contains(i))
			{
				datasetProbabilities.put(i, notFromClusterWeight);
			}
			else
			{
				datasetProbabilities.put(i, 0.0);
			}
		}
		return datasetProbabilities;
	}

	/**
	 * Update the probabilities of the observations from time step t to t + 1.
	 * 
	 * @param probabilities The probabilities from time t.
	 * @param graph The structure of the graph.
	 * @return A mapping from the observation indices to the probabilities of the observations at time t + 1.
	 */
	Map<Integer, Double> updateProbabilities(Map<Integer, Double> probabilities, Map<Integer, Map<Integer, Double>> graph)
	{
		Map<Integer, Double> newProbabilities = new HashMap<Integer, Double>();

		for (Integer i : graph.keySet())
		{
			// For each observation i in the graph, the probability of observation i at time t + 1 is calculated as:
			// sum(weight(i,j) * probability(j) for all j in neighbours(i)) / sum(weight(i,j) for all j in neighbours(i))
			double sumIWeights = 0.0;
			double sumNeighbourProbabilities = 0.0;
			for (Integer j : graph.get(i).keySet())
			{
				double weightIJ = graph.get(i).get(j);
				sumIWeights += weightIJ;
				sumNeighbourProbabilities += weightIJ * probabilities.get(j);
			}
			if (sumIWeights == 0.0)
			{
				// If the observation has no neighbours.
				newProbabilities.put(i, probabilities.get(i));
			}
			else
			{
				newProbabilities.put(i, sumNeighbourProbabilities / sumIWeights);
			}
		}

		return newProbabilities;
	}

}
