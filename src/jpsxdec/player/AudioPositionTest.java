/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.sound.sampled.*;

public class AudioPositionTest {


    public static void main(String[] args) throws LineUnavailableException, IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        Mixer.Info[] aoMixerInfos = AudioSystem.getMixerInfo();
        System.out.println("Select output mixer:");
        for (int i = 0; i < aoMixerInfos.length; i++) {
            System.out.println("  " + i + ". " + aoMixerInfos[i].toString());
        }

        Mixer.Info mixInfo = aoMixerInfos[Integer.parseInt(br.readLine())];
        Mixer mixer = AudioSystem.getMixer(mixInfo);
        for (Line.Info info1 : mixer.getSourceLineInfo()) {
            System.out.println(info1);
        }

        System.out.println("Enter sample rate (e.g. 18900, 44100):");
        int iSampleRate = Integer.parseInt(br.readLine());

        final AudioFormat audFmt = new AudioFormat(iSampleRate, 16, 1, true, true);
        final DataLine.Info dataInfo = new DataLine.Info(SourceDataLine.class, audFmt);

        SourceDataLine player = (SourceDataLine) mixer.getLine(dataInfo);
        System.out.println("Buffer size = " + player.getBufferSize());
        player.open(audFmt);
        player.start();
        
        byte[] abBuf = new byte[2 * 400];

        long lngTestLength = 5 * 1000; // 20 seconds
        long lngTestStart = System.currentTimeMillis();
        long lngTestEnd = lngTestStart + lngTestLength;

        System.out.println(System.getProperty("os.name") + "\tJava " +
                           System.getProperty("java.version"));
        StringBuilder sb = new StringBuilder(400);
        while (System.currentTimeMillis() < lngTestEnd) {
            player.write(abBuf, 0, abBuf.length);
            long lngTime = System.currentTimeMillis();
            long lngPos = player.getLongFramePosition();
            sb.append(lngTime - lngTestStart);
            sb.append('\t');
            sb.append(lngPos);
            System.out.println(sb);
            sb.setLength(0);
            Thread.yield();
        }

        player.stop();
        player.close();
    }
}
