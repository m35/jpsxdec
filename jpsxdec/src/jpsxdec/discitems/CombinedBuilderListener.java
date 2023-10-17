/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

import java.util.ArrayList;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** Bridge between {@link DiscItemSaverBuilder} events to controls listening
 * to events. Use this single change listener to notify all the controls so it's
 * easier to swap out when the {@link DiscItemSaverBuilder} is changed. */
public class CombinedBuilderListener<T extends DiscItemSaverBuilder> {
    /** Controls that will be notified of any changes. */
    private final ArrayList<ChangeListener> _controlsListening = new ArrayList<ChangeListener>();
    /** Current {@link DiscItemSaverBuilder} that is being listened to. */
    @Nonnull
    private T _saverBuilder;

    /** The class of the provided {@link DiscItemSaverBuilder} will be the
     * only class accepted in {@link #changeSourceBuilder(DiscItemSaverBuilder)}. */
    public CombinedBuilderListener(@Nonnull T saverBuilder) {
        _saverBuilder = saverBuilder;
        _saverBuilder.addChangeListener(_thisChangeListener);
    }

    /** Add controls to the list of listeners. */
    public void addListeners(ChangeListener ... aoControls) {
        _controlsListening.addAll(Arrays.asList(aoControls));
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener control : aoControls) {
            control.stateChanged(e);
        }
    }

    /** Swap out the {@link DiscItemSaverBuilder} being listened to.
     * Will only accept {@link DiscItemSaverBuilder}s of the same class
     * as the initial {@link DiscItemSaverBuilder}.
     * All listening controls will be notified of the change.
     * @return if the {@link DiscItemSaverBuilder} is the same class.
     */
    public boolean changeSourceBuilder(@Nonnull DiscItemSaverBuilder saverBuilder) {
        T oldBuilder = _saverBuilder;
        if (_saverBuilder.getClass().equals(saverBuilder.getClass())) {
            @SuppressWarnings("unchecked") // we just confirmed the new object has the exact same class
            T suppress = (T)saverBuilder;
            _saverBuilder = suppress;
            oldBuilder.removeChangeListener(_thisChangeListener);
            _saverBuilder.addChangeListener(_thisChangeListener);
            ChangeEvent e = new ChangeEvent(this);
            _thisChangeListener.stateChanged(e);
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    private final ChangeListener _thisChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(@Nonnull ChangeEvent e) {
            for (ChangeListener control : _controlsListening) {
                control.stateChanged(e);
            }
        }
    };

    /** Returns the current {@link DiscItemSaverBuilder}. */
    public @Nonnull T getBuilder() {
        return _saverBuilder;
    }

}
