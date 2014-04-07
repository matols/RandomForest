package utilities;

/**
 * Implements a class for associating an index with a data value.
 */
public class IndexedDoubleData implements Comparable<IndexedDoubleData>
{

	private double dataValue;  // The value associated with an index.
	private int index;  // The index.

	/**
	 * Class constructor for associating an index with a value.
	 * 
	 * @param data				The data associated with an index.
	 * @param originalIndex		The index.
	 */
	public IndexedDoubleData(double data, int originalIndex)
	{
		this.dataValue = data;
		this.index = originalIndex;
	}

	/**
	 * @return		The data.
	 */
	public double getData()
	{
		return dataValue;
	}

	/**
	 * @return		The index.
	 */
	public int getIndex()
	{
		return index;
	}


	public int compareTo(IndexedDoubleData other)
	{
		if (this.getData() == other.getData())
		{
			return 0;
		}
		else if (this.getData() > other.getData())
		{
			return 1;
		}
		else
		{
			return -1;
		}
	}
}