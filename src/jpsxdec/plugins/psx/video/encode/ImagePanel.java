/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.plugins.psx.video.encode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {

    private int m_iScale = 2;
    private int m_iBorder = 5;

    private BufferedImage image;
    private int m_iHighlightX = -1, m_iHighlightY = -1;

    public ImagePanel() {

    }
    public ImagePanel(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public void setScale(int i) {
        m_iScale = i;
    }

    public void setHighlight(int x, int y) {
        x = (x - m_iBorder) / 16 / m_iScale;
        y = (y - m_iBorder) / 16 / m_iScale;
        if (x < 0 || y < 0 ||
            x >= ((image.getWidth() + 15)/16) ||
            y >= ((image.getHeight() + 15) / 16))
            return;
        m_iHighlightX = x;
        m_iHighlightY = y;
        repaint();
    }

    public int getHighlightX() {
        return m_iHighlightX;
    }
    public int getHighlightY() {
        return m_iHighlightY;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (image == null) return;
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.drawImage(image, m_iBorder,m_iBorder, 
                           image.getWidth() * m_iScale,
                           image.getHeight()*m_iScale,
                           null);
        if (m_iHighlightX >= 0 && m_iHighlightY >= 0) {
            int x = m_iBorder+m_iHighlightX*16*m_iScale-1;
            int y = m_iBorder+m_iHighlightY*16*m_iScale-1;
            g.setXORMode(Color.red);
            g.drawRect(x, y, 16 * m_iScale + 2, 16 * m_iScale + 2);
            g.setPaintMode();
        }
    }
}
