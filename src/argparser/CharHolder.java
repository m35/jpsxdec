package argparser;

/**
  * Wrapper class which ``holds'' a character value,
  * enabling methods to return character values through
  * arguments.
  */
public class CharHolder implements java.io.Serializable
{
	/**
	 * Value of the character, set and examined
	 * by the application as needed.
	 */
	public char value;

	/**
	 * Constructs a new <code>CharHolder</code> with an initial
	 * value of 0.
	 */
	public CharHolder ()
	 { value = 0;
	 }

	/**
	 * Constructs a new <code>CharHolder</code> with a
	 * specific initial value.
	 *
	 * @param c Initial character value.
	 */
	public CharHolder (char c)
	 { value = c;
	 }
}


