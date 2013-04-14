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

	/**
	 * The number of observations that must be present in the node in order for it to be considered for splitting.
	 * Equivalent to the ndsize argument from Breiman's Fortran 77 code.
	 */
	public int minNodeSize = 1;

	/**
	 * The number of input variables randomly sampled as candidates at each node.
	 */
	public int mtry = Integer.MAX_VALUE;

	/**
	 * The number of trees to grow (only use for growing a forest).
	 */
	public int numberOfTreesToGrow = 500;

	/**
	 * The variables to exclude from the growing of the tree.
	 */
	public List<String> variablesToIgnore = new ArrayList<String>();

	/**
	 * Whether or not to use replacement when selecting the examples to grow the tree (only use for growing a forest).
	 */
	public boolean isReplacementUsed = false;

	/**
	 * The fraction of observations to select if replacement is not used (only use for growing a forest).
	 */
	public double selectionFraction = 0.632;

	/**
	 * Controls for the size of and fraction of each class in the bootstrap sample.
	 */
	public Map<String, Integer> sampSize = new HashMap<String, Integer>();

	/**
	 * Controls whether the bootstrap sample should be stratified. This takes precedence over any values in sampSize.
	 */
	public boolean isStratifiedBootstrapUsed = false;

	public boolean calculateOOB = true;

	public TreeGrowthControl()
	{
	}

	public TreeGrowthControl(TreeGrowthControl ctrl)
	{
		// Copy constructor.
		this.minNodeSize = ctrl.minNodeSize;
		this.mtry = ctrl.mtry;
		this.numberOfTreesToGrow = ctrl.numberOfTreesToGrow;
		this.variablesToIgnore = new ArrayList<String>(this.variablesToIgnore);
		this.isReplacementUsed = ctrl.isReplacementUsed;
		this.selectionFraction = ctrl.selectionFraction;
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
					System.out.println(s);
					String[] keyValueSplit = s.split("-");
					this.sampSize.put(keyValueSplit[0], Integer.parseInt(keyValueSplit[1]));
				}
			}
			this.isStratifiedBootstrapUsed = Boolean.parseBoolean(ctrlVariables[7]);
			this.calculateOOB = Boolean.parseBoolean(ctrlVariables[8]);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

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
			outputWriter.write(Boolean.toString(this.calculateOOB));
			outputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

}
