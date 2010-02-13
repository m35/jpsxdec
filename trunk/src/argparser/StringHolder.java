package argparser;

/**
  * Wrapper class which ``holds'' a String reference,
  * enabling methods to return String references through
  * arguments.
  */
public class StringHolder implements java.io.Serializable
{
	/**
	 * Value of the String reference, set and examined
	 * by the application as needed.
	 */
	public String value;

	/**
	 * Constructs a new <code>StringHolder</code> with an
	 * initial value of <code>null</code>.
	 */
	public StringHolder ()
	 { value = null;
	 }

	/**
	 * Constructs a new <code>StringHolder</code> with a
	 * specific initial value.
	 *
	 * @param s Initial String reference.
	 */
	public StringHolder (String s)
	 { value = s;
	 }
}

