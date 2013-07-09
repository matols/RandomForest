/**
 * 
 */
package tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Simon Bull
 *
 */
public class ProcessDataForGrowing
{

	/**
	 * The file containing the data from which the tree was grown.
	 */
	public String dataFileGrownFrom = "";

	/**
	 * 
	 */
	public Map<String, List<Double>> covariableData = new HashMap<String, List<Double>>();

	/**
	 * The number of observations in the data file.
	 */
	public int numberObservations;

	/**
	 * 
	 */
	public List<String> responseData = new ArrayList<String>();

	public ProcessDataForGrowing()
	{
	}

	/**
	 * Copy constructor.
	 * 
	 * @param toCopy
	 */
	public ProcessDataForGrowing(ProcessDataForGrowing toCopy)
	{
		this.dataFileGrownFrom = toCopy.dataFileGrownFrom;
		this.covariableData = new HashMap<String, List<Double>>();
		for (String s : toCopy.covariableData.keySet())
		{
			this.covariableData.put(s, new ArrayList<Double>(toCopy.covariableData.get(s)));
		}
		this.numberObservations = toCopy.numberObservations;
		this.responseData = new ArrayList<String>();
		this.responseData.addAll(toCopy.responseData);
	}

	/**
	 * Processes the data for growing.
	 *
	 * @return A class containing the mapping of covariable names to lists of their observed values, classifications to lists of their occurrences and the number of observations.
	 */
	public ProcessDataForGrowing(String data, TreeGrowthControl ctrl)
	{

		if (!ctrl.variablesToIgnore.isEmpty() && !ctrl.variablesToUse.isEmpty())
		{
			// If variables are specified as being ignored and kept, then ensure that there are no overlaps in the lists.
			Set<String> overlap = new HashSet<String>();
			for (String s : ctrl.variablesToIgnore)
			{
				if (ctrl.variablesToUse.contains(s))
				{
					overlap.add(s);
				}
			}
			if (!overlap.isEmpty())
			{
				System.out.print("The variables: ");
				System.out.print(overlap);
				System.out.println(" are specified as being both used and ignored in the TreeGrowthControl object.");
				System.exit(0);
			}
		}

		this.dataFileGrownFrom = data;
		int responseColumn = 0;  // The variable indicating which column in the data file is the classifications.
		this.numberObservations = 0;
		String[] variableNames = null;  // The names of all the variables in the order they are in the file.
		String[] variableTypes = null;  // The types of all the variables in the order they are in the file.
		Set<String> possibleClasses = new HashSet<String>();
		Path dataPath = Paths.get(this.dataFileGrownFrom);

		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = null;

			// Record the names of the variables.
			line = reader.readLine();
			line = line.replaceAll("\n", "");
			variableNames = line.split("\t");

			// If the number of variables to exclude means that there are less than two variables remaining,
			// then alert the user and exit.
			if ((variableNames.length - ctrl.variablesToIgnore.size()) < 2)
			{
				System.out.println("You have masked out too many variables. You must leave at least one covariable and one response variable.");
				System.exit(0);
			}

			// Determine the variables that are to be used.
			List<String> variablesToUse = ctrl.variablesToUse;
			if (variablesToUse.isEmpty())
			{
				variablesToUse = Arrays.asList(variableNames);
			}

			// Record the type of the variables in the file.
			line = reader.readLine();
			line = line.toLowerCase();
			line = line.replaceAll("\n", "");
			variableTypes = line.split("\t");

			//TODO - Remove this line/requirement.
			// Record the specified support (number of categories) of the categorical variables.
			line = reader.readLine();

			// Check that the types of the variables are correct, and that the correct number of response variables have been defined.
			int responseCount = 0;  // The number of variables that the user has indicated to be response variables.
			boolean isUnknownType = false;  // Whether or not an unknown type was encountered.
			for (int i = 0; i < variableTypes.length; i++)
			{
				if (variableTypes[i].equals("r"))
				{
					responseCount++;
					responseColumn = i;
				}
				else if (!variableTypes[i].equals("n") && !variableTypes[i].equals("x"))
				{
					isUnknownType = true;
				}
			}
			// If there was not only 1 class variable declared, then alert the user and exit.
			if (responseCount != 1)
			{
				System.out.format("You must define exactly one input variable to be the response. You defined %d.", responseCount);
				System.exit(0);
			}
			// If the response variable has been marked as being ignored by the user, then alert the user and exit.
			if (ctrl.variablesToIgnore.contains(variableNames[responseColumn]))
			{
				System.out.println("You have selected to ignore the response variable in your tree growth control parameters.");
				System.exit(0);
			}
			// If an unknown type was encountered, then alert the user and exit.
			if (isUnknownType)
			{
				System.out.println("The only variable types permitted are 'r', 'n', and 'x'");
				System.exit(0);
			}

			// Turn the data into distinct columns, one for each covariable.
			for (int i = 0; i < variableNames.length; i++)
			{
				if (variableTypes[i].equals("n"))
				{
					this.covariableData.put(variableNames[i], new ArrayList<Double>());
				}
			}
			int currentObservationIndex = 0;  // Used to determine the index of the current observation in the file.
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				if (!ctrl.trainingObservations.isEmpty() && !ctrl.trainingObservations.contains(currentObservationIndex))
				{
					// If the current observation is not in the list of observations to be used, and there is a subset of
					// observations specified, then ignore the observation.
					currentObservationIndex += 1;
					continue;
				}
				currentObservationIndex += 1;
				this.numberObservations += 1;
				line = line.replaceAll("\n", "");
				String[] splitLine = line.split("\t");
				for (int i = 0; i < splitLine.length; i++)
				{
					if (i == responseColumn)
					{
						this.responseData.add(splitLine[i]);  // Record the classification for this observation.
						possibleClasses.add(splitLine[i]);  // Record all the possible values that the class variable can take.
					}
					else if (!ctrl.variablesToIgnore.contains(variableNames[i]) && variablesToUse.contains(variableNames[i]))
					{
						// Record only the variables that are not marked as to be ignored (by either input or from the data
						// file having an x for the variable type) and are intended ot be used.
						if (variableTypes[i].equals("n"))
						{
							this.covariableData.get(variableNames[i]).add(Double.parseDouble(splitLine[i]));
						}
					}
				}
			}

			// Remove records of covariables that were specified as being ignored in the TreeGrowthControl object.
			for (String s : variableNames)
			{
				if (ctrl.variablesToIgnore.contains(s) || !variablesToUse.contains(s))
				{
					// If the variable is to be ignored, then remove it from the covariable data record.
					covariableData.remove(s);
				}
			}
		}
		catch (IOException e)
		{
			// Caught an error while reading the file. Indicate this and exit.
			System.out.println("There was an error while processing the input data file.");
			e.printStackTrace();
			System.exit(0);
		}

		

	}
	

	/**
	 * Add a new covariable to the dataset.
	 * 
	 * @param covar - The name of the covariable to add (must be different from all other names).
	 * @param dataList - The list of data that corresponds to the covariable.
	 */
	public void addCovariable(String covar, List<Double> dataList)
	{
		if (covariableData.containsKey(covar))
		{
			System.out.format("The covariable %s already exists in the dataset.", covar);
			System.exit(0);
		}
		this.covariableData.put(covar, dataList);
	}

}
