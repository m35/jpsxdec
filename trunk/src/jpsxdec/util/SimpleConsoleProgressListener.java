/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.util;

import java.util.logging.Logger;

/**
 *
 * @author Michael
 */
public class SimpleConsoleProgressListener implements ProgressListener {

    public void progressStart(String s) throws TaskCanceledException {
        System.out.println(s);
    }

    public void progressStart() throws TaskCanceledException {
    }

    public void progressEnd() throws TaskCanceledException {
    }

    public void progressUpdate(double dblPercentComplete) throws TaskCanceledException {
    }

    private static final long EVENT_INTERVAL = 1000;

    private long _lngLastEvent = 0;
    private String _sInfo;

    public void event(String sDescription) {
        System.out.println(sDescription);
        _lngLastEvent = System.currentTimeMillis();
    }

    public boolean seekingEvent() {
        return (System.currentTimeMillis() - _lngLastEvent > EVENT_INTERVAL);
    }

    public void info(String s) {
        _sInfo = s;
    }

    public Logger getLog() {
        return Logger.getAnonymousLogger();
    }

}
