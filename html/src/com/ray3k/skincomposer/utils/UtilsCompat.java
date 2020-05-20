package com.ray3k.skincomposer.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.google.gwt.regexp.shared.RegExp;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * GWT implementation for certain util methods
 */
class UtilsCompat {
    public static void openFileExplorer(FileHandle startDirectory) throws IOException {

    }

    public static boolean doesImageFitBox(FileHandle fileHandle, float width, float height) {
        return true;
    }

    public static void RGBtoHSB (int r, int g, int b, float[] hsb) {

    }

    public static void unzip(FileHandle zipFile, FileHandle destDirectory) throws IOException {

    }

    public static String osName () {
        return "GWT";
    }

    public static void beep () {

    }

    public static void writePNG (FileHandle outputFile, Pixmap savePixmap) {

    }

    public static String quote (String name) {
        return RegExp.quote(name);
    }

    public static void runAsync (Runnable runnable) {
        if (runnable == null) return;
        // no threads over here
        runnable.run();
    }

    public static String userHome () {
        // what do we put here?
        return "GWT";
    }

    public static String path (String text) {
        // wat do
        return text;
    }

    public static String formatDecimals (float value) {
        int v = (int)value;
        int v2 = (int)(value * 10);
        return v + "." + v2;
    }

    public static void initDefaults () {
        // nothing to do here
    }
}
