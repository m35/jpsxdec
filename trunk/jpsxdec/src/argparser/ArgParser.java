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

import java.io.PrintStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Vector;

import java.lang.reflect.Array;

/**
 * ArgParser is used to parse the command line arguments for a java
 * application program. It provides a compact way to specify options and match
 * them against command line arguments, with support for
 * <a href=#rangespec>range checking</a>,
 * <a href=#multipleOptionNames>multiple option names</a> (aliases),
 * <a href=#singleWordOptions>single word options</a>,
 * <a href=#multipleOptionValues>multiple values associated with an option</a>,
 * <a href=#multipleOptionInvocation>multiple option invocation</a>,
 * <a href=#helpInfo>generating help information</a>,
 * <a href=#customArgParsing>custom argument parsing</a>, and
 * <a href=#argsFromAFile>reading arguments from a file</a>. The
 * last feature is particularly useful and makes it
 * easy to create ad-hoc configuration files for an application.
 *
 * <h3><a name="example">Basic Example</a></h3>
 *
 * <p>Here is a simple example in which an application has three
 * command line options:
 * <code>-theta</code> (followed by a floating point value),
 * <code>-file</code> (followed by a string value), and 
 * <code>-debug</code>, which causes a boolean value to be set.
 * 
 * <pre>
 *
 * static public void main (String[] args)
 *  {
 *    // create holder objects for storing results ...
 * 
 *    DoubleHolder theta = new DoubleHolder();
 *    StringHolder fileName = new StringHolder();
 *    BooleanHolder debug = new BooleanHolder();
 * 
 *    // create the parser and specify the allowed options ...
 * 
 *    ArgParser parser = new ArgParser("java argparser.SimpleExample");
 *    parser.addOption ("-theta %f #theta value (in degrees)", theta); 
 *    parser.addOption ("-file %s #name of the operating file", fileName);
 *    parser.addOption ("-debug %v #enables display of debugging info", debug);
 *
 *    // match the arguments ...
 * 
 *    parser.matchAllArgs (args);
 *
 *    // and print out the values
 *
 *    System.out.println ("theta=" + theta.value);
 *    System.out.println ("fileName=" + fileName.value);
 *    System.out.println ("debug=" + debug.value);
 *  }
 * </pre>
 * <p>A command line specifying all three options might look like this:
 * <pre>
 * java argparser.SimpleExample -theta 7.8 -debug -file /ai/lloyd/bar 
 * </pre>
 * 
 * <p>The application creates an instance of ArgParser and then adds
 * descriptions of the allowed options using {@link #addOption addOption}.  The
 * method {@link #matchAllArgs(String[]) matchAllArgs} is then used to match
 * these options against the command line arguments. Values associated with
 * each option are returned in the <code>value</code> field of special
 * ``holder'' classes (e.g., {@link argparser.DoubleHolder DoubleHolder},
 * {@link argparser.StringHolder StringHolder}, etc.).
 *
 * <p> The first argument to {@link #addOption addOption} is a string that
 * specifies (1) the option's name, (2) a conversion code for its associated
 * value (e.g., <code>%f</code> for floating point, <code>%s</code> for a
 * string, <code>%v</code> for a boolean flag), and (3) an optional description
 * (following the <code>#</code> character) which is used for generating help
 * messages. The second argument is the holder object through which the value
 * is returned. This may be either a type-specific object (such as {@link
 * argparser.DoubleHolder DoubleHolder} or {@link argparser.StringHolder
 * StringHolder}), an array of the appropriate type, or
 * <a href=#multipleOptionInvocation> an instance of 
 * <code>java.util.Vector</code></a>.
 *
 * <p>By default, arguments that don't match the specified options, are <a
 * href=#rangespec>out of range</a>, or are otherwise formatted incorrectly,
 * will cause <code>matchAllArgs</code> to print a message and exit the
 * program. Alternatively, an application can use {@link
 * #matchAllArgs(String[],int,int) matchAllArgs(args,idx,exitFlags)} to obtain
 * an array of unmatched arguments which can then be
 * <a href=#customArgParsing>processed separately</a>
 *
 * <h3><a name="rangespec">Range Specification</a></h3>
 *
 * The values associated with options can also be given range specifications. A
 * range specification appears in curly braces immediately following the
 * conversion code. In the code fragment below, we show how to specify an
 * option <code>-name</code> that expects to be provided with one of three
 * string values (<code>john</code>, <code>mary</code>, or <code>jane</code>),
 * an option <code>-index</code> that expects to be supplied with a integer
 * value in the range 1 to 256, an option <code>-size</code> that expects to be
 * supplied with integer values of either 1, 2, 4, 8, or 16, and an option
 * <code>-foo</code> that expects to be supplied with floating point values in
 * the ranges -99 < foo <= -50, or 50 <= foo < 99.
 *
 * <pre>
 *    StringHolder name = new StringHolder();
 *    IntHolder index = new IntHolder();
 *    IntHolder size = new IntHolder();
 *    DoubleHolder foo = new DoubleHolder();
 * 
 *    parser.addOption ("-name %s {john,mary,jane}", name);
 *    parser.addOption ("-index %d {[1,256]}", index);
 *    parser.addOption ("-size %d {1,2,4,8,16}", size);
 *    parser.addOption ("-foo %f {(-99,-50],[50,99)}", foo);
 * </pre>
 *
 * If an argument value does not lie within a specified range, an error is
 * generated.
 *
 * <h3><a name="multipleOptionNames">Multiple Option Names</a></h3>
 *
 * An option may be given several names, or aliases, in the form of
 * a comma seperated list:
 *
 * <pre>
 *    parser.addOption ("-v,--verbose %v #print lots of info");
 *    parser.addOption ("-of,-outfile,-outputFile %s #output file");
 * </pre>
 *
 * <h3><a name="singleWordOptions">Single Word Options</a></h3>
 *
 * Normally, options are assumed to be "multi-word", meaning
 * that any associated value must follow the option as a
 * separate argument string. For 
 * example,
 * <pre>
 *    parser.addOption ("-file %s #file name");
 * </pre>
 * will cause the parser to look for two strings in the argument list
 * of the form
 * <pre>
 *    -file someFileName
 * </pre>
 * However, if there is no white space separting the option's name from
 * it's conversion code, then values associated with that
 * option will be assumed to be part of the same argument
 * string as the option itself. For example,
 * <pre>
 *    parser.addOption ("-file=%s #file name");
 * </pre>
 * will cause the parser to look for a single string in the argument
 * list of the form
 * <pre>
 *    -file=someFileName
 * </pre>
 * Such an option is called a "single word" option.
 *
 * <p>
 * In cases where an option has multiple names, then this single
 * word behavior is invoked if there is no white space between
 * the last indicated name and the conversion code. However, previous
 * names in the list will still be given multi-word behavior
 * if there is white space between the name and the
 * following comma. For example,
 * <pre>
 *    parser.addOption ("-nb=,-number ,-n%d #number of blocks");
 * </pre>
 * will cause the parser to look for one, two, and one word constructions
 * of the forms
 * <pre>
 *    -nb=N
 *    -number N
 *    -nN
 * </pre>
 *
 * <h3><a name="multipleOptionValues">Multiple Option Values</a></h3>
 *
 * If may be useful for an option to be followed by several values.
 * For instance, we might have an option <code>-velocity</code>
 * which should be followed by three numbers denoting
 * the x, y, and z components of a velocity vector.
 * We can require multiple values for an option
 * by placing a <i>multiplier</i> specification,
 * of the form <code>X</code>N, where N is an integer,
 * after the conversion code (or range specification, if present).
 * For example,
 * 
 * <pre>
 *    double[] pos = new double[3];
 *
 *    addOption ("-position %fX3 #position of the object", pos);
 * </pre>
 * will cause the parser to look for
 * <pre>
 *    -position xx yy zz
 * </pre>
 * 
 * in the argument list, where <code>xx</code>, <code>yy</code>, and
 * <code>zz</code> are numbers. The values are stored in the array
 * <code>pos</code>.
 *
 * Options requiring multiple values must use arrays to
 * return their values, and cannot be used in single word format.
 *
 * <h3><a name="multipleOptionInvocation">Multiple Option Invocation</a></h3>
 *
 * Normally, if an option appears twice in the command list, the
 * value associated with the second instance simply overwrites the
 * value associated with the first instance.
 *
 * However, the application can instead arrange for the storage of <i>all</i>
 * values associated with multiple option invocation, by supplying a instance
 * of <code>java.util.Vector</code> to serve as the value holder. Then every
 * time the option appears in the argument list, the parser will create a value
 * holder of appropriate type, set it to the current value, and store the
 * holder in the vector. For example, the construction
 *
 * <pre>
 *    Vector vec = new Vector(10);
 *
 *    parser.addOption ("-foo %f", vec);
 *    parser.matchAllArgs(args);
 * </pre>
 * when supplied with an argument list that contains
 * <pre>
 *    -foo 1.2 -foo 1000 -foo -78
 * </pre>
 * 
 * will create three instances of {@link argparser.DoubleHolder DoubleHolder},
 * initialized to <code>1.2</code>, <code>1000</code>, and <code>-78</code>,
 * and store them in <code>vec</code>.
 *
 * <h3><a name="helpInfo">Generating help information</a></h3>
 *
 * ArgParser automatically generates help information for the options, and this
 * information may be printed in response to a <i>help</i> option, or may be
 * queried by the application using {@link #getHelpMessage getHelpMessage}.
 * The information for each option consists of the option's name(s), it's
 * required value(s), and an application-supplied description.  Value
 * information is generated automaticlly from the conversion code, range, and
 * multiplier specifications (although this can be overriden, as
 * <a href=#valueInfo>described below</a>).
 * The application-supplied description is whatever
 * appears in the specification string after the optional <code>#</code>
 * character. The string returned by {@link #getHelpMessage getHelpMessage} for
 * the <a href=#example>first example above</a> would be
 * 
 * <pre>
 * Usage: java argparser.SimpleExample
 * Options include:
 * 
 * -help,-?                displays help information
 * -theta &lt;float&gt;          theta value (in degrees)
 * -file &lt;string&gt;          name of the operating file
 * -debug                  enables display of debugging info
 * </pre>
 * 
 * The options <code>-help</code> and <code>-?</code> are including in the
 * parser by default as help options, and they automatically cause the help
 * message to be printed. To exclude these
 * options, one should use the constructor {@link #ArgParser(String,boolean)
 * ArgParser(synopsis,false)}.
 * Help options can also be specified by the application using {@link
 * #addOption addOption} and the conversion code <code>%h</code>.  Help options
 * can be disabled using {@link #setHelpOptionsEnabled
 * setHelpOptionsEnabled(false)}.
 *
 * <p><a name=valueInfo>
 * A description of the required values for an option can be
 * specified explicitly 
 * by placing a second <code>#</code> character in the specification
 * string. Everything between the first and second <code>#</code>
 * characters then becomes the value description, and everything
 * after the second <code>#</code> character becomes the option
 * description.
 * For example, if the <code>-theta</code> option
 * above was specified with
 * <pre>
 *    parser.addOption ("-theta %f #NUMBER#theta value (in degrees)",theta);
 * </pre>
 * instead of
 * <pre>
 *    parser.addOption ("-theta %f #theta value (in degrees)", theta);
 * </pre>
 * then the corresponding entry in the help message would look
 * like
 * <pre>
 * -theta NUMBER          theta value (in degrees)
 * </pre>
 *
 * <h3><a name="customArgParsing">Custom Argument Parsing</a></h3>
 * 
 * An application may find it necessary to handle arguments that
 * don't fit into the framework of this class. There are a couple
 * of ways to do this.
 *
 * <p>
 * First, the method {@link #matchAllArgs(String[],int,int)
 * matchAllArgs(args,idx,exitFlags)} returns an array of
 * all unmatched arguments, which can then be handled
 * specially:
 * <pre>
 *    String[] unmatched =
 *       parser.matchAllArgs (args, 0, parser.EXIT_ON_ERROR);
 *    for (int i = 0; i < unmatched.length; i++)
 *     { ... handle unmatched arguments ...
 *     }
 * </pre>
 * 
 * For instance, this would be useful for an applicatoon that accepts an
 * arbitrary number of input file names.  The options can be parsed using
 * <code>matchAllArgs</code>, and the remaining unmatched arguments
 * give the file names.
 *
 * <p> If we need more control over the parsing, we can parse arguments one at
 * a time using {@link #matchArg matchArg}:
 * 
 * <pre>
 *    int idx = 0;
 *    while (idx < args.length)
 *     { try
 *        { idx = parser.matchArg (args, idx);
 *          if (parser.getUnmatchedArgument() != null)
 *           {
 *             ... handle this unmatched argument ourselves ...
 *           }
 *        }
 *       catch (ArgParserException e) 
 *        { // malformed or erroneous argument
 *          parser.printErrorAndExit (e.getMessage());
 *        }
 *     }
 * </pre>
 *
 * {@link #matchArg matchArg(args,idx)} matches one option at location
 * <code>idx</code> in the argument list, and then returns the location value
 * that should be used for the next match.  If an argument does
 * not match any option,
 * {@link #getUnmatchedArgument getUnmatchedArgument} will return a copy of the
 * unmatched argument.
 *
 * <h3><a name="argsFromAFile">Reading Arguments From a File</a></h3>
 *
 * The method {@link #prependArgs prependArgs} can be used to automatically
 * read in a set of arguments from a file and prepend them onto an existing
 * argument list. Argument words correspond to white-space-delimited strings,
 * and the file may contain the comment character <code>#</code> (which
 * comments out everything to the end of the current line). A typical usage
 * looks like this:
 *
 * <pre>
 *    ... create parser and add options ...
 * 
 *    args = parser.prependArgs (new File(".configFile"), args);
 *
 *    parser.matchAllArgs (args);
 * </pre>
 *
 * This makes it easy to generate simple configuration files for an
 * application.
 *
 * @author John E. Lloyd, Fall 2004
 */
public class ArgParser
{
        Vector matchList;
//	int tabSpacing = 8;
	String synopsisString;
	boolean helpOptionsEnabled = true;
	Record defaultHelpOption = null;
	Record firstHelpOption = null;
	PrintStream printStream = System.out;
	int helpIndent = 24;
	String errMsg = null;
	String unmatchedArg = null;

	static String validConversionCodes = "iodxcbfsvh";

	/**
	 * Indicates that the program should exit with an appropriate message
	 * in the event of an erroneous or malformed argument.*/
	public static int EXIT_ON_ERROR = 1;

	/**
	 * Indicates that the program should exit with an appropriate message
	 * in the event of an unmatched argument.*/
	public static int EXIT_ON_UNMATCHED = 2;

	/**
	 * Returns a string containing the valid conversion codes. These
	 * are the characters which may follow the <code>%</code> character in
	 * the specification string of {@link #addOption addOption}.
	 *
	 * @return Valid conversion codes
	 * @see #addOption
	 */
	public static String getValidConversionCodes()
	 { 
	   return validConversionCodes;
	 }

	static class NameDesc
	 {
	   String name;
	   // oneWord implies that any value associated with
	   // option is concatenated onto the argument string itself
	   boolean oneWord;
	   NameDesc next = null;
	 }

	static class RangePnt
	 {
	   double dval = 0;
	   long lval = 0;
	   String sval = null;
	   boolean bval = true;
	   boolean closed = true;

	   RangePnt (String s, boolean closed)
	    { sval = s;
	      this.closed = closed;
	    }

	   RangePnt (double d, boolean closed)
	    { dval = d;
	      this.closed = closed;
	    }

	   RangePnt (long l, boolean closed)
	    { lval = l;
	      this.closed = closed;
	    }

	   RangePnt (boolean b, boolean closed)
	    { bval = b;
	      this.closed = closed;
	    }

	   RangePnt (StringScanner scanner, int type)
	      throws IllegalArgumentException
	    {
	      String typeName = null;
	      try
	       { switch (type)
		  { 
		    case Record.CHAR:
		     { typeName = "character";
		       lval = scanner.scanChar();
		       break;
		     }
		    case Record.INT:
		    case Record.LONG:
		     { typeName = "integer";
		       lval = scanner.scanInt();
		       break;
		     }
		    case Record.FLOAT:
		    case Record.DOUBLE:
		     { typeName = "float";
		       dval = scanner.scanDouble();
		       break;
		     }
		    case Record.STRING:
		     { typeName = "string";
		       sval = scanner.scanString();
		       break;
		     }
		    case Record.BOOLEAN:
		     { typeName = "boolean";
		       bval = scanner.scanBoolean();
		       break;
		     }
		  }
	       }
	      catch (StringScanException e)
	       { throw new IllegalArgumentException (
		   "Malformed " + typeName + " '" +
		    scanner.substring(scanner.getIndex(),
				      e.getFailIndex()+1) +
		   "' in range spec");
	       }
//	      this.closed = closed;
	    }

	   void setClosed (boolean closed)
	    { this.closed = closed;
	    }

	   boolean getClosed()
	    { return closed;
	    }
	
	   int compareTo (double d)
	    { if (dval < d)
	       { return -1;
	       }
	      else if (d == dval)
	       { return 0;
	       }
	      else
	       { return 1;
	       }
	    }
	
	   int compareTo (long l)
	    { if (lval < l)
	       { return -1;
	       }
	      else if (l == lval)
	       { return 0;
	       }
	      else
	       { return 1;
	       }
	    }

	   int compareTo (String s)
	    { return sval.compareTo (s);
	    }

	   int compareTo (boolean b)
	    { if (b == bval)
	       { return 0;
	       }
	      else
	       { return 1;
	       }
	    }
	
	   public String toString()
	    { return "{ dval=" + dval + ", lval=" + lval + 
		     ", sval=" + sval + ", bval=" + bval +
		     ", closed=" + closed + "}";
	    }
	 }

	class RangeAtom
	 {
	   RangePnt low = null;
	   RangePnt high = null;
	   RangeAtom next = null;

	   RangeAtom (RangePnt p0, RangePnt p1, int type)
	      throws IllegalArgumentException
	    {
	      int cmp = 0;
	      switch (type)
	       { 
		 case Record.CHAR:
		 case Record.INT:
		 case Record.LONG:
		  { cmp = p0.compareTo (p1.lval);
		    break;
		  }
		 case Record.FLOAT:
		 case Record.DOUBLE:
		  { cmp = p0.compareTo (p1.dval);
		    break;
		  }
		 case Record.STRING:
		  { cmp = p0.compareTo (p1.sval);
		    break;
		  }
	       }
	      if (cmp > 0)
	       { // then switch high and low
		 low = p1;
		 high = p0;
	       }
	      else
	       { low = p0;
		 high = p1;
	       }
	    }

	   RangeAtom (RangePnt p0)
	      throws IllegalArgumentException
	    {
	      low = p0;
	    }

	   boolean match (double d)
	    { int lc = low.compareTo(d);
	      if (high != null)
	       { int hc = high.compareTo(d);
		 return (lc*hc < 0 ||
			 (low.closed && lc==0) ||
			 (high.closed && hc==0));
	       }
	      else
	       { return lc == 0;
	       }
	    }	   

	   boolean match (long l)
	    { int lc = low.compareTo(l);
	      if (high != null)
	       { int hc = high.compareTo(l);
		 return (lc*hc < 0 ||
			 (low.closed && lc==0) ||
			 (high.closed && hc==0));
	       }
	      else
	       { return lc == 0;
	       }
	    }	   

	   boolean match (String s)
	    { int lc = low.compareTo(s);
	      if (high != null)
	       { int hc = high.compareTo(s);
		 return (lc*hc < 0 ||
			 (low.closed && lc==0) ||
			 (high.closed && hc==0));
	       }
	      else
	       { return lc == 0;
	       }
	    }

	   boolean match (boolean b)
	    { return low.compareTo(b) == 0;
	    }

	   public String toString()
	    { return "low=" + (low==null ? "null" : low.toString()) +
		     ", high=" + (high==null ? "null" : high.toString());
	    }
	 }

	class Record
	 {
	   NameDesc nameList;
	   static final int NOTYPE = 0;
	   static final int BOOLEAN = 1;
	   static final int CHAR = 2;
	   static final int INT = 3;
	   static final int LONG = 4;
	   static final int FLOAT = 5;
	   static final int DOUBLE = 6;
	   static final int STRING = 7;
	   int type;
	   int numValues;
	   boolean vectorResult = false;

	   String helpMsg = null;
	   String valueDesc = null;
	   String rangeDesc = null;
	   Object resHolder = null;
	   RangeAtom rangeList = null;
	   RangeAtom rangeTail = null;
	   char convertCode;
	   boolean vval = true; // default value for now

	   NameDesc firstNameDesc()
	    {
	      return nameList;
	    }

	   RangeAtom firstRangeAtom()
	    {
	      return rangeList;
	    }

	   int numRangeAtoms()
	    { int cnt = 0;
	      for (RangeAtom ra=rangeList; ra!=null; ra=ra.next)
	       { cnt++;
	       }
	      return cnt;
	    }

	   void addRangeAtom (RangeAtom ra)
	    { if (rangeList == null)
	       { rangeList = ra;
	       }
	      else
	       { rangeTail.next = ra;
	       }
	      rangeTail = ra;
	    }

	   boolean withinRange (double d)
	    {
	      if (rangeList == null)
	       { return true;
	       }
	      for (RangeAtom ra=rangeList; ra!=null; ra=ra.next)
	       { if (ra.match (d))
		  { return true;
		  }
	       }
	      return false;
	    }

	   boolean withinRange (long l)
	    {
	      if (rangeList == null)
	       { return true;
	       }
	      for (RangeAtom ra=rangeList; ra!=null; ra=ra.next)
	       { if (ra.match (l))
		  { return true;
		  }
	       }
	      return false;
	    }

	   boolean withinRange (String s)
	    {
	      if (rangeList == null)
	       { return true;
	       }
	      for (RangeAtom ra=rangeList; ra!=null; ra=ra.next)
	       { if (ra.match (s))
		  { return true;
		  }
	       }
	      return false;
	    }
	   
	   boolean withinRange (boolean b)
	    {
	      if (rangeList == null)
	       { return true;
	       }
	      for (RangeAtom ra=rangeList; ra!=null; ra=ra.next)
	       { if (ra.match (b))
		  { return true;
		  }
	       }
	      return false;
	    }
	   
	   String valTypeName()
	    {
	      switch (convertCode)
	       {
		 case 'i':
		  { return ("integer");
		  }
		 case 'o':
		  { return ("octal integer");
		  }
		 case 'd':
		  { return ("decimal integer");
		  }
		 case 'x':
		  { return ("hex integer");
		  }
		 case 'c':
		  { return ("char");
		  }
		 case 'b':
		  { return ("boolean");
		  }
		 case 'f':
		  { return ("float");
		  }
		 case 's':
		  { return ("string");
		  }
	       }
	      return ("unknown");
	    }

	   void scanValue (Object result, String name, String s, int resultIdx)
	      throws ArgParseException
	    {
	      double dval = 0;
	      String sval = null;
	      long lval = 0;
	      boolean bval = false;

	      if (s.length()==0)
	       { throw new ArgParseException
		    (name, "requires a contiguous value");
	       }
	      StringScanner scanner = new StringScanner(s);
	      try
	       { 
		 switch (convertCode)
		  {
		    case 'i':
		     { lval = scanner.scanInt();
		       break;
		     }
		    case 'o':
		     { lval = scanner.scanInt (8, false);
		       break;
		     }
		    case 'd':
		     { lval = scanner.scanInt (10, false);
		       break;
		     }
		    case 'x':
		     { lval = scanner.scanInt (16, false);
		       break;
		     }
		    case 'c':
		     { lval = scanner.scanChar();
		       break;
		     }
		    case 'b':
		     { bval = scanner.scanBoolean();
		       break;
		     }
		    case 'f':
		     { dval = scanner.scanDouble();
		       break;
		     }
		    case 's':
		     { sval = scanner.getString();
		       break;
		     }
		  }
	       }
	      catch (StringScanException e)
	       { throw new ArgParseException (
		    name, "malformed " + valTypeName() + " '" + s + "'");
	       }
	      scanner.skipWhiteSpace();
	      if (!scanner.atEnd())
	       { throw new ArgParseException (
		    name, "malformed " + valTypeName() + " '" + s + "'");
	       }
	      boolean outOfRange = false;
	      switch (type)
	       {
		 case CHAR:
		 case INT:
		 case LONG:
		  { outOfRange = !withinRange (lval);
		    break;
		  }
		 case FLOAT:
		 case DOUBLE:
		  { outOfRange = !withinRange (dval);
		    break;
		  }
		 case STRING:
		  { outOfRange = !withinRange (sval);
		    break;
		  }
		 case BOOLEAN:
		  { outOfRange = !withinRange (bval);
		    break;
		  }
	       }
	      if (outOfRange)
	       { String errmsg = "value " + s + " not in range ";
		 throw new ArgParseException (
		    name, "value '" + s + "' not in range " + rangeDesc);
	       }
	      if (result.getClass().isArray())
	       { 
		 switch (type)
		  {
		    case BOOLEAN:
		     { ((boolean[])result)[resultIdx] = bval;
		       break;
		     }
		    case CHAR:
		     { ((char[])result)[resultIdx] = (char)lval;
		       break;
		     }
		    case INT:
		     { ((int[])result)[resultIdx] = (int)lval;
		       break;
		     }
		    case LONG:
		     { ((long[])result)[resultIdx] = lval;
		       break;
		     }
		    case FLOAT:
		     { ((float[])result)[resultIdx] = (float)dval;
		       break;
		     }
		    case DOUBLE:
		     { ((double[])result)[resultIdx] = dval;
		       break;
		     }
		    case STRING:
		     { ((String[])result)[resultIdx] = sval;
		       break;
		     }
		  }
	       }
	      else
	       { 
		 switch (type)
		  {
		    case BOOLEAN:
		     { ((BooleanHolder)result).value = bval;
		       break;
		     }
		    case CHAR:
		     { ((CharHolder)result).value = (char)lval;
		       break;
		     }
		    case INT:
		     { ((IntHolder)result).value = (int)lval;
		       break;
		     }
		    case LONG:
		     { ((LongHolder)result).value = lval;
		       break;
		     }
		    case FLOAT:
		     { ((FloatHolder)result).value = (float)dval;
		       break;
		     }
		    case DOUBLE:
		     { ((DoubleHolder)result).value = dval;
		       break;
		     }
		    case STRING:
		     { ((StringHolder)result).value = sval;
		       break;
		     }
		  }
	       }
	    }
	 }
	
	private String firstHelpOptionName()
	 {
	   if (firstHelpOption != null)
	    { return firstHelpOption.nameList.name;
	    }
	   else
	    { return null; 
	    }
	 }

	/**
	 * Creates an <code>ArgParser</code> with a synopsis
	 * string, and the default help options <code>-help</code> and
	 * <code>-&#063;</code>.
	 *
	 * @param synopsisString string that briefly describes program usage,
	 * for use by {@link #getHelpMessage getHelpMessage}.
	 * @see ArgParser#getSynopsisString
	 * @see ArgParser#getHelpMessage
	 */
	public ArgParser(String synopsisString)
	 {
	   this (synopsisString, true);
	 }

	/**
	 * Creates an <code>ArgParser</code> with a synopsis
	 * string. The help options <code>-help</code> and
	 * <code>-?</code> are added if <code>defaultHelp</code>
	 * is true.
	 *
	 * @param synopsisString string that briefly describes program usage,
	 * for use by {@link #getHelpMessage getHelpMessage}.
	 * @param defaultHelp if true, adds the default help options
	 * @see ArgParser#getSynopsisString
	 * @see ArgParser#getHelpMessage
	 */
	public ArgParser(String synopsisString, boolean defaultHelp)
	 {
	   matchList = new Vector(128);
	   this.synopsisString = synopsisString;
	   if (defaultHelp)
	    { addOption ("-help,-? %h #displays help information", null);
	      defaultHelpOption = firstHelpOption = (Record)matchList.get(0); 
	    }
	 }

	/**
	 * Returns the synopsis string used by the parser.
	 * The synopsis string is a short description of how to invoke
	 * the program, and usually looks something like
	 * <p>
	 * <prec>
	 * "java somepackage.SomeClass [options] files ..."
	 * </prec>
	 * 
	 * <p> It is used in help and error messages.
	 *
	 * @return synopsis string
	 * @see ArgParser#setSynopsisString
	 * @see ArgParser#getHelpMessage
	 */
	public String getSynopsisString ()
	 {
	   return synopsisString;
	 }

	/**
	 * Sets the synopsis string used by the parser.
	 *
	 * @param s new synopsis string
	 * @see ArgParser#getSynopsisString
	 * @see ArgParser#getHelpMessage
	 */
	public void setSynopsisString (String s)
	 { 
	   synopsisString = s;
	 }

	/**
	 * Indicates whether or not help options are enabled.
	 *
	 * @return true if help options are enabled
	 * @see ArgParser#setHelpOptionsEnabled 
	 * @see ArgParser#addOption
	 */ 
	public boolean getHelpOptionsEnabled ()
	 {
	   return helpOptionsEnabled;
	 }

	/**
	 * Enables or disables help options. Help options are those
	 * associated with a conversion code of <code>%h</code>. If
	 * help options are enabled, and a help option is matched,
	 * then the string produced by
	 * {@link #getHelpMessage getHelpMessage}
	 * is printed to the default print stream and the program
	 * exits with code 0. Otherwise, arguments which match help
	 * options are ignored.
         *
	 * @param enable enables help options if <code>true</code>.
	 * @see ArgParser#getHelpOptionsEnabled 
	 * @see ArgParser#addOption
	 * @see ArgParser#setDefaultPrintStream */
	public void setHelpOptionsEnabled(boolean enable)
	 { helpOptionsEnabled = enable;
	 }

	/**
	 * Returns the default print stream used for outputting help
	 * and error information.
	 *
	 * @return default print stream
	 * @see ArgParser#setDefaultPrintStream
	 */
	public PrintStream getDefaultPrintStream()
	 { return printStream;
	 }

	/**
	 * Sets the default print stream used for outputting help
	 * and error information.
	 *
	 * @param stream new default print stream
	 * @see ArgParser#getDefaultPrintStream
	 */
	public void setDefaultPrintStream (PrintStream stream)
	 {
	   printStream = stream;
	 }

	/**
	 * Gets the indentation used by {@link #getHelpMessage
	 * getHelpMessage}.
	 *
	 * @return number of indentation columns
	 * @see ArgParser#setHelpIndentation
	 * @see ArgParser#getHelpMessage
	 */
	public int getHelpIndentation()
	 {
	   return helpIndent;
	 }

	/**
	 * Sets the indentation used by {@link #getHelpMessage
	 * getHelpMessage}. This is the number of columns that an option's help
	 * information is indented. If the option's name and value information
	 * can fit within this number of columns, then all information about
	 * the option is placed on one line.  Otherwise, the indented help
	 * information is placed on a separate line.
	 *
	 * @param indent number of indentation columns
	 * @see ArgParser#getHelpIndentation
	 * @see ArgParser#getHelpMessage
	 */
	public void setHelpIndentation (int indent)
	 { helpIndent = indent; 
	 }

//  	public void setTabSpacing (int n)
//  	 { tabSpacing = n;
//  	 }

//  	public int getTabSpacing ()
//  	 { return tabSpacing;
//  	 }

	private void scanRangeSpec (Record rec, String s)
	   throws IllegalArgumentException
	 {
	   StringScanner scanner = new StringScanner (s);
	   int i0, i = 1;
	   char c, c0, c1;

	   scanner.setStringDelimiters (")],}");
	   c = scanner.getc(); // swallow the first '{'
	   scanner.skipWhiteSpace();
	   while ((c=scanner.peekc()) != '}')
	    { RangePnt p0, p1;

	      if (c == '[' || c == '(')
	       {
		 if (rec.convertCode == 'v' || rec.convertCode == 'b')
		  { throw new IllegalArgumentException
		      ("Sub ranges not supported for %b or %v"); 
		  }
		 c0 = scanner.getc(); // record & swallow character
		 scanner.skipWhiteSpace();
		 p0 = new RangePnt (scanner, rec.type);
		 scanner.skipWhiteSpace();
		 if (scanner.getc() != ',')
		  { throw new IllegalArgumentException
		       ("Missing ',' in subrange specification");
		  }
		 p1 = new RangePnt (scanner, rec.type);
		 scanner.skipWhiteSpace();
		 if ((c1=scanner.getc()) != ']' && c1 != ')')
		  { throw new IllegalArgumentException
		       ("Unterminated subrange");
		  }
		 if (c0 == '(')
		  { p0.setClosed (false);
		  }
		 if (c1 == ')')
		  { p1.setClosed (false);
		  }
		 rec.addRangeAtom (new RangeAtom (p0, p1, rec.type));
	       }
	      else
	       { scanner.skipWhiteSpace();
		 p0 = new RangePnt (scanner, rec.type);
		 rec.addRangeAtom (new RangeAtom (p0));
	       }
	      scanner.skipWhiteSpace();
	      if ((c=scanner.peekc()) == ',')
	       { scanner.getc();
		 scanner.skipWhiteSpace();
	       }
	      else if (c != '}')
	       { 
		 throw new IllegalArgumentException
		    ("Range spec: ',' or '}' expected");
	       }
	    }
	   if (rec.numRangeAtoms()==1)
	    { rec.rangeDesc = s.substring (1, s.length()-1);
	    }
	   else
	    { rec.rangeDesc = s; 
	    }
	 }

	private int defaultResultType (char convertCode)
	 { 
	   switch (convertCode)
	    { 
	      case 'i':
	      case 'o':
	      case 'd':
	      case 'x':
	       { return Record.LONG;
	       }
	      case 'c':
	       { return Record.CHAR;
	       }
	      case 'v':
	      case 'b':
	       { return Record.BOOLEAN;
	       }
	      case 'f':
	       { return Record.DOUBLE;
	       }
	      case 's':
	       { return Record.STRING;
	       }
	    }
	   return Record.NOTYPE;
	 }

	/**
	 * Adds a new option description to the parser. The method takes two
	 * arguments: a specification string, and a result holder in which to
	 * store the associated value.
	 *
	 * <p>The specification string has the general form
	 *
	 * <p> <var>optionNames</var>
	 * <code>%</code><var>conversionCode</var>
	 * [<code>{</code><var>rangeSpec</var><code>}</code>]
	 * [<code>X</code><var>multiplier</var>]
	 * [<code>#</code><var>valueDescription</var>]
	 * [<code>#</code><var>optionDescription</var>] </code>
	 * 
	 * <p>
	 * where
	 * <ul> <p><li><var>optionNames</var> is a
	 * comma-separated list of names for the option
	 * (such as <code>-f, --file</code>).
	 * 
	 * <p><li><var>conversionCode</var> is a single letter,
	 * following a <code>%</code> character, specifying
	 * information about what value the option requires:
	 *
	 * <table>
	 * <tr><td><code>%f</code></td><td>a floating point number</td>
	 * <tr><td><code>%i</code></td><td>an integer, in either decimal,
	 * hex (if preceeded by <code>0x</code>), or
	 * octal (if preceeded by <code>0</code>)</td>
	 * <tr valign=top>
	 * <td><code>%d</code></td><td>a decimal integer</td>
	 * <tr valign=top>
	 * <td><code>%o</code></td><td>an octal integer</td>
	 * <tr valign=top>
	 * <td><code>%h</code></td><td>a hex integer (without the
	 * preceeding <code>0x</code>)</td>
	 * <tr valign=top>
	 * <td><code>%c</code></td><td>a single character, including
	 * escape sequences (such as <code>\n</code> or <code>\007</code>),
	 * and optionally enclosed in single quotes
	 * <tr valign=top>
	 * <td><code>%b</code></td><td>a boolean value (<code>true</code>
	 * or <code>false</code>)</td>
	 * <tr valign=top>
	 * <td><code>%s</code></td><td>a string. This will
	 * be the argument string itself (or its remainder, in
	 * the case of a single word option)</td>
	 * <tr valign=top>
	 * <td><code>%v</code></td><td>no explicit value is expected,
	 * but a boolean value of <code>true</code> (by default)
	 * will be stored into the associated result holder if this
	 * option is matched. If one wishes to have a value of
	 * <code>false</code> stored instead, then the <code>%v</code>
	 * should be followed by a "range spec" containing
	 * <code>false</code>, as in <code>%v{false}</code>.
	 * </table>
	 *
	 * <p><li><var>rangeSpec</var> is an optional range specification,
	 * placed inside curly braces, consisting of a
	 * comma-separated list of range items each specifying
	 * permissible values for the option. A range item may be an
	 * individual value, or it may itself be a subrange,
	 * consisting of two individual values, separated by a comma,
	 * and enclosed in square or round brackets. Square and round
	 * brackets denote closed and open endpoints of a subrange, indicating
	 * that the associated endpoint value is included or excluded
	 * from the subrange.
	 * The values specified in the range spec need to be
	 * consistent with the type of value expected by the option.
	 *
	 * <p><b>Examples:</b>
	 *
	 * <p>A range spec of <code>{2,4,8,16}</code> for an integer
	 * value will allow the integers 2, 4, 8, or 16.
	 *
	 * <p>A range spec of <code>{[-1.0,1.0]}</code> for a floating
	 * point value will allow any floating point number in the
	 * range -1.0 to 1.0.
	 * 
	 * <p>A range spec of <code>{(-88,100],1000}</code> for an integer
	 * value will allow values > -88 and <= 100, as well as 1000.
	 *
	 * <p>A range spec of <code>{"foo", "bar", ["aaa","zzz")} </code> for a
	 * string value will allow strings equal to <code>"foo"</code> or
	 * <code>"bar"</code>, plus any string lexically greater than or equal
	 * to <code>"aaa"</code> but less then <code>"zzz"</code>.
	 *
	 * <p><li><var>multiplier</var> is an optional integer,
	 * following a <code>X</code> character, 
	 * indicating the number of values which the option expects.
	 * If the multiplier is not specified, it is assumed to be
	 * 1. If the multiplier value is greater than 1, then the
	 * result holder should be either an array (of appropriate
	 * type) with a length greater than or equal to the multiplier
	 * value, or a <code>java.util.Vector</code>
	 * <a href=#vectorHolder>as discussed below</a>.
	 *
	 * <p><li><var>valueDescription</var> is an optional
	 * description of the option's value requirements,
	 * and consists of all
	 * characters between two <code>#</code> characters.
	 * The final <code>#</code> character initiates the
	 * <i>option description</i>, which may be empty.
	 * The value description is used in
	 * <a href=#helpInfo>generating help messages</a>.
	 *
	 * <p><li><var>optionDescription</var> is an optional
	 * description of the option itself, consisting of all
	 * characters between a <code>#</code> character
	 * and the end of the specification string.
	 * The option description is used in 
	 * <a href=#helpInfo>generating help messages</a>.
	 * </ul>
	 *
	 * <p>The result holder must be an object capable of holding
	 * a value compatible with the conversion code,
	 * or it must be a <code>java.util.Vector</code>.
	 * When the option is matched, its associated value is
	 * placed in the result holder. If the same option is
	 * matched repeatedly, the result holder value will be overwritten,
	 * unless the result holder is a <code>java.util.Vector</code>,
	 * in which
	 * case new holder objects for each match will be allocated
	 * and added to the vector. Thus if
	 * multiple instances of an option are desired by the
	 * program, the result holder should be a
	 * <code>java.util.Vector</code>.
	 *
	 * <p>If the result holder is not a <code>Vector</code>, then
	 * it must correspond as follows to the conversion code:
	 *
	 * <table>
	 * <tr valign=top>
	 * <td><code>%i</code>, <code>%d</code>, <code>%x</code>,
	 * <code>%o</code></td>
	 * <td>{@link argparser.IntHolder IntHolder},
	 * {@link argparser.LongHolder LongHolder}, <code>int[]</code>, or
	 * <code>long[]</code></td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%f</code></td>
	 * <td>{@link argparser.FloatHolder FloatHolder},
	 * {@link argparser.DoubleHolder DoubleHolder},
	 * <code>float[]</code>, or
	 * <code>double[]</code></td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%b</code>, <code>%v</code></td>
	 * <td>{@link argparser.BooleanHolder BooleanHolder} or
	 * <code>boolean[]</code></td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%s</code></td>
	 * <td>{@link argparser.StringHolder StringHolder} or
	 * <code>String[]</code></td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%c</code></td>
	 * <td>{@link argparser.CharHolder CharHolder} or
	 * <code>char[]</code></td>
	 * </tr>
	 * </table>
	 *
	 * <p>In addition, if the multiplier is greater than 1,
	 * then only the array type indicated above may be used,
	 * and the array must be at least as long as the multiplier.
	 *
	 * <p><a name=vectorHolder>If the result holder is a
	 * <code>Vector</code>, then the system will create an appropriate
	 * result holder object and add it to the vector. Multiple occurances
	 * of the option will cause multiple results to be added to the vector.
	 *
	 * <p>The object allocated by the system to store the result
	 * will correspond to the conversion code as follows:
	 * 
	 * <table>
	 * <tr valign=top>
	 * <td><code>%i</code>, <code>%d</code>, <code>%x</code>,
	 * <code>%o</code></td>
	 * <td>{@link argparser.LongHolder LongHolder}, or
	 * <code>long[]</code> if the multiplier value exceeds 1</td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%f</code></td>
	 * <td>{@link argparser.DoubleHolder DoubleHolder}, or
	 * <code>double[]</code> if the multiplier value exceeds 1</td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%b</code>, <code>%v</code></td>
	 * <td>{@link argparser.BooleanHolder BooleanHolder}, or
	 * <code>boolean[]</code>
	 * if the multiplier value exceeds 1</td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%s</code></td>
	 * <td>{@link argparser.StringHolder StringHolder}, or
	 * <code>String[]</code>
	 * if the multiplier value exceeds 1</td>
	 * </tr>
	 * 
	 * <tr valign=top>
	 * <td><code>%c</code></td>
	 * <td>{@link argparser.CharHolder CharHolder}, or <code>char[]</code>
	 * if the multiplier value exceeds 1</td>
	 * </tr>
	 * </table>
	 *
	 * @param spec the specification string
	 * @param resHolder object in which to store the associated
	 * value
	 * @throws IllegalArgumentException if there is an error in
	 * the specification or if the result holder is of an invalid
	 * type.  */
	public void addOption (String spec, Object resHolder)
	   throws IllegalArgumentException
	 {
	   // null terminated string is easier to parse
	   StringScanner scanner = new StringScanner(spec);
	   Record rec = null;
	   NameDesc nameTail = null;
	   NameDesc ndesc;
	   int i0, i1;
	   char c;

	   do
	    { ndesc = new NameDesc();
	      boolean nameEndsInWhiteSpace = false;

	      scanner.skipWhiteSpace();
	      i0 = scanner.getIndex();
	      while (!Character.isWhitespace(c=scanner.getc()) &&
		      c != ',' && c != '%' && c != '\000')
		 ;
	      i1 = scanner.getIndex();
	      if (c!='\000')
	       { i1--;
	       }
	      if (i0==i1)
	       { // then c is one of ',' '%' or '\000'
		 throw new IllegalArgumentException
		    ("Null option name given");
	       }
	      if (Character.isWhitespace(c))
	       { nameEndsInWhiteSpace = true;
		 scanner.skipWhiteSpace();
		 c = scanner.getc();
	       }
	      if (c=='\000')
	       { throw new IllegalArgumentException
		   ("No conversion character given");
	       }
	      if (c != ',' && c != '%')
	       { throw new IllegalArgumentException
		    ("Names not separated by ','");
	       }
	      ndesc.name = scanner.substring (i0, i1);
	      if (rec == null)
	       { rec = new Record();
		 rec.nameList = ndesc;
	       }
	      else
	       { nameTail.next = ndesc;
	       }
	      nameTail = ndesc;
	      ndesc.oneWord = !nameEndsInWhiteSpace;
	    }
	   while (c != '%');

	   if (nameTail == null)
	    { throw new IllegalArgumentException
		 ("Null option name given");
	    }
	   if (!nameTail.oneWord)
	    { for (ndesc=rec.nameList; ndesc!=null; ndesc=ndesc.next)
	       { ndesc.oneWord = false; 
	       }
	    }
	   c = scanner.getc();
	   if (c=='\000')
	    { throw new IllegalArgumentException
		 ("No conversion character given");
	    }
	   if (validConversionCodes.indexOf(c) == -1)
	    { throw new IllegalArgumentException
		("Conversion code '" + c + "' not one of '" +
		 validConversionCodes + "'");
	    }
	   rec.convertCode = c;

	   if (resHolder instanceof Vector)
	    { rec.vectorResult = true;
	      rec.type = defaultResultType (rec.convertCode);
	    }
	   else
	    { 
	      switch (rec.convertCode)
	       { 
		 case 'i':
		 case 'o':
		 case 'd':
		 case 'x':
		  { if (resHolder instanceof LongHolder ||
			resHolder instanceof long[])
		     { rec.type = Record.LONG;
		     }
		    else if (resHolder instanceof IntHolder ||
			     resHolder instanceof int[])
		     { rec.type = Record.INT;
		     }
		    else
		     { throw new IllegalArgumentException (
			 "Invalid result holder for %" + c);
		     }
		    break;
		  }
		 case 'c':
		  { if (!(resHolder instanceof CharHolder) &&
			!(resHolder instanceof char[]))
		     { throw new IllegalArgumentException (
			 "Invalid result holder for %c");
		     }
		    rec.type = Record.CHAR;
		    break;
		  }
		 case 'v':
		 case 'b':
		  { if (!(resHolder instanceof BooleanHolder) &&
			!(resHolder instanceof boolean[]))
		     { throw new IllegalArgumentException (
			 "Invalid result holder for %" + c);
		     }
		    rec.type = Record.BOOLEAN;
		    break;
		  }
		 case 'f':
		  { if (resHolder instanceof DoubleHolder ||
			resHolder instanceof double[])
		     { rec.type = Record.DOUBLE;
		     }
		    else if (resHolder instanceof FloatHolder ||
			     resHolder instanceof float[])
		     { rec.type = Record.FLOAT;
		     }
		    else
		     { throw new IllegalArgumentException (
			 "Invalid result holder for %f");
		     }
		    break;
		  }
		 case 's':
		  { if (!(resHolder instanceof StringHolder) &&
			!(resHolder instanceof String[]))
		     { throw new IllegalArgumentException (
			  "Invalid result holder for %s");
		     }
		    rec.type = Record.STRING;
		    break;
		  }
		 case 'h':
		  { // resHolder is ignored for this type
		    break;
		  }
	       }
	    }
	   if (rec.convertCode == 'h')
	    { rec.resHolder = null;
	    }
	   else
	    { rec.resHolder = resHolder;
	    }

	   scanner.skipWhiteSpace();
	   // get the range specification, if any
	   if (scanner.peekc() == '{')
	    {
	      if (rec.convertCode == 'h')
	       { throw new IllegalArgumentException
		   ("Ranges not supported for %h"); 
	       }
//	      int bcnt = 0;
	      i0 = scanner.getIndex();	// beginning of range spec
	      do
	       { c = scanner.getc();
		 if (c=='\000')
		  { throw new IllegalArgumentException
		       ("Unterminated range specification");
		  }
//  		 else if (c=='[' || c=='(')
//  		  { bcnt++;
//  		  }
//  		 else if (c==']' || c==')')
//  		  { bcnt--;
//  		  }
//  		 if ((rec.convertCode=='v'||rec.convertCode=='b') && bcnt>1)
//  		  { throw new IllegalArgumentException
//  		      ("Sub ranges not supported for %b or %v");
//  		  }
	       }
	      while (c != '}');
//  	      if (c != ']')
//  	       { throw new IllegalArgumentException
//  		    ("Range specification must end with ']'");
//  	       }
	      i1 = scanner.getIndex();	// end of range spec
	      scanRangeSpec (rec, scanner.substring (i0, i1));
	      if (rec.convertCode == 'v' && rec.rangeList!=null)
	       { rec.vval = rec.rangeList.low.bval;
	       }
	    }
	   // check for value multiplicity information, if any 
	   if (scanner.peekc() == 'X')
	    {
	      if (rec.convertCode == 'h')
	       { throw new IllegalArgumentException
		   ("Multipliers not supported for %h");
	       }
	      scanner.getc();
	      try
	       { rec.numValues = (int)scanner.scanInt();
	       }
	      catch (StringScanException e)
	       { throw new IllegalArgumentException
		   ("Malformed value multiplier");
	       }
	      if (rec.numValues <= 0)
	       { throw new IllegalArgumentException
		   ("Value multiplier number must be > 0"); 
	       }
	    }
	   else
	    { rec.numValues = 1;
	    }
	   if (rec.numValues > 1)
	    { for (ndesc=rec.nameList; ndesc!=null; ndesc=ndesc.next)
	       { if (ndesc.oneWord)
		  { throw new IllegalArgumentException (
"Multiplier value incompatible with one word option " + ndesc.name);
		  }
	       }
	    }
	   if (resHolder != null && resHolder.getClass().isArray())
	    { if (Array.getLength(resHolder) < rec.numValues)
	       { throw new IllegalArgumentException (
"Result holder array must have a length >= " + rec.numValues);
	       }
	    }
	   else
	    { if (rec.numValues > 1 && !(resHolder instanceof Vector))
	       { throw new IllegalArgumentException (
"Multiplier requires result holder to be an array of length >= "
+ rec.numValues);
	       }
	    }

	   // skip white space following conversion information
	   scanner.skipWhiteSpace();

	   // get the help message, if any

	   if (!scanner.atEnd())
	    { if (scanner.getc() != '#')
	       { throw new IllegalArgumentException
		   ("Illegal character(s), expecting '#'");
	       }
	      String helpInfo = scanner.substring (scanner.getIndex());
	      // look for second '#'. If there is one, then info
	      // between the first and second '#' is the value descriptor.
	      int k = helpInfo.indexOf ("#");
	      if (k != -1)
	       { rec.valueDesc = helpInfo.substring (0, k);
		 rec.helpMsg = helpInfo.substring (k+1);
	       }
	      else
	       { rec.helpMsg = helpInfo; 
	       }
	    }
	   else
	    { rec.helpMsg = "";
	    }
	   // add option information to match list
	   if (rec.convertCode == 'h' && firstHelpOption == defaultHelpOption)
	    { matchList.remove (defaultHelpOption);
	      firstHelpOption = rec;
	    }
	   matchList.add (rec);
	 }

	Record lastMatchRecord ()
	 { return (Record)matchList.lastElement();
	 }

	private Record getRecord (String arg, ObjectHolder ndescHolder)
	 {
	   NameDesc ndesc;
	   for (int i=0; i<matchList.size(); i++)
	    { Record rec = (Record)matchList.get(i);
	      for (ndesc=rec.nameList; ndesc!=null; ndesc=ndesc.next)
	       { if (rec.convertCode != 'v' && ndesc.oneWord)
		  { if (arg.startsWith (ndesc.name))
		     { if (ndescHolder != null)
			{ ndescHolder.value = ndesc;
			}
		       return rec;
		     }
		  }
		 else
		  { if (arg.equals (ndesc.name))
		     { if (ndescHolder != null)
			{ ndescHolder.value = ndesc;
			}
		       return rec;
		     }
		  }
	       }
	    }
	   return null;
	 }

	Object getResultHolder (String arg)
	 {
	   Record rec = getRecord(arg, null);
	   return (rec != null) ? rec.resHolder : null;
	 }

	String getOptionName (String arg)
	 {
	   ObjectHolder ndescHolder = new ObjectHolder();
	   Record rec = getRecord(arg, ndescHolder);
	   return (rec != null) ? ((NameDesc)ndescHolder.value).name : null;
	 }

	String getOptionRangeDesc (String arg)
	 {
	   Record rec = getRecord(arg, null);
	   return (rec != null) ? rec.rangeDesc : null;
	 }

	String getOptionTypeName (String arg)
	 {
	   Record rec = getRecord(arg, null);
	   return (rec != null) ? rec.valTypeName() : null;
	 }

	private Object createResultHolder (Record rec)
	 {
	   if (rec.numValues == 1)
	    { switch (rec.type)
	       { case Record.LONG:
		  { return new LongHolder();
		  }
		 case Record.CHAR:
		  { return new CharHolder();
		  }
		 case Record.BOOLEAN:
		  { return new BooleanHolder();
		  }
		 case Record.DOUBLE:
		  { return new DoubleHolder();
		  }
		 case Record.STRING:
		  { return new StringHolder();
		  }
	       }		    
	    }
	   else
	    { switch (rec.type)
	       { case Record.LONG:
		  { return new long[rec.numValues];
		  }
		 case Record.CHAR:
		  { return new char[rec.numValues];
		  }
		 case Record.BOOLEAN:
		  { return new boolean[rec.numValues];
		  }
		 case Record.DOUBLE:
		  { return new double[rec.numValues];
		  }
		 case Record.STRING:
		  { return new String[rec.numValues];
		  }
	       }		    
	    }
	   return null; // can't happen
	 }

	static void stringToArgs (Vector vec, String s,
				  boolean allowQuotedStrings)
	   throws StringScanException
	 {
	   StringScanner scanner = new StringScanner(s);
	   scanner.skipWhiteSpace();
	   while (!scanner.atEnd())
	    { if (allowQuotedStrings)
	       { vec.add (scanner.scanString()); 
	       }
	      else
	       { vec.add (scanner.scanNonWhiteSpaceString()); 
	       }
	      scanner.skipWhiteSpace();
	    }
	 }

	/**
	 * Reads in a set of strings from a reader and prepends them to an
	 * argument list.  Strings are delimited by either whitespace or
	 * double quotes <code>"</code>.  The character <code>#</code> acts as
	 * a comment character, causing input to the end of the current line to
	 * be ignored.
	 *
	 * @param reader Reader from which to read the strings
	 * @param args Initial set of argument values. Can be
	 * specified as <code>null</code>.
	 * @throws IOException if an error occured while reading.
	 */
	public static String[] prependArgs (Reader reader, String[] args)
	   throws IOException
	 {
	   if (args == null)
	    { args = new String[0];
	    }
	   LineNumberReader lineReader = new LineNumberReader (reader);
	   Vector vec = new Vector(100, 100);
	   String line;
	   int i, k;

	   while ((line = lineReader.readLine()) != null)
	    { int commentIdx = line.indexOf ("#");
	      if (commentIdx != -1)
	       { line = line.substring (0, commentIdx); 
	       }
	      try
	       { stringToArgs (vec, line, /*allowQuotedStings=*/true);
	       }
	      catch (StringScanException e)
	       { throw new IOException (
		    "malformed string, line "+lineReader.getLineNumber());
	       }	      
	    }
	   String[] result = new String[vec.size()+args.length];
	   for (i=0; i<vec.size(); i++)
	    { result[i] = (String)vec.get(i);
	    }
	   for (k=0; k<args.length; k++)
	    { result[i++] = args[k];
	    }
	   return result;
	 }

	/**
	 * Reads in a set of strings from a file and prepends them to an
	 * argument list.  Strings are delimited by either whitespace or double
	 * quotes <code>"</code>.  The character <code>#</code> acts as a
	 * comment character, causing input to the end of the current line to
	 * be ignored.
	 *
	 * @param file File to be read
	 * @param args Initial set of argument values. Can be
	 * specified as <code>null</code>.
	 * @throws IOException if an error occured while reading the file.
	 */
	public static String[] prependArgs (File file, String[] args)
	   throws IOException
	 {
	   if (args == null)
	    { args = new String[0];
	    }
	   if (!file.canRead())
	    { return args;
	    }
	   try
	    { return prependArgs (new FileReader (file), args);
	    }
	   catch (IOException e)
	    { throw new IOException (
"File " + file.getName() + ": " + e.getMessage());
	    }
	 }

	/**
          * Sets the parser's error message.
	  *
	  * @param s Error message
	  */
	protected void setError (String msg)
	 {
	   errMsg = msg;
	 }

	/**
	 * Prints an error message, along with a pointer to help options,
	 * if available, and causes the program to exit with code 1.
	 */
	public void printErrorAndExit (String msg)
	 {
	   if (helpOptionsEnabled && firstHelpOptionName() != null)
	    { msg += "\nUse "+firstHelpOptionName()+" for help information";
	    }
	   if (printStream != null)
	    { printStream.println (msg);
	    }
	   System.exit(1);
	 }
	
	/**
	 * Matches arguments within an argument list.
	 *
	 * <p>In the event of an erroneous or unmatched argument, the method
	 * prints a message and exits the program with code 1.
	 *
	 * <p>If help options are enabled and one of the arguments matches a
	 * help option, then the result of {@link #getHelpMessage
	 * getHelpMessage} is printed to the default print stream and the
	 * program exits with code 0.  If help options are not enabled, they
	 * are ignored.
	 *
	 * @param args argument list
	 * @see ArgParser#getDefaultPrintStream
	 */
	public void matchAllArgs (String[] args)
	 { 
	   matchAllArgs (args, 0, EXIT_ON_UNMATCHED | EXIT_ON_ERROR);
	 }

	/**
	 * Matches arguments within an argument list and returns
	 * those which were not matched. The matching starts at a location
	 * in <code>args</code> specified by <code>idx</code>, and
	 * unmatched arguments are returned in a String array.
	 *
	 * <p>In the event of an erroneous argument, the method either prints a
	 * message and exits the program (if {@link #EXIT_ON_ERROR} is
	 * set in <code>exitFlags</code>)
	 * or terminates the matching and creates a error message that
	 * can be retrieved by {@link #getErrorMessage}.
	 *
	 * <p>In the event of an umatched argument, the method will print a
	 * message and exit if {@link #EXIT_ON_UNMATCHED} is set
	 * in <code>errorFlags</code>.
	 * Otherwise, the unmatched argument will be appended to the returned
	 * array of unmatched values, and the matching will continue at the
	 * next location.
	 *
	 * <p>If help options are enabled and one of the arguments matches a
	 * help option, then the result of {@link #getHelpMessage
	 * getHelpMessage} is printed to the the default print stream and the
	 * program exits with code 0.  If help options are not enabled, then
	 * they will not be matched.
	 *
	 * @param args argument list
	 * @param idx starting location in list
	 * @param exitFlags conditions causing the program to exit.  Should be
	 * an or-ed combintion of {@link #EXIT_ON_ERROR} or {@link
	 * #EXIT_ON_UNMATCHED}.
	 * @return array of arguments that were not matched, or
	 * <code>null</code> if all arguments were successfully matched
	 * @see ArgParser#getErrorMessage
	 * @see ArgParser#getDefaultPrintStream
	 */
	public String[] matchAllArgs (String[] args, int idx, int exitFlags)
	 { 
	   Vector unmatched = new Vector(10);

	   while (idx < args.length)
	    { try
	       { idx = matchArg (args, idx);
		 if (unmatchedArg != null)
		  { if ((exitFlags & EXIT_ON_UNMATCHED) != 0)
		     { printErrorAndExit (
			  "Unrecognized argument: " + unmatchedArg);
		     }
		    else
		     { unmatched.add (unmatchedArg);
		     }
		  }
	       }
	      catch (ArgParseException e)
	       { if ((exitFlags & EXIT_ON_ERROR) != 0)
		  { printErrorAndExit (e.getMessage());
		  }
		 break;
	       }
	    }
	   if (unmatched.size() == 0)
	    { return null; 
	    }
	   else
	    { return (String[])unmatched.toArray(new String[0]);
	    }
	 }

	/**
	 * Matches one option starting at a specified location in an argument
	 * list. The method returns the location in the list where the next
	 * match should begin.
	 *
	 * <p>In the event of an erroneous argument, the method throws
	 * an {@link argparser.ArgParseException ArgParseException}
	 * with an appropriate error message. This error
	 * message can also be retrieved using
	 * {@link #getErrorMessage getErrorMessage}.
	 *
	 * <p>In the event of an umatched argument, the method will return idx
	 * + 1, and {@link #getUnmatchedArgument getUnmatchedArgument} will
	 * return a copy of the unmatched argument. If an argument is matched,
	 * {@link #getUnmatchedArgument getUnmatchedArgument} will return
	 * <code>null</code>.
	 *
	 * <p>If help options are enabled and the argument matches a help
	 * option, then the result of {@link #getHelpMessage getHelpMessage} is printed to
	 * the the default print stream and the program exits with code 0.  If
	 * help options are not enabled, then they are ignored.
	 *
	 * @param args argument list
	 * @param idx location in list where match should start
	 * @return location in list where next match should start
	 * @throws ArgParseException if there was an error performing
	 * the match (such as improper or insufficient values).
	 * @see ArgParser#setDefaultPrintStream
	 * @see ArgParser#getHelpOptionsEnabled
	 * @see ArgParser#getErrorMessage
	 * @see ArgParser#getUnmatchedArgument
	 */
	public int matchArg (String[] args, int idx)
	   throws ArgParseException
	 {
	   unmatchedArg = null;
	   setError (null);
	   try
	    { ObjectHolder ndescHolder = new ObjectHolder();
	      Record rec = getRecord (args[idx], ndescHolder);
	      if (rec == null || (rec.convertCode=='h' && !helpOptionsEnabled))
	       { // didn't match
		 unmatchedArg = new String(args[idx]);
		 return idx+1;
	       }
	      NameDesc ndesc = (NameDesc)ndescHolder.value;
	      Object result;
	      if (rec.resHolder instanceof Vector)
	       { result = createResultHolder (rec);
	       }
	      else
	       { result = rec.resHolder;
	       }
	      if (rec.convertCode == 'h')
	       { if (helpOptionsEnabled)
		  { printStream.println (getHelpMessage());
		    System.exit (0);
		  }
		 else
		  { return idx+1;
		  }
	       }
	      else if (rec.convertCode != 'v')
	       { if (ndesc.oneWord)
		  { rec.scanValue (
		       result, ndesc.name, 
		       args[idx].substring (ndesc.name.length()), 0);
		  }
		 else
		  { if (idx+rec.numValues >= args.length)
		     { throw new ArgParseException (
			  ndesc.name, "requires " + rec.numValues + " value" +
			  (rec.numValues > 1 ? "s" : ""));
		     }
		    for (int k=0; k<rec.numValues; k++)
		     { rec.scanValue (result, ndesc.name, args[++idx], k);
		     }
		  }
	       }
	      else
	       { if (rec.resHolder instanceof BooleanHolder)
		  { ((BooleanHolder)result).value = rec.vval;
		  }
		 else
		  { for (int k=0; k<rec.numValues; k++)
		     { ((boolean[])result)[k] = rec.vval; 
		     } 
		  }
	       }
	      if (rec.resHolder instanceof Vector)
	       { ((Vector)rec.resHolder).add (result); 
	       }
	    }
	   catch (ArgParseException e)
	    { setError (e.getMessage());
	      throw e;
	    }
	   return idx+1;
	 }

	private String spaceString (int n)
	 {
	   StringBuffer sbuf = new StringBuffer(n);
	   for (int i=0; i<n; i++)
	    { sbuf.append(' ');
	    }
	   return sbuf.toString();
	 }

// 	public String getShortHelpMessage ()
// 	 {
// 	   String s;
// 	   Record rec;
// 	   NameDesc ndesc;
// 	   int initialIndent = 8;
// 	   int col = initialIndent;

// 	   if (maxcols <= 0)
// 	    { maxcols = 80; 
// 	    }
// 	   if (matchList.size() > 0)
// 	    { ps.print (spaceString(initialIndent));
// 	    }
// 	   for (int i=0; i<matchList.size(); i++)
// 	    { rec = (Record)matchList.get(i);
// 	      s = "[";
// 	      for (ndesc=rec.nameList; ndesc!=null; ndesc=ndesc.next)
// 	       { s = s + ndesc.name;
// 		 if (ndesc.oneWord == false)
// 		  { s = s + " ";
// 		  }
// 		 if (ndesc.next != null)
// 		  { s = s + ",";
// 		  }
// 	       }
// 	      if (rec.convertCode != 'v' && rec.convertCode != 'h')
// 	       { if (rec.valueDesc != null)
// 		  { s += rec.valueDesc; 
// 		  }
// 		 else
// 		  { s = s + "<" + rec.valTypeName() + ">";
// 		    if (rec.numValues > 1)
// 		     { s += "X" + rec.numValues;
// 		     }
// 		  }
// 	       }
// 	      s = s + "]";
// 	      /* 
// 		 (col+=s.length()) > (maxcols-1) => we will spill over edge. 
// 			 we use (maxcols-1) because if we go right to the edge
// 			 (maxcols), we get wrap new line inserted "for us".
// 		 i != 0 means we print the first entry, no matter
// 			 how long it is. Subsequent entries are printed
// 			 full length anyway. */		     

// 	      if ((col+=s.length()) > (maxcols-1) && i != 0)
// 	       { col = initialIndent+s.length();
// 		 ps.print ("\n" + spaceString(initialIndent));
// 	       }
// 	      ps.print (s);
// 	    }
// 	   if (matchList.size() > 0)
// 	    { ps.print ('\n');
// 	      ps.flush();
// 	    }
// 	 }

	/**
	 * Returns a string describing the allowed options
	 * in detail.
	 *
	 * @return help information string.
	 */
	public String getHelpMessage ()
	 {
	   Record rec;
	   NameDesc ndesc;
	   boolean hasOneWordAlias = false;
	   String s;

	   s = "Usage: " + synopsisString + "\n";
	   s += "Options include:\n\n";
	   for (int i=0; i<matchList.size(); i++)
	    { String optionInfo = "";
	      rec = (Record)matchList.get(i);
	      if (rec.convertCode=='h' && !helpOptionsEnabled)
	       { continue;
	       }
	      for (ndesc=rec.nameList; ndesc!=null; ndesc=ndesc.next)
	       { if (ndesc.oneWord)
		  { hasOneWordAlias = true;
		    break;
		  }
	       }
	      for (ndesc=rec.nameList; ndesc!=null; ndesc=ndesc.next)
	       { optionInfo += ndesc.name;
		 if (hasOneWordAlias && !ndesc.oneWord)
		  { optionInfo += " ";
		  }
		 if (ndesc.next != null)
		  { optionInfo += ",";
		  }
	       }
	      if (!hasOneWordAlias)
	       { optionInfo += " "; 
	       }
	      if (rec.convertCode != 'v' && rec.convertCode != 'h')
	       { if (rec.valueDesc != null)
		  { optionInfo += rec.valueDesc;
		  }
		 else
		  { if (rec.rangeDesc != null)
		     { optionInfo += "<" + rec.valTypeName() + " "
			              + rec.rangeDesc + ">";
		     }
		    else
		     { optionInfo += "<" + rec.valTypeName() + ">";
		     }
		  }
	       }
	      if (rec.numValues > 1)
	       { optionInfo += "X" + rec.numValues;
	       }
	      s += optionInfo;
	      if (rec.helpMsg.length() > 0)
	       { int pad = helpIndent - optionInfo.length();
		 if (pad < 2)
		  { s += '\n';
		    pad = helpIndent;
		  }
		 s += spaceString(pad) + rec.helpMsg;
	       }
	      s += '\n';
	    }
	   return s;
	 }
		
	/**
	 * Returns the parser's error message. This is automatically
	 * set whenever an error is encountered in <code>matchArg</code>
	 * or <code>matchAllArgs</code>, and is automatically set to
	 * <code>null</code> at the beginning of these methods.
	 *
	 * @return error message
	 */
	public String getErrorMessage()
	 { 
	   return errMsg;
	 }

	/**
	 * Returns the value of an unmatched argument discovered {@link
	 * #matchArg matchArg} or {@link #matchAllArgs(String[],int,int)
	 * matchAllArgs}.  If there was no unmatched argument,
	 * <code>null</code> is returned.
	 *
	 * @return unmatched argument
	 */
	public String getUnmatchedArgument()
	 {
	   return unmatchedArg;
	 }
}


