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

package testutil;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import jpsxdec.util.IO;
import org.junit.Assert;

public class Util {

    public static File resourceAsTempFile(Class<?> cls, String sResource) throws IOException {
        File f = File.createTempFile(sResource, "-tmp");
        resourceAsFile(cls, sResource, f);
        return f;
    }

    public static File resourceAsFile(Class<?> cls, String sResource, File fileToCreate) throws IOException {
        InputStream is = cls.getResourceAsStream(sResource);
        try {
            IO.writeIStoFile(is, fileToCreate);
        } finally {
            is.close();
        }
        return fileToCreate;
    }

    public static File resourceAsFile(Class<?> cls, String sResource) throws IOException {
        File f = new File(new File(sResource).getName());
        return resourceAsFile(cls, sResource, f);
    }

    public static byte[] readResource(Class<?> cls, String sResource) throws IOException {
        InputStream is = cls.getResourceAsStream(sResource);
        try {
            return IO.readEntireStream(is);
        } finally {
            is.close();
        }
    }

    public static Object getField(Object instance, String sField)
            throws SecurityException, NoSuchFieldException, ClassNotFoundException,
                   IllegalArgumentException, IllegalAccessException
    {
        Field field = instance.getClass().getDeclaredField(sField);
        field.setAccessible(true);
        return field.get(instance);
    }

    public static void setField(Object instance, String sField, Object value)
            throws SecurityException, NoSuchFieldException, ClassNotFoundException,
                   IllegalArgumentException, IllegalAccessException
    {
        Field field = instance.getClass().getDeclaredField(sField);
        field.setAccessible(true);
        field.set(instance, value);
    }

    public static void writeIntArrayBE(String sFileName, int[] ai) throws IOException {
        FileOutputStream fos = new FileOutputStream(sFileName);
        for (int i : ai) {
            IO.writeInt32BE(fos, i);
        }
        fos.close();
    }

    public static int[] readIntArrayBE(byte[] ab) {
        Assert.assertEquals(0, ab.length % 4);
        int[] ai = new int[ab.length / 4];
        for (int i = 0; i < ai.length; i++) {
            ai[i] = IO.readSInt32BE(ab, i * 4);
        }
        return ai;
    }
}
