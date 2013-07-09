package tree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class TreeGrowthControl
{
	//===================================================================
	//============== Input Data Parsing Control Attributes ==============
	//===================================================================
	/**
	 * The input variables to exclude from being considered as a candidate for splitting a node. The variables in this list
	 * are simply not be read in from the input file.
	 * There can be no overlap between variables in this.variablesToIgnore and this.variablesToUse.
	 */
	public List<String> variablesToIgnore = new ArrayList<String>();

	/**
	 * The input variables to use. If this list is not empty, then all variables in the dataset not in the list will be ignored.
	 * The ignored variables are simply not read in from the input file.
	 * There can be no overlap between variables in this.variablesToIgnore and this.variablesToUse.
	 */
	public List<String> variablesToUse = new ArrayList<String>();

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
		this.variablesToIgnore = new ArrayList<String>(ctrl.variablesToIgnore);
		this.variablesToUse = new ArrayList<String>(ctrl.variablesToUse);
		this.trainingObservations = new ArrayList<Integer>(ctrl.trainingObservations);
		this.minNodeSize = ctrl.minNodeSize;
		this.mtry = ctrl.mtry;
		this.numberOfTreesToGrow = ctrl.numberOfTreesToGrow;
		this.maxTreeDepth = ctrl.maxTreeDepth;
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
			outputWriter.write("\t" + Integer.toString(this.minNodeSize) + "\t");
			if (!this.variablesToUse.isEmpty())
			{
				outputWriter.write(this.variablesToUse.get(0));
				for (int i = 1; i < this.variablesToUse.size(); i++)
				{
					outputWriter.write("," + this.variablesToUse.get(i));
				}
			}
			outputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
