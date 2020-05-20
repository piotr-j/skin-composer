package com.ray3k.skincomposer.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.gwt.FreetypeInjector;
import com.badlogic.gdx.graphics.g2d.freetype.gwt.inject.OnCompletion;
import com.badlogic.gdx.utils.Array;
import com.ray3k.skincomposer.CloseListener;
import com.ray3k.skincomposer.DesktopWorker;
import com.ray3k.skincomposer.FilesDroppedListener;
import com.ray3k.skincomposer.Main;

import java.io.File;
import java.util.List;

/**
 * Main entry point
 */
public class GWTLauncher extends GwtApplication {

    // PADDING is to avoid scrolling in iframes, set to 20 if you have problems
    private static final int PADDING = 0;
    private GwtApplicationConfiguration cfg;

    @Override public GwtApplicationConfiguration getConfig () {
        // fixed or resizeable?
        cfg = new GwtApplicationConfiguration(800, 800);
//        int w = Window.getClientWidth() - PADDING;
//        int h = Window.getClientHeight() - PADDING;
//        cfg = new GwtApplicationConfiguration(w, h);
//        Window.enableScrolling(false);
//        Window.setMargin("0");
//        Window.addResizeHandler(event -> {
//            int width = event.getWidth() - PADDING;
//            int height = event.getHeight() - PADDING;
//            getRootPanel().setWidth("" + width + "px");
//            getRootPanel().setHeight("" + height + "px");
//            getApplicationListener().resize(width, height);
//            Gdx.graphics.setWindowedMode(width, height);
//        });
//        cfg.preferFlash = false;
        return cfg;
    }

    @Override
    public void onModuleLoad () {
        FreetypeInjector.inject(GWTLauncher.super::onModuleLoad);
    }

    public ApplicationListener createApplicationListener () {
//        return new ApplicationAdapter() {
//            @Override
//            public void create () {
//                super.create();
//            }
//        };

        var main = new Main(null);
        main.setDesktopWorker(new DesktopWorker() {
            @Override
            public void texturePack (Array<FileHandle> handles, FileHandle localFile, FileHandle targetFile,
                FileHandle settingsFile) {

            }

            @Override
            public void packFontImages (Array<FileHandle> files, FileHandle saveFile) {

            }

            @Override
            public void sizeWindowToFit (int maxWidth, int maxHeight, int displayBorder, Graphics graphics) {

            }

            @Override
            public void centerWindow (Graphics graphics) {

            }

            @Override
            public void addFilesDroppedListener (FilesDroppedListener filesDroppedListener) {

            }

            @Override
            public void removeFilesDroppedListener (FilesDroppedListener filesDroppedListener) {

            }

            @Override
            public void setCloseListener (CloseListener closeListener) {

            }

            @Override
            public void attachLogListener () {

            }

            @Override
            public List<File> openMultipleDialog (String title, String defaultPath, String[] filterPatterns,
                String filterDescription) {
                return null;
            }

            @Override
            public File openDialog (String title, String defaultPath, String[] filterPatterns, String filterDescription) {
                return null;
            }

            @Override
            public File saveDialog (String title, String defaultPath, String[] filterPatterns, String filterDescription) {
                return null;
            }

            @Override
            public void closeSplashScreen () {

            }

            @Override
            public char getKeyName (int keyCode) {
                return 0;
            }

            @Override
            public void writeFont (FreeTypeFontGenerator.FreeTypeBitmapFontData data, Array<PixmapPacker.Page> pages,
                FileHandle target) {

            }
        });
        return main;
    }
}