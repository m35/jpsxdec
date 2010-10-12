
package jpsxdec.discitems;

import java.io.IOException;
import java.io.PrintStream;
import java.util.WeakHashMap;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.util.FeedbackStream;


/** Created by DiscItems to manage the item's possible saving options. 
 *  Accepts the command-line text to parse and interpret as the saving options,
 *  and/or creates the dialog interface for visually selecting saving options.
 *  Call {@link #makeSaver()} when ready to save.  */
public abstract class DiscItemSaverBuilder {

    private WeakHashMap<ChangeListener, Boolean> _changeListeners;
    private ChangeEvent _event;

    final public void addChangeListener(ChangeListener listener) {
        if (_changeListeners == null)
            _changeListeners = new WeakHashMap<ChangeListener, Boolean>();
        _changeListeners.put(listener, Boolean.TRUE);
    }

    public void removeChangeListener(ChangeListener listener) {
        if (_changeListeners == null)
            return;
        _changeListeners.remove(listener);
    }

    /** Subclasses should call this when any option might have changed. */
    protected void firePossibleChange() {
        if (_changeListeners == null || _changeListeners.isEmpty())
            return;
        if (_event == null)
            _event = new ChangeEvent(this);
        for (ChangeListener listener : _changeListeners.keySet()) {
            listener.stateChanged(_event);
        }
    }

    /** Create a GUI for all the options that can be placed in a window. */
    abstract public JPanel getOptionPane();
    /** Prints the item's specific possible command-line options. */
    abstract public void printHelp(FeedbackStream fbs);
    /** Parse command-line options from an array of command-line arguments. */
    abstract public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs);
    /** Prints the current options settings. */
    abstract public void printSelectedOptions(PrintStream ps);
    abstract public void resetToDefaults();
    /** Creates the saver using a snapshot of current options. */
    abstract public IDiscItemSaver makeSaver() throws IOException;
    
}
