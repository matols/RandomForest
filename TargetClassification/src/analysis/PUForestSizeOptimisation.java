package analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tree.PUProcessDataForGrowing;
import tree.PUTreeGrowthControl;

public class PUForestSizeOptimisation
{

	/**
	 * Used in the optimisation of the numberOfTreesToGrow parameter.
	 * 
	 * @param args - The file system locations of the files and directories used in the optimisation.
	 */
	public static void main(String[] args)
	{
		String inputFile = args[0];  // The location of the dataset used to grow the forests.
		String resultsDir = args[1];  // The location where the results and records of the optimisation will go.
		String discountLoc = args[2];  // The location of a dataset containing the information to sue for the discounting.

		Map<String, Map<Integer, Double>> discounts = new HashMap<String, Map<Integer, Double>>();
		discounts.put("Positive", new HashMap<Integer, Double>());
		discounts.put("Unlabelled", new HashMap<Integer, Double>());

		Map<String, Integer> indexMapping = new HashMap<String, Integer>();
		Path dataPath = Paths.get(inputFile);
		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = null;
			line = reader.readLine();
			line = reader.readLine();
			line = reader.readLine();
			int index = 0;
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.trim();
				String[] splitLine = line.split("\t");
				String acc = splitLine[0];
				indexMapping.put(acc, index);
				index++;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		dataPath = Paths.get(discountLoc);
		try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8))
		{
			String line = null;
			line = reader.readLine();
			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
				{
					// If the line is made up of all whitespace, then ignore the line.
					continue;
				}
				line = line.trim();
				String[] splitLine = line.split("\t");
				if (splitLine[3].equals("Unlabelled"))
				{
					String acc = splitLine[0];
					double posWeight = Double.parseDouble(splitLine[1]);
					double unlabWeight = Double.parseDouble(splitLine[2]);
					double posFrac = posWeight / (posWeight + unlabWeight);
					discounts.get("Positive").put(indexMapping.get(acc), posFrac);
					discounts.get("Unlabelled").put(indexMapping.get(acc), 1 - posFrac);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		String testFileLocation = null;  // The location of a dataset to test on the forests grown, but not to use in their growing.
		if (args.length >= 4)
		{
			// Only record an actual location if there are at least three argument supplied.
			testFileLocation = args[3];
		}

		main(inputFile, resultsDir, discounts, testFileLocation);
	}

	/**
	 * @param inputFile - The location of the dataset used to grow the forests.
	 * @param resultsDir - The location where the results and records of the optimisation will go.
	 * @param discounts - The values to discount the positive and unlabelled weight with.
	 * @param testFileLocation - The location of a dataset to test on the forests grown, but not to use in their growing.
	 */
	public static void main(String inputFile, String resultsDir, Map<String, Map<Integer, Double>> discounts, String testFileLocation)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		int numberOfForestsToCreate = 100;  // The number of forests to create for each forest size.
		Integer[] forestSizesToUse = {  // The different number of trees to test in the forest.
				50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900,
				950, 1000, 1050, 1100, 1150, 1200, 1250, 1300, 1350, 1400, 1450, 1500, 1550, 1600, 1650, 1700, 1750, 1800, 1850,
				1900, 1950, 2000, 2050, 2100, 2150, 2200, 2250, 2300, 2350, 2400, 2450, 2500, 2550, 2600, 2650, 2700, 2750, 2800,
				2850, 2900, 2950, 3000, 3050, 3100, 3150, 3200, 3250, 3300, 3350, 3400, 3450, 3500, 3550, 3600, 3650, 3700, 3750,
				3800, 3850, 3900, 3950, 4000, 4050, 4100, 4150, 4200, 4250, 4300, 4350, 4400, 4450, 4500, 4550, 4600, 4650, 4700,
				4750, 4800, 4850, 4900, 4950, 5000};
		Integer[] trainingObsToUse = {};  // The observations in the training set that will be used in growing the forests.

		Map<String, Double> weights = new HashMap<String, Double>();  // A mapping from class names to the weight that will be used for the class.
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);

		// Default parameters for the tree growth and input dataset processing controller object.
		PUTreeGrowthControl ctrl = new PUTreeGrowthControl();
		ctrl.minNodeSize = 1;
		ctrl.mtry = 10;
		ctrl.positiveFractionTerminalCutoff = 0.5;
		ctrl.trainingObservations = Arrays.asList(trainingObsToUse);
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Setup the directory for the results.
		File resultsDirectory = new File(resultsDir);
		if (!resultsDirectory.exists())
		{
			boolean isDirCreated = resultsDirectory.mkdirs();
			if (!isDirCreated)
			{
				System.out.println("The results directory could not be created.");
				System.exit(0);
			}
		}
		else if (!resultsDirectory.isDirectory())
		{
			// Exists and is not a directory.
			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
			System.exit(0);
		}

		// Process the input dataset.
		PUProcessDataForGrowing procData = new PUProcessDataForGrowing(inputFile, ctrl);

		// Determine the classes in the input dataset.
		List<String> classesInDataset = new ArrayList<String>(new HashSet<String>(procData.responseData));
		Collections.sort(classesInDataset);

		// Initialise the parameter and controller object record files.
		String errorRateResultsLocation = resultsDir + "/ErrorResults.txt";
		String gMeanResultsLocation = resultsDir + "/GMeanResults.txt";
		String mccResultsLocation = resultsDir + "/MCCResults.txt";
		String parameterLocation = resultsDir + "/Parameters.txt";
		String controllerLocation = resultsDir + "/ControllerUsed.txt";
		try
		{
			// Record the parameters.
			FileWriter parameterOutputFile = new FileWriter(parameterLocation);
			BufferedWriter parameterOutputWriter = new BufferedWriter(parameterOutputFile);
			parameterOutputWriter.write("Forest sizes used - " + Arrays.toString(forestSizesToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Number of forests grown - " + Integer.toString(numberOfForestsToCreate));
			parameterOutputWriter.newLine();
			if (testFileLocation == null)
			{
				parameterOutputWriter.write("No test set used");
			}
			else
			{
				parameterOutputWriter.write("A set is used.");
			}
			parameterOutputWriter.newLine();
			parameterOutputWriter.write("Weights used");
			parameterOutputWriter.newLine();
			for (String s : weights.keySet())
			{
				parameterOutputWriter.write("\t" + s + " - " + Double.toString(weights.get(s)));
				parameterOutputWriter.newLine();
			}
			parameterOutputWriter.write("Training observations used - " + Arrays.toString(trainingObsToUse));
			parameterOutputWriter.newLine();
			parameterOutputWriter.close();

			ctrl.save(controllerLocation);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// Generate all the random seeds to use in growing the forests. The same numberOfForestsToCreate seeds will be used for every forest size.
		// This ensures that the only difference in the results is due to the chosen forest size.
		Random randGen = new Random();
		List<Long> seeds = new ArrayList<Long>();
		for (int i = 0; i < numberOfForestsToCreate; i++)
		{
			long seedToUse = randGen.nextLong();
			while (seeds.contains(seedToUse))
			{
				seedToUse = randGen.nextLong();
			}
			seeds.add(seedToUse);
		}

		for (Integer i : forestSizesToUse)
		{
			DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date startTime = new Date();
		    String strDate = sdfDate.format(startTime);
			System.out.format("Now testing forests of size %d at %s.\n", i, strDate);

			ctrl.numberOfTreesToGrow = i;

			PUTestRunner runner = new PUTestRunner();
			runner.generateForests(weights, ctrl, inputFile, seeds, numberOfForestsToCreate, testFileLocation, discounts);
			Map<String, List<Double>> statistics = runner.calculateStats();  // The record of the statistics of the predictions.

			// Write out the statistics for this forest size.
			try
			{
				FileWriter errorResultsFile = new FileWriter(errorRateResultsLocation, true);
				FileWriter  gMeanResultsFile = new FileWriter(gMeanResultsLocation, true);
				FileWriter mccResultsFile = new FileWriter(mccResultsLocation, true);

				BufferedWriter errorResultsOutputWriter = new BufferedWriter(errorResultsFile);
				BufferedWriter gMeanResultsOutputWriter = new BufferedWriter(gMeanResultsFile);
				BufferedWriter mccResultsOutputWriter = new BufferedWriter(mccResultsFile);

				errorResultsOutputWriter.write(Integer.toString(i));
				gMeanResultsOutputWriter.write(Integer.toString(i));
				mccResultsOutputWriter.write(Integer.toString(i));

				for (Double d : statistics.get("ErrorRate"))
				{
					errorResultsOutputWriter.write("\t" + Double.toString(d));
				}
				for (Double d : statistics.get("GMean"))
				{
					gMeanResultsOutputWriter.write("\t" + Double.toString(d));
				}
				for (Double d : statistics.get("MCC"))
				{
					mccResultsOutputWriter.write("\t" + Double.toString(d));
				}

				errorResultsOutputWriter.newLine();
				gMeanResultsOutputWriter.newLine();
				mccResultsOutputWriter.newLine();

				errorResultsOutputWriter.close();
				gMeanResultsOutputWriter.close();
				mccResultsOutputWriter.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

}