package utilities;

/**
 * @author Simon
 * 
 * Implements a class for permanently associating two values.
 *
 * @param <S>
 * @param <T>
 * @param <U>
 * @param <V>
 */
public class ImmutableFourValues<S, T, U, V>
{

	public final S first;
	public final T second;
	public final U third;
	public final V fourth;

	public ImmutableFourValues(S first, T second, U third, V fourth)
	{
		this.first = first;
		this.second = second;
		this.third = third;
		this.fourth = fourth;
	}

}
