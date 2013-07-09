package tree;

/**
 * @author Simon Bull
 *
 */
public class IndexedDoubleData implements Comparable<IndexedDoubleData>
{

	private double dataValue;
	private int index;

	public IndexedDoubleData(double data, int originalIndex)
	{
		this.dataValue = data;
		this.index = originalIndex;
	}

	public double getData()
	{
		return dataValue;
	}

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