/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.storage.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.PNGDecodeParam;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;

import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.presentation.util.PropertyReader;

/**
 * Mehtods to help wotk with images
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImageUtils
{
    private static Logger logger = Logger.getLogger(ImageUtils.class);
    /**
     * If true, the rescale will keep the better quality of the images
     */
    private static boolean RESCALE_HIGH_QUALITY = true;

    /**
     * Prepare the image for the upload: <br/>
     * if it is original image upload, do nothing <br/>
     * if it is another resolution, resize it <br/>
     * if it is a tiff to be resized, transformed it to jpeg and resize it
     * 
     * @param stream
     * @param contentCategory
     * @param format
     * @return
     * @throws IOException
     * @throws Exception
     */
    public static byte[] transformImage(byte[] bytes, FileResolution resolution, String mimeType) throws IOException,
            Exception
    {
        // If it is orginal resolution, don't touch the file, otherwise transform (compress and/or )
        if (!FileResolution.ORIGINAL.equals(resolution))
        {
            if (FileResolution.THUMBNAIL.equals(resolution) || StorageUtils.getMimeType("tif").equals(mimeType))
            {
                // If it is the thumbnail, compress the images (in jpeg), if it is a tif compress even for WEb
                // resolution, since resizing not possible with tif
                byte[] compressed = compressImage(bytes, mimeType);
                if (!Arrays.equals(compressed, bytes))
                {
                    mimeType = StorageUtils.getMimeType("jpg");
                }
                bytes = compressed;
            }
            // Read the bytes as BufferedImage
            BufferedImage image;
            if (StorageUtils.getMimeType("jpg").equals(mimeType))
            {
                image = JpegUtils.readJpeg(bytes);
            }
            else
            {
                image = ImageIO.read(new ByteArrayInputStream(bytes));
            }
            {
            	if (image == null)
                // The image couldn't be read
                return null;
            }
            // Resize image
            if (StorageUtils.getMimeType("gif").equals(mimeType) && GifUtils.isAnimatedGif(bytes))
            {
                // If it is an animated gif, resize all frame and build a new gif with this resized frames
                bytes = GifUtils.resizeAnimatedGif(bytes, resolution);
            }
            else
            {
                bytes = toBytes(scaleImage(image, resolution), mimeType);
            }
        }
        return bytes;
    }

    /**
     * Scale a {@link BufferedImage} to new size. Is faster than the basic {@link ImageUtils}.scaleImage method, has the
     * same quality. If it is a thumbnail, cut the images to fit into the raster
     * 
     * @param image original image
     * @param size the size to be resized to
     * @param resolution the type of the image. Might be thumb or web
     * @return the resized images
     * @throws Exception
     */
    public static BufferedImage scaleImageFast(BufferedImage image, int size, FileResolution resolution)
            throws Exception
    {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        BufferedImage newImg = null;
        Image rescaledImage;
        if (width > height)
        {
            if (FileResolution.THUMBNAIL.equals(resolution))
            {
                newImg = new BufferedImage(height, height, BufferedImage.TYPE_INT_RGB);
                Graphics g1 = newImg.createGraphics();
                g1.drawImage(image, (height - width) / 2, 0, null);
                if (height > size)
                    rescaledImage = getScaledInstance(newImg, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                            RESCALE_HIGH_QUALITY);
                else
                    rescaledImage = newImg;
            }
            else
                rescaledImage = getScaledInstance(image, size, height * size / width,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR, RESCALE_HIGH_QUALITY);
        }
        else
        {
            if (FileResolution.THUMBNAIL.equals(resolution))
            {
                newImg = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
                Graphics g1 = newImg.createGraphics();
                g1.drawImage(image, 0, (width - height) / 2, null);
                if (width > size)
                    rescaledImage = getScaledInstance(newImg, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                            RESCALE_HIGH_QUALITY);
                else
                    rescaledImage = newImg;
            }
            else
                rescaledImage = getScaledInstance(image, width * size / height, size,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR, RESCALE_HIGH_QUALITY);
        }
        BufferedImage rescaledBufferedImage = new BufferedImage(rescaledImage.getWidth(null),
                rescaledImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics g2 = rescaledBufferedImage.getGraphics();
        g2.drawImage(rescaledImage, 0, 0, null);
        return rescaledBufferedImage;
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@link BufferedImage}.
     * 
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param hint one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step scaling technique that provides higher quality
     *            than the usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
     *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
     *            {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@link BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint,
            boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w, h;
        if (higherQuality)
        {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        }
        else
        {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }
        do
        {
            if (higherQuality && w > targetWidth)
            {
                w /= 2;
                if (w < targetWidth)
                {
                    w = targetWidth;
                }
            }
            if (higherQuality && h > targetHeight)
            {
                h /= 2;
                if (h < targetHeight)
                {
                    h = targetHeight;
                }
            }
            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        }
        while (w != targetWidth || h != targetHeight);
        return ret;
    }

    /**
     * Transform a tiff image into a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] tiff2Jpeg(byte[] bytes)
    {
        try
        {
            File tiffFile = File.createTempFile("upload", "tif.tmp");
            FileUtils.writeByteArrayToFile(tiffFile, bytes);
            SeekableStream s = new FileSeekableStream(tiffFile);
            TIFFDecodeParam param = new TIFFDecodeParam();
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
            return image2Jpeg(tiffFile, dec);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming tiff to jpeg", e);
        }
    }

    /**
     * Transform a png image into a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] png2Jpeg(byte[] bytes)
    {
        try
        {
            File pngFile = File.createTempFile("upload", "png.tmp");
            FileUtils.writeByteArrayToFile(pngFile, bytes);
            SeekableStream s = new FileSeekableStream(pngFile);
            PNGDecodeParam param = new PNGDecodeParam();
            ImageDecoder dec = ImageCodec.createImageDecoder("png", s, param);
            return image2Jpeg(pngFile, dec);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming png to jpeg", e);
        }
    }

    /**
     * Transform a bmp image into a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] bmp2Jpeg(byte[] bytes)
    {
        return image2Jpeg(bytes);
    }

    /**
     * Transform a gif image to a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] gif2Jpeg(byte[] bytes)
    {
        return image2Jpeg(bytes);
    }

    /**
     * Transform a image to a jpeg image. The input image must have a format supported by {@link ImageIO}
     * 
     * @param bytes
     * @return
     */
    private static byte[] image2Jpeg(byte[] bytes)
    {
        try
        {
            InputStream ins = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(ins);
            ByteArrayOutputStream ous = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", ous);
            return ous.toByteArray();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming image to jpeg", e);
        }
    }

    /**
     * Transform a image {@link File} to a jpeg file
     * 
     * @param f - the {@link File} where the image is
     * @param dec - The {@link ImageDecoder} needed to decode the passed image
     * @return the image as {@link Byte} array
     */
    private static byte[] image2Jpeg(File f, ImageDecoder dec)
    {
        try
        {
            RenderedImage ri = dec.decodeAsRenderedImage(0);
            File jpgFile = File.createTempFile("imeji_upload", "jpg.tmp");
            FileOutputStream fos = new FileOutputStream(jpgFile);
            JPEGEncodeParam jParam = new JPEGEncodeParam();
            jParam.setQuality(1.0f);
            ImageEncoder imageEncoder = ImageCodec.createImageEncoder("JPEG", fos, jParam);
            imageEncoder.encode(ri);
            fos.flush();
            return FileUtils.readFileToByteArray(jpgFile);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming image file to jpeg", e);
        }
    }

    /**
     * Return the format of an image (jpg, tif), according to its mime-type
     * 
     * @param mimeType
     * @return
     */
    public static String getImageFormat(String mimeType)
    {
        if (mimeType.equals(StorageUtils.getMimeType("tif")))
        {
            return "tif";
        }
        return mimeType.toLowerCase().replaceAll("image/", "");
    }

    /**
     * Compress an image in jpeg. Useful to reduce size of thumbnail and web resolution images
     * 
     * @param bytes
     * @param mimeType
     * @return
     */
    public static byte[] compressImage(byte[] bytes, String mimeType)
    {
        try
        {
            if (mimeType.equals(StorageUtils.getMimeType("tif")))
            {
                bytes = ImageUtils.tiff2Jpeg(bytes);
            }
            else if (mimeType.equals(StorageUtils.getMimeType("png")))
            {
                bytes = ImageUtils.png2Jpeg(bytes);
            }
            else if (mimeType.equals(StorageUtils.getMimeType("bmp")))
            {
                bytes = ImageUtils.bmp2Jpeg(bytes);
            }
            else if (mimeType.equals(StorageUtils.getMimeType("gif")))
            {
                bytes = GifUtils.toJPEG(bytes);
            }
        }
        catch (Exception e)
        {
            logger.info("Image could not be compressed: ", e);
        }
        return bytes;
    }

    /**
     * TRansform a {@link BufferedImage} to a {@link Byte} array
     * 
     * @param image
     * @param mimeType
     * @return
     * @throws IOException
     */
    public static byte[] toBytes(BufferedImage image, String mimeType) throws IOException
    {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        // use imageIO.write to encode the image back into a byte[]
        ImageIO.write(image, ImageUtils.getImageFormat(mimeType), byteOutput);
        return byteOutput.toByteArray();
    }

    /**
     * cale the image if too big for the size
     * 
     * @param image
     * @param resolution
     * @return
     * @throws Exception
     */
    public static BufferedImage scaleImage(BufferedImage image, FileResolution resolution) throws Exception
    {
        BufferedImage bufferedImage = null;
        int size = getResolution(resolution);
        if (image.getWidth() > size || image.getHeight() > size)
        {
            bufferedImage = scaleImageFast(image, size, resolution);
        }
        else
        {
            bufferedImage = image;
        }
        return bufferedImage;
    }

    /**
     * Return the maximum size of an image according to its {@link FileResolution}. The values are defined in the
     * properties
     * 
     * @param FileResolution
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static int getResolution(FileResolution resolution) throws IOException, URISyntaxException
    {
        switch (resolution)
        {
            case THUMBNAIL:
                return Integer.parseInt(PropertyReader.getProperty("xsd.resolution.thumbnail"));
            case WEB:
                return Integer.parseInt(PropertyReader.getProperty("xsd.resolution.web"));
            default:
                return 0;
        }
    }

    public static String getThumb() throws IOException, URISyntaxException
    {
        return PropertyReader.getProperty("xsd.metadata.content-category.thumbnail");
    }

    public static String getWeb() throws IOException, URISyntaxException
    {
        return PropertyReader.getProperty("xsd.metadata.content-category.web-resolution");
    }

    public static String getOrig() throws IOException, URISyntaxException
    {
        return PropertyReader.getProperty("xsd.metadata.content-category.original-resolution");
    }
}
