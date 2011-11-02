/*
 * Copyright 2009, Morten Nobel-Joergensen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mortennobel.imagescaling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Based on work from Java Image Util ( http://schmidt.devlib.org/jiu/ )
 *
 * Note that the filter method is not thread safe
 *
 * @author Morten Nobel-Joergensen
 * @author Heinz Doerr
 */
public class ResampleOp
{
	private List<ProgressListener> listeners = new ArrayList<ProgressListener>();

	protected void fireProgressChanged(float fraction){
        for (ProgressListener progressListener:listeners){
            progressListener.notifyProgress(fraction);
        }
    }

    public final void addProgressListener(ProgressListener progressListener) {
        listeners.add(progressListener);
    }

    public final boolean removeProgressListener(ProgressListener progressListener) {
        return listeners.remove(progressListener);
    }

    private final static int SCALE = 2;

	private int srcWidth;
	private int srcHeight;
	private int dstWidth;
	private int dstHeight;

	static class SubSamplingData{
		private final int[] arrN; // individual - per row or per column - nr of contributions
		private final int[] arrPixel;  // 2Dim: [wid or hei][contrib]
		private final double[] arrWeight; // 2Dim: [wid or hei][contrib]
		private final int numContributors; // the primary index length for the 2Dim arrays : arrPixel and arrWeight

		private SubSamplingData(int[] arrN, int[] arrPixel, double[] arrWeight, int numContributors) {
			this.arrN = arrN;
			this.arrPixel = arrPixel;
			this.arrWeight = arrWeight;
			this.numContributors = numContributors;
		}


		public int getNumContributors() {
			return numContributors;
		}

		public int[] getArrN() {
			return arrN;
		}

		public int[] getArrPixel() {
			return arrPixel;
		}

		public double[] getArrWeight() {
			return arrWeight;
		}
	}

	private SubSamplingData horizontalSubsamplingData;
	private SubSamplingData verticalSubsamplingData;

	private int processedItems;
	private float totalItems;

	private int numberOfThreads = Runtime.getRuntime().availableProcessors();

	private AtomicInteger multipleInvocationLock = new AtomicInteger();

	private ResampleFilter filter = ResampleFilters.getLanczos3Filter();


	public ResampleOp() {
	}

	public ResampleFilter getFilter() {
		return filter;
	}

	public void setFilter(ResampleFilter filter) {
		this.filter = filter;
	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public void doFilter(final double[] srcImg, int srcWidth, int srcHeight, final double[] dest) {
        
		assert multipleInvocationLock.incrementAndGet()==1:"Multiple concurrent invocations detected";

		this.dstWidth = srcWidth * SCALE;
		this.dstHeight = srcHeight * SCALE;

        if (dest.length < dstWidth * dstHeight)
            throw new IllegalArgumentException("Output buffer not big enough");

		this.srcWidth = srcWidth;
        this.srcHeight = srcHeight;

        final double[][] workPixels = new double[srcHeight][dstWidth];

        this.processedItems = 0;
		this.totalItems = srcHeight + dstWidth;

		// Pre-calculate  sub-sampling
		horizontalSubsamplingData = createSubSampling(filter, srcWidth, dstWidth);
		verticalSubsamplingData = createSubSampling(filter,srcHeight, dstHeight);


        Thread[] threads = new Thread[numberOfThreads-1];
        for (int i=1;i<numberOfThreads;i++){
            final int finalI = i;
            threads[i-1] = new Thread(new Runnable(){
                public void run(){
                    horizontallyFromSrcToWork(srcImg, workPixels,finalI,numberOfThreads);
                }
            });
            threads[i-1].start();
        }
        horizontallyFromSrcToWork(srcImg, workPixels,0,numberOfThreads);
        waitForAllThreads(threads);


        // --------------------------------------------------
		// Apply filter to sample vertically from Work to Dst
		// --------------------------------------------------
        for (int i=1;i<numberOfThreads;i++){
            final int finalI = i;
            threads[i-1] = new Thread(new Runnable(){
                public void run(){
					verticalFromWorkToDst(workPixels, dest, finalI,numberOfThreads);
                }
            });
            threads[i-1].start();
        }
        verticalFromWorkToDst(workPixels, dest, 0,numberOfThreads);
        waitForAllThreads(threads);

		assert multipleInvocationLock.decrementAndGet()==0:"Multiple concurrent invocations detected";
    }

    private void waitForAllThreads(Thread[] threads) {
        try {
            for (Thread t:threads){
                t.join(Long.MAX_VALUE);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static SubSamplingData createSubSampling(ResampleFilter filter, int srcSize, int dstSize) {
		int[] arrN= new int[dstSize];
		int numContributors;
		double[] arrWeight;
		int[] arrPixel;

		final double fwidth= filter.getSamplingRadius();

        // super-sampling
        // Scales from smaller to bigger height
        numContributors= (int)(fwidth * 2.0 + 1);
        arrWeight= new double[dstSize * numContributors];
        arrPixel= new int[dstSize * numContributors];
        //
        for (int i= 0; i < dstSize; i++) {
            final int subindex= i * numContributors;
            double center= (i + 0.5) / (double)SCALE;
            int left= (int)Math.floor(center - fwidth);
            int right= (int)Math.ceil(center + fwidth);
            for (int j= left; j <= right; j++) {
                double weight= filter.apply(center - j - 0.5);
                if (weight == 0.0) {
                    continue;
                }
                int n;
                if (j < 0) {
                    n= -j;
                } else if (j >= srcSize) {
                    n= srcSize - j + srcSize - 1;
                } else {
                    n= j;
                }
                int k= arrN[i];
                arrN[i]++;
                if (n < 0 || n >= srcSize) {
                    weight= 0.0;// Flag that cell should not be used
                }
                arrPixel[subindex +k]= n;
                arrWeight[subindex + k]= weight;
            }
            // normalize the filter's weight's so the sum equals to 1.0, very important for avoiding box type of artifacts
            final int max= arrN[i];
            double tot= 0;
            for (int k= 0; k < max; k++)
                tot+= arrWeight[subindex + k];
            assert tot!=0:"should never happen except bug in filter";
            if (tot != 0.0) {
                for (int k= 0; k < max; k++)
                    arrWeight[subindex + k]/= tot;
            }
		}
		return new SubSamplingData(arrN, arrPixel, arrWeight, numContributors);
	}

	private void verticalFromWorkToDst(double[][] workPixels, double[] outPixels, int start, int delta) {
		for (int x = start; x < dstWidth; x+=delta)
        {
			final int xLocation = x;
			for (int y = dstHeight-1; y >=0 ; y--)
			{
				final int yTimesNumContributors = y * verticalSubsamplingData.numContributors;
				final int max= verticalSubsamplingData.arrN[y];
				final int sampleLocation = (y*dstWidth+x);


				double sample0 = 0.0;
				int index= yTimesNumContributors;
				for (int j= max-1; j >=0 ; j--) {
					int valueLocation = verticalSubsamplingData.arrPixel[index];
					double arrWeight = verticalSubsamplingData.arrWeight[index];
					sample0+= (workPixels[valueLocation][xLocation]) *arrWeight ;

					index++;
				}

                if (sample0 < -128)
                    outPixels[sampleLocation] = -128;
                else if (sample0 > 127)
                    outPixels[sampleLocation] = 127;
                else
                    outPixels[sampleLocation] = sample0;
			}
			processedItems++;
			if (start==0){ // only update progress listener from main thread
            	setProgress();
			}
        }
    }

	/**
     * Apply filter to sample horizontally from Src to Work
     * @param srcImg
     * @param workPixels
     */
    private void horizontallyFromSrcToWork(double[] srcImg, double[][] workPixels, int start, int delta) {
		final double[] srcPixels = new double[srcWidth]; // create reusable row to minimize memory overhead

		for (int k = start; k < srcHeight; k=k+delta)
        {
            System.arraycopy(srcImg, k * srcWidth, srcPixels, 0, srcWidth);

			for (int i = dstWidth-1;i>=0 ; i--)
			{
				int sampleLocation = i;
				final int max = horizontalSubsamplingData.arrN[i];

				double sample0 = 0.0;
				int index= i * horizontalSubsamplingData.numContributors;
				for (int j= max-1; j >= 0; j--) {
					double arrWeight = horizontalSubsamplingData.arrWeight[index];
					int pixelIndex = horizontalSubsamplingData.arrPixel[index];

					sample0 += (srcPixels[pixelIndex]) * arrWeight;
					index++;
				}

				workPixels[k][sampleLocation] = sample0;
			}
			processedItems++;
			if (start==0){ // only update progress listener from main thread
				setProgress();
			}
		}
    }

	private void setProgress(){
        fireProgressChanged(processedItems/totalItems);
    }

}

