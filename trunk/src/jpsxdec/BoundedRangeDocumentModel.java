/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * MediaHandler.java
 */

package jpsxdec;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;


/** A model that syncronizes a BoundedRangeModel and a Document model. */
public class BoundedRangeDocumentModel
        implements 
        Document, BoundedRangeModel, 
        DocumentListener
{
    private PlainDocument doc = new PlainDocument();
    private DefaultBoundedRangeModel range = new DefaultBoundedRangeModel();

    public BoundedRangeDocumentModel() {
        doc.addDocumentListener(this);
    }
    
    // Document
    //<editor-fold defaultstate="collapsed" desc="Document">
    
    public int getLength() {
        return doc.getLength();
    }

    public void addDocumentListener(DocumentListener listener) {
        doc.addDocumentListener(listener);
    }

    public void removeDocumentListener(DocumentListener listener) {
        doc.removeDocumentListener(listener);
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        doc.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        doc.removeUndoableEditListener(listener);
    }

    public Object getProperty(Object key) {
        return doc.getProperty(key);
    }

    public void putProperty(Object key, Object value) {
        doc.putProperty(key, value);
    }

    public void remove(int offs, int len) throws BadLocationException {
        doc.remove(offs, len);
    }

    public void insertString(int offset,
                String string, AttributeSet attributes)
                throws BadLocationException 
    {

        if (string == null) {
            return;
        } else {
            String newValue;
            int length = getLength();
            if (length == 0) {
                newValue = string;
            } else {
                String currentContent =
                        doc.getText(0, length);
                StringBuffer currentBuffer =
                        new StringBuffer(currentContent);
                currentBuffer.insert(offset, string);
                newValue = currentBuffer.toString();
            }
            
            // limit the number of leading zeros, or whatnot
            if (newValue.length() > Integer.toString(range.getMaximum()).length())
                return;
            
            try {
                // make sure it's a number
                int i = Integer.parseInt(newValue);
                
                // make sure it's within range
                if (i < range.getMinimum() || i > range.getMaximum())
                    return;
                
                doc.insertString(offset, string, attributes);
            } catch (NumberFormatException ex) {

            }
        }
    }
    
   
    public String getText(int offset, int length) throws BadLocationException {
        return doc.getText(offset, length);
    }

    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        doc.getText(offset, length, txt);
    }

    public Position getStartPosition() {
        return doc.getStartPosition();
    }

    public Position getEndPosition() {
        return doc.getEndPosition();
    }

    public Position createPosition(int offs) throws BadLocationException {
        return doc.createPosition(offs);
    }

    public Element[] getRootElements() {
        return doc.getRootElements();
    }

    public Element getDefaultRootElement() {
        return doc.getDefaultRootElement();
    }

    public void render(Runnable r) {
        doc.render(r);
    }
    //</editor-fold>
    

    // BoundedRangeModel
    //<editor-fold defaultstate="collapsed" desc="BoundedRangeModel">
    
    public int getMinimum() {
        return range.getMinimum();
    }

    public void setMinimum(int newMinimum) {
        range.setMinimum(newMinimum);
    }

    public int getMaximum() {
        return range.getMaximum();
    }

    public void setMaximum(int newMaximum) {
        range.setMaximum(newMaximum);
    }

    public int getValue() {
        return range.getValue();
    }

    boolean tmp = true;
    public void setValue(int newValue) {
        range.setValue(newValue);
        
        try {
            doc.replace(0, doc.getLength(), Integer.toString(range.getValue()), null);
        } catch (BadLocationException ex) {
            
        }
         
    }

    public void setValueIsAdjusting(boolean b) {
        range.setValueIsAdjusting(b);
    }

    public boolean getValueIsAdjusting() {
        return range.getValueIsAdjusting();
    }

    public int getExtent() {
        return range.getExtent();
    }

    public void setExtent(int newExtent) {
        range.setExtent(newExtent);
    }

    public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
        range.setRangeProperties(value, extent, min, max, adjusting);
    }

    public void addChangeListener(ChangeListener x) {
        range.addChangeListener(x);
    }

    public void removeChangeListener(ChangeListener x) {
        range.removeChangeListener(x);
    }
    //</editor-fold>
    
    // DocumentListener
    
    public void insertUpdate(DocumentEvent e) {
        updateRange();
    }

    public void removeUpdate(DocumentEvent e) {
        updateRange();
    }

    public void changedUpdate(DocumentEvent e) {
        updateRange();
    }
    
    private void updateRange() {
        try {
            String txt = doc.getText(0, doc.getLength());
            if (txt.length() == 0)
                range.setValue(range.getMinimum());
            else
                range.setValue(Integer.parseInt(txt));
        } catch (BadLocationException ex) {
            
        }
    }

}
