/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;


public class ZippedCdReader {

    private static final Logger LOG = Logger.getLogger(ZippedCdReader.class.getName());

    public static boolean isZipEnabled() {
        Object zipProp = System.getProperty("zip");
        return zipProp != null;
    }


    private static final String[] DISC_EXTENSIONS = {
        ".iso",
        ".bin",
        ".img",
        ".mdf",
        ".str",
        ".xa" ,
    };

    private static boolean hasDiscExtension(@Nonnull String sFileName) {
        String sLowerCaseFileName = sFileName.toLowerCase();
        for (String sDiscExt : DISC_EXTENSIONS) {
            if (sLowerCaseFileName.endsWith(sDiscExt))
                return true;
        }
        return false;
    }

    public static @CheckForNull BufferedBytesReader tryReadZippedDisc(@Nonnull File sourceFile)
            throws CdException.Read, CdException.FileNotFound, CdException.FileTooLarge
    {
        if (!isZipEnabled())
            return null;

        try {
            return readZippedDisc(sourceFile);
        } catch (ZipException ex) {
            return null;
        }
    }

    public static @Nonnull BufferedBytesReader readZippedDisc(@Nonnull File sourceFile)
            throws ZipException, CdException.FileNotFound, CdException.Read, CdException.FileTooLarge
    {
        Set<ZipEntry> supportedEntries = new TreeSet<ZipEntry>(new Comparator<ZipEntry>() {
            @Override
            public int compare(ZipEntry o1, ZipEntry o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        ZipFile zipFile;
        try {
            zipFile = new ZipFile(sourceFile);
        } catch (ZipException ex) {
            throw ex;
        } catch (FileNotFoundException ex) {
            // Docs don't mention this exception, but it would be nice if it threw it
            throw new CdException.FileNotFound(sourceFile, ex);
        } catch (IOException ex) {
            throw new CdException.Read(sourceFile, ex);
        }

        IOException closeEx = null;
        byte[] abDiscImage;
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            long lngTotalSize = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String sName = entry.getName();
                if (hasDiscExtension(sName)) {
                    long lngEntrySize = entry.getSize();
                    if (lngEntrySize < 0)
                        throw new RuntimeException();
                    lngTotalSize += lngEntrySize;
                    supportedEntries.add(entry);
                }
            }

            if (lngTotalSize > Integer.MAX_VALUE)
                throw new CdException.FileTooLarge(sourceFile, lngTotalSize, Integer.MAX_VALUE);

            LOG.log(Level.INFO, "{0} allocating {1,number,#}", new Object[]{sourceFile, lngTotalSize});
            abDiscImage = new byte[(int) lngTotalSize];
            int iOffset = 0;
            for (ZipEntry entry : supportedEntries) {
                LOG.log(Level.INFO, "Reading {0} size {1,number,#}", new Object[]{entry.getName(), entry.getSize()});
                InputStream is = null;
                try {
                    is = zipFile.getInputStream(entry);
                    int iSize = (int) entry.getSize();
                    IO.readByteArray(is, abDiscImage, iOffset, iSize);
                    iOffset += iSize;
                } catch (IOException ex) {
                    throw new CdException.Read(sourceFile, ex);
                } finally {
                    closeEx = IO.closeSilently(is, LOG);
                }
                if (closeEx != null)
                    throw new CdException.Read(sourceFile, closeEx);
            }
            assert iOffset == abDiscImage.length;
        } finally {
            try {
                zipFile.close();
            } catch (IOException ex) {
                Misc.log(LOG, Level.SEVERE, ex, "Error closing zip file {0}", sourceFile);
                closeEx = ex;
            }
        }
        if (closeEx != null)
            throw new CdException.Read(sourceFile, closeEx);

        return new BufferedBytesReader(sourceFile, abDiscImage);
    }

}
