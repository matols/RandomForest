package tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeGrowthControl
{
	//===================================================================
	//=================== Sampling Control Attributes ===================
	//===================================================================
	/**
	 * Whether replacement is used when sampling the observations to grow a tree on.
	 */
	public boolean isReplacementUsed = true;

	/**
	 * The fraction of the observations to select if replacement is not used when sampling.
	 */
	public double selectionFraction = 0.632;

	/**
	 * Controls the number of observations of each class to sample from the input dataset. An empty map means that
	 * this.isReplacementUsed and this.selectionFraction will be used to determine the number of observations sampled.
	 * If this variable is non-empty, then there must be a value supplied for every class in the dataset.
	 * If this.isReplacementUsed == false, then the number of observations of a class s in the dataset must be
	 * >= the value (sampSize.get(s) / this.selectionFraction).
	 * Cannot be used when this.isStratifiedBootstrapUsed == true.
	 */
	public Map<String, Integer> sampSize = new HashMap<String, Integer>();

	/**
	 * Controls whether the sampling of the observations should be stratified.
	 * Can only be used when this.sampSize is an empty Map.
	 */
	public boolean isStratifiedBootstrapUsed = true;

	//===================================================================
	//============== Input Data Parsing Control Attributes ==============
	//===================================================================
	/**
	 * The input variables to exclude from being considered as a candidate for splitting a node. The variables in this list
	 * are simply not be read in from the input file.
	 */
	public List<String> variablesToIgnore = new ArrayList<String>();

	/**
	 * The observations to include when growing a tree. The list contains the integer index of the observations to keep (with 0 being the first
	 * observation in the file containing the dataset). Any observation in the file which does not have its index in this list will not be read
	 * in from the file. If the list is empty, then it is assumed that all observations in the file should be used. Any indices that are in the
	 * list but not in the dataset will be ignored. For example, if 10 is in the list but there are only 5 observations in the dataset, then 10
	 * will be ignored.
	 * The indices of the observations used will be shifted if this list is non-empty. The smallest observation index in the list will be set to
	 * be index 0 in the ProcessDataForGrowing object. For example, if trainingObservations = {3, 6, 1, 8, 5} (i.e. use only
	 * observations with index 1, 3, 5, 6 and 8), then the old indices will be mapped to their new indices so that 3->1, 6->3, 1->0, 8->4 and 5->2.
	 * Therefore, feature F from observation 3 in the original file, will now be found as feature F from observation 1 in the ProcessDataForGrowing
	 * object.
	 */
	public List<Integer> trainingObservations = new ArrayList<Integer>();

	/**
	 * If this is true the variables have their values scaled using ((Xi - Min) / (Max - Min)), where Xi is the value of the
	 * variable for observation i, Min is the minimum value of the variable and Max is the maximum value of the variable. 
	 */
	public boolean isScaled = false;

	/**
	 * If this is true then the values of a variables are standardised so that they have 0 mean and a standard deviation of 1.
	 * If any variables have 0 standard deviations, then the values for the variable will all become NaN. It is necessary to either
	 * add these variables to this.variablesToIgnore, or to compensate for the NaNs.
	 */
	public boolean isStandardised = false;

	//===================================================================
	//============== Forest/Tree Growth Control Attributes ==============
	//===================================================================
	/**
	 * The minimum number of observations that must be present in a node in order for it to be considered for splitting.
	 * If a split would cause less than this number of observations to be in any of the child nodes, then the split is
	 * not permitted to go ahead.
	 * Increasing this will speed up the growth of the tree.
	 */
	public int minNodeSize = 1;

	/**
	 * The number of input variables that are randomly selected as candidates for splitting in a node.
	 * Increasing this will slow down the growth of the tree.
	 */
	public int mtry = Integer.MAX_VALUE;

	/**
	 * The number of trees to grow in the forest.
	 * Increasing this will make the forest take longer to grow.
	 */
	public int numberOfTreesToGrow = 500;

	/**
	 * Whether or not the OOB error rate and confusion matrix should be calculated.
	 * Setting this to true will increase the training time of the forest.
	 */
	public boolean isCalculateOOB = true;

	/**
	 * The maximum depth that a tree should be grown to. Assumes that a tree containing only a root node has a depth of 1.
	 * Increasing this will slow down the growth of the tree.
	 */
	public int maxTreeDepth = Integer.MAX_VALUE;


	//===================================================================
	//======================== Class Constructor ========================
	//===================================================================
	public TreeGrowthControl()
	{
	}

	public TreeGrowthControl(TreeGrowthControl ctrl)
	{
		// Copy constructor.
		this.isReplacementUsed = ctrl.isReplacementUsed;
		this.selectionFraction = ctrl.selectionFraction;
		this.sampSize = new HashMap<String, Integer>(ctrl.sampSize);
		this.isStratifiedBootstrapUsed = ctrl.isStratifiedBootstrapUsed;
		this.variablesToIgnore = new ArrayList<String>(ctrl.variablesToIgnore);
		this.trainingObservations = new ArrayList<Integer>(ctrl.trainingObservations);
		this.isScaled = ctrl.isScaled;
		this.isStandardised = ctrl.isStandardised;
		this.minNodeSize = ctrl.minNodeSize;
		this.mtry = ctrl.mtry;
		this.numberOfTreesToGrow = ctrl.numberOfTreesToGrow;
		this.isCalculateOOB = ctrl.isCalculateOOB;
		this.maxTreeDepth = ctrl.maxTreeDepth;
	}

	public TreeGrowthControl(String location)
	{
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(location), StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			String[] ctrlVariables = line.split("\t");

			this.minNodeSize = Integer.parseInt(ctrlVariables[0]);
			this.mtry = Integer.parseInt(ctrlVariables[1]);
			this.numberOfTreesToGrow = Integer.parseInt(ctrlVariables[2]);
			this.variablesToIgnore = new ArrayList<String>();
			if (!ctrlVariables[3].equals(""))
			{
				String[] covToIgnore = ctrlVariables[3].split(",");
				for (String s : covToIgnore)
				{
					this.variablesToIgnore.add(s);
				}
			}
			this.isReplacementUsed = Boolean.parseBoolean(ctrlVariables[4]);
			this.selectionFraction = Double.parseDouble(ctrlVariables[5]);
			this.sampSize = new HashMap<String, Integer>();
			if (!ctrlVariables[6].equals(""))
			{
				String[] mappings = ctrlVariables[6].split(",");
				for (String s : mappings)
				{
					String[] keyValueSplit = s.split("-");
					this.sampSize.put(keyValueSplit[0], Integer.parseInt(keyValueSplit[1]));
				}
			}
			this.isStratifiedBootstrapUsed = Boolean.parseBoolean(ctrlVariables[7]);
			this.isCalculateOOB = Boolean.parseBoolean(ctrlVariables[8]);
			this.trainingObservations = new ArrayList<Integer>();
			if (!ctrlVariables[9].equals("-"))
			{
				String[] trainingObservations = ctrlVariables[9].split(",");
				for (String s : trainingObservations)
				{
					this.trainingObservations.add(Integer.parseInt(s));
				}
			}
			this.maxTreeDepth = Integer.parseInt(ctrlVariables[10]);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}


	//===================================================================
	//========================== Class Methods ==========================
	//===================================================================
	/**
	 * Method to save the control object to a file.
	 * 
	 * @param location The location where the file should be saved.
	 */
	public void save(String location)
	{
		try
		{
			FileWriter outputFile = new FileWriter(location);
			BufferedWriter outputWriter = new BufferedWriter(outputFile);
			outputWriter.write(Integer.toString(this.minNodeSize) + "\t");
			outputWriter.write(Integer.toString(this.mtry) + "\t");
			outputWriter.write(Integer.toString(this.numberOfTreesToGrow) + "\t");
			if (!this.variablesToIgnore.isEmpty())
			{
				outputWriter.write(this.variablesToIgnore.get(0));
				for (int i = 1; i < this.variablesToIgnore.size(); i++)
				{
					outputWriter.write("," + this.variablesToIgnore.get(i));
				}
			}
			outputWriter.write("\t");
			outputWriter.write(Boolean.toString(this.isReplacementUsed) + "\t");
			outputWriter.write(Double.toString(this.selectionFraction) + "\t");
			if (!this.sampSize.isEmpty())
			{
				List<String> classes = new ArrayList<String>(this.sampSize.keySet());
				outputWriter.write(classes.get(0) + "-" + Integer.toString(this.sampSize.get(classes.get(0))));
				for (int i = 1; i < classes.size(); i++)
				{
					outputWriter.write("," + classes.get(i) + "-" + Integer.toString(this.sampSize.get(classes.get(i))));
				}
				
			}
			outputWriter.write("\t");
			outputWriter.write(Boolean.toString(this.isStratifiedBootstrapUsed) + "\t");
			outputWriter.write(Boolean.toString(this.isCalculateOOB) + "\t");
			if (!this.trainingObservations.isEmpty())
			{
				outputWriter.write(this.trainingObservations.get(0));
				for (int i = 1; i < this.trainingObservations.size(); i++)
				{
					outputWriter.write("," + this.trainingObservations.get(i));
				}
				
			}
			else
			{
				outputWriter.write("-");
			}
			outputWriter.write("\t" + Integer.toString(this.minNodeSize));
			outputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
