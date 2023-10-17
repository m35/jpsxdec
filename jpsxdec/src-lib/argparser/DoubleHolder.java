package argparser;

/**
  * Wrapper class which ``holds'' a double value,
  * enabling methods to return double values through
  * arguments.
  */
public class DoubleHolder implements java.io.Serializable
{
	/**
	 * Value of the double, set and examined
	 * by the application as needed.
	 */
	public double value;

	/**
	 * Constructs a new <code>DoubleHolder</code> with an initial
	 * value of 0.
	 */
	public DoubleHolder ()
	 { value = 0;
	 }

	/**
	 * Constructs a new <code>DoubleHolder</code> with a
	 * specific initial value.
	 *
	 * @param d Initial double value.
	 */
	public DoubleHolder (double d)
	 { value = d;
	 }
}

