package argparser;

import java.io.IOException;

/** 
  * Exception class used by <code>StringScanner</code> when
  * command line arguments do not parse correctly.
  * 
  * @author John E. Lloyd, Winter 2001
  * @see StringScanner
  */
class StringScanException extends IOException
{
	int failIdx;

	/** 
	  * Creates a new StringScanException with the given message. 
	  * 
	  * @param msg Error message
	  * @see StringScanner
	  */

	public StringScanException (String msg)
	 { super (msg);
	 }

	public StringScanException (int idx, String msg)
	 {
	   super (msg);
	   failIdx = idx;
	 }

	public int getFailIndex()
	 {
	   return failIdx;
	 }
}
