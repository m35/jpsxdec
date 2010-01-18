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

package jpsxdec.plugins.iso9660;

import argparser.ArgParser;
import argparser.BooleanHolder;
import java.io.IOException;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.DiscItemSaver;
import jpsxdec.plugins.DiscItemSerialization;
import javax.swing.JPanel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.ProgressListener;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


public class DiscItemISO9660File extends DiscItem {

    private static final Logger log = Logger.getLogger(DiscItemISO9660File.class.getName());
    public static final String TYPE_ID = "File";

    private ArrayList<DiscItem> _aoChildren;
    private final File _path;
    private final long _lngSize;

    public DiscItemISO9660File(int iStartSector, int iEndSector, File path, long lngSize) {
        super(iStartSector, iEndSector);
        _path = path;
        _lngSize = lngSize;
    }

    public int getOverlap(DiscItem item) {
        // does not intersect this file at all
        if (item.getEndSector() < getStartSector() || item.getStartSector() > getEndSector())
            return 0;
        
        // there is definitely intersection somewhere

        int iOverlap;
        if (item.getStartSector() < getStartSector()) {
            log.warning(item + " breaks this file boundaries " + this);
            if (item.getEndSector() > getEndSector()) {
                // this file is totally inside item
                iOverlap = getSectorLength();
            } else {
                // last part of item is inside this file
                iOverlap = item.getSectorLength();
            }
        } else {
            if (item.getEndSector() > getEndSector()) {
                // first part of item is inside this file
                log.warning(item + " breaks this file boundaries " + this);
                iOverlap = getEndSector() - item.getStartSector() + 1;
            } else {
                // item is totally inside this file
                iOverlap = item.getEndSector() - item.getStartSector() + 1;
            }
        }
        assert(iOverlap >= 0);
        return iOverlap;
    }

    public void addChild(DiscItem item) {
        if (_aoChildren == null)
            _aoChildren = new ArrayList<DiscItem>();

        item.setSuggestedBaseName(String.format("%s[%d]",
                Misc.getBaseName(_path.getName()), _aoChildren.size()));

        if (log.isLoggable(Level.INFO)) log.info("Added " + item + " to " + this);
        _aoChildren.add(item);
    }
    
    @Override
    public DiscItemSerialization serialize() {
        DiscItemSerialization fields = super.superSerial(TYPE_ID);
        fields.addNumber("Size", _lngSize);
        fields.addString("Path", _path.toString());
        return fields;
    }

    public DiscItemISO9660File(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
        _lngSize = fields.getLong("Size");
        _path = new File(fields.getString("Path"));
    }

    @Override
    public DiscItemSaver getSaver() {
        return new ISO9660Saver();
    }

    @Override
    public int getMediaType() {
        return DiscItem.MEDIA_TYPE_STATIC;
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    private class ISO9660Saver extends DiscItemSaver {

        private boolean __blnSaveRaw = true;

        public boolean getSaveRaw() {
            return __blnSaveRaw;
        }

        public void setSaveRaw(boolean blnSaveRaw) {
            __blnSaveRaw = blnSaveRaw;
        }

        @Override
        public JPanel getOptionPane() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String[] commandLineOptions(String[] asArgs, FeedbackStream infoStream) {
            if (asArgs == null) return null;

            ArgParser parser = new ArgParser("", false);

            BooleanHolder save2048 = new BooleanHolder();
            parser.addOption("-iso %v", save2048);

            String[] asRemain = null;
            asRemain = parser.matchAllArgs(asArgs, 0, 0);

            setSaveRaw(!save2048.value);

            return asRemain;
        }

        @Override
        public void printHelp(FeedbackStream fbs) {
            fbs.indent();
            if (getSourceCD().hasSectorHeader())
                fbs.printlnNorm("-iso   save as 2048 sectors (default raw 2352 sectors)");
            else
                fbs.printlnNorm("[no options available]");
            fbs.outdent();
        }

        @Override
        public void startSave(ProgressListener pl) throws IOException {
            final double iSectLen = getSectorLength();
            final int iStartSect = getStartSector();
            final int iEndSect = getEndSector();
            FileOutputStream fos = new FileOutputStream(_path.getName());
            pl.progressStart();
            for (int iSector = iStartSect; iSector <= iEndSect; iSector++) {
                CDSector cdSector = getSourceCD().getSector(iSector);
                if (getSaveRaw())
                    fos.write(cdSector.getRawSectorData());
                else
                    fos.write(cdSector.getCdUserDataCopy());
                pl.progressUpdate((iSector - iStartSect) / iSectLen);
            }
            pl.progressEnd();
        }

    }

}
