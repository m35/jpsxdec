/*
 *  Code originally from MEAPsoft, but gutted to only write bytes.
 * 
 *  Copyright 2006-2007 Columbia University.
 *
 *  This file is was originally part of MEAPsoft.
 *  http://labrosa.ee.columbia.edu/meapsoft/
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 */

package jpsxdec.util;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Can write to a file or to an audio stream.  For some reason this
 * appears to be difficult to do in the JavaSound framework.  This is
 * just a wrapper class to unify the interface.
 *
 * @author Mike Mandel (mim@ee.columbia.edu)
 */

public class AudioWriter implements Runnable {
  AudioFormat format;
  File file;
  AudioFileFormat.Type targetType;

  PipedOutputStream pos;
  PipedInputStream pis;
  AudioInputStream ais;
  byte[] bytes;
  IOException e;

  // Write to a file
  public AudioWriter(File file, AudioFormat format, 
		     AudioFileFormat.Type targetType) throws IOException {
    //System.out.println("AudioWriter File constructor");
    this.format = format;
    this.targetType = targetType;
    this.file = file;

    // Write to the output stream
    pos = new PipedOutputStream();

    // It will then go to the file via the input streams
    pis = new PipedInputStream(pos);
    ais = new AudioInputStream(pis, format, AudioSystem.NOT_SPECIFIED);
    
    new Thread(this).start();
  }

  public void run() {
    try {
      AudioSystem.write(ais, targetType, file);
    } catch(IOException ex) {
      this.e = ex;
    }
  }

  public void write(byte[] bytes) throws IOException {
    if(pos != null)
      pos.write(bytes, 0, bytes.length);
  }

  public void close() throws IOException {
    if(pos != null) {
      ais.close();
      pis.close();
      pos.close();
    }
  }
  
  public AudioFormat getFormat() { return format; }

}