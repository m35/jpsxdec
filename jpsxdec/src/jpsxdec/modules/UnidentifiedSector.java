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

package jpsxdec.modules;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;


/** If all else fails, we don't know what kind of data this sector contains.
 * Can represent any sector except {@link SectorCdAudio}.
 * Currently not actually used due to the TODO below. */
public class UnidentifiedSector extends IdentifiedSector {

    public static final String TYPE_NAME = "Unknown";

    /**
     * @throws IllegalArgumentException if {@link CdSector#isCdAudioSector()} is true.
     */
    // TODO: don't want to use IllegalArgumentException
    public UnidentifiedSector(@Nonnull CdSector cdSector) throws IllegalArgumentException {
        super(cdSector);

        if (cdSector.isCdAudioSector())
            throw new IllegalArgumentException();

        setProbability(100);
    }

    @Override
    public @Nonnull String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String toString() {
        CdSector cdSector = getCdSector();
        StringBuilder sb = new StringBuilder(" ");
        // add the first 32 bytes for unknown sectors
        // may be helpful for identifying them
        for (int i = 0; i < 32; i++) {
            sb.append(String.format("%02x", cdSector.readUserDataByte(i)));
        }

        return getTypeName() + " " + cdToString() + sb;
    }

}
