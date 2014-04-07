package utilities;

/**
 * @author Simon
 * 
 * Implements a class for permanently associating three values.
 *
 * @param <S>
 * @param <T>
 * @param <U>
 */
public class ImmutableThreeValues<S, T, U>
{

	public final S first;
	public final T second;
	public final U third;

	public ImmutableThreeValues(S first, T second, U third)
	{
		this.first = first;
		this.second = second;
		this.third = third;
	}

}
