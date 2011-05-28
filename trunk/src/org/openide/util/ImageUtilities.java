/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.openide.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/** 
 * Useful static methods for manipulation with images/icons, results are cached.
 * 
 * @author Jaroslav Tulach, Tomas Holy
 * @since 7.15
 */
public final class ImageUtilities {
    /** separator for individual parts of tool tip text */
    static final String TOOLTIP_SEPAR = "<br>"; // NOI18N

    /** Resource paths for which we have had to strip initial slash.
     * @see "#20072"
     */
    private static final Set<String> extraInitialSlashes = new HashSet<String>();
    private static volatile Object currentLoader;
    private static boolean noLoaderWarned = false;
    private static final Component component = new Component() {
    };

    private static final MediaTracker tracker = new MediaTracker(component);
    private static int mediaTrackerID;
    
    private static ImageReader PNG_READER;
//    private static ImageReader GIF_READER;
    
    private static final Logger ERR = Logger.getLogger(ImageUtilities.class.getName());
    
    private ImageUtilities() {
    }

    static {
        ImageIO.setUseCache(false);
        PNG_READER = ImageIO.getImageReadersByMIMEType("image/png").next();
//        GIF_READER = ImageIO.getImageReadersByMIMEType("image/gif").next();
    }


    public static final Icon image2Icon(Image image) {
        if (image instanceof ToolTipImage) {
            return ((ToolTipImage) image).getIcon();
        } else {
            return new ImageIcon(image);
        }
    }
    
    /**
     * Converts given icon to a {@link java.awt.Image}.
     *
     * @param icon {@link javax.swing.Icon} to be converted.
     */
    public static final Image icon2Image(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else {
            ToolTipImage image = new ToolTipImage("", icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.getGraphics();
            icon.paintIcon(new JLabel(), g, 0, 0);
            g.dispose();
            return image;
        }
    }
    

    /**
     * Creates disabled (color saturation lowered) icon.
     * Icon image conversion is performed lazily.
     * @param icon original icon used for conversion
     * @return less saturated Icon
     * @since 7.28
     */
    public static Icon createDisabledIcon(Icon icon)  {
        Parameters.notNull("icon", icon);
        return new LazyDisabledIcon(icon2Image(icon));
    }


    private static void ensureLoaded(Image image) {
        if (
            (Toolkit.getDefaultToolkit().checkImage(image, -1, -1, null) &
                (ImageObserver.ALLBITS | ImageObserver.FRAMEBITS)) != 0
        ) {
            return;
        }

        synchronized (tracker) {
            int id = ++mediaTrackerID;
            tracker.addImage(image, id);

            try {
                tracker.waitForID(id, 0);
            } catch (InterruptedException e) {
                System.out.println("INTERRUPTED while loading Image");
            }

            assert (tracker.statusID(id, false) == MediaTracker.COMPLETE) : "Image loaded";
            tracker.removeImage(image, id);
        }
    }
    
    static private ColorModel colorModel(int transparency) {
        ColorModel model;
        try {
            model = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .getColorModel(transparency);
        }
        catch(HeadlessException he) {
            model = ColorModel.getRGBdefault();
        }
        return model;
    }

    /**
     * Key used for composite images -- it holds image identities
     */
    private static class CompositeImageKey {
        Image baseImage;
        Image overlayImage;
        int x;
        int y;

        CompositeImageKey(Image base, Image overlay, int x, int y) {
            this.x = x;
            this.y = y;
            this.baseImage = base;
            this.overlayImage = overlay;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CompositeImageKey)) {
                return false;
            }

            CompositeImageKey k = (CompositeImageKey) other;

            return (x == k.x) && (y == k.y) && (baseImage == k.baseImage) && (overlayImage == k.overlayImage);
        }

        @Override
        public int hashCode() {
            int hash = ((x << 3) ^ y) << 4;
            hash = hash ^ baseImage.hashCode() ^ overlayImage.hashCode();

            return hash;
        }

        @Override
        public String toString() {
            return "Composite key for " + baseImage + " + " + overlayImage + " at [" + x + ", " + y + "]"; // NOI18N
        }
    }
    
    /**
     * Key used for ToolTippedImage
     */
    private static class ToolTipImageKey {
        Image image;
        String str;

        ToolTipImageKey(Image image, String str) {
            this.image = image;
            this.str = str;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ToolTipImageKey)) {
                return false;
            }
            ToolTipImageKey k = (ToolTipImageKey) other;
            return (str.equals(k.str)) && (image == k.image);
        }

        @Override
        public int hashCode() {
            int hash = image.hashCode() ^ str.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return "ImageStringKey for " + image + " + " + str; // NOI18N
        }
    }


    /**
     * Image with tool tip text (for icons with badges)
     */
    private static class ToolTipImage extends BufferedImage implements Icon {
        final String toolTipText;
        ImageIcon imageIcon;
        final URL url;

        public static ToolTipImage createNew(String toolTipText, Image image, URL url) {
            ImageUtilities.ensureLoaded(image);
            boolean bitmask = (image instanceof Transparency) && ((Transparency) image).getTransparency() != Transparency.TRANSLUCENT;
            ColorModel model = colorModel(bitmask ? Transparency.BITMASK : Transparency.TRANSLUCENT);
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            ToolTipImage newImage = new ToolTipImage(
                toolTipText,
                model,
                model.createCompatibleWritableRaster(w, h),
                model.isAlphaPremultiplied(), null, url
            );

            java.awt.Graphics g = newImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            return newImage;
        }
        
        public ToolTipImage(
            String toolTipText, ColorModel cm, WritableRaster raster,
            boolean isRasterPremultiplied, Hashtable<?, ?> properties, URL url
        ) {
            super(cm, raster, isRasterPremultiplied, properties);
            this.toolTipText = toolTipText;
            this.url = url;
        }

        public ToolTipImage(String toolTipText, int width, int height, int imageType) {
            super(width, height, imageType);
            this.toolTipText = toolTipText;
            this.url = null;
        }
        
        synchronized ImageIcon getIcon() {
            if (imageIcon == null) {
                imageIcon = new ImageIcon(this);
            }
            return imageIcon;
        }

        public int getIconHeight() {
            return super.getHeight();
        }

        public int getIconWidth() {
            return super.getWidth();
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.drawImage(this, x, y, null);
        }

        @Override
        public Object getProperty(String name, ImageObserver observer) {
            if ("url".equals(name)) { // NOI18N
                if (url != null) {
                    return url;
                } else {
                    return imageIcon.getImage().getProperty("url", observer);
                }
            }
            return super.getProperty(name, observer);
        }
    }

    private static class LazyDisabledIcon implements Icon {

        /** Shared instance of filter for disabled icons */
        private static final RGBImageFilter DISABLED_BUTTON_FILTER = new DisabledButtonFilter();
        private Image img;
        private Icon disabledIcon;

        public LazyDisabledIcon(Image img) {
            assert null != img;
            this.img = img;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            getDisabledIcon().paintIcon(c, g, x, y);
        }

        public int getIconWidth() {
            return getDisabledIcon().getIconWidth();
        }

        public int getIconHeight() {
            return getDisabledIcon().getIconHeight();
        }

        private synchronized Icon getDisabledIcon() {
            if (null == disabledIcon) {
                disabledIcon = new ImageIcon(createDisabledImage(img));
            }
            return disabledIcon;
        }

        static Image createDisabledImage(Image img) {
            ImageProducer prod = new FilteredImageSource(img.getSource(), DISABLED_BUTTON_FILTER);
            return Toolkit.getDefaultToolkit().createImage(prod);
        }
    }

    private static class DisabledButtonFilter extends RGBImageFilter {

        DisabledButtonFilter() {
            canFilterIndexColorModel = true;
        }

        public int filterRGB(int x, int y, int rgb) {
            // Reduce the color bandwidth in quarter (>> 2) and Shift 0x88.
            return (rgb & 0xff000000) + 0x888888 + ((((rgb >> 16) & 0xff) >> 2) << 16) + ((((rgb >> 8) & 0xff) >> 2) << 8) + (((rgb) & 0xff) >> 2);
        }

        // override the superclass behaviour to not pollute
        // the heap with useless properties strings. Saves tens of KBs
        @Override
        public void setProperties(Hashtable props) {
            props = (Hashtable) props.clone();
            consumer.setProperties(props);
        }
    }
}
