package argparser;

/**
  * Wrapper class which ``holds'' a long value,
  * enabling methods to return long values through
  * arguments.
  */
public class LongHolder implements java.io.Serializable
{
	/**
	 * Value of the long, set and examined
	 * by the application as needed.
	 */
	public long value;

	/**
	 * Constructs a new <code>LongHolder</code> with an initial
	 * value of 0.
	 */
	public LongHolder ()
	 { value = 0;
	 }

	/**
	 * Constructs a new <code>LongHolder</code> with a
	 * specific initial value.
	 *
	 * @param l Initial long value.
	 */
	public LongHolder (long l)
	 { value = l;
	 }
}

