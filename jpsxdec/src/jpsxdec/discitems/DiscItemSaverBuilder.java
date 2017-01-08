/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

package jpsxdec.discitems;

import java.io.File;
import java.util.WeakHashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.util.ArgParser;
import jpsxdec.util.FeedbackStream;


/** Created by {@link DiscItem}s to manage the item's possible saving options.
 *<p>
 *  Accepts the command-line text to parse and interpret as the saving options,
 *  and/or creates a dialog interface for visually selecting saving options.
 *<p>
 *  Call {@link #makeSaver(java.io.File)} when ready to save.  */
public abstract class DiscItemSaverBuilder {

    @CheckForNull
    private WeakHashMap<ChangeListener, Boolean> _changeListeners;
    @CheckForNull
    private ChangeEvent _event;


    final public void addChangeListener(@CheckForNull ChangeListener listener) {
        if (_changeListeners == null)
            _changeListeners = new WeakHashMap<ChangeListener, Boolean>();
        _changeListeners.put(listener, Boolean.TRUE);
    }

    public void removeChangeListener(@CheckForNull ChangeListener listener) {
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

    /** Create a GUI for options that can be placed in a window. */
    abstract public @Nonnull DiscItemSaverBuilderGui getOptionPane();
    /** Prints the item's specific possible command-line options. */
    abstract public void printHelp(@Nonnull FeedbackStream fbs);
    /** Configure the saver builder from command-line arguments. */
    abstract public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs);
    abstract public void resetToDefaults();
    abstract public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other);
    /** Creates the saver using a snapshot of current options. */
    abstract public @Nonnull IDiscItemSaver makeSaver(@CheckForNull File directory);
    
}
