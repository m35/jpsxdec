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

package jpsxdec.cdreaders;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import javax.annotation.Nonnull;
import jpsxdec.i18n.ILocalizedMessage;


/** Encapsulates the reading of a CD image (BIN/CUE, ISO),
 * or a file containing some (possibly raw) sectors of a CD.
 * <ul>
 * <li>{@link CdSector#SECTOR_SIZE_2048_ISO}
 * <li>{@link CdSector#SECTOR_SIZE_2336_BIN_NOSYNC}
 * <li>{@link CdSector#SECTOR_SIZE_2352_BIN}
 * <li>{@link CdSector#SECTOR_SIZE_2448_BIN_SUBCHANNEL}
 * </ul>
 */
public interface ICdSectorReader extends Closeable {

    @Nonnull CdSector getSector(int iSector) throws CdException.Read;

    /** Returns the number of sectors in the disc image. */
    int getSectorCount();

    /** Size of the raw sectors of the source disc image. */
    int getRawSectorSize();

    @Nonnull ILocalizedMessage getTypeDescription();

    /** If sectors of this disc image could have raw sector headers
     * (i.e. not ISO 2048 images). */
    boolean hasSectorHeader();

    @Nonnull File getSourceFile();

    boolean matchesSerialization(@Nonnull String sSerialization);

    @Nonnull String serialize();

    @Override
    void close() throws IOException;
}
