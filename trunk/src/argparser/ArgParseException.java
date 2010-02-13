package argparser;

import java.io.IOException;

/** 
  * Exception class used by <code>ArgParser</code> when
  * command line arguments contain an error.
  * 
  * @author John E. Lloyd, Fall 2004
  * @see ArgParser
  */
public class ArgParseException extends IOException
{
	/** 
	  * Creates a new ArgParseException with the given message. 
	  * 
	  * @param msg Exception message
	  */
	public ArgParseException (String msg)
	 { super (msg);
	 }

	/** 
	  * Creates a new ArgParseException from the given
	  * argument and message. 
	  * 
	  * @param arg Offending argument
	  * @param msg Error message
	  */
	public ArgParseException (String arg, String msg)
	 { super (arg + ": " + msg);
	 }
}
