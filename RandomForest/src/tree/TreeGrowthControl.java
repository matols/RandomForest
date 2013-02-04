package tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

	public TreeGrowthControl()
	{
	}

	public TreeGrowthControl(String location)
	{
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(location), StandardCharsets.UTF_8))
		{
			String line = reader.readLine();
			line = line.replaceAll("\n", "");
			String ctrlVariables[] = line.split("\t");

			this.minNodeSize = Integer.parseInt(ctrlVariables[0]);
			this.mtry = Integer.parseInt(ctrlVariables[1]);
			this.numberOfTreesToGrow = Integer.parseInt(ctrlVariables[2]);
			this.variablesToIgnore = new ArrayList<String>();
			if (!ctrlVariables[3].equals(""))
			{
				String covToIgnore[] = ctrlVariables[10].split(",");
				for (String s : covToIgnore)
				{
					this.variablesToIgnore.add(s);
				}
			}
			this.isReplacementUsed = Boolean.parseBoolean(ctrlVariables[4]);
			this.selectionFraction = Double.parseDouble(ctrlVariables[5]);
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
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
			outputWriter.close();
		}
		catch (Exception e)
		{
			System.err.println(e.getStackTrace());
			System.exit(0);
		}
	}

}
