/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.plugins.xa;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;


public interface IAudioReceiver {
    void write(AudioFormat inFormat, byte[] abData, int iStart, int iLen, int iEndSector) throws IOException;
    void close() throws IOException;
}
