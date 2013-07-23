package randomjyrest;

import java.util.Arrays;
import java.util.Map;

public class TestDriver {

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Map<String, Map<String, double[]>> processedData = ProcessDataset.main(args[0], Arrays.asList(new String[]{"UPAccession"}), new double[]{99.0});

		int numberOfTrees = 1;
		int mtry = 10;
		int numberOfProcesses = 1;
		boolean isCalculateOOB = true;
		Forest forest = new Forest();
		forest.main(processedData, numberOfTrees, mtry, numberOfProcesses, isCalculateOOB);
	}

}
