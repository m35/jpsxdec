/**
  * Copyright John E. Lloyd, 2004. All rights reserved. Permission to use,
  * copy, modify and redistribute is granted, provided that this copyright
  * notice is retained and the author is given credit whenever appropriate.
  *
  * This  software is distributed "as is", without any warranty, including 
  * any implied warranty of merchantability or fitness for a particular
  * use. The author assumes no responsibility for, and shall not be liable
  * for, any special, indirect, or consequential damages, or any damages
  * whatsoever, arising out of or in connection with the use of this
  * software.
  */
package argparser;

class StringScanner
{
	private char[] buf;
	private int idx;
	private int len;
	private String stringDelimiters = "";

	public StringScanner (String s)
	 {
	   buf = new char[s.length()+1];
	   s.getChars (0, s.length(), buf, 0);
	   len = s.length();
	   buf[len] = 0;
	   idx = 0;
	 }

	public int getIndex()
	 { return idx;
	 }

	public void setIndex(int i)
	 { if (i < 0)
	    { idx = 0;
	    }
	   else if (i > len)
	    { idx = len;
	    }
	   else
	    { idx = i;
	    }
	 }

	public void setStringDelimiters (String s)
	 { stringDelimiters = s;
	 }
	
	public String getStringDelimiters()
	 { return stringDelimiters;
	 }

	public char scanChar ()
	   throws StringScanException 
	 {
	   int idxSave = idx;
	   skipWhiteSpace();
	   try
	    { if (buf[idx] == '\'')
	       { return scanQuotedChar();
	       }
	      else
	       { return scanUnquotedChar();
	       }
	    }
	   catch (StringScanException e)
	    { idx = idxSave;
	      throw e;
	    }
	 }

	public char scanQuotedChar ()
	   throws StringScanException
	 {
	   StringScanException exception = null;
	   char retval = 0;
	   int idxSave = idx;

	   skipWhiteSpace();
	   if (idx == len)
	    { exception = new StringScanException (idx, "end of input");
	    }
	   else if (buf[idx++] == '\'')
	    { try
	       { retval = scanUnquotedChar();
	       }
	      catch (StringScanException e)
	       { exception = e;
	       }
	      if (exception==null)
	       { if (idx==len)
		  { exception = new StringScanException
		       (idx, "end of input");
		  }
		 else if (buf[idx++] != '\'')
		  { exception = new StringScanException
		       (idx-1, "unclosed quoted character");
		  }
	       }
	    }
	   else
	    { exception = new StringScanException
		 (idx-1, "uninitialized quoted character");
	    }
	   if (exception!=null)
	    { idx = idxSave;
	      throw exception;
	    }
	   return retval;
	 }

	public char scanUnquotedChar ()
	   throws StringScanException
	 {
	   StringScanException exception = null;
	   char c, retval = 0;
	   int idxSave = idx;

	   if (idx == len)
	    { exception = new StringScanException (idx, "end of input");
	    }
	   else if ((c = buf[idx++]) == '\\')
	    { if (idx == len)
	       { exception = new StringScanException (idx, "end of input");
	       }
	      else
	       { 
		 c = buf[idx++];
		 if (c == '"')
		  { retval = '"';
		  }
		 else if (c == '\'')
		  { retval = '\'';
		  }
		 else if (c == '\\')
		  { retval = '\\';
		  }
		 else if (c == 'n')
		  { retval = '\n';
		  }
		 else if (c == 't')
		  { retval = '\t';
		  }
		 else if (c == 'b')
		  { retval = '\b';
		  }
		 else if (c == 'r')
		  { retval = '\r';
		  }
		 else if (c == 'f')
		  { retval = '\f';
		  }
		 else if ('0' <= c && c < '8')
		  { int v = c - '0';
		    for (int j=0; j<2; j++)
		     { if (idx==len)
			{ break;
			}
		       c = buf[idx];
		       if ('0' <= c && c < '8' && (v*8 + (c-'0')) <= 255)
			{ v = v*8 + (c-'0');
			  idx++;
			}
		       else
			{ break;
			}
		     }
		    retval = (char)v;
		  }
		 else
		  { exception = new StringScanException
		       (idx-1, "illegal escape character '" + c + "'");
		  }
	       }
	    }
	   else
	    { retval = c;
	    }
	   if (exception!=null)
	    { idx = idxSave;
	      throw exception;
	    }
	   return retval;
	 }

	public String scanQuotedString ()
	   throws StringScanException
	 { 
	   StringScanException exception = null;
	   StringBuffer sbuf = new StringBuffer(len);
	   char c;
	   int idxSave = idx;

	   skipWhiteSpace();
	   if (idx == len)
	    { exception = new StringScanException (idx, "end of input");
	    }
	   else if ((c=buf[idx++]) == '"')
	    { while (idx<len && (c=buf[idx]) != '"' && c != '\n')
	       { if (c == '\\')
		  { try
		     { c = scanUnquotedChar();
		     }
		    catch (StringScanException e)
		     { exception = e;
		       break;
		     }
		  }
		 else
		  { idx++;
		  }
		 sbuf.append (c);
	       }
	      if (exception == null && idx>=len)
	       { exception = new StringScanException (len, "end of input");
	       }
	      else if (exception == null && c == '\n')
	       { exception = new StringScanException 
		    (idx, "unclosed quoted string");
	       }
	      else
	       { idx++;
	       }
	    }
	   else
	    { exception = new StringScanException (idx-1,
"quoted string must start with \"");
	    }
	   if (exception != null)
	    { idx = idxSave;
	      throw exception;
	    }
	   return sbuf.toString();
	 }

	public String scanNonWhiteSpaceString()
	   throws StringScanException
	 {
	   StringBuffer sbuf = new StringBuffer(len);
	   int idxSave = idx;
	   char c;

	   skipWhiteSpace();
	   if (idx == len)
	    { StringScanException e = new StringScanException (
				       idx, "end of input");
	      idx = idxSave;
	      throw e;
	    }
	   else
	    { c = buf[idx++];
	      while (idx<len && !Character.isWhitespace(c)
			     && stringDelimiters.indexOf(c) == -1)
	       { sbuf.append(c);
		 c = buf[idx++];
	       }
	      if (Character.isWhitespace(c) ||
		  stringDelimiters.indexOf(c) != -1)
	       { idx--;
	       }
	      else
	       { sbuf.append(c);
	       }
	    }
	   return sbuf.toString();
	 }

	public String scanString ()
	   throws StringScanException 
	 {
	   int idxSave = idx;
	   skipWhiteSpace();
	   try
	    { if (buf[idx] == '"')
	       { return scanQuotedString();
	       }
	      else
	       { return scanNonWhiteSpaceString();
	       }
	    }
	   catch (StringScanException e)
	    { idx = idxSave;
	      throw e;
	    }
	 }

	public String getString ()
	   throws StringScanException 
	 {
	   StringBuffer sbuf = new StringBuffer(len);
	   while (idx < len)
	    { sbuf.append (buf[idx++]);
	    }
	   return sbuf.toString();
	 }

	public long scanInt ()
	   throws StringScanException
	 {
	   int idxSave = idx;
	   char c;
	   int sign = 1;

	   skipWhiteSpace();
	   if ((c=buf[idx]) == '-' || c == '+')
	    { sign = (c == '-' ? -1 : 1);
	      idx++;
	    }
	   try
	    { if (idx==len)
	       { throw new StringScanException (len, "end of input");
	       }
	      else if ((c=buf[idx]) == '0')
	       { if ((c=buf[idx+1]) == 'x' || c == 'X')
		  { idx += 2;
		    return sign*scanInt (16, false);
		  }
		 else
		  { return sign*scanInt (8, false);
		  }
	       }
	      else
	       { return sign*scanInt (10, false);
	       }
	    }
	   catch (StringScanException e)
	    { idx = idxSave;
	      throw e;
	    }
	 }

	public long scanInt (int radix)
	   throws StringScanException
	 {
	   return scanInt (radix, /*skipWhite=*/true);
	 }

	private String baseDesc (int radix)
	 {
	   switch (radix)
	    { case 10:
	       { return "decimal";
	       }
	      case 8:
	       { return "octal";
	       }
	      case 16:
	       { return "hex";
	       }
	      default:
	       { return "base " + radix;
	       }
	    }
	 }

	public long scanInt (int radix, boolean skipWhite)
	   throws StringScanException
	 {
	   StringScanException exception = null;
	   int charval, idxSave = idx;
	   char c;
	   long val = 0;
	   boolean negate = false;

	   if (skipWhite)
	    { skipWhiteSpace();
	    }
	   if ((c=buf[idx]) == '-' || c == '+')
	    { negate = (c == '-');
	      idx++;
	    }
	   if (idx >= len)
	    { exception = new StringScanException (len, "end of input");
	    }
	   else if ((charval=Character.digit(buf[idx++],radix)) == -1)
	    { exception = new StringScanException
		       (idx-1, "malformed " + baseDesc(radix) + " integer");
	    }
	   else
	    { val = charval;
	      while ((charval=Character.digit(buf[idx],radix)) != -1)
	       { val = val*radix + charval;
		 idx++;
	       }
	      if (Character.isLetter(c=buf[idx]) ||
		  Character.isDigit(c) || c == '_')
	       { exception = new StringScanException 
		       (idx, "malformed " + baseDesc(radix) + " integer");
	       }
	    }
	   if (exception != null)
	    { idx = idxSave;
	      throw exception;
	    }
	   return negate ? -val : val;
	 }

	public double scanDouble ()
	   throws StringScanException
	 {
	   StringScanException exception = null;
	   int idxSave = idx;
	   char c;
	   // parse [-][0-9]*[.][0-9]*[eE][-][0-9]*
	   boolean hasDigits = false;
	   boolean signed;
	   double value = 0;

	   skipWhiteSpace();
	   if (idx == len)
	    { exception = new StringScanException ("end of input");
	    }
	   else
	    { 
	      if ((c=buf[idx]) == '-' || c == '+')
	       { signed = true;
		 idx++;
	       }
	      if (matchDigits())
	       { hasDigits = true;
	       }
	      if (buf[idx] == '.')
	       { idx++;
	       }
	      if (!hasDigits && (buf[idx] < '0' || buf[idx] > '9'))
	       { if (idx==len)
		  { exception = new StringScanException (idx, "end of input");
		  }
		 else
		  { exception = new StringScanException (
		       idx, "malformed floating number: no digits");
		  }
	       }
	      else
	       { matchDigits();

		 if ((c=buf[idx]) == 'e' || c == 'E')
		  { idx++;
		    if ((c=buf[idx]) == '-' || c == '+')
		     { signed = true;
		       idx++;
		     }
		    if (buf[idx] < '0' || buf[idx] > '9')
		     { if (idx==len)
			{ exception = new StringScanException(
			     idx, "end of input");
			}
		       else
			{ exception = new StringScanException (idx,
"malformed floating number: no digits in exponent");
			}
		     }
		    else
		     { matchDigits();
		     }
		  }
	       }
	    }
	   if (exception == null)
	    { 
//	      if (Character.isLetterOrDigit(c=buf[idx]) || c == '_')
//	       { exception = new StringScanException (idx, 
//"malformed floating number");
//	       }
//	      else
	       {
		 try
		  { value = Double.parseDouble(new String(buf, idxSave,
							       idx-idxSave));
		  }
		 catch (NumberFormatException e)
		  { exception = new StringScanException (	
		       idx, "malformed floating number");
		  }
	       }
	    }
	   if (exception != null)
	    { idx = idxSave;
	      throw exception;
	    }
	   return value;
	 }

	public boolean scanBoolean ()
	   throws StringScanException
	 {
	   StringScanException exception = null;
	   int idxSave = idx;
	   String testStr = "false";
	   boolean testval = false;
	   char c;

	   skipWhiteSpace();
	   if (buf[idx] == 't')
	    { testStr = "true";
	      testval = true;
	    } 
	   else
	    { testval = false;
	    }
	   int i = 0;
	   for (i=0; i<testStr.length(); i++)
	    { if (testStr.charAt(i) != buf[idx])
	       { if (idx==len)
		  { exception = new StringScanException (idx, "end of input");
		  }
		 break;
	       }
	      idx++;
	    }
	   if (exception==null)
	    { if (i<testStr.length() || 
		  Character.isLetterOrDigit(c=buf[idx]) || c == '_')
	       { exception = new StringScanException (idx, "illegal boolean");
	       }
	    }				      
	   if (exception != null)
	    { idx = idxSave;
	      throw exception;
	    }
	   return testval;
	 }

	public boolean matchString (String s)
	 {
	   int k = idx;
	   for (int i=0; i<s.length(); i++)
	    { if (k >= len || s.charAt(i) != buf[k++])
	       { return false;
	       }
	    }
	   idx = k;
	   return true;
	 }

	public boolean matchDigits ()
	 {
	   int k = idx;
	   char c;

	   while ((c=buf[k]) >= '0' && c <= '9')
	    { k++;
	    }
	   if (k > idx)
	    { idx = k;
	      return true;
	    }
	   else
	    { return false;
	    }
	 }

	public void skipWhiteSpace()
	 {
	   while (Character.isWhitespace(buf[idx]))
	    { idx++;
	    }
	 }

	private int skipWhiteSpace(int k)
	 {
	   while (Character.isWhitespace(buf[k]))
	    { k++;
	    }
	   return k;
	 }

	public boolean atEnd()
	 {
	   return idx == len;
	 }

	public boolean atBeginning()
	 {
	   return idx == 0;
	 }

	public void ungetc()
	 {
	   if (idx > 0)
	    { idx--;
	    }
	 }

	public char getc()
	 {
	   char c = buf[idx];
	   if (idx < len)
	    { idx++;
	    }
	   return c;
	 }

	public char peekc()
	 {
	   return buf[idx];
	 }

	public String substring (int i0, int i1)
	 {
	   if (i0 < 0)
	    { i0 = 0;
	    }
	   else if (i0 >= len)
	    { i0= len-1;
	    }
	   if (i1 < 0)
	    { i1 = 0;
	    }
	   else if (i1 > len)
	    { i1= len;
	    }
	   if (i1 <= i0)
	    { return "";
	    }
	   return new String (buf, i0, i1-i0);
	 }

	public String substring (int i0)
	 {
	   if (i0 < 0)
	    { i0 = 0;
	    }
	   if (i0 >= len)
	    { return "";
	    }
	   else
	    { return new String (buf, i0, len-i0);
	    }
	 }
}
