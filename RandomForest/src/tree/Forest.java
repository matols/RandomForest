/**
 * 
 */
package tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * @author Simon Bull
 *
 */
public class Forest
{

	/**
	 * A list of the trees in the forest.
	 */
	public List<CARTTree> forest = new ArrayList<CARTTree>();

	/**
	 * A list where the ith element corresponds to the ith tree. The ith element of the list records
	 * all observations that are oob on the ith tree.
	 */
	public List<List<Integer>> oobObservations = new ArrayList<List<Integer>>();

	/**
	 * A mapping from each integer to a list of the trees that it is oob on.
	 */
	public Map<Integer, List<Integer>> oobOnTree = new HashMap<Integer, List<Integer>>();

	/**
	 * The oob error estimate.
	 */
	public double oobErrorEstimate = 0.0;

	/**
	 * The oob confusion matrix.
	 */
	public Map<String, Map<String, Double>> oobConfusionMatrix = new HashMap<String, Map<String, Double>>();

	/**
	 * The file containing the data that the forest was grown from.
	 */
	public String dataFileGrownFrom = "";

	/**
	 * The object recording the control parameters for the forest and its trees.
	 */
	public TreeGrowthControl ctrl;

	/**
	 * 
	 */
	public ProcessDataForGrowing processedData;

	public Map<Integer, Double> weights = new HashMap<Integer, Double>();

	public long seed;


	public Forest(String dataForGrowing)
	{
		this.ctrl = new TreeGrowthControl();
		this.seed = System.currentTimeMillis();
		this.dataFileGrownFrom = dataForGrowing;
		this.processedData = new ProcessDataForGrowing(dataForGrowing, this.ctrl);
	}

	public Forest(String dataForGrowing, Boolean isLoadingSavedPerformed)
	{
		if (!isLoadingSavedPerformed)
		{
			this.ctrl = new TreeGrowthControl();
			this.seed = System.currentTimeMillis();
			this.dataFileGrownFrom = dataForGrowing;
			this.processedData = new ProcessDataForGrowing(dataForGrowing, this.ctrl);
		}
		else
		{
			// Loading from a saved forest.
			// Load the control object.
			String controllerLoadLocation = dataForGrowing + "/Controller.txt";
			this.ctrl = new TreeGrowthControl(controllerLoadLocation);

			// Load the processed data.
			String processedDataLoadLocation = dataForGrowing + "/ProcessedData.txt";
			this.processedData = new ProcessDataForGrowing(processedDataLoadLocation);

			// Load the other forest attributes.
			String attributeLoadLocation = dataForGrowing + "/Attributes.txt";
			try (BufferedReader reader = Files.newBufferedReader(Paths.get(attributeLoadLocation), StandardCharsets.UTF_8))
			{
				String line = reader.readLine();
				line.trim();
				String[] splitLine = line.split("\t");
				String[] oobSplits = splitLine[0].split(";");
				for (String s : oobSplits)
				{
					List<Integer> currentOobs = new ArrayList<Integer>();
					for (String p : s.split(","))
					{
						currentOobs.add(Integer.parseInt(p));
					}
					this.oobObservations.add(currentOobs);
				}
				this.oobErrorEstimate = Double.parseDouble(splitLine[1]);
				String[] confMatSplits = splitLine[2].split("#");
				for (String s : confMatSplits)
				{
					String[] subMapSplit = s.split("-");
					String topLevelKey = subMapSplit[0];
					this.oobConfusionMatrix.put(topLevelKey, new HashMap<String, Double>());
					String[] subDirSplit = subMapSplit[1].split(";");
					for (String p : subDirSplit)
					{
						String[] indivValues = p.split(",");
						this.oobConfusionMatrix.get(topLevelKey).put(indivValues[0], Double.parseDouble(indivValues[1]));
					}
				}
				this.dataFileGrownFrom = splitLine[3];
				String[] weightSplits = splitLine[4].split(";");
				for (String s : weightSplits)
				{
					String[] indivWeights = s.split(",");
					this.weights.put(Integer.parseInt(indivWeights[0]), Double.parseDouble(indivWeights[1]));
				}
				this.seed = Long.parseLong(splitLine[5]);

				String[] obsOOBSplits = splitLine[6].split(";");
				for (String s : obsOOBSplits)
				{
					String[] indivObsOOB = s.split(",");
					int obs = Integer.parseInt(indivObsOOB[0]);
					List<Integer> treesOOBOn = new ArrayList<Integer>();
					for (int i = 1; i < indivObsOOB.length; i++)
					{
						treesOOBOn.add(Integer.parseInt(indivObsOOB[i]));
					}
					this.oobOnTree.put(obs, treesOOBOn);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}

			// Load the trees.
			Map<Integer, CARTTree> orderedForest = new HashMap<Integer, CARTTree>();
			File forestDirectory = new File(dataForGrowing);
			for (String s : forestDirectory.list())
			{
				if (!s.contains(".txt"))
				{
					// If the location is not a text file, and therefore contains the information about a tree.
					String treeLoadLocation = dataForGrowing + "/" + s;
					orderedForest.put(Integer.parseInt(s), new CARTTree(treeLoadLocation));
				}
			}
			for (int i = 0; i < orderedForest.size(); i++)
			{
				this.forest.add(orderedForest.get(i));
			}
		}
	}

	public Forest(String dataForGrowing, TreeGrowthControl ctrl)
	{
		this.ctrl = new TreeGrowthControl(ctrl);
		this.seed = System.currentTimeMillis();
		this.dataFileGrownFrom = dataForGrowing;
		this.processedData = new ProcessDataForGrowing(dataForGrowing, this.ctrl);
	}

	public Forest(String dataForGrowing, TreeGrowthControl ctrl, Long seed)
	{
		this.ctrl = new TreeGrowthControl(ctrl);
		this.seed = seed;
		this.dataFileGrownFrom = dataForGrowing;
		this.processedData = new ProcessDataForGrowing(dataForGrowing, this.ctrl);
	}

	public Forest(ProcessDataForGrowing procData)
	{
		this.ctrl = new TreeGrowthControl();
		this.seed = System.currentTimeMillis();
		this.processedData = procData;
		this.dataFileGrownFrom = procData.dataFileGrownFrom;
	}

	public Forest(ProcessDataForGrowing procData, TreeGrowthControl ctrl)
	{
		this.ctrl = new TreeGrowthControl(ctrl);
		this.seed = System.currentTimeMillis();
		this.processedData = procData;
		this.dataFileGrownFrom = procData.dataFileGrownFrom;
	}

	public Forest(ProcessDataForGrowing procData, TreeGrowthControl ctrl, Long seed)
	{
		this.ctrl = new TreeGrowthControl(ctrl);
		this.seed = seed;
		this.processedData = procData;
		this.dataFileGrownFrom = procData.dataFileGrownFrom;
	}


	public void setWeightsByClass(Map<String, Double> inputWeights)
	{
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			String response = this.processedData.responseData.get(i);
			if (!inputWeights.containsKey(response))
			{
				// If there is no weight for the observation, then set it to 1.0.
				this.weights.put(i, 1.0);
			}
			else
			{
				this.weights.put(i, inputWeights.get(response));
			}
		}
	}

	public void setWeightsByObservations(Map<Integer, Double> inputWeights)
	{
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			if (!inputWeights.containsKey(i))
			{
				// If there is no weight for the observation, then set it to 1.0.
				this.weights.put(i, 1.0);
			}
			else
			{
				this.weights.put(i, inputWeights.get(i));
			}
		}
	}


	public Map<Integer, Map<Integer, Double>> calculatProximities()
	{
		return calculatProximities(this.processedData);
	}

	public Map<Integer, Map<Integer, Double>> calculatProximities(ProcessDataForGrowing procData)
	{
		Map<Integer, Map<Integer, Double>> proximities = new HashMap<Integer, Map<Integer, Double>>();
		for (int i = 0; i < procData.numberObservations; i++)
		{
			// Add a record of all the observations to the proximities.
			proximities.put(i, new HashMap<Integer, Double>());
		}

		for (CARTTree t : this.forest)
		{
			List<List<Integer>> treeProximities = t.getProximities(procData);  // Get the proximities for the tree.
			for (List<Integer> l : treeProximities)
			{
				Collections.sort(l);  // Sort the list of observation indices so that you only have to keep half the matrix.
				for (int i = 0; i < l.size(); i++)
				{
					Integer obsI = l.get(i);
					for (int j = i + 1; j < l.size(); j++)
					{
						Integer obsJ = l.get(j);
						// Go through all pairs of observation indices that ended up in the same terminal node.
						if (!proximities.get(obsI).containsKey(obsJ))
						{
							// If obsI and obsJ have never occurred in the same terminal node before.
							proximities.get(obsI).put(obsJ, 1.0);
						}
						else
						{
							Double oldProximityCount = proximities.get(obsI).get(obsJ);
							proximities.get(obsI).put(obsJ, oldProximityCount + 1.0);
						}
					}
				}
			}
		}

		// Normalise the proximites by the number of trees.
		for (Integer i : proximities.keySet())
		{
			for (Integer j : proximities.get(i).keySet())
			{
				Double oldProximityCount = proximities.get(i).get(j);
				proximities.get(i).put(j, oldProximityCount / this.forest.size());
			}
		}

		return proximities;
	}

	public Map<Integer, Map<Integer, Double>> calculatProximities(String outputLocation)
	{
		return calculatProximities(this.processedData, outputLocation);
	}

	public Map<Integer, Map<Integer, Double>> calculatProximities(ProcessDataForGrowing procData, String outputLocation)
	{
		try
		{
			FileWriter proxOutputFile = new FileWriter(outputLocation, true);
			BufferedWriter proxOutputWriter = new BufferedWriter(proxOutputFile);
			
			for (CARTTree t : this.forest)
			{
				String treeProxString = "";
				List<List<Integer>> treeProximities = t.getProximities(procData);  // Get the proximities for the tree.
				for (List<Integer> l : treeProximities)
				{
					for (Integer i : l)
					{
						treeProxString += Integer.toString(i) + ",";
					}
					treeProxString = treeProxString.substring(0, treeProxString.length() - 1);
					treeProxString += "\t";
				}
				treeProxString = treeProxString.substring(0, treeProxString.length() - 1);
				proxOutputWriter.write(treeProxString);
				proxOutputWriter.newLine();
			}

			proxOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		return null;
	}


	public Map<String, Double> condVariableImportance()
	{
		return this.condVariableImportance(0.2);
	}

	public Map<String, Double> condVariableImportance(double maxCorrelation)
	{
		Map<String, Double> variableImportance = new HashMap<String, Double>();

		// Determine correlations.
		double[][] datasetArrays = new double[this.processedData.numberObservations][this.processedData.covariableData.size()];
		List<String> covariableOrdering = new ArrayList<String>(this.processedData.covariableData.keySet());
		for (int i = 0; i < covariableOrdering.size(); i++)
		{
			for (int j = 0; j < this.processedData.numberObservations; j++)
			{
				datasetArrays[j][i] = this.processedData.covariableData.get(covariableOrdering.get(i)).get(j);
			}
		}
		PearsonsCorrelation correlationMatrix = new PearsonsCorrelation(datasetArrays);
		RealMatrix correlations = correlationMatrix.getCorrelationMatrix();
		Map<String, List<String>> correlatedVariables = new HashMap<String, List<String>>();
		for (int i = 0; i < covariableOrdering.size(); i++)
		{
			String covariable = covariableOrdering.get(i);
			List<String> toSimilarToI = new ArrayList<String>();
			for (int j = 0; j < covariableOrdering.size(); j++)
			{
				if (i == j)
				{
					continue;
				}
				double correlationIJ = correlations.getEntry(i, j);
				if (Math.abs(correlationIJ) >= maxCorrelation)
				{
					String covarJ = covariableOrdering.get(j);
					toSimilarToI.add(covarJ);
				}
			}
			correlatedVariables.put(covariable, toSimilarToI);
		}

		for (String s : this.processedData.covariableData.keySet())
		{
			Double cumulativeImportance = 0.0;
			for (int i = 0; i < this.forest.size(); i++)
			{
				CARTTree currentTree = this.forest.get(i);
				List<Integer> oobOnThisTree = this.oobObservations.get(i);
				List<List<Integer>> conditionalGrid = currentTree.getConditionalGrid(this.processedData, oobOnThisTree, correlatedVariables.get(s));
	
				// Create the permuted copy of the data.
				ProcessDataForGrowing permData = new ProcessDataForGrowing(this.processedData);
				List<List<Integer>> permutedCondGrid = new ArrayList<List<Integer>>();
				for (List<Integer> l : conditionalGrid)
				{
					List<Integer> permutedGridCell = new ArrayList<Integer>(l);
					Collections.shuffle(permutedGridCell);
					permutedCondGrid.add(permutedGridCell);
				}
				for (int j = 0; j < conditionalGrid.size(); j++)
				{
					List<Integer> gridCell = conditionalGrid.get(j);
					List<Integer> permGridCell = permutedCondGrid.get(j);
					for (int k = 0; k < gridCell.size(); k++)
					{
						int obsIndex = gridCell.get(k);  // Index of the observation that is being changed to a different value for the covariable s.
						int permObsIndex = permGridCell.get(k);  // Index of the observation that is having its value placed in the obsIndex index.
						double permValue = this.processedData.covariableData.get(s).get(permObsIndex);
						permData.covariableData.get(s).set(obsIndex, permValue);
					}
				}
	
				// Determine the accuracy, and the change in it induced by the permutation.
				List<Integer> treesToUse = new ArrayList<Integer>();
				treesToUse.add(i);
				Double originalAccuracy = 1 - predict(this.processedData, oobOnThisTree, treesToUse).first;  // Determine the predictive accuracy for the non-permuted observations.
				Double permutedAccuracy = 1 - predict(permData, oobOnThisTree, treesToUse).first;  // Determine the predictive accuracy for the permuted observations.
				cumulativeImportance += (originalAccuracy - permutedAccuracy);
			}
			cumulativeImportance /= this.forest.size();  // Get the mean change in the accuracy. This is the importance for the variable.
			variableImportance.put(s, cumulativeImportance);
		}
		return variableImportance;
	}


	public void growForest()
	{
		// Seed the random generator used to control all the randomness in the algorithm,
		Random randGenerator = new Random(this.seed);

		// Determine the possible classes of the observations.
		Set<String> responseClasses = new HashSet<String>(this.processedData.responseData);

		// Set up the mapping of trees that each observation is OOB on.
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			oobOnTree.put(i, new ArrayList<Integer>());
		}

		// If sampling by class is being performed, then record the observations in each class.
		boolean isSampSizeUsed = this.ctrl.sampSize.size() > 0 || ctrl.isStratifiedBootstrapUsed;
		Map<String, List<Integer>> observationsInEachClass = new HashMap<String, List<Integer>>();
		if (isSampSizeUsed)
		{
			for (String s : responseClasses)
			{
				observationsInEachClass.put(s, new ArrayList<Integer>());
			}
			for (int i = 0; i < this.processedData.numberObservations; i++)
			{
				observationsInEachClass.get(this.processedData.responseData.get(i)).add(i);
			}
		}

		// Perform some error checking to ensure that the choice of sampling strategy is consistent.
		boolean samplingParameterError = false;
		if (this.ctrl.sampSize.size() > 0 && ctrl.isStratifiedBootstrapUsed)
		{
			// Raise an error because the user has specified a specific sample size for each class, and to use stratified bootstrapping.
			System.out.println("ERROR : sampSize and isStratifiedBootstrapUsed from TreeGrowthControl can not be used together.");
			System.out.println("ERROR : Either set sampSize to an empty Map or set isStratifiedBootstrapUsed to false.");
			samplingParameterError = true;
		}
		if (this.ctrl.sampSize.size() > 0 && !this.ctrl.sampSize.keySet().containsAll(responseClasses))
		{
			// Raise an error because the user has only specified a specific sample size for some classes.
			System.out.println("ERROR : sampSize in the TreeGrowthControl object supplied must contain all the classes in the dataset supplied.");
			samplingParameterError = true;
		}
		if (!this.ctrl.isReplacementUsed && this.ctrl.sampSize.size() > 0)
		{
			for (String s : observationsInEachClass.keySet())
			{
				if (this.ctrl.sampSize.containsKey(s) && ((observationsInEachClass.get(s).size() * this.ctrl.selectionFraction)) > this.ctrl.sampSize.get(s))
				{
					// Raise an error because the number of observations of class s that the user said to sample is less than the available
					// observations when replacement is not used.
					System.out.format("ERROR : There are not enough observations of class %s to sample %d observations", s, (int) (observationsInEachClass.get(s).size() * this.ctrl.selectionFraction));
					System.out.println("ERROR : Please either revert to sampling with replacment, or change the number and fraction of observations to sample.");
					samplingParameterError = true;
				}
			}
		}
		if (samplingParameterError)
		{
			System.exit(0);
		}

		// If stratified bootstrap is specified, then set the sample sizes to ensure that the correct number of observations are sampled from each class.
		if (ctrl.isStratifiedBootstrapUsed)
		{
			for (String s : responseClasses)
			{
				ctrl.sampSize.put(s, observationsInEachClass.get(s).size());
			}
		}

		// Determine the default weightings.
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			if (!this.weights.containsKey(i))
			{
				// If there is no weight for the observation, then set it to 1.0.
				this.weights.put(i, 1.0);
			}
		}

		// Setup the observation selection variables.
		List<Integer> observations = new ArrayList<Integer>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			observations.add(i);
		}

		// Grow the trees in the forest.
		for (int i = 0; i < ctrl.numberOfTreesToGrow; i++)
		{
			// Randomly determine the observations used for growing this tree.
			List<Integer> observationsForTheTree = new ArrayList<Integer>();
			if (isSampSizeUsed)
			{
				for (String s : responseClasses)
				{
					int observationsToSelect = this.ctrl.sampSize.get(s);
					List<Integer> thisClassObservations = new ArrayList<Integer>(observationsInEachClass.get(s));
					if (!ctrl.isReplacementUsed)
					{
						Collections.shuffle(thisClassObservations, new Random(randGenerator.nextLong()));
						observationsForTheTree = thisClassObservations.subList(0, (int) (observationsToSelect * this.ctrl.selectionFraction));
					}
					else
					{
						int selectedObservation;
						for (int j = 0; j < observationsToSelect; j++)
						{
							selectedObservation = randGenerator.nextInt(thisClassObservations.size());
							observationsForTheTree.add(thisClassObservations.get(selectedObservation));
						}
					}
				}
			}
			else
			{
				if (!ctrl.isReplacementUsed)
				{
					Collections.shuffle(observations, new Random(randGenerator.nextLong()));
					observationsForTheTree = observations.subList(0, (int) (Math.floor(this.ctrl.selectionFraction * observations.size())));
				}
				else
				{
					int numberObservationsToSelect = observations.size();
					int selectedObservation;
					for (int j = 0; j < numberObservationsToSelect; j++)
					{
						selectedObservation = randGenerator.nextInt(observations.size());
						observationsForTheTree.add(observations.get(selectedObservation));
					}
				}
			}

			// Update the list of which observations are OOB on this tree.
			List<Integer> oobOnThisTree = new ArrayList<Integer>(observations);
			oobOnThisTree.removeAll(observationsForTheTree);
			this.oobObservations.add(oobOnThisTree);
			for (Integer j : oobOnThisTree)
			{
				this.oobOnTree.get(j).add(i);
			}

			// Grow this tree from the chosen observations.
			long seedForTree = randGenerator.nextLong();
			CARTTree newTree = new CARTTree(this.processedData, this.ctrl, seedForTree);
			newTree.growTree(this.weights, observationsForTheTree);
			this.forest.add(newTree);
		}

		if (this.ctrl.isCalculateOOB)
		{
			// Calculate the OOB error and confusion matrix. This is done by putting each observation down the trees where it is OOB.
			double cumulativeErrorRate = 0.0;
			Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
			Set<String> responsePossibilities = new HashSet<String>(this.processedData.responseData);
			for (String s : responsePossibilities)
			{
				Map<String, Double> classEntry = new HashMap<String, Double>();
				classEntry.put("TruePositive", 0.0);
				classEntry.put("FalsePositive", 0.0);
				confusionMatrix.put(s, classEntry);
			}
			int numberOobObservations = 0;
			for (int i = 0; i < this.processedData.numberObservations; i++)
			{
				boolean isIOob = !oobOnTree.get(i).isEmpty();
				List<Integer> obsToPredict = new ArrayList<Integer>();
				obsToPredict.add(i);
				if (isIOob)
				{
					numberOobObservations += 1;
					ImmutableTwoValues<Double, Map<String, Map<String, Double>>> oobPrediction = predict(this.processedData, obsToPredict, oobOnTree.get(i));
					cumulativeErrorRate += oobPrediction.first;
					for (String s : oobPrediction.second.keySet())
					{
						for (String p : oobPrediction.second.get(s).keySet())
						{
							Double oldValue = confusionMatrix.get(s).get(p);
							confusionMatrix.get(s).put(p, oldValue + oobPrediction.second.get(s).get(p));
						}
					}
				}			
			}
			this.oobErrorEstimate = cumulativeErrorRate / numberOobObservations;
			this.oobConfusionMatrix = confusionMatrix;
		}
	}


	public ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predict(ProcessDataForGrowing predData)
	{
		List<Integer> observationsToPredict = new ArrayList<Integer>();
		for (int i = 0; i < predData.numberObservations; i++)
		{
			observationsToPredict.add(i);
		}
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predict(predData, observationsToPredict, treesToUseForPrediction);
	}

	public ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predict(predData, observationsToPredict, treesToUseForPrediction);
	}

	public ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict, List<Integer> treesToUseForPrediction)
	{
		Double errorRate = 0.0;
		Map<Integer, String> observationToClassification = new HashMap<Integer, String>();

		Set<String> classNames = new HashSet<String>(this.processedData.responseData);  // A set containing the names of all the classes in the dataset.

		// Set up the mapping from observation index to predictions. The key is the index of the observation in the dataset, the Map contains
		// a mapping from each class to the weighted vote for it from the forest.
		Map<Integer, Map<String, Double>> predictions = new HashMap<Integer,Map<String, Double>>();
		Map<String, Double> possiblePredictions = new HashMap<String, Double>();
		for (String s : classNames)
		{
			possiblePredictions.put(s, 0.0);
		}
		for (int i : observationsToPredict)
		{
			predictions.put(i, new HashMap<String, Double>(possiblePredictions));
		}

		// Get the raw predictions for each tree.
		for (int i : treesToUseForPrediction)
		{
			Map<Integer, Map<String, Double>> predictedValues = forest.get(i).predict(predData, observationsToPredict);
			for (int j : predictedValues.keySet())
			{
				for (String s : predictedValues.get(j).keySet())
				{
					Double oldPrediction = predictions.get(j).get(s);
					Double newPrediction = predictedValues.get(j).get(s);
					predictions.get(j).put(s, oldPrediction + newPrediction);
				}
			}
		}

		// Make sense of the prediction for each observation.
		for (int i : predictions.keySet())
		{
			// Get the list of predictions for observation i. The predictions are ordered so that the jth value in the list
			// is the prediction for the jth value in the list of treesToUseForPrediction.
			Map<String, Double> predictedValues = predictions.get(i);

			// Determine the majority classification for the observation.
			String majorityClass = "";
			double largestNumberClassifications = -Double.MAX_VALUE;
			for (String s : predictedValues.keySet())
			{
				if (predictedValues.get(s) > largestNumberClassifications)
				{
					majorityClass = s;
					largestNumberClassifications = predictedValues.get(s);
				}
			}

			// Record the majority classification for the observation.
			observationToClassification.put(i, majorityClass);
		}

		// Set up the confusion matrix.
		Map<String, Map<String, Double>> confusionMatrix = new HashMap<String, Map<String, Double>>();
		Set<String> responsePossibilities = new HashSet<String>(this.processedData.responseData);
		for (String s : responsePossibilities)
		{
			Map<String, Double> classEntry = new HashMap<String, Double>();
			classEntry.put("TruePositive", 0.0);
			classEntry.put("FalsePositive", 0.0);
			confusionMatrix.put(s, classEntry);
		}

		// Record the error rate for all observations.
		for (int i : observationToClassification.keySet())
		{
			String predictedClass = observationToClassification.get(i);
			if (!predData.responseData.get(i).equals(predictedClass))
			{
				// If the classification is not correct.
				errorRate += 1.0; // Indicate that an incorrect prediction has been encountered.
				// Increment the number of false positives for the predicted class.
				Double currentFalsePos = confusionMatrix.get(predictedClass).get("FalsePositive");
				confusionMatrix.get(predictedClass).put("FalsePositive", currentFalsePos + 1);
			}
			else
			{
				// Increment the number of true positives for the predicted class.
				Double currentTruePos = confusionMatrix.get(predictedClass).get("TruePositive");
				confusionMatrix.get(predictedClass).put("TruePositive", currentTruePos + 1);
			}
		}
		// Divide the number of observations predicted incorrectly by the total number of observations in order to get the
		// overall error rate of the set of observations provided on the set of trees provided.
		errorRate = errorRate / observationToClassification.size();

		return new ImmutableTwoValues<Double, Map<String,Map<String,Double>>>(errorRate, confusionMatrix);
	}


	public Map<Integer, Map<String, Double>> predictRaw(ProcessDataForGrowing predData)
	{
		List<Integer> observationsToPredict = new ArrayList<Integer>();
		for (int i = 0; i < predData.numberObservations; i++)
		{
			observationsToPredict.add(i);
		}
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predictRaw(predData, observationsToPredict, treesToUseForPrediction);
	}

	public Map<Integer, Map<String, Double>> predictRaw(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		List<Integer> treesToUseForPrediction = new ArrayList<Integer>();
		for (int i = 0; i < forest.size(); i++)
		{
			treesToUseForPrediction.add(i);
		}
		return predictRaw(predData, observationsToPredict, treesToUseForPrediction);
	}

	public Map<Integer, Map<String, Double>> predictRaw(ProcessDataForGrowing predData, List<Integer> observationsToPredict, List<Integer> treesToUseForPrediction)
	{
		Set<String> classNames = new HashSet<String>(this.processedData.responseData);  // A set containing the names of all the classes in the dataset.

		// Set up the mapping from observation index to predictions. The key is the index of the observation in the dataset, the Map contains
		// a mapping from each class to the weighted vote for it from the forest.
		Map<Integer, Map<String, Double>> predictions = new HashMap<Integer,Map<String, Double>>();
		Map<String, Double> possiblePredictions = new HashMap<String, Double>();
		for (String s : classNames)
		{
			possiblePredictions.put(s, 0.0);
		}
		for (int i : observationsToPredict)
		{
			predictions.put(i, new HashMap<String, Double>(possiblePredictions));
		}

		// Get the raw predictions for each tree.
		for (Integer i : treesToUseForPrediction)
		{
			Map<Integer, Map<String, Double>> predictedValues = forest.get(i).predict(predData, observationsToPredict);
			for (Integer j : predictedValues.keySet())
			{
				for (String s : predictedValues.get(j).keySet())
				{
					Double oldPrediction = predictions.get(j).get(s);
					Double newPrediction = predictedValues.get(j).get(s);
					predictions.get(j).put(s, oldPrediction + newPrediction);
				}
			}
		}

		return predictions;
	}


	public void regrowForest()
	{
		// Regrow using old seeds.
		this.forest = new ArrayList<CARTTree>();
		this.oobObservations = new ArrayList<List<Integer>>();
		this.oobErrorEstimate = 0.0;
		this.growForest();
	}

	public void regrowForest(long newSeed)
	{
		// Regrow using a different seed.
		this.seed = newSeed;
		this.regrowForest();
	}

	public void regrowForest(TreeGrowthControl newCtrl)
	{
		// Regrow with the old seed, but a different controller.
		// This allows you to change replacement/mtry while keeping the random seed the same.
		this.ctrl = newCtrl;
		this.regrowForest();
	}

	public void regrowForest(long newSeed, TreeGrowthControl newCtrl)
	{
		// Regrow using a different seed and a new controller.
		this.seed = newSeed;
		this.ctrl = newCtrl;
		this.regrowForest();
	}


	public void save(String savedirLoc)
	{
		File outputDirectory = new File(savedirLoc);
		if (!outputDirectory.exists())
		{
			boolean isDirCreated = outputDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The output directory does not exist, but could not be created.");
				System.exit(0);
			}
		}
		else if (!outputDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("The output directory location exists, but is not a directory.");
			System.exit(0);
		}

		// Save the trees.
		for (int i = 0; i < this.forest.size(); i++)
		{
			String treeSaveLocation = savedirLoc + "/" + Integer.toString(i);
			this.forest.get(i).save(treeSaveLocation);
		}

		// Save the control object.
		String controllerSaveLocation = savedirLoc + "/Controller.txt";
		this.ctrl.save(controllerSaveLocation);

		// Save the processed data.
		String processedDataSaveLocation = savedirLoc + "/ProcessedData.txt";
		this.processedData.save(processedDataSaveLocation);

		// Save the other forest attributes.
		String attributeSaveLocation = savedirLoc + "/Attributes.txt";
		try
		{
			FileWriter outputFile = new FileWriter(attributeSaveLocation);
			BufferedWriter outputWriter = new BufferedWriter(outputFile);
			String oobObsOutput = "";
			for (Integer i : this.oobObservations.get(0))
			{
				oobObsOutput += Integer.toString(i) + ",";
			}
			oobObsOutput = oobObsOutput.substring(0, oobObsOutput.length() - 1);  // Chop off the last ','.
			for (int i = 1; i < this.oobObservations.size(); i++)
			{
				oobObsOutput += ";";
				for (Integer j : this.oobObservations.get(i))
				{
					oobObsOutput += Integer.toString(j) + ",";
				}
				oobObsOutput = oobObsOutput.substring(0, oobObsOutput.length() - 1);  // Chop off the last ','.
			}
			outputWriter.write(oobObsOutput + "\t");
			outputWriter.write(Double.toString(this.oobErrorEstimate) + "\t");
			String oobConfMatOutput = "";
			for (String s : this.oobConfusionMatrix.keySet())
			{
				oobConfMatOutput += s + "-";
				for (String p : this.oobConfusionMatrix.get(s).keySet())
				{
					oobConfMatOutput += p + "," + Double.toString(this.oobConfusionMatrix.get(s).get(p)) + ";";
				}
				oobConfMatOutput = oobConfMatOutput.substring(0, oobConfMatOutput.length() - 1);  // Chop off the last ';'.
				oobConfMatOutput += "#";
			}
			oobConfMatOutput = oobConfMatOutput.substring(0, oobConfMatOutput.length() - 1);  // Chop off the last '#'.
			outputWriter.write(oobConfMatOutput + "\t");
			outputWriter.write(this.dataFileGrownFrom + "\t");
			String weightsOutput = "";
			for (Integer i : this.weights.keySet())
			{
				weightsOutput += Integer.toString(i) + "," + Double.toString(this.weights.get(i)) + ";";
			}
			weightsOutput = weightsOutput.substring(0, weightsOutput.length() - 1);  // Chop off the last ';'.
			outputWriter.write(weightsOutput + "\t");
			outputWriter.write(Long.toString(this.seed));
			outputWriter.write("\t");
			String oobTreeOutput = "";
			for (Integer i : oobOnTree.keySet())
			{
				oobTreeOutput += Integer.toString(i) + ",";
				for (Integer j : oobOnTree.get(i))
				{
					oobTreeOutput += Integer.toString(j) + ",";
				}
				oobTreeOutput = oobTreeOutput.substring(0, oobTreeOutput.length() - 1);  // Chop off the last ','.
				oobTreeOutput += ";";
			}
			oobTreeOutput = oobTreeOutput.substring(0, oobTreeOutput.length() - 1);  // Chop off the last ';'.
			outputWriter.write(oobTreeOutput);
			outputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}
	}


	public ImmutableTwoValues<Map<String, Double>, Map<String, Double>> variableImportance()
	{
		// Determine the counts for each class in the dataset.
		Map<String, Integer> countsOfClass = new HashMap<String, Integer>();
		for (String s : this.processedData.responseData)
		{
			countsOfClass.put(s, countsOfClass.get(s) + 1);
		}

		// Determine base accuracy for each tree.
		List<Double> baseOOBAccuracy = new ArrayList<Double>();
		List<Double> baseOOBGMeans = new ArrayList<Double>();
		for (int i = 0; i < this.forest.size(); i++)
		{
			List<Integer> oobOnThisTree = this.oobObservations.get(i);
			List<Integer> treesToUse = new ArrayList<Integer>();
			treesToUse.add(i);
			ImmutableTwoValues<Double, Map<String, Map<String, Double>>> originalPredictions = predict(this.processedData, oobOnThisTree, treesToUse);
			Double originalAccuracy = 1 - originalPredictions.first;
			baseOOBAccuracy.add(originalAccuracy);
			Map<String, Map<String, Double>> confusionMatrix = originalPredictions.second;
			double macroGMean = 1.0;
			for (String s : confusionMatrix.keySet())
			{
				double TP = confusionMatrix.get(s).get("TruePositive");
	    		double FN = countsOfClass.get(s) - TP;  // The number of false positives is the number of observations from the class  - the number of true positives.
	    		double recall = (TP / (TP + FN));
	    		macroGMean *= recall;
			}
			double gMean = Math.pow(macroGMean, (1.0 / confusionMatrix.size()));
			baseOOBGMeans.add(gMean);
		}

		// Determine permuted importance.
		Map<String, Double> accVariableImportance = new HashMap<String, Double>();
		Map<String, Double> gMeanVariableImportance = new HashMap<String, Double>();
		for (String s : this.processedData.covariableData.keySet())
		{
			double cumulativeAccChange = 0.0;
			double cumulativeGMeanChange = 0.0;
			for (int i = 0; i < this.forest.size(); i++)
			{
				List<Integer> oobOnThisTree = this.oobObservations.get(i);
				List<Integer> permutedOobOnThisTree = new ArrayList<Integer>(this.oobObservations.get(i));
				Collections.shuffle(permutedOobOnThisTree);

				// Create the permuted copy of the data.
				ProcessDataForGrowing permData = new ProcessDataForGrowing(this.processedData);
				for (int j = 0; j < permutedOobOnThisTree.size(); j++)
				{
					int obsIndex = oobOnThisTree.get(j);  // Index of the observation that is being changed to a different value for the covariable s.
					int permObsIndex = permutedOobOnThisTree.get(j);  // Index of the observation that is having its value placed in the obsIndex index.
					double permValue = this.processedData.covariableData.get(s).get(permObsIndex);
					permData.covariableData.get(s).set(obsIndex, permValue);
				}

				List<Integer> treesToUse = new ArrayList<Integer>();
				treesToUse.add(i);
				ImmutableTwoValues<Double, Map<String, Map<String, Double>>> permutedPredictions = predict(permData, oobOnThisTree, treesToUse);
				Double permutedAccuracy = 1 - permutedPredictions.first;  // Determine the predictive accuracy for the permuted observations.
				cumulativeAccChange += (baseOOBAccuracy.get(i) - permutedAccuracy);
				Map<String, Map<String, Double>> confusionMatrix = permutedPredictions.second;
				double permutedMacroGMean = 1.0;
				for (String p : confusionMatrix.keySet())
				{
					double TP = confusionMatrix.get(p).get("TruePositive");
		    		double FN = countsOfClass.get(p) - TP;  // The number of false positives is the number of observations from the class  - the number of true positives.
		    		double recall = (TP / (TP + FN));
		    		permutedMacroGMean *= recall;
				}
				double permutedGMean = Math.pow(permutedMacroGMean, (1.0 / confusionMatrix.size()));
				cumulativeGMeanChange += (baseOOBGMeans.get(i) - permutedGMean);
			}
			cumulativeAccChange /= this.forest.size();  // Get the mean change in the accuracy. This is the importance for the variable.
			cumulativeGMeanChange /= this.forest.size();  // Get the mean change in the accuracy. This is the importance for the variable.
			accVariableImportance.put(s, cumulativeAccChange);
			gMeanVariableImportance.put(s, cumulativeGMeanChange);
		}

		return new ImmutableTwoValues<Map<String,Double>, Map<String,Double>>(accVariableImportance, gMeanVariableImportance);
	}

}
