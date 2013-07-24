package utilities;

import java.util.HashMap;
import java.util.Map;

public class ArrayManipulation
{
	
	public static final double[] cumulativeArray(double[] vector, int[] indicesToUse)
	{
		double[] returnValue = new double[indicesToUse.length];
		double cumulativeTotal = 0.0;
		int currentInsertionIndex = 0;
		for (int i : indicesToUse)
		{
			cumulativeTotal += vector[i];
			returnValue[currentInsertionIndex] = cumulativeTotal;
			currentInsertionIndex++;
		}
		return returnValue;
	}

	public static final double[] selectSubset(double[] entireArray, int[] subsetToSelect)
	{
		double[] returnValue = new double[subsetToSelect.length];
		int currentInsertionIndex = 0;
		for (int i : subsetToSelect)
		{
			returnValue[currentInsertionIndex] = entireArray[i];
			currentInsertionIndex++;
		}
		return returnValue;
	}
	
	public static final double sumArray(double[] toSum)
	{
		double sum = 0.0;
		for (double d : toSum)
		{
			sum += d;
		}
		return sum;
	}
	
	public static final Map<Double, Integer> unique(double[] vector)
	{
		Map<Double, Integer> unique = new HashMap<Double, Integer>();
		int vectorLength = vector.length;
		for (int i = 0; i < vectorLength; i++)
		{
			unique.put(vector[i], i);
		}
		return unique;
	}

}
