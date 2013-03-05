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
	 * The oob error estimate.
	 */
	public double oobErrorEstimate = 0.0;

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

	public Map<String, Double> weights = new HashMap<String, Double>();

	public long seed;


	public Forest(String dataForGrowing)
	{
		this.ctrl = new TreeGrowthControl();
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, new HashMap<String, Double>());
	}

	public Forest(String dataForGrowing, Boolean isLoadingSavedPerformed)
	{
		if (!isLoadingSavedPerformed)
		{
			this.ctrl = new TreeGrowthControl();
			this.seed = System.currentTimeMillis();
			growForest(dataForGrowing, new HashMap<String, Double>());
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
				this.dataFileGrownFrom = splitLine[2];
				String[] weightSplits = splitLine[3].split(";");
				for (String s : weightSplits)
				{
					String[] indivWeights = s.split(",");
					this.weights.put(indivWeights[0], Double.parseDouble(indivWeights[1]));
				}
				this.seed = Long.parseLong(splitLine[4]);
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
		this.ctrl = ctrl;
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, new HashMap<String, Double>());
	}

	public Forest(String dataForGrowing, Map<String, Double> weights)
	{
		this.ctrl = new TreeGrowthControl();
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, weights);
	}

	public Forest(String dataForGrowing, TreeGrowthControl ctrl, Map<String, Double> weights)
	{
		this.ctrl = ctrl;
		this.seed = System.currentTimeMillis();
		growForest(dataForGrowing, weights);
	}

	public Forest(String dataForGrowing, TreeGrowthControl ctrl, Map<String, Double> weights, Long seed)
	{
		this.ctrl = ctrl;
		this.seed = seed;
		growForest(dataForGrowing, weights);
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
				Collections.sort(l);  // Sort the list of observation indices o that you only have to keep half the matrix.
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


	void growForest(String dataForGrowing, Map<String, Double> potentialWeights)
	{
		// Seed the random generator used to control all the randomness in the algorithm,
		Random randGenerator = new Random(this.seed);

		this.dataFileGrownFrom = dataForGrowing;
		ProcessDataForGrowing procData = new ProcessDataForGrowing(dataForGrowing, this.ctrl);
		this.processedData = procData;

		// Generate the default weightings.
		for (String s : this.processedData.responseData)
		{
			if (!potentialWeights.containsKey(s))
			{
				// Any classes without a weight are assigned a weight of 1.
				potentialWeights.put(s, 1.0);
			}
		}
		this.weights = potentialWeights;

		// Setup the observation selection variables.
		List<Integer> observations = new ArrayList<Integer>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			observations.add(i);
		}
		int numberObservationsToSelect = 0;
		if (!this.ctrl.isReplacementUsed)
		{
			numberObservationsToSelect = (int) Math.floor(this.ctrl.selectionFraction * this.processedData.numberObservations);
		}
		else
		{
			numberObservationsToSelect = this.processedData.numberObservations;
		}

		for (int i = 0; i < ctrl.numberOfTreesToGrow; i++)
		{
			// Randomly determine the observations used for growing this tree.
			List<Integer> observationsForTheTree = new ArrayList<Integer>();
			if (!ctrl.isReplacementUsed)
			{
				Collections.shuffle(observations, new Random(randGenerator.nextLong()));
				for (int j = 0; j < numberObservationsToSelect; j++)
				{
					observationsForTheTree.add(observations.get(j));
				}
			}
			else
			{
				int selectedObservation;
				for (int j = 0; j < numberObservationsToSelect; j++)
				{
					selectedObservation = randGenerator.nextInt(this.processedData.numberObservations);
					observationsForTheTree.add(observations.get(selectedObservation));
				}
			}

			// Update the list of which observations are oob on this tree.
			List<Integer> oobOnThisTree = new ArrayList<Integer>();
			for (int j = 0; j < this.processedData.numberObservations; j++)
			{
				if (!observationsForTheTree.contains(j))
				{
					// If the observation is not in the observations to use when growing the tree, then it is oob for the tree.
					oobOnThisTree.add(j);
				}
			}
			this.oobObservations.add(oobOnThisTree);

			// Grow this tree from the chosen observations.
			long seedForTree = randGenerator.nextLong();
			this.forest.add(new CARTTree(this.processedData, this.ctrl, weights, observationsForTheTree, seedForTree));
		}

		// Calculate the oob error. This is done by putting each observation down the trees where it is oob.
		double cumulativeErrorRate = 0.0;
		int numberOobObservations = 0;
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			boolean isIOob = false;
			List<Integer> obsToPredict = new ArrayList<Integer>();
			obsToPredict.add(i);
			// Gather the trees or which observation i is oob.
			List<Integer> treesToPredictFrom = new ArrayList<Integer>();
			for (int j = 0; j < this.ctrl.numberOfTreesToGrow; j++)
			{
				if (this.oobObservations.get(j).contains(i))
				{
					// If the jth tree contains the ith observation as an oob observation.
					treesToPredictFrom.add(j);
					isIOob = true;
				}
			}
			if (isIOob)
			{
				numberOobObservations += 1;
				cumulativeErrorRate += predict(this.processedData, obsToPredict, treesToPredictFrom).first;
			}			
		}
		this.oobErrorEstimate = cumulativeErrorRate / numberOobObservations;
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

		// Set up the mapping from observation index to predictions.
		// One key for each observation being predicted. The list of objects contains one entry for each tree the
		// observation is being predicted on.
		Map<Integer, List<ImmutableTwoValues<String, Double>>> predictions = new HashMap<Integer, List<ImmutableTwoValues<String, Double>>>();
		for (int i : observationsToPredict)
		{
			predictions.put(i, new ArrayList<ImmutableTwoValues<String, Double>>());
		}

		// Get the raw predictions for each tree.
		for (int i : treesToUseForPrediction)
		{
			Map<Integer, ImmutableTwoValues<String, Double>> predictedValues = forest.get(i).predict(predData, observationsToPredict);
			for (int j : observationsToPredict)
			{
				predictions.get(j).add(predictedValues.get(j));
			}
		}

		// Make sense of the prediction for each observation.
		for (int i : predictions.keySet())
		{
			// Get the list of predictions for observation i. The predictions are ordered so that the jth value in the list
			// is the prediction for the jth value in the list of treesToUseForPrediction.
			List<ImmutableTwoValues<String, Double>> predictedValues = predictions.get(i);
			// Mapping from a class to the number of times that the class was selected as the classification for the observation.
			Map<String, Double> predictedClasses = new HashMap<String, Double>();

			for (ImmutableTwoValues<String, Double> s : predictedValues)
			{
				String classPrediction = s.first;
				double classPredictionWeight = s.second;
				if (!predictedClasses.containsKey(classPrediction))
				{
					// If the class has not been predicted for the observation before, then set the count of the
					// number of times the class has been predicted to 1.
					predictedClasses.put(classPrediction, classPredictionWeight);
				}
				else
				{
					// If the class has been predicted for the observation before, then increment the count of the
					// number of times the class has been predicted.
					predictedClasses.put(classPrediction, predictedClasses.get(classPrediction) + classPredictionWeight);
				}
			}

			// Determine the majority classification for the observation.
			String majorityClass = "";
			double largestNumberClassifications = 0.0;
			for (String s : predictedClasses.keySet())
			{
				if (predictedClasses.get(s) > largestNumberClassifications)
				{
					majorityClass = s;
					largestNumberClassifications = predictedClasses.get(s);
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

	public void regrowForest()
	{
		// Regrow using old seeds.
		this.forest = new ArrayList<CARTTree>();
		this.oobObservations = new ArrayList<List<Integer>>();
		this.oobErrorEstimate = 0.0;
		this.growForest(this.dataFileGrownFrom, this.weights);
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
		// This allows you to change replacement/mtry while keeping the random seed the same.
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
			outputWriter.write(this.dataFileGrownFrom + "\t");
			String weightsOutput = "";
			for (String s : this.weights.keySet())
			{
				weightsOutput += s + "," + Double.toString(this.weights.get(s)) + ";";
			}
			weightsOutput = weightsOutput.substring(0, weightsOutput.length() - 1);  // Chop off the last ';'.
			outputWriter.write(weightsOutput + "\t");
			outputWriter.write(Long.toString(this.seed));
			outputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}
	}


	public Map<String, Double> variableImportance()
	{
		Map<String, Double> variableImportance = new HashMap<String, Double>();

		for (String s : this.processedData.covariableData.keySet())
		{
			double cumulativeAccChange = 0.0;
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
				Double originalAccuracy = 1 - predict(this.processedData, oobOnThisTree, treesToUse).first;  // Determine the predictive accuracy for the non-permuted observations.
				Double permutedAccuracy = 1 - predict(permData, oobOnThisTree, treesToUse).first;  // Determine the predictive accuracy for the permuted observations.
				cumulativeAccChange += (originalAccuracy - permutedAccuracy);
			}
			cumulativeAccChange /= this.forest.size();  // Get the mean change in the accuracy. This is the importance for the variable.
			variableImportance.put(s, cumulativeAccChange);
		}

		return variableImportance;
	}

}
