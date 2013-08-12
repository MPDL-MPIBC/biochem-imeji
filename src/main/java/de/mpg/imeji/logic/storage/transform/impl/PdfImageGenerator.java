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

import java.io.File;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.storage.transform.ImageGenerator;
import de.mpg.imeji.logic.storage.util.PdfUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * {@link ImageGenerator} to generate image out of pdf
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class PdfImageGenerator implements ImageGenerator
{
    private static Logger logger = Logger.getLogger(PdfImageGenerator.class);

    /*
     * (non-Javadoc)
     * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
     */
    @Override
    public byte[] generateJPG(File file, String extension)
    {
        try
        {
            if (StorageUtils.getMimeType(extension).equals("application/pdf"))
                return PdfUtils.pdfsToImageBytes(file);
        }
        catch (Exception e)
        {
            logger.debug("Error transforming a pdf to an image", e);
        }
        return null;
    }
}
