/**
 * 
 */
package tree;

/**
 * @author Simon Bull
 */
public final class ImmutableTwoValues<S, T>
{

	public final S first;
	public final T second;

	public ImmutableTwoValues(S first, T second)
	{
		this.first = first;
		this.second = second;
	}

}