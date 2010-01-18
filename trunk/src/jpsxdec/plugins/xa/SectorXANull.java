/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.xa;

import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.cdreaders.CDSector.CDXAHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.NotThisTypeException;
    
/** XA files have lots of audio channels. When a channel is no longer used
 * because the audio is finished, it is sometimes filled with what I call
 * 'null' sectors. These sectors have absolutely no SubMode flags set,
 * and are often full of zeros. */
public class SectorXANull extends IdentifiedSector {

    public SectorXANull(CDSector cdSector)
            throws NotThisTypeException 
    {
        super(cdSector);
        // if it doesn't have a sector header, then it can't be a null sector
        if (!cdSector.hasSectorHeader())
            throw new NotThisTypeException();
        // if it's not a Form 2 sector, then it can't be a null sector
        if (cdSector.getSubMode().getForm() != 2)
            throw new NotThisTypeException();

        // if it's not flagged as a null sector...
        if (cdSector.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.NULL)
        {
            // if it's flagged as an audio sector, then it's not a null sector
            if (cdSector.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.AUDIO)
                throw new NotThisTypeException();

            // if it has a valid channel number, then it's not a null sector
            if (cdSector.getChannel() >= 0 || cdSector.getChannel() < 32) {
                throw new NotThisTypeException();
                // Ace Combat 3 has several AUDIO sectors with channel 255
                // that seem to be "null" sectors
            }
        }

    }

    public String toString() {
        return "XA Null " + super.toString();
    }

    public int getPSXUserDataSize() {
            return 1; // just making the sector 1 byte long for no reason
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                0, getPSXUserDataSize());
    }
    
    public int getSectorType() {
        return -1;
    }
    
    public String getTypeName() {
        return "Null";
    }

    public JPSXPlugin getSourcePlugin() {
        return JPSXPluginXAAudio.getPlugin();
    }

}
