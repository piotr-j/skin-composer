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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class Utils {
    public static String os;
    public static Color averageColor(FileHandle file) {
        Pixmap pixmap = new Pixmap(file);
        Color returnValue = averageColor(pixmap);
        pixmap.dispose();
        return returnValue;
    }
    
    /**
     * Does not dispose pixmap.
     * @param pixmap
     * @return 
     */
    public static Color averageColor(Pixmap pixmap) {
        Color temp = new Color();
        float sumR = 0.0f;
        float sumG = 0.0f;
        float sumB = 0.0f;
        int count = 0;
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                temp.set(pixmap.getPixel(x, y));
                if (temp.a > 0) {
                    sumR += temp.r;
                    sumG += temp.g;
                    sumB += temp.b;
                    count++;
                }
            }
        }
        
        if (count == 0) {
            return new Color(Color.BLACK);
        } else {
            return new Color(sumR / count, sumG / count, sumB / count, 1.0f);
        }
    }
    
    public static Color averageEdgeColor(FileHandle file) {
        Pixmap pixmap = new Pixmap(file);
        Color returnValue = averageEdgeColor(pixmap, file.name().matches("(?i).*\\.9\\.png$"));
        pixmap.dispose();
        return returnValue;
    }
    
    public static Color averageEdgeColor(FileHandle file, Color color) {
        Pixmap pixmap = new Pixmap(file);
        pixmap = tintPixmap(pixmap, color);
        Color returnValue = averageEdgeColor(pixmap, file.name().matches("(?i).*\\.9\\.png$"));
        pixmap.dispose();
        return returnValue;
    }
    
    public static Vector2 imageDimensions(FileHandle file) {
        Vector2 vector = new Vector2();
        Pixmap pixmap = new Pixmap(file);
        vector.x = pixmap.getWidth();
        vector.y = pixmap.getHeight();
        if (file.name().matches("(?i).*\\.9\\.png$")) {
            vector.x = MathUtils.clamp(vector.x - 2, 0.0f, vector.x);
            vector.y = MathUtils.clamp(vector.y - 2, 0.0f, vector.y);
        }
        pixmap.dispose();
        return vector;
    }
    
    /**
     * Does not dispose pixmap
     * @param pixmap
     * @return 
     */
    public static Pixmap tintPixmap(Pixmap pixmap, Color color) {
        Color tempColor = new Color();
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                tempColor.set(pixmap.getPixel(x, y));
                float a = tempColor.a;
                tempColor.mul(color);
                tempColor.a = a;
                pixmap.setColor(tempColor);
                pixmap.drawPixel(x, y);
                tempColor.set(pixmap.getPixel(x, y));
            }
        }
        return pixmap;
    }
    
    /**
     * Does not dispose pixmap.
     * @param pixmap
     * @param ninePatch
     * @return 
     */
    public static Color averageEdgeColor(Pixmap pixmap, boolean ninePatch) {
        int border = 0;
        if (ninePatch) {
            border = 1;
        }
        
        Color temp = new Color();
        float sumR = 0.0f;
        float sumG = 0.0f;
        float sumB = 0.0f;
        int count = 0;

        //left edge
        for (int y = border; y < pixmap.getHeight() - border; y++) {
            for (int x = border; x < pixmap.getWidth() - border; x++) {
                temp.set(pixmap.getPixel(x, y));
                if (temp.a > 0) {
                    sumR += temp.r;
                    sumG += temp.g;
                    sumB += temp.b;
                    count++;
                    break;
                }
            }
        }
        
        //right edge
        for (int y = border; y < pixmap.getHeight() - border; y++) {
            for (int x = pixmap.getWidth() - 1 - border; x > border; x--) {
                temp.set(pixmap.getPixel(x, y));
                if (temp.a > 0) {
                    sumR += temp.r;
                    sumG += temp.g;
                    sumB += temp.b;
                    count++;
                    break;
                }
            }
        }
        
        //top edge
        for (int x = border; x < pixmap.getWidth() - border; x++) {
            for (int y = border; y < pixmap.getHeight() - border; y++) {
                temp.set(pixmap.getPixel(x, y));
                if (temp.a > 0) {
                    sumR += temp.r;
                    sumG += temp.g;
                    sumB += temp.b;
                    count++;
                    break;
                }
            }
        }
        
        //bottom edge
        for (int x = border; x < pixmap.getWidth() - border; x++) {
            for (int y = pixmap.getHeight() - 1 - border; y > border; y--) {
                temp.set(pixmap.getPixel(x, y));
                if (temp.a > 0) {
                    sumR += temp.r;
                    sumG += temp.g;
                    sumB += temp.b;
                    count++;
                    break;
                }
            }
        }
        
        if (count == 0) {
            return new Color(Color.BLACK);
        } else {
            return new Color(sumR / count, sumG / count, sumB / count, 1.0f);
        }
    }
    
    public static Color inverseColor(Color color) {
        return new Color(1 - color.r, 1 - color.g, 1 - color.b, color.a);
    }
    
    public static Color blackOrWhiteBgColor(Color color) {
        return brightness(color) > .5f ? new Color(Color.BLACK) : new Color(Color.WHITE);
    }
    
    public static float brightness(Color color) {
        return (float) (Math.sqrt(0.299f * Math.pow(color.r, 2) + 0.587 * Math.pow(color.g, 2) + 0.114 * Math.pow(color.b, 2)));
    }

    public static void openFileExplorer(FileHandle startDirectory) throws IOException {
        UtilsCompat.openFileExplorer(startDirectory);
    }

    public static boolean isWindows() {
        if (os == null) {
            os = UtilsCompat.osName();
        }
        return os.startsWith("Windows");
    }

    public static boolean isLinux() {
        if (os == null) {
            os = UtilsCompat.osName();
        }
        return os.startsWith("Linux");
    }
    
    public static boolean isMac() {
        if (os == null) {
            os = UtilsCompat.osName();
        }
        return os.startsWith("Mac");
    }
    
    public static float floorPot(float value) {
        float returnValue = 0.0f;
        for (float newValue = 2.0f; newValue < value; newValue *= 2.0f) {
            returnValue = newValue;
        }
        
        return returnValue;
    }

    public static boolean doesImageFitBox(FileHandle fileHandle, float width, float height) {
        return UtilsCompat.doesImageFitBox(fileHandle, width, height);
    }
    
    public static void writeWarningsToFile(Array<String> warnings, FileHandle file) {
        for (String warning : warnings) {
            String formatted = warning.replaceAll("(?<!\\[)\\[(?!\\[).*?\\]", "") + "\n";
            file.writeString(formatted, true);
        }
    }

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFile
     * @param destDirectory
     * @throws IOException
     */
    public static void unzip(FileHandle zipFile, FileHandle destDirectory) throws IOException {
        UtilsCompat.unzip(zipFile, destDirectory);
    }

    public static Pixmap textureRegionToPixmap(TextureRegion textureRegion) {
        var texture = textureRegion.getTexture();
        if (!texture.getTextureData().isPrepared()) {
            texture.getTextureData().prepare();
        }
        
        var pixmap = texture.getTextureData().consumePixmap();
        var returnValue = new Pixmap(textureRegion.getRegionWidth(), textureRegion.getRegionHeight(), Pixmap.Format.RGBA8888);
        returnValue.setBlending(Pixmap.Blending.None);
        
        for (int x = 0; x < textureRegion.getRegionWidth(); x++) {
            for (int y = 0; y < textureRegion.getRegionHeight(); y++) {
                int colorInt = pixmap.getPixel(textureRegion.getRegionX() + x, textureRegion.getRegionY() + y);
                returnValue.drawPixel(x, y, colorInt);
            }
        }
        
        pixmap.dispose();
        
        return returnValue;
    }
    
    public static Cursor textureRegionToCursor(TextureRegion textureRegion, int xHotspot, int yHotspot) {
        return Gdx.graphics.newCursor(textureRegionToPixmap(textureRegion), xHotspot, yHotspot);
    }
    
    public static int colorToInt(Color color) {
        return ((int)(255 * color.r) << 24) | ((int)(255 * color.g) << 16) | ((int)(255 * color.b) << 8) | ((int)(255 * color.a));
    }
    
    public static String removeDuplicateCharacters(String string) {
        char[] chars = string.toCharArray();
        Set<Character> charSet = new LinkedHashSet<Character>();
        for (char c : chars) {
            charSet.add(c);
        }

        var sb = new StringBuilder();
        charSet.forEach((character) -> {
            sb.append(character);
        });
        return sb.toString();
    }
    
    public static String sanitizeFilePath(String path) {
        var file = new FileHandle(path);
        return file.path();
    }
    
    public static boolean isIntegerValue(BigDecimal bigDecimal) {
        return bigDecimal.signum() == 0 || bigDecimal.scale() <= 0 || bigDecimal.stripTrailingZeros().scale() <= 0;
    }
    
    public static boolean isEqual(float... values) {
        if (values.length > 0) {
            float first = values[0];
            for (int i = 1; i < values.length; i++) {
                float value = values[i];
                if (!MathUtils.isEqual(first, value)) return false;
            }
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isNinePatch(String name) {
        return name.matches(".*\\.9\\.[a-zA-Z0-9]*$");
    }

    public static void RGBtoHSB (int r, int g, int b, float[] hsb) {
        UtilsCompat.RGBtoHSB(r, g, b, hsb);
    }

    public static void beep () {
        UtilsCompat.beep();
    }

    public static void writePNG (FileHandle outputFile, Pixmap savePixmap) {
        UtilsCompat.writePNG(outputFile, savePixmap);
    }

    /**
     * Pattern.quote() wrapper
     */
    public static String quote (String name) {
        return UtilsCompat.quote(name);
    }

    /**
     * @param runnable to be run in the background
     */
    public static void runAsync (Runnable runnable) {
        UtilsCompat.runAsync(runnable);
    }

    public static String userHome () {
        return Utils.sanitizeFilePath(UtilsCompat.userHome());
    }

    public static String path (String text) {
        return UtilsCompat.path(text);
    }

    public static String format (float value) {
        return UtilsCompat.formatDecimals(value);
    }

    public static void initDefaults () {
        UtilsCompat.initDefaults();
    }
}