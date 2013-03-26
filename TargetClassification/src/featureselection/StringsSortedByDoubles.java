package featureselection;

/**
 * @author Simon Bull
 *
 */

public class StringsSortedByDoubles implements Comparable<StringsSortedByDoubles>
{

	private double dataValue;
	private String id;

	public StringsSortedByDoubles(double data, String id)
	{
		this.dataValue = data;
		this.id = id;
	}

	public double getData()
	{
		return this.dataValue;
	}

	public String getId()
	{
		return this.id;
	}

	public int compareTo(StringsSortedByDoubles other)
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