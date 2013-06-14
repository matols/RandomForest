package instanceselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.TreeGrowthControl;

public class Controller
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Required inputs.
		String inputLocation = args[0];  // The location of the file containing the dataset to use in the instance selection.
		File inputFile = new File(inputLocation);
		if (!inputFile.isFile())
		{
			System.out.println("The first argument must be a valid file location, and must contain the dataset for instance selection.");
			System.exit(0);
		}
		String outputLocation = args[1];  // The location to store any and all results.
		File outputDirectory = new File(outputLocation);
		if (!outputDirectory.exists())
		{
			boolean isDirCreated = outputDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The output directory could not be created.");
				System.exit(0);
			}
		}
		else if (!outputDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
			System.exit(0);
		}

		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 5;
		ctrl.mtry = 10;
		ctrl.isStratifiedBootstrapUsed = true;
		ctrl.isCalculateOOB = false;

		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);

		Map<String, Integer> fractionsToSelect = new HashMap<String, Integer>();
		fractionsToSelect.put("Unlabelled", 5);
		fractionsToSelect.put("Positive", 5);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		String[] newGAArgs = new String[args.length];
		for (int k = 2; k < args.length; k++)
		{
			newGAArgs[k] = args[k];
		}

		// Run the GA.
		newGAArgs[0] = args[0];
		newGAArgs[1] = outputLocation;
		TreeGrowthControl thisGAControl = new TreeGrowthControl(ctrl);
		new chc.InstanceSelection(newGAArgs, thisGAControl, weights, fractionsToSelect);

		// Extract the best individual.
		List<Integer> bestIndividual = new ArrayList<Integer>();
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(outputLocation + "/BestIndividuals.txt"), StandardCharsets.UTF_8))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				else if (line.contains("Fitness"))
				{
					// The line indicates the fitness of the individual, so skip it.
					continue;
				}
				line = line.replace("[", "");
				line = line.replace("]", "");
				String[] splitLine = line.split("\t");
				for (String r : splitLine[0].split(", "))
				{
					bestIndividual.add(Integer.parseInt(r));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		int numberOfObservations = 0;
		Map<Integer, String> indexToLineMap = new HashMap<Integer, String>();
		String headerOne = "";
		String headerTwo = "";
		String headerThree = "";
		try
		{
			BufferedReader inputReader = new BufferedReader(new FileReader(inputLocation));
			headerOne = inputReader.readLine().trim();
			headerTwo = inputReader.readLine().trim();
			headerThree = inputReader.readLine().trim();
			String line;
			while ((line = inputReader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				indexToLineMap.put(numberOfObservations, line);
				numberOfObservations += 1;
			}
			inputReader.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		try
		{
			String keptProteinsOutputLocation = outputLocation + "/ProteinsToKeep.txt";
			String removedProteinsOutputLocation = outputLocation + "/ProteinsToDiscard.txt";
			FileWriter keptProteinsOutputFile = new FileWriter(keptProteinsOutputLocation);
			BufferedWriter keptProteinsOutputWriter = new BufferedWriter(keptProteinsOutputFile);
			FileWriter removedProteinsOutputFile = new FileWriter(removedProteinsOutputLocation);
			BufferedWriter removedProteinsOutputWriter = new BufferedWriter(removedProteinsOutputFile);
			keptProteinsOutputWriter.write(headerOne);
			keptProteinsOutputWriter.newLine();
			keptProteinsOutputWriter.write(headerTwo);
			keptProteinsOutputWriter.newLine();
			keptProteinsOutputWriter.write(headerThree);
			keptProteinsOutputWriter.newLine();
			removedProteinsOutputWriter.write(headerOne);
			removedProteinsOutputWriter.newLine();
			removedProteinsOutputWriter.write(headerTwo);
			removedProteinsOutputWriter.newLine();
			removedProteinsOutputWriter.write(headerThree);
			removedProteinsOutputWriter.newLine();
			for (int i = 0; i < numberOfObservations; i++)
			{
				if (bestIndividual.contains(i))
				{
					keptProteinsOutputWriter.write(indexToLineMap.get(i));
					keptProteinsOutputWriter.newLine();
				}
				else
				{
					removedProteinsOutputWriter.write(indexToLineMap.get(i));
					removedProteinsOutputWriter.newLine();
				}
			}
			keptProteinsOutputWriter.close();
			removedProteinsOutputWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
}
