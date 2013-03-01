package tree;

/**
 * @author Simon Bull
 *
 */
public class IndexedDoubleLongData implements Comparable<IndexedDoubleLongData>
{

	private double dataValue;
	private long randomSeed;
	private int index;

	public IndexedDoubleLongData(double data, long randomSeed, int originalIndex)
	{
		this.dataValue = data;
		this.randomSeed = randomSeed;
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

	public long getSeed()
	{
		return randomSeed;
	}

	public int compareTo(IndexedDoubleLongData other)
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