/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

package jpsxdec.cmdline;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ShouldNotLog;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.ArgParser;
import jpsxdec.util.IO;

/** Handle {@code -visualize} option. */
class Command_Visualize extends Command {
    @Nonnull
    private String _sOutfile;

    public Command_Visualize() {
        super("-visualize");
    }

    @Override
    protected @CheckForNull ILocalizedMessage validate(@Nonnull String s) {
        _sOutfile = s;
        return null;
    }

    @Override
    public void execute(@Nonnull ArgParser ap) throws CommandLineException {
        DiscIndex index = getIndex();
        ICdSectorReader cd = index.getSourceCd();
        FileOutputStream pdfStream = null;
        try {
            final int SECTOR_SECTION_SIZE = 32;
            final int TEXT_LINE_HEIGHT = 16;
            final int BOX_AREA_WIDTH = 16;
            final int BOX_MARGIN_LEFT = 2;
            final int BOX_MARGIN_RIGHT = 2;
            final int BOX_WIDTH = BOX_AREA_WIDTH - (BOX_MARGIN_RIGHT + BOX_MARGIN_LEFT);
            final double MAX_PDF_SIZE = 200.0 * 72.0 - 18.0;
            /* priority:
             * ISO file
             * video
             * audio
             * tim
             *
             * summarize to just the important data-points
             */
            _fbs.println(I.CMD_GENERATING_VISUALIZATION());
            int[] aiDataPoints = extractDataPoints(index);
            // pre-determine the tree-area width based on max point of overlapping items
            int iMaxOverlap = findMaxOverlap(aiDataPoints, index);
            //########################################################
            int iWidth = SECTOR_SECTION_SIZE + iMaxOverlap * TEXT_LINE_HEIGHT + iMaxOverlap * BOX_AREA_WIDTH;
            int iHeight = cd.getSectorCount() + 1;
            final double SCALE;
            if (iHeight < MAX_PDF_SIZE) {
                SCALE = 1;
            } else {
                SCALE = MAX_PDF_SIZE / iHeight;
            }
            pdfStream = new FileOutputStream(_sOutfile);
            com.pdfjet.PDF pdf = new com.pdfjet.PDF(pdfStream);
            com.pdfjet.Font pdfFont = new com.pdfjet.Font(pdf, "Helvetica");
            pdfFont.setSize(6 * SCALE);
            com.pdfjet.Page pdfPage = new com.pdfjet.Page(pdf, new double[]{iWidth * SCALE, iHeight * SCALE});
            SectorClaimSystem it = SectorClaimSystem.create(cd);
            ILocalizedLogger log = new ShouldNotLog();
            for (int iSector = 0; it.hasNext(); iSector++) {
                try {
                    IIdentifiedSector idSector = it.next(log);
                    Color c = classToColor(idSector.getClass());
                    int[] aiRgb = {c.getRed(), c.getGreen(), c.getBlue()};

                    com.pdfjet.Box pdfBox = new com.pdfjet.Box(0 * SCALE, iSector * SCALE, SECTOR_SECTION_SIZE * SCALE, 1 * SCALE);
                    pdfBox.setFillShape(true);
                    pdfBox.setLineWidth(0);
                    pdfBox.setColor(aiRgb);
                    pdfBox.drawOn(pdfPage);
                } catch (IOException ex) {
                    ex.printStackTrace(); // TODO?
                }
            }
            it.flush(log);
            DiscItem[] aoRunningItems = new DiscItem[iMaxOverlap];
            /*
             * at each datapoint, there are basically 3 different things that can happen
             * 1) 1 item begins
             * 2) 2 or more items begin
             * and
             * 3) one or more items end
             *
             * Also, disc items can begin and end at the same sector
             *
             */
            for (int iDataPoint : aiDataPoints) {
                // open
                for (DiscItem item : index) {
                    if (item.getStartSector() == iDataPoint) {
                        int i = findFree(aoRunningItems);
                        aoRunningItems[i] = item;
                        double x = (SECTOR_SECTION_SIZE + i * BOX_AREA_WIDTH + BOX_MARGIN_LEFT) * SCALE;
                        double y = item.getStartSector() * SCALE;
                        double w = BOX_WIDTH * SCALE;
                        double h = item.getSectorLength() * SCALE;
                        // draw box
                        com.pdfjet.Box pdfBox = new com.pdfjet.Box(x, y, w, h);
                        Color c = classToColor(item.getClass());
                        int[] aiRgb = {c.getRed(), c.getGreen(), c.getBlue()};
                        pdfBox.setColor(aiRgb);
                        pdfBox.setFillShape(true);
                        pdfBox.setLineWidth(0);
                        pdfBox.drawOn(pdfPage);
                        pdfBox.setFillShape(false);
                        pdfBox.setColor(com.pdfjet.RGB.WHITE);
                        pdfBox.setLineWidth(0.3 * SCALE);
                        pdfBox.drawOn(pdfPage);
                        com.pdfjet.TextLine pdfText = new com.pdfjet.TextLine(pdfFont, item.toString());
                        pdfText.setPosition(x, y);
                        pdfText.setColor(com.pdfjet.RGB.DARK_GRAY);
                        pdfText.drawOn(pdfPage);
                    }
                }
                for (int i = 0; i < aoRunningItems.length; i++) {
                    if (aoRunningItems[i] != null) {
                        if (iDataPoint >= aoRunningItems[i].getEndSector()) {
                            aoRunningItems[i] = null;
                        }
                    }
                }
            }
            pdf.flush();
        } catch (Exception ex) {
            throw new CommandLineException(I.CMD_VISUALIZATION_ERR(), ex);
        } finally {
            IO.closeSilently(pdfStream, Logger.getLogger(Command_Visualize.class.getName()));
        }
    }

    private final HashMap<Class<?>, Color> colorLookup = new HashMap<Class<?>, Color>();

    private @Nonnull Color classToColor(@Nonnull Class<?> c) {
        Color color = colorLookup.get(c);
        if (color == null) {
            int iClr = c.getName().hashCode();
            color = new Color(iClr);
            colorLookup.put(c, color);
        }
        return color;
    }

    private static int findFree(@Nonnull Object[] ao) {
        for (int i = 0; i < ao.length; i++) {
            if (ao[i] == null)
                return i;
        }
        return -1;
    }

    private static @Nonnull int[] extractDataPoints(@Nonnull DiscIndex index) {
        TreeSet<Integer> dataPoints = new TreeSet<Integer>();
        for (DiscItem item : index) {
            dataPoints.add(item.getStartSector());
            dataPoints.add(item.getEndSector());
        }

        int[] aiDataPoints = new int[dataPoints.size()];
        int i = 0;
        for (Integer point : dataPoints) {
            aiDataPoints[i] = point.intValue();
            i++;
        }
        return aiDataPoints;
    }

    private static int findMaxOverlap(@Nonnull int[] aiDataPoints, @Nonnull DiscIndex index) {
        int iMaxOverlap = 0;
        for (int iSector : aiDataPoints) {
            int iSectorOverlap = 0;
            for (DiscItem item : index) {
                if (iSector >= item.getStartSector() && iSector <= item.getEndSector())
                    iSectorOverlap++;
            }
            if (iSectorOverlap > iMaxOverlap)
                iMaxOverlap = iSectorOverlap;
        }
        return iMaxOverlap;
    }

}
