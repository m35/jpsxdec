package argparser;

/**
  * Wrapper class which ``holds'' an integer value,
  * enabling methods to return integer values through
  * arguments.
  */
public class IntHolder implements java.io.Serializable
{
	/**
	 * Value of the integer, set and examined
	 * by the application as needed.
	 */
	public int value;

	/**
	 * Constructs a new <code>IntHolder</code> with an initial
	 * value of 0.
	 */
	public IntHolder ()
	 { value = 0;
	 }

	/**
	 * Constructs a new <code>IntHolder</code> with a
	 * specific initial value.
	 *
	 * @param i Initial integer value.
	 */
	public IntHolder (int i)
	 { value = i;
	 }
}

