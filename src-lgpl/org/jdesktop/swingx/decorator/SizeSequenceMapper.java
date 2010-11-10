/*
 * $Id: SizeSequenceMapper.java,v 1.2 2007/06/14 10:06:40 kleopatra Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.decorator;

import java.util.Arrays;

import javax.swing.SizeSequence;

/**
 * @author Jeanette Winzenburg
 */
public class SizeSequenceMapper {

    private SizeSequence viewSizes;

    private SizeSequence modelSizes;

    private FilterPipeline pipeline;

    private PipelineListener pipelineListener;

    private int defaultHeight;

    public SizeSequenceMapper() {
    }

    public SizeSequenceMapper(FilterPipeline pipeline) {
        this();
        setFilters(pipeline);
    }
    /**
     * 
     * @param pipeline
     * @param selection
     */
    public SizeSequenceMapper(FilterPipeline pipeline, SizeSequence selection, int defaultHeight) {
        this();
        setViewSizeSequence(selection, defaultHeight);
        setFilters(pipeline);
    }

    public void setViewSizeSequence(SizeSequence selection, int height) {
        SizeSequence old = this.viewSizes;
        if (old != null) {
            clearModelSizes();
        }
        this.viewSizes = selection;
        this.defaultHeight = height;
        mapTowardsModel();
    }

    public SizeSequence getViewSizeSequence() {
        return viewSizes;
    }

    public void setFilters(FilterPipeline pipeline) {
        FilterPipeline old = this.pipeline;
        if (old != null) {
            old.removePipelineListener(pipelineListener);
        }
        this.pipeline = pipeline;
        if (pipeline != null) {
            pipeline.addPipelineListener(getPipelineListener());
        }
        restoreSelection();
    }



    public void clearModelSizes() {
        modelSizes = null;
    }

    public void insertIndexInterval(int start, int length, int value) {
        if (modelSizes == null) return;
        modelSizes.insertEntries(start, length, value);
    }

    public void removeIndexInterval(int start, int length) {
        if (modelSizes == null) return;
        modelSizes.removeEntries(start, length);
    }

    public void restoreSelection() {
        if (viewSizes == null) return;
        int[] sizes = new int[getOutputSize()];
        Arrays.fill(sizes, defaultHeight);
        viewSizes.setSizes(sizes);
//        viewSizes.setSizes(new int[0]);
//        viewSizes.insertEntries(0, getOutputSize(), defaultHeight);

        int[] selected = modelSizes.getSizes();
        for (int i = 0; i < selected.length; i++) {
          int index = convertToView(i);
          // index might be -1, ignore. 
          if (index >= 0) {
              viewSizes.setSize(index, selected[i]);
          }
        }
    }

    private void mapTowardsModel() {
        if (viewSizes == null) return;
        modelSizes = new SizeSequence(getInputSize(), defaultHeight);
        int[] selected = viewSizes.getSizes(); 
        for (int i = 0; i < selected.length; i++) {
            int modelIndex = convertToModel(i);
            modelSizes.setSize(modelIndex, selected[i]); 
        }
    }

    private int getInputSize() {
        return pipeline != null ? pipeline.getInputSize() : 0;
    }

    private int getOutputSize() {
        return pipeline != null ? pipeline.getOutputSize() : 0;
    }

    private int convertToModel(int index) {
        return pipeline != null ? pipeline.convertRowIndexToModel(index) : index;
    }
    
    private int convertToView(int index) {
        return pipeline != null ? pipeline.convertRowIndexToView(index) : index;
    }
    

    protected void updateFromPipelineChanged() {
        restoreSelection();
    }

    private PipelineListener getPipelineListener() {
        if (pipelineListener == null) {
            pipelineListener = new PipelineListener() {

                public void contentsChanged(PipelineEvent e) {
                    updateFromPipelineChanged();
                    
                }
                
            };
        }
        return pipelineListener;
    }


}
