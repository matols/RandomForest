package datasetgeneration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubSampleGeneration
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String inputFileLocation = args[0];
		String outputLocation = args[1];
		String[] sizeOfDatasets = args[2].split("-");
		String[] fractionOfMinority = args[3].split("-");
		int numberOfEachComboToGen = Integer.parseInt(args[4]);

		List<String> positiveObservations = new ArrayList<String>();
		List<String> unlabelledObservations = new ArrayList<String>();

		String headerOne = "";
		String headerTwo = "";
		String headerThree = "";
		// Sort the observations into the positive and unlabelled lists.
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFileLocation), StandardCharsets.UTF_8))
		{
			// Knock off the first three header lines.
			headerOne = reader.readLine();
			int indexOfClassification = headerOne.split("\t").length - 1;
			headerTwo = reader.readLine();
			headerThree = reader.readLine();

			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] splitLine = line.split("\t");
				if (splitLine[indexOfClassification].equals("Positive"))
				{
					positiveObservations.add(line);
				}
				else
				{
					unlabelledObservations.add(line);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		int numberOfObservations = positiveObservations.size() + unlabelledObservations.size();
		int numberPosObs = positiveObservations.size();
		int numberUnlabObs = unlabelledObservations.size();

		// Setup the output directory.
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
			System.out.println("The second argument must be a valid directory location or location where a directory can be created.");
			System.exit(0);
		}

		// Generate the subsets.
		for (String s : sizeOfDatasets)
		{
			int datasetSize = Integer.parseInt(s);
			String datasetSizeLocation = outputLocation + "/" + s;
			File dataSetSizeDirectory = new File(datasetSizeLocation);
			if (!dataSetSizeDirectory.exists())
			{
				boolean isDirCreated = dataSetSizeDirectory.mkdirs();
				if (!isDirCreated)
				{
					System.out.println("The dataset size directory does not exist, but could not be created.");
					System.exit(0);
				}
			}
			else if (!dataSetSizeDirectory.isDirectory())
			{
				// Exists and is not a directory.
				System.out.println("The dataset size directory location exists but is not a directory.");
				System.exit(0);
			}

			for (String p : fractionOfMinority)
			{
				double minorityFraction = Double.parseDouble(p);
				if (minorityFraction == 0)
				{
					// If the fraction of the minority class to include is 0, then set the minority class to be the same fraction as it is in the whole dataset.
					minorityFraction = ((double) numberPosObs) / numberOfObservations;
				}
				int minorityObservations = (int) Math.floor(minorityFraction * datasetSize);
				minorityObservations = Math.min(minorityObservations, numberPosObs);  // Can't have more positive observations than there are.
				int majorityObservations = datasetSize - minorityObservations;

				// Create the dataset size/fraction combo directory.
				String fractionLocation = datasetSizeLocation + "/" + Double.toString(minorityFraction);
				File fractionDirectory = new File(fractionLocation);
				if (!fractionDirectory.exists())
				{
					boolean isDirCreated = fractionDirectory.mkdirs();
					if (!isDirCreated)
					{
						System.out.println("The fraction directory does not exist, but could not be created.");
						System.exit(0);
					}
				}
				else if (!fractionDirectory.isDirectory())
				{
					// Exists and is not a directory.
					System.out.println("The fraction directory location exists but is not a directory.");
					System.exit(0);
				}

				for (int i = 0; i < numberOfEachComboToGen; i++)
				{
					// Randomise the observations for the dataset.
					Collections.shuffle(positiveObservations);
					Collections.shuffle(unlabelledObservations);

					// Write out the dataset.
					String datasetLocation = fractionLocation + "/" + Integer.toString(i) + ".txt";
					try
					{
						// Write out the training data.
						FileWriter datasetOutputFile = new FileWriter(datasetLocation);
						BufferedWriter datasetOutputWriter = new BufferedWriter(datasetOutputFile);
						datasetOutputWriter.write(headerOne);
						datasetOutputWriter.newLine();
						datasetOutputWriter.write(headerTwo);
						datasetOutputWriter.newLine();
						datasetOutputWriter.write(headerThree);
						datasetOutputWriter.newLine();
						for (int j = 0; j < minorityObservations; j++)
						{
							datasetOutputWriter.write(positiveObservations.get(j));
							datasetOutputWriter.newLine();
						}
						for (int j = 0; j < majorityObservations; j++)
						{
							datasetOutputWriter.write(unlabelledObservations.get(j));
							datasetOutputWriter.newLine();
						}
						datasetOutputWriter.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						System.exit(0);
					}
				}
			}
		}
	}

}
