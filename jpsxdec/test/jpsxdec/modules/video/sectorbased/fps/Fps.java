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

package jpsxdec.modules.video.sectorbased.fps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import jpsxdec.util.Fraction;
import org.junit.*;
import static org.junit.Assert.*;


public class Fps {

    @Test
    public void _20_A8() throws IOException {
        test("20FPS_A8.dat", new Fraction(15,2), 0);
    }
    @Test
    public void _20_A16() throws IOException {
        test("20FPS_A16.dat", new Fraction(15,2), 0);
    }
    @Test
    public void _DREDD15() throws IOException {
        test("DREDD15FPS.dat", new Fraction(10,1), 0);
    }
    @Test
    public void _NTSC20_A8() throws IOException {
        test("NTSC20_A8.dat", new Fraction(3003,400), 0);
    }
    @Test
    public void _NTSC20_A8_SB() throws IOException {
        test("NTSC20_A8-SB.dat", new Fraction(3003,400), 0);
    }
    @Test
    public void _NTSC15_A8_100() throws IOException {
        test("NTSC15_A8-100,999.dat", new Fraction(1001,100), 0);
    }
    @Test
    public void _NTSC15_A8_101() throws IOException {
        test("NTSC15_A8-101,1000.dat", new Fraction(1001,100), 0);
    }
    @Test
    public void _LUNAR2_24FPS_A16_S43() throws IOException {
        test("LUNAR2_24FPS_A16(S43).dat", new Fraction(25,4), 0);
    }
    @Test
    public void _LUNAR2_24FPS_A16_S56() throws IOException {
        test("LUNAR2_24FPS_A16(S56).dat", new Fraction(25,4), 0);
    }

    private static void test(String sFile, Fraction expected, int iFrameStart) throws IOException {
        System.out.println(sFile);
        System.out.println(expected);

        BufferedReader reader = new BufferedReader(new InputStreamReader(Fps.class.getResourceAsStream(sFile)));

        reader.readLine(); // skip header

        int iStartSector = 0;
        for (int i = 0; i < iFrameStart; i++) {
            String sLine = reader.readLine();
            InconsistentFrameSequence.LineParse lp = new InconsistentFrameSequence.LineParse(sLine);
            iStartSector = lp.iFrameStartSector;
        }

        String sLine;
        StrFrameRateCalc f = null;
        while ((sLine = reader.readLine()) != null) {
            InconsistentFrameSequence.LineParse lp = new InconsistentFrameSequence.LineParse(sLine);
            if (f == null)
                f = new StrFrameRateCalc(lp.iFrameStartSector, lp.iFrameEndSector);
            else
                f.addFrame(lp.iFrameStartSector - iStartSector, lp.iFrameEndSector - iStartSector);
        }

        reader.close();

        Fraction actual = f.getSectorsPerFrame(-1, -1, -1);
        System.out.println(expected);
        System.out.println(actual);
        assertEquals(expected, actual);
    }

}
