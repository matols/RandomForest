package comparison;

import java.io.File;

public class ComparisonController
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================
		double[] fractionOfPositiveObservationsToKeep = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
		//===================================================================
		//==================== CONTROL PARAMETER SETTING ====================
		//===================================================================

		// Parse input.
		String inputFile = args[0];
		String outputLocation = args[1];
		File outputLDir = new File(outputLocation);
		if (!outputLDir.exists())
		{
			boolean isDirCreated = outputLDir.mkdirs();
			if (!isDirCreated)
			{
				System.out.format("The output directory (%s) does not exist, and could not be created.\n", outputLocation);
				System.exit(0);
			}
		}

		// Split the input file so that each class is treated as positive (and all others as negative)
		SplitDataset datasetSplitter = new SplitDataset();
		datasetSplitter.main(inputFile, outputLocation, fractionOfPositiveObservationsToKeep);
	}

}
