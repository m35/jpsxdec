/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.util.ArgParser;
import jpsxdec.util.TaskCanceledException;


/** Created by {@link DiscItem}s to manage the item's possible saving options.
 *<p>
 *  Accepts the command-line text to parse and interpret as the saving options,
 *  and/or creates a dialog interface for visually selecting saving options.
 *<p>
 *  Call {@link #startSave(jpsxdec.util.ProgressLogger, java.io.File)} when ready to save.  */
public abstract class DiscItemSaverBuilder {

    @CheckForNull
    private WeakHashMap<ChangeListener, Boolean> _changeListeners;
    @CheckForNull
    private ChangeEvent _event;

    private final List<File> _generatedFiles = new ArrayList<File>();


    final public void addChangeListener(@CheckForNull ChangeListener listener) {
        if (_changeListeners == null)
            _changeListeners = new WeakHashMap<ChangeListener, Boolean>();
        _changeListeners.put(listener, Boolean.TRUE);
    }

    final public void removeChangeListener(@CheckForNull ChangeListener listener) {
        if (_changeListeners == null)
            return;
        _changeListeners.remove(listener);
    }

    /** Subclasses should call this when any option might have changed. */
    final protected void firePossibleChange() {
        if (_changeListeners == null || _changeListeners.isEmpty())
            return;
        if (_event == null)
            _event = new ChangeEvent(this);
        for (ChangeListener listener : _changeListeners.keySet()) {
            listener.stateChanged(_event);
        }
    }

    /** Before starting to save, subclasses should call this to clear the generated files. */
    final protected void clearGeneratedFiles() {
        _generatedFiles.clear();
    }
    /** During the saving process, subclasses should call this to add generated files. */
    final protected void addGeneratedFile(@Nonnull File file) {
        _generatedFiles.add(file);
    }
    /** Returns a list of files generated during the last saving process. */
    final public @Nonnull List<File> getGeneratedFiles() {
        return _generatedFiles;
    }

    /** Get the disc item that generated this saver builder. */
    abstract public @Nonnull DiscItem getDiscItem();
    /** The description of the source (input) that the generated files
     * will come from (basically a disc item, but with a flexible description). */
    final public @Nonnull String getInput() {
        return getDiscItem().getIndexId().serialize();
    }

    /** Attempts to copy the settings of this disc item to another disc item.
     * Returns true if the other disc item is similar enough to copy setting to.
     * (usually because it is the exact same type) */
    abstract public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other);

    /** Prints the item's specific possible command-line options. */
    abstract public void printHelp(@Nonnull FeedbackStream fbs);
    /** Configure the saver builder from command-line arguments. */
    abstract public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs);
    /** Prints the options used for saving. */
    abstract public void printSelectedOptions(@Nonnull ILocalizedLogger log);

    /** Get a localized summary of what files will be generated on save. */
    abstract public @Nonnull ILocalizedMessage getOutputSummary();
    /** Create a GUI for options that can be placed in a window. */
    abstract public @Nonnull DiscItemSaverBuilderGui getOptionPane();

    /** Initiates the saving process. */
    abstract public void startSave(@Nonnull ProgressLogger pl, @CheckForNull File directory)
            throws LoggedFailure, TaskCanceledException;
}
