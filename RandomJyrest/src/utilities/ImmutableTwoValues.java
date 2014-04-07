/**
 * 
 */
package utilities;

/**
 * @author Simon
 * 
 * Implements a class for permanently associating two values.
 *
 * @param <S>
 * @param <T>
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