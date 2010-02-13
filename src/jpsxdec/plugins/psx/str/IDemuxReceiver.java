/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.plugins.psx.str;

import java.io.IOException;

public interface IDemuxReceiver {
    int getWidth();
    int getHeight();
    void receive(byte[] abDemux, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException;
}
