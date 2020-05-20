/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Raymond Buckley
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.ray3k.skincomposer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.ray3k.skincomposer.GwtIncompatible;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * GWT incompatible methods, with custom implementation in gwt
 */
@GwtIncompatible
class UtilsCompat {
    public static void openFileExplorer(FileHandle startDirectory) throws IOException {
        if (startDirectory.exists()) {
            File file = startDirectory.file();
            Desktop desktop = Desktop.getDesktop();
            desktop.open(file);
        } else {
            throw new IOException("Directory doesn't exist: " + startDirectory.path());
        }
    }

    public static boolean doesImageFitBox(FileHandle fileHandle, float width, float height) {
        boolean result = false;
        String suffix = fileHandle.extension();
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            try (var stream = new FileImageInputStream(fileHandle.file())) {
                reader.setInput(stream);
                int imageWidth = reader.getWidth(reader.getMinIndex());
                int imageHeight = reader.getHeight(reader.getMinIndex());
                result = imageWidth < width && imageHeight < height;
            } catch (IOException e) {
                Gdx.app.error(UtilsCompat.class.getName(), "error checking image dimensions", e);
            } finally {
                reader.dispose();
            }
        } else {
            Gdx.app.error(UtilsCompat.class.getName(), "No reader available to check image dimensions");
        }
        return result;
    }

    public static void RGBtoHSB (int r, int g, int b, float[] hsb) {
        java.awt.Color.RGBtoHSB((int) (255.0f * r), (int) (255.0f * g), (int) (255.0f * b), hsb);
    }

    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFile
     * @param destDirectory
     * @throws IOException
     */
    public static void unzip(FileHandle zipFile, FileHandle destDirectory) throws IOException {
        destDirectory.mkdirs();

        InputStream is = zipFile.read();
        ZipInputStream zis = new ZipInputStream(is);

        ZipEntry entry = zis.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zis, destDirectory.child(entry.getName()));
            } else {
                // if the entry is a directory, make the directory
                destDirectory.child(entry.getName()).mkdirs();
            }
            zis.closeEntry();
            entry = zis.getNextEntry();
        }
        is.close();
        zis.close();
    }

    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, FileHandle filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(filePath.write(false));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public static String osName () {
        return System.getProperty("os.name");
    }

    public static void beep () {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    public static void writePNG (FileHandle outputFile, Pixmap savePixmap) {
        PixmapIO.writePNG(outputFile, savePixmap);
    }

    public static String quote (String name) {
        return Pattern.quote(name);
    }

    public static void runAsync (Runnable runnable) {
        if (runnable == null) return;
        // executor?
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public static String userHome () {
        return System.getProperty("user.home");
    }

    public static String path (String text) {
        return Paths.get(text).toString();
    }

    static DecimalFormat df;
    public static String formatDecimals (float value) {
        if (df == null) {
            DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);
            df = new DecimalFormat("#.#", decimalFormatSymbols);
        }
        return df.format(value);
    }

    public static void initDefaults () {
        if (Utils.isMac()) System.setProperty("java.awt.headless", "true");
    }
}