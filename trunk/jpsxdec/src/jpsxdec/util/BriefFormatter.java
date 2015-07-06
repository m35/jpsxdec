/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import jpsxdec.Version;


/** Brief logging formatter.
 *<p>
 * Never localizes strings.
 *<p>
 * Originally from the book "JDK 1.4 Tutorial" by Gregory M. Travis.
 * http://www.manning.com/travis/
 */
public class BriefFormatter extends Formatter {

    /** Line separator string.  This is the value of the line.separator
     * property at the moment that the SimpleFormatter was created. */
    private final static String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Date _date = new Date();
    private final SimpleDateFormat _timeFormatter = new SimpleDateFormat("HH:mm");

    @Override
    public String getHead(Handler h) {
        StringBuilder sb = new StringBuilder();
        sb.append(Version.VerString.getEnglishMessage()).append(LINE_SEPARATOR);
        sb.append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version")).append(LINE_SEPARATOR);
        sb.append("Java ").append(System.getProperty("java.version")).append(LINE_SEPARATOR);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sb.append(dateFormatter.format(Calendar.getInstance().getTime())).append(LINE_SEPARATOR);
        return sb.toString();
    }


    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {
	StringBuilder sb = new StringBuilder();
	// Minimize memory allocations here.
	_date.setTime(record.getMillis());
	sb.append(_timeFormatter.format(_date));
	sb.append(' ');
        sb.append(record.getLoggerName());
	sb.append(' ');
	sb.append(record.getLevel().getName());
	sb.append(": ");
        
        // don't localize the message
        String sMsg = record.getMessage();
        Object aoParams[] = record.getParameters();
        if (aoParams != null && aoParams.length > 0)
            sMsg = java.text.MessageFormat.format(sMsg, aoParams);
	sb.append(sMsg);
        
	sb.append(LINE_SEPARATOR);
	if (record.getThrown() != null) {
            sb.append(Misc.stack2string(record.getThrown()));
	}
	return sb.toString();
    }
}
