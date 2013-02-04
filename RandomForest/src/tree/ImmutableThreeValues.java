/**
 * 
 */
package tree;

/**
 * @author Simon
 *
 */
public final class ImmutableThreeValues<R, S, T>
{

	public final R first;
	public final S second;
	public final T third;

	public ImmutableThreeValues(R first, S second, T third)
	{
		this.first = first;
		this.second = second;
		this.third = third;
	}

}
