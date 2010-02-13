package argparser;

/**
  * Wrapper class which ``holds'' a float value,
  * enabling methods to return float values through
  * arguments.
  */
public class FloatHolder implements java.io.Serializable
{
	/**
	 * Value of the float, set and examined
	 * by the application as needed.
	 */
	public float value;

	/**
	 * Constructs a new <code>FloatHolder</code> with an initial
	 * value of 0.
	 */
	public FloatHolder ()
	 { value = 0;
	 }

	/**
	 * Constructs a new <code>FloatHolder</code> with a
	 * specific initial value.
	 *
	 * @param f Initial float value.
	 */
	public FloatHolder (float f)
	 { value = f;
	 }
}


