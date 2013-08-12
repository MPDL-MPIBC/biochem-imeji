/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License"). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE
 * or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at license/ESCIDOC.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft
 * für wissenschaftlich-technische Information mbH and Max-Planck-
 * Gesellschaft zur Förderung der Wissenschaft e.V.
 * All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.storage.transform.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.storage.transform.ImageGenerator;
import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * {@link ImageGenerator} for all unknown/unsupported format. It creates a default image
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class RawFileImageGenerator implements ImageGenerator
{
    private static Logger logger = Logger.getLogger(RawFileImageGenerator.class);
    private static String PATH_TO_DEFAULT_IMAGE = "images/file-icon.jpg";
    /**
     * Coordinates where the text is written on the image
     */
    private static int TEXT_POSITION_X = 630;
    private static int TEXT_POSITION_Y = 700;
    /**
     * The Font size of the text
     */
    private static int TEXT_FONT_SIZE = 150;

    /*
     * (non-Javadoc)
     * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
     */
    @Override
    public byte[] generateJPG(File file, String extension)
    {
        try
        {
            BufferedImage icon = ImageIO.read(new FileImageInputStream(new File(RawFileImageGenerator.class
                    .getClassLoader().getResource(PATH_TO_DEFAULT_IMAGE).toURI())));
            icon = writeTextOnImage(icon, extension);
            return ImageUtils.toBytes(icon, StorageUtils.getMimeType("jpg"));
        }
        catch (Exception e)
        {
            logger.debug("Error reading default image file", e);
        }
        return null;
    }

    /**
     * Write the extension on the {@link BufferedImage}
     * 
     * @param old
     * @param extension
     * @return
     */
    private BufferedImage writeTextOnImage(BufferedImage old, String extension)
    {
        int w = old.getWidth();
        int h = old.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(old, 0, 0, null);
        g2d.setPaint(Color.WHITE);
        g2d.setFont(new Font("Serif", Font.BOLD, TEXT_FONT_SIZE));
        FontMetrics fm = g2d.getFontMetrics();
        extension = formatExtension(extension);
        g2d.drawString(extension, TEXT_POSITION_X - fm.stringWidth(extension), TEXT_POSITION_Y);
        g2d.dispose();
        return img;
    }

    /**
     * Format the extension to avoid to broke the design of the created icon
     * 
     * @param extension
     * @return
     */
    private String formatExtension(String extension)
    {
        if (extension.length() > 3)
            extension = extension.substring(0, 3);
        return extension.toUpperCase();
    }
}
