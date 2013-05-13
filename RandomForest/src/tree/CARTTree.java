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

/**
 * @author		Simon Bull
 * @version		1.0
 * @since		2013-01-31
 */
public class CARTTree
{

	/**
	 * The tree that has been grown.
	 */
	Node cartTree = null;

	/**
	 * The object recording the control parameters for the tree.
	 */
	TreeGrowthControl ctrl;

	/**
	 * 
	 */
	ProcessDataForGrowing processedData;

	long seed;

	public CARTTree(String loadString)
	{
		// Load the tree.
		String treeLoadLocation = loadString + "/Tree.txt";
		// Contains a skeleton of all the information needed to reconstruct the tree.
		Map<String, Map<String, String>> treeSkeleton = new HashMap<String, Map<String, String>>();
		String rootID = "";  // The ID of the root of the tree.
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(treeLoadLocation), StandardCharsets.UTF_8))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				line.trim();
				String[] splitLine = line.split("\t", 4);
				String nodeID = splitLine[0];
				String parentID = splitLine[1];
				String nodeType = splitLine[2];
				String nodeValue = splitLine[3];

				if (rootID.isEmpty())
				{
					rootID = nodeID;
				}
				else
				{
					// If the parent has a left child recorded, then this is the right child.
					if (treeSkeleton.get(parentID).containsKey("LeftChild"))
					{
						treeSkeleton.get(parentID).put("RightChild", nodeID);
					}
					else
					{
						treeSkeleton.get(parentID).put("LeftChild", nodeID);
					}
				}

				Map<String, String> nodeData = new HashMap<String, String>();
				nodeData.put("Type", nodeType);
				nodeData.put("Data", nodeValue);
				treeSkeleton.put(nodeID, nodeData);  // Add the data about the new node to the tree skeleton
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Start reconstructing the tree.
		if (treeSkeleton.get(rootID).get("Type").equals("Terminal"))
		{
			// If the root node of the tree is a terminal node.
			this.cartTree = new NodeTerminal(treeSkeleton, rootID);
		}
		else
		{
			// The root node is a non-terminal.
			this.cartTree = new NodeNonTerminal(treeSkeleton, rootID);
		}

		// Load the control object.
		String controllerLoadLocation = loadString + "/Controller.txt";
		this.ctrl = new TreeGrowthControl(controllerLoadLocation);

		// Load the processed data.
		String processedDataLoadLocation = loadString + "/ProcessedData.txt";
		this.processedData = new ProcessDataForGrowing(processedDataLoadLocation);

		// Load the seed.
		String seedLoadLocation = loadString + "/Seed.txt";
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(seedLoadLocation), StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line.trim();
			this.seed = Long.parseLong(line);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public CARTTree(ProcessDataForGrowing processedData)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
		controlTreeGrowth();
	}

	public CARTTree(ProcessDataForGrowing processedData, TreeGrowthControl ctrl)
	{
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
		controlTreeGrowth();
	}

	public CARTTree(ProcessDataForGrowing processedData, Map<String, Double> weights)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
		controlTreeGrowth(weights);
	}

	public CARTTree(ProcessDataForGrowing processedData, List<Integer> observationsToUse)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
		controlTreeGrowth(observationsToUse);
	}

	public CARTTree(ProcessDataForGrowing processedData, TreeGrowthControl ctrl, Map<String, Double> weights,
			List<Integer> observationsToUse)
	{
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = System.currentTimeMillis();
		controlTreeGrowth(weights, observationsToUse);
	}

	public CARTTree(ProcessDataForGrowing processedData, TreeGrowthControl ctrl, Map<String, Double> weights,
			List<Integer> observationsToUse, long seed)
	{
		this.ctrl = ctrl;
		this.processedData = processedData;
		this.seed = seed;
		controlTreeGrowth(weights, observationsToUse);
	}

	/**
	 * Controls the growth of the tree.
	 */
	void controlTreeGrowth()
	{
		controlTreeGrowth(new HashMap<String, Double>());
	}

	void controlTreeGrowth(Map<String, Double> potentialWeights)
	{
		// Setup the list of observations.
		List<Integer> observationsUsed = new ArrayList<Integer>();
		for (int i = 0; i < this.processedData.numberObservations; i++)
		{
			observationsUsed.add(i);
		}

		// Grow the tree.
		controlTreeGrowth(potentialWeights, observationsUsed);
	}

	void controlTreeGrowth(List<Integer> observationsUsed)
	{
		// Grow the tree.
		controlTreeGrowth(new HashMap<String, Double>(), observationsUsed);
	}

	void controlTreeGrowth(Map<String, Double> potentialWeights, List<Integer> observationsUsed)
	{
		// Setup the class weights.
		for (String s : this.processedData.responseData)
		{
			if (!potentialWeights.containsKey(s))
			{
				// Any classes without a weight are assigned a weight of 1.
				potentialWeights.put(s, 1.0);
			}
		}

		// Grow the tree.
		this.cartTree = growTree(observationsUsed, potentialWeights, 0);
	}

	public int countTerminalNodes()
	{
		return this.cartTree.countTerminalNodes();
	}

	/**
	 * Displays the tree.
	 */
	public String display()
	{
		return this.cartTree.display();
	}

	public List<List<Integer>> getProximities(ProcessDataForGrowing procData)
	{
		// Cluster the procData dataset based on the tree.
		List<Integer> observationIndices = new ArrayList<Integer>();
		for (int i = 0; i < procData.numberObservations; i++)
		{
			observationIndices.add(i);
		}
		return this.cartTree.getProximities(procData, observationIndices);
	}

	public List<List<Integer>> getConditionalGrid(ProcessDataForGrowing procData, List<Integer> oobOnThisTree, List<String> covToConditionOn)
	{
		List<List<Integer>> currentGrid = new ArrayList<List<Integer>>();
		currentGrid.add(oobOnThisTree);
		return this.cartTree.getConditionalGrid(procData, currentGrid, covToConditionOn);
	}

	Node growTree(List<Integer> observationsInNode, Map<String, Double> weights, int currentDepth)
	{
		// Determine the counts of each class in the current node.
		int numberOfObservationsInNode = observationsInNode.size();
		Map<String, Integer> classCountsForNode = new HashMap<String, Integer>();
		for (String s : this.processedData.responseData)
		{
			classCountsForNode.put(s, 0);
		}
		for (Integer i : observationsInNode)
		{
			String responseValue = this.processedData.responseData.get(i);
			classCountsForNode.put(responseValue, classCountsForNode.get(responseValue) + 1);
		}
		Set<String> classesPresentInNode = new HashSet<String>();
		for (String s : classCountsForNode.keySet())
		{
			if (classCountsForNode.get(s) > 0)
			{
				classesPresentInNode.add(s);
			}
		}

		//**********************************************
		// Check whether growth should stop.
		//**********************************************
		if (!(classesPresentInNode.size() > 1))
		{
			// There are too few classes present in the observations in the node to warrant a split.
			// A terminal node must therefore be created.
			return new NodeTerminal(classCountsForNode, currentDepth, weights);
		}
		

		//**********************************************
		// Determine the variables to split on.
		//**********************************************
		Set<String> covariablesAvailable = this.processedData.covariableData.keySet();
		List<String> shuffledCovariables = new ArrayList<String>(covariablesAvailable);
		Collections.shuffle(shuffledCovariables, new Random(this.seed));
		int numVarsToSelect = Math.min(covariablesAvailable.size(), ctrl.mtry);
		List<String> variablesToSplitOn = shuffledCovariables.subList(0, numVarsToSelect);

		//**********************************************
		// Try to find a split.
		//**********************************************
		boolean isSplitFound = false;
		double splitValue;
		String covarToSplitOn;

		DetermineSplit splitCalculator = new DetermineSplit();
		ImmutableThreeValues<Boolean, Double, String> splitResult = splitCalculator.findBestSplit(this.processedData.covariableData,
				this.processedData.responseData, observationsInNode, variablesToSplitOn, weights, this.ctrl);
		isSplitFound = splitResult.first;
		splitValue = splitResult.second;
		covarToSplitOn = splitResult.third;

		// Return the Node and continue building the tree.
		if (isSplitFound)
		{
			List<Integer> rightObservations = new ArrayList<Integer>();
			List<Integer> leftObserations = new ArrayList<Integer>();
			// If a valid split was found, then generate a non-terminal node and recurse through its children.
			for (Integer i : observationsInNode)
			{
				// Sort out which observations will go into the right child, and which will go into the left child.
				if (this.processedData.covariableData.get(covarToSplitOn).get(i) > splitValue)
				{
					rightObservations.add(i);
				}
				else
				{
					leftObserations.add(i);
				}
			}
			Node leftChild = growTree(leftObserations, weights, currentDepth + 1);
			Node rightChild = growTree(rightObservations, weights, currentDepth + 1);
			return new NodeNonTerminal(currentDepth, covarToSplitOn, splitValue, leftChild,
					rightChild, classCountsForNode, weights);
		}
		else
		{
			// If there was no valid split found, then return a terminal node.
			return new NodeTerminal(classCountsForNode, currentDepth, weights);
		}

	}

	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData)
	{
		List<Integer> observationsToPredict = new ArrayList<Integer>();
		for (int i = 0; i < predData.numberObservations; i++)
		{
			observationsToPredict.add(i);
		}
		return predict(predData, observationsToPredict);
	}

	Map<Integer, Map<String, Double>> predict(ProcessDataForGrowing predData, List<Integer> observationsToPredict)
	{
		Map<Integer, Map<String, Double>> returnValue = new HashMap<Integer, Map<String, Double>>();
		if (this.cartTree == null)
		{
			System.out.println("The tree can not be used for prediction before it has been trained.");
			System.exit(0);
		}
		else
		{
			for (Integer i : observationsToPredict)
			{
				// The current observation is a mapping from the covariable names to their values.
				Map<String, Double> currentObservation = new HashMap<String, Double>();
				for (String s : predData.covariableData.keySet())
				{
					currentObservation.put(s, predData.covariableData.get(s).get(i));
				}
				returnValue.put(i, this.cartTree.predict(currentObservation));
			}
		}

		return returnValue;
	}

	void save(String outputLocation)
	{
		File outputDirectory = new File(outputLocation);
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

		// Save the tree itself.
		String treeSaveLocation = outputLocation + "/Tree.txt";
		ImmutableTwoValues<String, Integer> treeSave = this.cartTree.save(1, 0);
		try
		{
			FileWriter outputFile = new FileWriter(treeSaveLocation);
			BufferedWriter outputWriter = new BufferedWriter(outputFile);
			outputWriter.write(treeSave.first);
			outputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}

		// Save the control object.
		String controllerSaveLocation = outputLocation + "/Controller.txt";
		this.ctrl.save(controllerSaveLocation);

		// Save the processed data.
		String processedDataSaveLocation = outputLocation + "/ProcessedData.txt";
		this.processedData.save(processedDataSaveLocation);

		// Save the seed.
		String seedSaveLocation = outputLocation + "/Seed.txt";
		try
		{
			FileWriter outputFile = new FileWriter(seedSaveLocation);
			BufferedWriter outputWriter = new BufferedWriter(outputFile);
			outputWriter.write(Long.toString(this.seed));
			outputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}
	}

}
