/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

package jpsxdec.psxvideo.encode;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;

/** Encodes a {@link PsxYCbCrImage} into an {@link MdecInputStream}.
 * After encoding, the MdecInputStream will most likely be then
 * compressed as a bitstream.  */
public class MdecEncoder implements Iterable<MacroBlockEncoder> {

    private final Iterable<MdecCode>[] _aoMacroBlocks;
    private final ArrayList<MacroBlockEncoder> _replaceMbs = new ArrayList<MacroBlockEncoder>();
    private final int _iMacBlockWidth;
    private final int _iPixWidth, _iPixHeight;
    private final int _iMacBlockHeight;

    /** Used for full frame replace. */
    public MdecEncoder(PsxYCbCrImage ycbcr, int iWidth, int iHeight) {

        if (ycbcr.getLumaWidth() % 16 != 0 || ycbcr.getLumaHeight() % 16 != 0)
            throw new IllegalArgumentException();

        _iPixWidth = iWidth;
        _iPixHeight = iHeight;
        _iMacBlockWidth = ycbcr.getLumaWidth() / 16;
        _iMacBlockHeight = ycbcr.getLumaHeight() / 16;

        _aoMacroBlocks = new Iterable[_iMacBlockWidth * _iMacBlockHeight];

        for (int iMbX = 0; iMbX < _iMacBlockWidth; iMbX++) {
            for (int iMbY = 0; iMbY < _iMacBlockHeight; iMbY++) {
                MacroBlockEncoder enc = new MacroBlockEncoder(ycbcr, iMbX, iMbY);
                _aoMacroBlocks[iMbX * _iMacBlockHeight + iMbY] = enc;
                _replaceMbs.add(enc);
            }
        }

    }

    /** Used for partial replace. */
    public MdecEncoder(ParsedMdecImage original, PsxYCbCrImage newYcbcr, List<Point> replaceMbs) {

        if (newYcbcr.getLumaWidth() % 16 != 0 || newYcbcr.getLumaHeight() % 16 != 0)
            throw new IllegalArgumentException();

        _iPixWidth = original.getWidth();
        _iPixHeight = original.getHeight();
        _iMacBlockWidth = Calc.macroblockDim(newYcbcr.getLumaWidth());
        _iMacBlockHeight = Calc.macroblockDim(newYcbcr.getLumaHeight());

        _aoMacroBlocks = new Iterable[_iMacBlockWidth * _iMacBlockHeight];

        Point p = new Point();
        for (int iMbX = 0; iMbX < _iMacBlockWidth; iMbX++) {
            for (int iMbY = 0; iMbY < _iMacBlockHeight; iMbY++) {
                p.setLocation(iMbX, iMbY);
                Iterable<MdecCode> enc;
                if (replaceMbs.contains(p)) {
                    MacroBlockEncoder e = new MacroBlockEncoder(newYcbcr, iMbX, iMbY);
                    _replaceMbs.add(e);
                    enc = e;
                } else {
                    enc = original.getMacroBlockCodes(iMbX, iMbY);
                }
                _aoMacroBlocks[iMbX * _iMacBlockHeight + iMbY] = enc;
            }
        }

    }

    /** Iterator for only the macro blocks that will be replaced. */
    public Iterator<MacroBlockEncoder> iterator() {
        return _replaceMbs.iterator();
    }

    public MdecInputStream getStream() {
        return new EncodedMdecInputStream();
    }

    public int getMacroBlockCount() {
        return _iMacBlockWidth * _iMacBlockHeight;
    }
    
    public int getMacroBlockWidth() {
        return _iMacBlockWidth;
    }
    
    public int getMacroBlockHeight() {
        return _iMacBlockHeight;
    }

    public int getPixelWidth() {
        return _iPixWidth;
    }

    public int getPixelHeight() {
        return _iPixHeight;
    }

    private class EncodedMdecInputStream extends MdecInputStream {

        private int __iCurMacBlk = 0;
        private Iterator<MdecCode> __curMb;

        public EncodedMdecInputStream() {
            __curMb = _aoMacroBlocks[__iCurMacBlk].iterator();
        }

        public boolean readMdecCode(MdecCode code) throws MdecException.Read {

            if (!__curMb.hasNext()) {
                // end of current macroblock, move to next
                if (__iCurMacBlk >= _aoMacroBlocks.length)
                    throw new IllegalStateException("Read beyond EncodedMdecInputStream");
                __iCurMacBlk++;
                __curMb = _aoMacroBlocks[__iCurMacBlk].iterator();
            }
            code.set(__curMb.next());
            return code.isEOD(); // hopefully no bad EOD codes are part of the list
        }

    }

}
