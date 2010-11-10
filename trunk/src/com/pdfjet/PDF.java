/**
 *  PDF.java
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
import java.text.*;
import java.util.*;
import java.util.zip.*;


//>>>>pdfjet {
public class PDF {

    protected static boolean original_zlib = false;

    private int compliance = 0;
    
	private OutputStream buf = null;
    protected int objNumber = 0;
    protected int metadataObjNumber = 0;
    protected int outputIntentObjNumber = 0;

    protected List<Font> fonts = new ArrayList<Font>();
    protected List<Image> images = new ArrayList<Image>();
    protected List<Page> pages = new ArrayList<Page>();

    private List<Integer> objOffset = new ArrayList<Integer>();

    private String producer = "PDFjet v2.78 (http://pdfjet.com)";
    private String creationDate;
    private String createDate;
    private String title = "";
    private String subject = "";
    private String author = "";

    private int byte_count = 0;

    private boolean CR_LF = false;
    private List< PDFobj > objects = null;
    private List< PDFobj > objects2 = null;
    private List< PDFobj > objects3 = null;
    private List< PDFobj > objects4 = null;


    public PDF(OutputStream buf) throws Exception { this(buf, 0); }


    // Here is the layout of the PDF document:
    //
    // Metadata Object
    // Output Intent Object
    // Fonts
    // Images
    // Resources Object
    // Pages
    // Page1
    // Page2
    // ...
    // PageN
    // Info
    // Root
    // xref table
    // Trailer
    public PDF(OutputStream buf, int compliance) throws Exception {

        this.buf = buf;
        this.compliance = compliance;

        Date date = new Date();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        creationDate = sdf1.format(date);
        createDate = sdf2.format(date);

        append("%PDF-1.4\n");
        append('%');
        append((byte) 0x00F2);
        append((byte) 0x00F3);
        append((byte) 0x00F4);
        append((byte) 0x00F5);
        append((byte) 0x00F6);
        append('\n');

        if (compliance == Compliance.PDF_A_1B) {
            metadataObjNumber = addMetadataObject();
            outputIntentObjNumber = addOutputIntentObject();
        }

    }


    protected void newobj() throws IOException {
        objOffset.add(byte_count);
        append(++objNumber);
        append(" 0 obj\n");
    }


    protected void endobj() throws IOException {
        append("endobj\n");
    }


    private int addMetadataObject() throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xpacket begin='\uFEFF' id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n");
        sb.append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n");
        sb.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");

        sb.append("<rdf:Description rdf:about=\"\" xmlns:pdf=\"http://ns.adobe.com/pdf/1.3/\" pdf:Producer=\"");
        sb.append(producer);
        sb.append("\"></rdf:Description>\n");

        sb.append("<rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
        sb.append("<dc:format>application/pdf</dc:format>\n");
        sb.append("<dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">");
        sb.append(title);
        sb.append("</rdf:li></rdf:Alt></dc:title>\n");

        sb.append("<dc:creator><rdf:Seq><rdf:li>");
        sb.append(producer);
        sb.append("</rdf:li></rdf:Seq></dc:creator>\n");

        sb.append("<dc:description><rdf:Alt><rdf:li xml:lang=\"en-US\">");
        sb.append(DroidFontsCopyright.NOTICE);
        sb.append("</rdf:li></rdf:Alt></dc:description>\n");
/*
        sb.append("<dc:rights><rdf:Alt><rdf:li>");
        sb.append(DroidFontsCopyright.NOTICE);
        sb.append("</rdf:li></rdf:Alt></dc:rights>\n");
*/
        sb.append("</rdf:Description>\n");

        sb.append("<rdf:Description rdf:about=\"\" xmlns:pdfaid=\"http://www.aiim.org/pdfa/ns/id/\">");
        sb.append("<pdfaid:part>1</pdfaid:part>");
        sb.append("<pdfaid:conformance>B</pdfaid:conformance>");
        sb.append("</rdf:Description>");

        sb.append("<rdf:Description rdf:about=\"\" xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\">\n");
        sb.append("<xmp:CreateDate>");
        sb.append(createDate);
        sb.append("</xmp:CreateDate>\n");
        sb.append("</rdf:Description>\n");
        sb.append("</rdf:RDF>\n");
        sb.append("</x:xmpmeta>\n");

        // Add the recommended 2000 bytes padding
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 10; j++) {
                sb.append("          ");
            }
            sb.append("\n");
        }

        sb.append("<?xpacket end=\"w\"?>");

        byte[] xml = sb.toString().getBytes("UTF-8");

        // This is the metadata object
        newobj();
        append("<<\n");
        append("/Type /Metadata\n");
        append("/Subtype /XML\n");
        append("/Length ");
        append(xml.length);
        append("\n");
        append(">>\n");
        append("stream\n");
        for (int i = 0; i < xml.length; i++) {
            append(xml[i]);
        }
        append("\nendstream\n");
        endobj();

        return objNumber;
    }


    protected int addOutputIntentObject() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(
                getClass().getClassLoader().getResourceAsStream("icc-profiles/sRGB_IEC61966-2-1_black_scaled.icc"));
        int ch;
        while ((ch = bis.read()) != -1) {
            baos.write(ch);
        }
        bis.close();

        byte[] sRGB = baos.toByteArray();

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        DeflaterOutputStream dos =
                new DeflaterOutputStream(baos2, new Deflater());
        dos.write(sRGB, 0, sRGB.length);
        dos.finish();

        newobj();
        append("<<\n");
        append("/N 3\n");

        append("/Length ");
        append(baos2.size());
        append("\n");

        append("/Filter /FlateDecode\n");
        append(">>\n");
        append("stream\n");
        append(baos2);
        append("\nendstream\n");
        endobj();

        // OutputIntent object
        newobj();
        append("<<\n");
        append("/Type /OutputIntent\n");
        append("/S /GTS_PDFA1\n");
        append("/OutputCondition (sRGB IEC61966-2.1)\n");
        append("/OutputConditionIdentifier (sRGB IEC61966-2.1)\n");
        append("/Info (sRGB IEC61966-2.1)\n");
        append("/DestOutputProfile ");
        append(objNumber - 1);
        append(" 0 R\n");
        append(">>\n");
        endobj();

        return objNumber;
    }


    private int addResourcesObject() throws Exception {

        newobj();
        append("<<\n");
        append("/Font\n");
        append("<<\n");
        for (int i = 0; i < fonts.size(); i++) {
            Font font = fonts.get(i);
            append("/F");
            append(font.objNumber);
            append(" ");
            append(font.objNumber);
            append(" 0 R\n");
        }
        append(">>\n");
        append("/XObject\n");
        append("<<\n");
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            append("/Im");
            append(image.objNumber);
            append(" ");
            append(image.objNumber);
            append(" 0 R\n");
        }
        append(">>\n");
        append(">>\n");
        endobj();
        return objNumber;
    }


    protected int addPagesObject() throws Exception {
        newobj();
        append("<<\n");
        append("/Type /Pages\n");
        append("/Kids [ ");
        int pageObjNumber = objNumber + 1;
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            append(pageObjNumber);
            append(" 0 R ");
            pageObjNumber += 2;
            pageObjNumber += page.annots.size();
        }
        append("]\n");
        append("/Count ");
        append(pages.size());
        append('\n');
        append(">>\n");
        endobj();
        return objNumber;
    }


    protected int addInfoObject() throws Exception {
        // This is the info object
        newobj();
        append("<<\n");
        append("/Title (");
        append(title);
        append(")\n");
        append("/Subject (");
        append(subject);
        append(")\n");
        append("/Author (");
        append(author);
        append(")\n");
        append("/Producer (");
        append(producer);
        append(")\n");

        if (compliance != Compliance.PDF_A_1B) {
            append("/CreationDate (D:");
            append(creationDate);
            append(")\n");
        }

        append(">>\n");
        endobj();
        return objNumber;
    }


    protected void addAllPages(int pagesObjNumber, int resObjNumber) throws Exception {
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);

            // Page object
            newobj();
            append("<<\n");
            append("/Type /Page\n");
            append("/Parent ");
            append(pagesObjNumber);
            append(" 0 R\n");
            append("/MediaBox [0.0 0.0 ");
            append(page.width);
            append(" ");
            append(page.height);
            append("]\n");
            append("/Resources ");
            append(resObjNumber);
            append(" 0 R\n");
            append("/Contents ");
            append(objNumber + 1);
            append(" 0 R\n");
            if (page.annots.size() > 0) {
                append("/Annots [ ");
                for (int j = 0; j < page.annots.size(); j++) {
                    append(objNumber + 2 + j);
                    append(" 0 R ");
                }
                append("]\n");
            }
            append(">>\n");
            endobj();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DeflaterOutputStream dos =
                    new DeflaterOutputStream(baos, new Deflater());
            dos.write(page.buf.toByteArray(), 0, page.buf.toByteArray().length);
            dos.finish();

            // Page contents
            newobj();
            append("<<\n");
            append("/Filter /FlateDecode\n");
            append("/Length ");
            append(baos.size());
            append("\n");
            append(">>\n");
            append("stream\n");
            append(baos);
            append("\nendstream\n");
            endobj();

            addAnnotDictionaries(page);
        }
    }


    protected void addAnnotDictionaries(Page page) throws Exception {
        for (int i = 0; i < page.annots.size(); i++) {
            Annotation annot = page.annots.get(i);
            newobj();
            append("<<\n");
            append("/Type /Annot\n");
            append("/Subtype /Link\n");
            append("/Rect [");
            append(annot.x1);
            append(' ');
            append(annot.y1);
            append(' ');
            append(annot.x2);
            append(' ');
            append(annot.y2);
            append("]\n");
            append("/Border[0 0 0]\n");
            append("/F 4\n");
            append("/A <<\n");
            append("/S /URI\n");
            append("/URI (");
            append(annot.uri);
            append(")\n");
            append(">>\n");
            append(">>\n");
            endobj();
        }
    }


    public void flush() throws Exception {

        int resObjNumber = addResourcesObject();
        int infoObjNumber = addInfoObject();
        int pagesObjNumber = addPagesObject();
        addAllPages(pagesObjNumber, resObjNumber);

        // This is the root object
        newobj();
        append("<<\n");
        append("/Type /Catalog\n");

        append("/Pages ");
        append(pagesObjNumber);
        append(" 0 R\n");

        if (compliance == Compliance.PDF_A_1B) {
            append("/Metadata ");
            append(metadataObjNumber);
            append(" 0 R\n");

            append("/OutputIntents [");
            append(outputIntentObjNumber);
            append(" 0 R]\n");
        }

        append(">>\n");
        endobj();

        int startxref = byte_count;
        // Create the xref table
        append("xref\n");
        append("0 ");
        append(objNumber + 1);
        append('\n');
        append("0000000000 65535 f \n");
        for (int i = 0; i < objOffset.size(); i++) {
            int offset = objOffset.get(i);
            String str = String.valueOf(offset);
            for (int j = 0; j < 10 - str.length(); j++) {
                append('0');
            }
            append(str);
            append(" 00000 n \n");
        }
        append("trailer\n");
        append("<<\n");
        append("/Size ");
        append(objNumber + 1);
        append('\n');

        String id = (new Salsa20()).getID();
        append("/ID[<");
        append(id);
        append("><");
        append(id);
        append(">]\n");

        append("/Root ");
        append(objNumber);
        append(" 0 R\n");
        append("/Info ");
        append(infoObjNumber);
        append(" 0 R\n");
        append(">>\n");
        append("startxref\n");
        append(startxref);
        append('\n');
        append("%%EOF\n");
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public void setSubject(String subject) {
        this.subject = subject;
    }


    public void setAuthor(String author) {
        this.author = author;
    }


    protected void append(int num) throws IOException {
        append(String.valueOf(num));
    }


    protected void append(double val) throws IOException {
        append(String.valueOf(val).replace(',', '.'));
    }


    protected void append(String str) throws IOException {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            buf.write((byte) str.charAt(i));
        }
        byte_count += len;
    }


    protected void append(char ch) throws IOException {
        append((byte) ch);
    }


    protected void append(byte b) throws IOException {
        buf.write(b);
        byte_count += 1;
    }


    protected void append(byte[] buf2, int off, int len) throws IOException {
        buf.write(buf2, off, len);
        byte_count += len;
    }


    protected void append(ByteArrayOutputStream baos) throws IOException {
        baos.writeTo(buf);
        byte_count += (int) baos.size();
    }


    public void setCompressor(boolean original_zlib) {
        PDF.original_zlib = original_zlib;
    }


    private int indexOfStartXRef(byte[] buf) {
        for (int i = ( buf.length - 10 ); i >= 0; i--) {
            if ( buf[i] == 's' &&
                    buf[i + 1] == 't' &&
                    buf[i + 2] == 'a' &&
                    buf[i + 3] == 'r' &&
                    buf[i + 4] == 't' &&
                    buf[i + 5] == 'x' &&
                    buf[i + 6] == 'r' &&
                    buf[i + 7] == 'e' &&
                    buf[i + 8] == 'f' ) {
                return i;
            }
        }
        return -1;
    }


    public List< PDFobj > read(InputStream inputStream) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int ch;
        while ( ( ch = inputStream.read() ) != -1 ) {
            baos.write(ch);
        }

        byte[] pdf = baos.toByteArray();

        int startxref = indexOfStartXRef(pdf);

        if ( pdf[ startxref + 9 ] == 0x0D &&
                pdf[ startxref + 10 ] == 0x0A ) {
            // System.out.println( "This PDF file is using CR/LF for EOL." );
            CR_LF = true;
        }

        objects = new ArrayList< PDFobj >();

        PDFobj obj = getObject( pdf, startxref, pdf.length );

        String xref = obj.dict.get(1);

        obj = getObject( pdf, Integer.valueOf( xref ), pdf.length );

        if ( obj.dict.get(0).equals( "xref" ) ) {
            for ( int i = 3; i < obj.dict.size(); i += 3 ) {
                String token = obj.dict.get(i);
                if ( !token.equals( "trailer" ) ) {
                    int off = Integer.valueOf( token );
                    if ( off != 0 ) {
                        objects.add( getObject( pdf, off, pdf.length ) );
                    }
                }
            }
        }
        else {
            getObjOffsets( pdf, xref );
        }

        for ( int i = 0; i < objects.size(); i++ ) {
            obj = objects.get(i);
            int offset = obj.dict.size() - 1;
            if ( obj.dict.get(offset).equals( "stream" ) ) {
                obj.setStream( pdf, obj.getLength( objects ) );
            }
        }

        objects2 = new ArrayList< PDFobj >();

        for ( int i = 0; i < objects.size(); i++ ) {
            obj = objects.get(i);
            if ( obj.getValue( "/Type" ).equals( "/XRef" ) ||
                    !obj.getValue( "/S" ).equals( "" ) ) {  // Hint Table
                continue;
            }

            if ( obj.getValue( "/Type" ).equals( "/XObject" ) ||
                    obj.getValue( "/Subtype" ).equals( "/Type1C" ) ||
                    obj.getValue( "/Subtype" ).equals( "/CIDFontType0C" ) ) {
                objects2.add( obj );
                continue;
            }

            if ( obj.getValue( "/Filter" ).equals( "/FlateDecode" ) ) {
                Decompressor decompressor = new Decompressor(obj.stream);
                obj.data = decompressor.getDecompressedData();
                objects2.add( obj );
            }
        }


        objects3 = new ArrayList< PDFobj >();

        for ( int i = 0; i < objects2.size(); i++ ) {
            obj = objects2.get(i);
            if ( obj.getValue( "/Type" ).equals( "/ObjStm" ) ) {
                int first = Integer.valueOf( obj.getValue( "/First" ) );
                int n = Integer.valueOf( obj.getValue( "/N" ) );
                PDFobj o2 = getObject( obj.data, 0, first );
                for ( int j = 0; j < o2.dict.size(); j += 2 ) {
                    int num = Integer.valueOf( o2.dict.get(j) );
                    int off = Integer.valueOf( o2.dict.get(j + 1) );
                    int end = obj.data.length;
                    if ( j <= o2.dict.size() - 4 ) {
                        end = first + Integer.valueOf( o2.dict.get(j + 3) );
                    }

                    PDFobj o3 = getObject( obj.data, first + off, end );
                    o3.dict.add(0, "obj");
                    o3.dict.add(0, "0");
                    o3.dict.add(0, String.valueOf(num));
                    objects3.add( o3 );
                }
            }
            else {
                objects3.add( obj );
            }
        }


        objects4 = new ArrayList< PDFobj >();

        for ( int i = 0; i < objects.size(); i++ ) {
            obj = objects.get(i);
            if ( !obj.getValue( "/Type" ).equals( "/ObjStm" ) &&
                    !obj.getValue( "/Type" ).equals( "/XRef" ) ) {
                obj.number = Integer.valueOf( obj.dict.get(0) );
                objects4.add(obj);
            }
        }

        for ( int i = 0; i < objects3.size(); i++ ) {
            obj = objects3.get(i);
            obj.number = Integer.valueOf( obj.dict.get(0) );
            objects4.add(obj);
        }

        return objects4;
    }


    private boolean append(PDFobj obj, StringBuilder sb, int offset) {
        String token = sb.toString().trim();
        if ( !token.equals( "" ) ) {
            obj.dict.add( token );
        }
        sb.setLength(0);
        if ( token.equals( "stream" ) ||
                token.equals( "endobj" ) ||
                token.equals( "trailer" ) ) {
            if ( token.equals( "stream" ) ) {
                if ( CR_LF ) {
                    obj.stream_offset = offset + 1;
                }
                else {
                    obj.stream_offset = offset;
                }
            }
            return false;
        }
        return true;
    }


    private PDFobj getObject( byte[] buf, int off, int end ) {

        PDFobj obj = new PDFobj( off );

        StringBuilder sb = new StringBuilder();

        int n = 0;
        char c1 = ' ';
        while ( true ) {
            if ( off == end ) {
                append( obj, sb, off );
                break;
            }

            char c2 = (char) buf[off++];
            if ( c2 == '(' ) {
                if ( n == 0 ) {
                    if ( !append( obj, sb, off ) ) break;
                }
                sb.append( c2 );
                ++n;
            }
            else if ( c2 == ')' ) {
                sb.append( c2 );
                --n;
                if ( n == 0 ) {
                    if ( !append( obj, sb, off ) ) break;
                }
            }
            else if ( n > 0 ) {
                sb.append( c2 );
            }
            else if (  c2 == 0x00       // Null
                    || c2 == 0x09       // Horizontal Tab
                    || c2 == 0x0A       // Line Feed (LF)
                    || c2 == 0x0C       // Form Feed
                    || c2 == 0x0D       // Carriage Return (CR)
                    || c2 == 0x20 ) {   // Space
                if ( !append( obj, sb, off ) ) break;
                c1 = ' ';
            }
            else if ( c2 == '/' ) {
                if ( !append( obj, sb, off ) ) break;
                sb.append( c2 );
                c1 = c2;
            }
            else if ( c2 == '<' || c2 == '>' ) {
                if ( c2 != c1 ) {
                    if ( !append( obj, sb, off ) ) break;
                    sb.append( c2 );
                    c1 = c2;
                }
                else {
                    sb.append( c2 );
                    if ( !append( obj, sb, off ) ) break;
                    c1 = ' ';
                }
            }
            else if ( c2 == '[' || c2 == ']' ) {
                if ( !append( obj, sb, off ) ) break;
                obj.dict.add( String.valueOf( c2 ) );
                c1 = c2;
            }
            else if ( c2 == '{' || c2 == '}' ) {
                if ( !append( obj, sb, off ) ) break;
                obj.dict.add( String.valueOf( c2 ) );
                c1 = c2;
            }
            else if ( c2 == '%' ) {
                if ( c2 != c1 ) {
                    if ( !append( obj, sb, off ) ) break;
                    sb.append( c2 );
                    c1 = c2;
                }
                else {
                    sb.append( c2 );
                    if ( !append( obj, sb, off ) ) break;
                    c1 = ' ';
                }
            }
            else {
                sb.append( c2 );
                c1 = c2;
            }

        }

        return obj;
    }


    /**
     * Converts an array of bytes to an integer.
     * @param buf byte[]
     * @return int
     */
    public int toInt( byte[] buf, int off, int len ) {
        int i = 0;
        for ( int j = 0; j < len; j++ ) {
            i |= buf[off + j] & 0xFF;
            if ( j < len - 1 ) {
                i <<= 8;
            }
        }
        return i;
    }


    private void getObjOffsets( byte[] pdf, String xref ) throws Exception {

        PDFobj obj = getObject( pdf, Integer.valueOf( xref ), pdf.length );
        obj.setStream( pdf, obj.getLength( null ) );

        xref = obj.getValue( "/Prev" );
        if ( !xref.equals( "" ) ) {
            getObjOffsets( pdf, xref );
        }

        Decompressor decompressor = new Decompressor(obj.stream);
        byte[] data = decompressor.getDecompressedData();

        int p1 = 0; // Predictor byte
        int f1 = 0; // Field 1
        int f2 = 0; // Field 2
        int f3 = 0; // Field 3
        for ( int i = 0; i < obj.dict.size(); i++ ) {
            String token = obj.dict.get(i);
            if ( token.equals( "/Predictor" ) ) {
                if ( obj.dict.get(i + 1).equals("12") ) {
                    p1 = 1;
                }
                else {
                    // TODO:
                }
            }

            if ( token.equals( "/W" ) ) {
                // "/W [ 1 3 1 ]"
                f1 = Integer.valueOf(obj.dict.get(i + 2));
                f2 = Integer.valueOf(obj.dict.get(i + 3));
                f3 = Integer.valueOf(obj.dict.get(i + 4));
            }

        }

        int n = p1 + f1 + f2 + f3;   // Number of bytes per entry

        byte[] entry = new byte[n];

        for ( int i = 0; i < data.length; i += n ) {

            // Apply the 'Up' filter.
            for ( int j = 0; j < n; j++ ) {
                entry[j] += data[i + j];
            }

            if ( entry[1] == 0x01 ) {
                int off = toInt( entry, p1 + f1, f2 );
                objects.add( getObject( pdf, off, pdf.length ) );
            }

        }

    }

}   // End of PDF.java
//<<<<}
