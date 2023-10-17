package com.pdfjet;

import java.lang.*;
import java.util.*;


//>>>>pdfjet {
public class PDFobj {

    protected static final String TYPE = "/Type";
    protected static final String SUBTYPE = "/Subtype";
    protected static final String FILTER = "/Filter";
    protected static final String WIDTH = "/Width";
    protected static final String HEIGHT = "/Height";
    protected static final String COLORSPACE = "/ColorSpace";
    protected static final String BITSPERCOMPONENT = "/BitsPerComponent";

    protected int offset;
    protected int number;
    public List< String > dict;

    public byte[] stream;
    protected int stream_offset;

    public byte[] data;


    public PDFobj( int offset ) {
        this.offset = offset;
        this.dict = new ArrayList< String >();
    }


    public String getValue( String key ) {

        for ( int i = 0; i < dict.size(); i++ ) {
            String token = dict.get(i);
            if ( token.equals( "stream" ) ||
                    token.equals( "endobj" ) ) {
                break;
            }
            
            if ( token.equals( key ) ) {
                return dict.get(i + 1);
            }
        }

        return "";
    }


    public int getLength( List< PDFobj> objects ) {

        for ( int i = 0; i < dict.size(); i++ ) {
            String token = dict.get(i);
            if ( token.equals( "/Length" ) ) {
                int number = Integer.valueOf( dict.get(i + 1) );
                if ( dict.get(i + 2).equals( "0" ) &&
                        dict.get(i + 3).equals( "R" ) ) {
                    return getLength( objects, number );
                }
                else {
                    return number;
                }
            }
        }

        return 0;
    }


    public int getLength( List< PDFobj> objects, int number ) {

        for ( int i = 0; i < objects.size(); i++ ) {
            PDFobj obj = objects.get(i);
            if ( obj.number == number ) {
                return Integer.valueOf( obj.dict.get(3) );
            }
        }

        return 0;
    }


    public void setStream( byte[] pdf, int length ) {
        stream = new byte[ length ];
        for ( int i = 0; i < length; i++ ) {
            stream[ i ] = pdf[ this.stream_offset + i ];
        }
    }

}
//<<<<}
