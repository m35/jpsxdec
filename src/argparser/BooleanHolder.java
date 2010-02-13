package argparser;

/**
  * Wrapper class which ``holds'' a boolean value,
  * enabling methods to return boolean values through
  * arguments.
  */
public class BooleanHolder implements java.io.Serializable
{
	/**
	 * Value of the boolean, set and examined
	 * by the application as needed.
	 */
	public boolean value;

	/**
	 * Constructs a new <code>BooleanHolder</code> with an initial
	 * value of <code>false</code>.
	 */
	public BooleanHolder ()
	 { value = false;
	 }

	/**
	 * Constructs a new <code>BooleanHolder</code> with a
	 * specific initial value.
	 *
	 * @param b Initial boolean value.
	 */
	public BooleanHolder (boolean b)
	 { value = b;
	 }
}
