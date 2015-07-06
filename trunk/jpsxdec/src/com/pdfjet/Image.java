/**
 *  Image.java
 *
Copyright (c) 2007, 2008, 2009, 2010 Innovatics Inc.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
 
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and / or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.pdfjet;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;


//>>>>pdfjet {
public class Image {

    protected int objNumber = 0;

    protected double x = 0.0;   // Position of the image on the page
    protected double y = 0.0;
    protected double w = 0;     // Image width
    protected double h = 0;     // Image height

    private double box_x = 0.0;
    private double box_y = 0.0;

    private byte[] data = null;


    public Image(PDF pdf, java.io.InputStream inputStream, int imageType)
            throws Exception {

        if (imageType == ImageType.JPEG) {
            JPEGImage jpg = new JPEGImage(inputStream);
            data = jpg.getData();
            w = jpg.getWidth();
            h = jpg.getHeight();
            if ( jpg.getColorComponents() == 1 ) {
                addImage(pdf, data, imageType, "DeviceGray", 8);            
            }
            else if ( jpg.getColorComponents() == 3 ) {
                addImage(pdf, data, imageType, "DeviceRGB", 8);
            }
        }
        else if (imageType == ImageType.PNG) {
            PNGImage png = new PNGImage(inputStream);
            data = png.getData();
            w = png.getWidth();
            h = png.getHeight();
            if ( png.colorType == 0 ) {
                addImage(pdf, data, imageType, "DeviceGray", png.bitDepth);                
            }
            else {
                if ( png.bitDepth == 16 ) {
                    addImage(pdf, data, imageType, "DeviceRGB", 16);
                }
                else {
                    addImage(pdf, data, imageType, "DeviceRGB", 8);
                }
            }
        }
        else if (imageType == ImageType.BMP) {
            BMPImage bmp = new BMPImage(inputStream);
            data = bmp.getData();
            w = bmp.getWidth();
            h = bmp.getHeight();
            addImage(pdf, data, imageType, "DeviceRGB", 8);
        }

        inputStream.close();
    }


    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public void scaleBy(double factor) {
        this.w *= factor;
        this.h *= factor;
    }


    public void placeIn(Box box) throws Exception {
        box_x = box.x;
        box_y = box.y;
    }


    public void drawOn(Page page) throws Exception {
        x += box_x;
        y += box_y;
        page.append("q\n");
        page.append(w);
        page.append(" 0 0 ");
        page.append(h);
        page.append(' ');
        page.append(x);
        page.append(' ');
        page.append((page.height - y) - h);
        page.append(" cm\n");
        page.append("/Im");
        page.append(objNumber);
        page.append(" Do\n");
        page.append("Q\n");
    }


    private void addImage(
            PDF pdf,
            byte[] data,
            int imageType,
            String colorSpace,
            int bitsPerComponent) throws Exception {
        // Add the image
        pdf.newobj();
        pdf.append("<<\n");
        pdf.append("/Type /XObject\n");
        pdf.append("/Subtype /Image\n");
        if (imageType == ImageType.JPEG) {
            pdf.append("/Filter /DCTDecode\n");
        }
        else if (imageType == ImageType.PNG || imageType == ImageType.BMP) {
            pdf.append("/Filter /FlateDecode\n");
        }
        pdf.append("/Width ");
        pdf.append(( int ) w);
        pdf.append('\n');
        pdf.append("/Height ");
        pdf.append(( int ) h);
        pdf.append('\n');
        pdf.append("/ColorSpace /");
        pdf.append(colorSpace);
        pdf.append('\n');
        pdf.append("/BitsPerComponent ");
        pdf.append(bitsPerComponent);
        pdf.append('\n');
        pdf.append("/Length ");
        pdf.append(data.length);
        pdf.append('\n');
        pdf.append(">>\n");
        pdf.append("stream\n");
        pdf.append(data, 0, data.length);
        pdf.append("\nendstream\n");
        pdf.endobj();
        pdf.images.add(this);
        objNumber = pdf.objNumber;
    }


    public Image(PDF pdf, PDFobj obj) throws Exception {
        pdf.newobj();
        pdf.append("<<\n");
        pdf.append("/Type /XObject\n");
        pdf.append("/Subtype /Image\n");
        pdf.append("/Filter ");
        pdf.append(obj.getValue(PDFobj.FILTER));
        pdf.append('\n');
        pdf.append("/Width ");
        pdf.append(obj.getValue(PDFobj.WIDTH));
        pdf.append('\n');
        pdf.append("/Height ");
        pdf.append(obj.getValue(PDFobj.HEIGHT));
        pdf.append('\n');
        pdf.append("/ColorSpace ");
        pdf.append(obj.getValue(PDFobj.COLORSPACE));
        pdf.append('\n');
        pdf.append("/BitsPerComponent ");
        pdf.append(obj.getValue(PDFobj.BITSPERCOMPONENT));
        pdf.append('\n');
        pdf.append("/Length ");
        pdf.append(obj.stream.length);
        pdf.append('\n');
        pdf.append(">>\n");
        pdf.append("stream\n");
        pdf.append(obj.stream, 0, obj.stream.length);
        pdf.append("\nendstream\n");
        pdf.endobj();
        pdf.images.add(this);
        objNumber = pdf.objNumber;
    }


    public double getWidth() {
        return this.w;
    }


    public double getHeight() {
        return this.h;
    }

}   // End of Image.java

//<<<<}
