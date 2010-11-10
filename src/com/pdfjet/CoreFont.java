/**
 *  CoreFont.java
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


//>>>>pdfjet {
public abstract class CoreFont {

    public static String COURIER = "Courier";
    public static String COURIER_BOLD = "Courier-Bold";
    public static String COURIER_OBLIQUE = "Courier-Oblique";
    public static String COURIER_BOLD_OBLIQUE = "Courier-BoldOblique";
    public static String HELVETICA = "Helvetica";
    public static String HELVETICA_BOLD = "Helvetica-Bold";
    public static String HELVETICA_OBLIQUE = "Helvetica-Oblique";
    public static String HELVETICA_BOLD_OBLIQUE = "Helvetica-BoldOblique";
    public static String TIMES_ROMAN = "Times-Roman";
    public static String TIMES_BOLD = "Times-Bold";
    public static String TIMES_ITALIC = "Times-Italic";
    public static String TIMES_BOLD_ITALIC = "Times-BoldItalic";
    public static String SYMBOL = "Symbol";
    public static String ZAPF_DINGBATS = "ZapfDingbats";

    protected abstract int getBBoxLLx();
    protected abstract int getBBoxLLy();
    protected abstract int getBBoxURx();
    protected abstract int getBBoxURy();
    
    protected abstract int getUnderlineThickness();
    protected abstract int getUnderlinePosition();    

    protected abstract int[][] getMetrics();

}   // End of CoreFont.java
//<<<<}
