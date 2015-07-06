package jpsxdec.psxvideo.mdec.idct;

/*
 * Simple IDCT
 *
 * Copyright (c) 2001 Michael Niedermayer <michaelni@gmx.at>
 * quick lame Java port by Alexander Strange
 * small enhancements by Michael Sabin
 *
 * This file origionally came from FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

/**
 based upon some outcommented c code from mpeg2dec (idct_mmx.c
 written by <a href="mailto:aholtzma@ess.engr.uvic.ca">Aaron Holtzman</a> )
 */

public class simple_idct {

	final static int W1 = 22725, W2 = 21407, W3 = 19266, W4 = 16383,
					 W5 = 12873, W6 =  8867, W7 = 4520;
	
	final static int ROW_SHIFT = 11, COL_SHIFT = 20;
		
	// input is actually short and output is actually byte
	// but Java makes that really hard
	protected void idct1D(int coeff[], int off, int stride, int shift, int outoff, int[] outbuff) {
		int i0 = coeff[off+0*stride], i1 = coeff[off+1*stride], i2 = coeff[off+2*stride],
			i3 = coeff[off+3*stride], i4 = coeff[off+4*stride], i5 = coeff[off+5*stride],
			i6 = coeff[off+6*stride], i7 = coeff[off+7*stride];
		
		boolean lastzero = (i4|i5|i6|i7) == 0;
		int     dc = (i0 * W4) + (1 << (shift - 1));
		
		// i'm not sure this bit is identical to simpleidct
		/*
		if ((i1|i2|i3) == 0 && lastzero) {			
			coeff[off+0*stride] = coeff[off+1*stride] = coeff[off+2*stride] = 
			coeff[off+3*stride] = coeff[off+4*stride] = coeff[off+5*stride] =
			coeff[off+6*stride] = coeff[off+7*stride] = dc >> shift;
			
			return;
		}*/
		
		int     e0, e1, e2, e3;
		int     o0, o1, o2, o3;
		
		int     m11 = i1*W1, m13 = i1*W3, m15 = i1*W5, m17 = i1*W7;
		int     m22 = i2*W2, m26 = i2*W6;
		int     m31 = i3*W1, m33 = i3*W3, m35 = i3*W5, m37 = i3*W7;
		
		e0 = dc + m22;
		e1 = dc + m26;
		e2 = dc - m26;
		e3 = dc - m22;
		
		o0 = m11 + m33;
		o1 = m13 - m37;
		o2 = m15 - m31;
		o3 = m17 - m35;
		
		if (!lastzero) {
			int     m44 = i4*W4;
			int     m51 = i5*W1, m53 = i5*W3, m55 = i5*W5, m57 = i5*W7;
			int     m62 = i6*W2, m66 = i6*W6;
			int     m71 = i7*W1, m73 = i7*W3, m75 = i7*W5, m77 = i7*W7;
			
			e0 += m44 + m66;
			e1+= -m44 - m62;
			e2+= -m44 + m62;
			e3 += m44 - m66;
			
			o0 += m55 + m77;
			o1+= -m51 - m75;
			o2 += m57 + m73;
			o3 += m53 - m71;
		}
		
		outbuff[outoff+off+0*stride] = (e0+o0) >> shift;
		outbuff[outoff+off+7*stride] = (e0-o0) >> shift;
		outbuff[outoff+off+1*stride] = (e1+o1) >> shift;
		outbuff[outoff+off+6*stride] = (e1-o1) >> shift;
		outbuff[outoff+off+2*stride] = (e2+o2) >> shift;
		outbuff[outoff+off+5*stride] = (e2-o2) >> shift;
		outbuff[outoff+off+3*stride] = (e3+o3) >> shift;
		outbuff[outoff+off+4*stride] = (e3-o3) >> shift;
	}
	
	public void invers_dct(int[] coeff, int outoff, int[] outbuff) {
		int i;
		
		for (i = 0; i < 8; i++) idct1D(coeff, i*8, 1, ROW_SHIFT,      0, coeff);
		for (i = 0; i < 8; i++) idct1D(coeff, i, 8, COL_SHIFT, outoff, outbuff);
	}
	
	public void invers_dct_special(int[] coeff, int nonzero_pos, int outoff, int[] outbuff) {
		int i;
		
		for (i = 0; i <= (nonzero_pos >>> 3); i++) idct1D(coeff, i*8, 1, ROW_SHIFT, 0, coeff);
		for (i = 0; i < 8; i++) idct1D(coeff, i, 8, COL_SHIFT, outoff, outbuff);
	}
	
	
	/*public static void main(String args[]) {
		int dct[] = new int[64];
		simple_idct id = new simple_idct();
		
		dct[0] = 20;
		dct[63]= 1;
		
		id.invers_dct(dct);
		int i;
		
		for (i = 0; i < 64; i++) {
			if (i != 0 && i % 8 == 0) System.out.println("");
			System.out.print(""+dct[i]+" ");
		}
	}*/
}