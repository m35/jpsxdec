/*



    PSX VAG-Packer, hacked by bITmASTER@bigfoot.com

    v0.1                              

    2016 - Fixed tag name underflow or being too long

         - Converted to Java

*/

package jpsxdec.adpcm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;


public class vagpack {



private static final int BUFFER_SIZE = 128*28;


private static final short[] wave = new short[BUFFER_SIZE];



public static int main( int argc, String[] argv ) throws IOException

{

    AudioInputStream fp = null;
    FileOutputStream vag = null;

    char[] fname = new char[128];

    int p;

    int ptr;

    double[] d_samples = new double[28];

    short[] four_bit = new short[28];

    find_predict find_predict = new find_predict();
    pack pack = new pack();

    int flags;

    int size;

    int i, j, k;    

    int d;

    int e;

    int sample_freq, sample_len;

    

    if ( argc != 2 ) {

        printf( "usage: vag-pack *.wav\n" );

        return( -1 );

    }

            
    try {
    fp = AudioSystem.getAudioInputStream(new File(argv[1]));

    } catch (IOException ex) {

        printf( "cant open %s\n", argv[1] );
        ex.printStackTrace(System.out);

        return( -2 );

    }

    catch (UnsupportedAudioFileException ex) {

        printf( "not a compatible audio file\n" );
        ex.printStackTrace(System.out);

        return( -3 );

    }




    e = fp.getFormat().getChannels();

    if ( e != 1 ) {

        printf( "must be MONO\n" );

        return( -5 );

    }



    sample_freq = Math.round(fp.getFormat().getSampleRate());


    e = fp.getFormat().getSampleSizeInBits();

    if ( e != 16 ) {

        printf( "only 16 bit samples\n" );

        return( -6 );

    }       

        

    sample_len = (int) fp.getFrameLength();

    System.arraycopy(argv[1].toCharArray(), 0, fname, 0, argv[1].length());

    p = argv[1].lastIndexOf('.');

    p++;

    System.arraycopy( "VAG".toCharArray(), 0, fname, p, "VAG".length() );

    try {
    vag = new FileOutputStream( new String(fname, 0, p+"VAG".length()) );

    } catch (FileNotFoundException ex) {

        printf( "cant write output file\n" );
        ex.printStackTrace(System.out);

        return( -8 );

    }



    vag.write( Misc.stringToAscii("VAGp") );  // ID

    fputi( 0x20, vag );                 // Version

    fputi( 0x00, vag );                 // Reserved

    size = sample_len / 28;

    if( sample_len % 28 != 0 )

        size++;

    fputi( 16 * ( size + 2 ), vag );    // Data size

    fputi( sample_freq, vag );          // Sampling frequency

    

    for ( i = 0; i < 12; i++ )          // Reserved

        fputc( 0, vag );



    p -= 2;

    i = 0;

    while ( i < 16 && p >= 0 && Character.isLetterOrDigit( fname[p] ) ) {

        i++;

        p--;

    }

    p++;

    for ( j = 0; j < i; j++ )           // Name

        fputc( fname[p+j], vag );

    for( j = 0; j < 16-i; j++ )

        fputc( 0, vag );

        

    for( i = 0; i < 16; i++ )

        fputc( 0, vag );                // ???



    flags = 0;  

    while( sample_len > 0 ) {

        size = ( sample_len >= BUFFER_SIZE ) ? BUFFER_SIZE : sample_len; 

        fread( wave, 2, size, fp );

        i = size / 28;

        if ( size % 28 != 0 ) {

            for ( j = size % 28; j < 28; j++ )

                wave[28*i+j] = 0;

            i++;

        }

        

        for ( j = 0; j < i; j++ ) {                                     // pack 28 samples

            ptr = j * 28;

            find_predict.find_predict( wave, ptr, d_samples );

            pack.pack( d_samples, four_bit, find_predict.predict_nr, find_predict.shift_factor );

            d = ( find_predict.predict_nr << 4 ) | find_predict.shift_factor;

            fputc( d, vag );

            fputc( flags, vag );

            for ( k = 0; k < 28; k += 2 ) {

                d = ( ( four_bit[k+1] >> 8 ) & 0xf0 ) | ( ( four_bit[k] >> 12 ) & 0xf );

                fputc( d, vag );

            }

            sample_len -= 28;

            if ( sample_len < 28 )

                flags = 1;

        }

    }

    

    fputc( ( find_predict.predict_nr << 4 ) | find_predict.shift_factor, vag );

    fputc( 7, vag );            // end flag

    for ( i = 0; i < 14; i++ )

        fputc( 0, vag );



    

    fp.close();

    vag.close();

//    getch(); 

    return( 0 );

}





static double[/*5*/][/*2*/] f = { { 0.0, 0.0 },

                            {  -60.0 / 64.0, 0.0 },

                            { -115.0 / 64.0, 52.0 / 64.0 },

                            {  -98.0 / 64.0, 55.0 / 64.0 },

                            { -122.0 / 64.0, 60.0 / 64.0 } };

                  
static class find_predict {

    double _s_1 = 0.0;                            // s[t-1]
    double _s_2 = 0.0;                            // s[t-2]

int predict_nr;
int shift_factor ;
void find_predict( short[] samples, int samples_ptr, double[] d_samples )

{

    int i, j;

    double[][] buffer = new double[28][5];

    double min = 1e10;

    double[] max = new double[5];

    double ds;

    int min2;

    int shift_mask;


    double s_0, s_1 = 0, s_2 = 0;



    for ( i = 0; i < 5; i++ ) {

        max[i] = 0.0;

        s_1 = _s_1;

        s_2 = _s_2;

        for ( j = 0; j < 28; j ++ ) {

            s_0 = (double) samples[samples_ptr+j];              // s[t-0]

            if ( s_0 > 30719.0 )

                s_0 = 30719.0;

            if ( s_0 < - 30720.0 )

                s_0 = -30720.0;

            ds = s_0 + s_1 * f[i][0] + s_2 * f[i][1];

            buffer[j][i] = ds;

            if ( Math.abs( ds ) > max[i] )

                max[i] = Math.abs( ds );

//                printf( "%+5.2f\n", s2 );

                s_2 = s_1;                                  // new s[t-2]

                s_1 = s_0;                                  // new s[t-1]

        }

        

        if ( max[i] < min ) {

            min = max[i];

            predict_nr = i;

        }

        if ( min <= 7 ) {

            predict_nr = 0;

            break;

        }

        

    }



// store s[t-2] and s[t-1] in a static variable

// these than used in the next function call



    _s_1 = s_1;

    _s_2 = s_2;

    

    for ( i = 0; i < 28; i++ )

        d_samples[i] = buffer[i][predict_nr];



//  if ( min > 32767.0 )

//      min = 32767.0;

        

    min2 = ( int ) min;

    shift_mask = 0x4000;

    shift_factor = 0;

    

    while( shift_factor < 12 ) {

        if ( ( shift_mask  & ( min2 + ( shift_mask >> 3 ) ) ) != 0 )

            break;

        shift_factor++;

        shift_mask = shift_mask >> 1;

    }

      

}

}

static class pack {

    double s_1 = 0.0;

    double s_2 = 0.0;

void pack( double[] d_samples, short[] four_bit, int predict_nr, int shift_factor )

{

    double ds;

    int di;

    double s_0;

    int i;



    for ( i = 0; i < 28; i++ ) {

        s_0 = d_samples[i] + s_1 * f[predict_nr][0] + s_2 * f[predict_nr][1];

        ds = s_0 * (double) ( 1 << shift_factor );



        di = ( (int) ds + 0x800 ) & 0xfffff000;



        if ( di > 32767 )

            di = 32767;

        if ( di < -32768 )

            di = -32768;

            

        four_bit[i] = (short) di;



        di = di >> shift_factor;

        s_2 = s_1;

        s_1 = (double) di - s_0;



    }

}

}

static void fputi( int d, FileOutputStream fp ) throws IOException

{

    fp.write( d >> 24 );

    fp.write( d >> 16 );

    fp.write( d >> 8  );

    fp.write( d       );

}

public static void main( String[] args ) throws IOException {
    String[] cargs = new String[args.length+1];
    System.arraycopy(args, 0, cargs, 1, args.length);
    System.exit(main(cargs.length, cargs));
}

private static void printf(String format, Object ... args) { System.out.printf(format, args); }
private static void fputc(int c, FileOutputStream fos) throws IOException { fos.write(c); }
private static void fread( short[] wave, int sizeof_elem, int size, AudioInputStream fp ) throws IOException {
    byte[] buff = IO.readByteArray(fp, size * sizeof_elem);
    for (int i = 0; i < size; i++) {
        wave[i] = IO.readSInt16LE(buff, i*2);
    }
}
}
