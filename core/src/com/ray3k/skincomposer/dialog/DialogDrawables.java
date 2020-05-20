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
package com.ray3k.skincomposer.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap.Values;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Sort;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.ray3k.skincomposer.FilesDroppedListener;
import com.ray3k.skincomposer.Main;
import com.ray3k.skincomposer.Undoable;
import com.ray3k.skincomposer.UndoableManager;
import com.ray3k.skincomposer.UndoableManager.CustomDrawableUndoable;
import com.ray3k.skincomposer.UndoableManager.DrawableUndoable;
import com.ray3k.skincomposer.data.*;
import com.ray3k.skincomposer.data.DrawableData.DrawableType;
import com.ray3k.skincomposer.dialog.DialogTenPatch.TenPatchData;
import com.ray3k.skincomposer.utils.Utils;
import com.ray3k.stripe.PopTableClickListener;
import com.ray3k.stripe.Spinner;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class DialogDrawables extends Dialog {
    public static DialogDrawables instance;
    private final static int[] sizes = {40, 135, 160, 210, 260};
    private static float scrollPosition = 0.0f;
    private static int zoomLevel = 1;
    private static int sortSelection = 0;
    private SelectBox sortSelectBox;
    private ScrollPane scrollPane;
    private Slider zoomSlider;
    private StyleProperty property;
    private CustomProperty customProperty;
    private Array<DrawableData> drawables;
    private Table contentTable;
    private FilesDroppedListener filesDroppedListener;
    private DialogDrawablesListener listener;
    private Main main;
    private boolean showing9patchButton;
    private boolean showingOptions;
    private FilterOptions filterOptions;
    private FilterInputListener filterInputListener;
    
    public interface DialogDrawablesListener {
        void confirmed(DrawableData drawable, DialogDrawables dialog);
        void emptied(DialogDrawables dialog);
        void cancelled(DialogDrawables dialog);
    }
    
    public DialogDrawables(Main main, StyleProperty property, DialogDrawablesListener listener) {
        super("", main.getSkin(), "dialog");
        this.property = property;
        initialize(main, listener);
    }
    
    public DialogDrawables(Main main, CustomProperty property, DialogDrawablesListener listener) {
        super("", main.getSkin(), "dialog");
        this.customProperty = property;
        initialize(main, listener);
    }
    
    public void initialize(Main main, DialogDrawablesListener listener) {
        Table table = new Table();
        table.setFillParent(true);
        table.setTouchable(Touchable.disabled);
        addActor(table);
        
        Label label = new Label("", getSkin(), "filter");
        label.setName("filter-label");
        label.setColor(1, 1, 1, 0);
        table.add(label).bottom().right().expand().pad(50).padBottom(20);
        
        filterOptions = new FilterOptions();
        filterInputListener = new FilterInputListener(this);
        addListener(filterInputListener);
        showing9patchButton = true;
        showingOptions = true;
        this.main = main;
        
        instance = this;
        
        this.listener = listener;
        
        filesDroppedListener = (Array<FileHandle> files) -> {
            Iterator<FileHandle> iter = files.iterator();
            while (iter.hasNext()) {
                FileHandle file = iter.next();
                if (file.isDirectory()) {
                    files.addAll(file.list());
                    iter.remove();
                } else if (!(file.name().toLowerCase().endsWith(".png") || file.name().toLowerCase().endsWith(".jpg") || file.name().toLowerCase().endsWith(".jpeg") || file.name().toLowerCase().endsWith(".bmp") || file.name().toLowerCase().endsWith(".gif"))) {
                    iter.remove();
                }
            }
            
            var filesLimited = new Array<FileHandle>(files);
            iter = filesLimited.iterator();
            while (iter.hasNext()) {
                var file = iter.next();
                if (!file.name().toLowerCase(Locale.ROOT).endsWith(".9.png")) {
                    if (files.contains(file.sibling(file.nameWithoutExtension() + ".9.png"), false)) {
                        iter.remove();
                    }
                }
            }
            
            if (filesLimited.size > 0) {
                drawablesSelected(filesLimited);
            }
        };
        
        main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);
        
        gatherDrawables();
        
        main.getAtlasData().produceAtlas();
        
        populate();
    }
    
    /**
     * Recreates the drawables array only including visible drawables.
     */
    private void gatherDrawables() {
        drawables = new Array<>(main.getAtlasData().getDrawables());
    }
    
    public void populate() {
        getContentTable().clear();
        getButtonTable().clearChildren();
        
        getButtonTable().padBottom(15.0f);
        
        if (property == null && customProperty == null) {
            getContentTable().add(new Label("Drawables", getSkin(), "title"));
        } else {
            getContentTable().add(new Label("Select a Drawable", getSkin(), "title"));
        }
        
        getContentTable().row();
        Table table = new Table(getSkin());
        table.defaults().pad(6.0f);
        getContentTable().add(table).growX();
        
        var button = new Button(getSkin(), "filter");
        button.setName("filter");
        button.setProgrammaticChangeEvents(false);
        table.add(button);
        button.addListener(main.getHandListener());
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                var button = (Button) actor;
                button.setChecked(true);
                main.getDialogFactory().showDialogDrawablesFilter(filterOptions, new DialogDrawablesFilter.FilterListener() {
                    @Override
                    public void applied() {
                        sortBySelectedMode();
                    }
                    
                    @Override
                    public void disabled() {
                        sortBySelectedMode();
                    }

                    @Override
                    public void cancelled() {
                        sortBySelectedMode();
                    }
                });
            }
        });
        
        table.add("Sort:");
        
        sortSelectBox = new SelectBox(getSkin());
        sortSelectBox.setItems("A-Z", "Z-A", "Oldest", "Newest");
        sortSelectBox.setSelectedIndex(sortSelection);
        sortSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                sortSelection = sortSelectBox.getSelectedIndex();
                sortBySelectedMode();
            }
        });
        sortSelectBox.addListener(main.getHandListener());
        sortSelectBox.getList().addListener(main.getHandListener());
        table.add(sortSelectBox);
        
        TextButton textButton = new TextButton("Add...", getSkin());
        table.add(textButton);
        textButton.addListener(main.getHandListener());
        textButton.addListener(new AddClickListener());
        
        table.add(new Label("Zoom:", getSkin())).right().expandX();
        zoomSlider = new Slider(0, 4, 1, false, getSkin());
        zoomSlider.setValue(zoomLevel);
        zoomSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                zoomLevel = MathUtils.round(zoomSlider.getValue());
                refreshDrawableDisplay();
            }
        });
        zoomSlider.addListener(main.getHandListener());
        table.add(zoomSlider);
        
        getContentTable().row();
        contentTable = new Table();
        scrollPane = new ScrollPane(contentTable, getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setFlickScroll(false);
        getContentTable().add(scrollPane).grow();
        sortBySelectedMode();
        scrollPane.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                getStage().setScrollFocus(scrollPane);
            }
        });
        
        getContentTable().row();
        if (property != null || customProperty != null) {
            button("Clear Drawable", true);
            button("Cancel", false);
            getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
            getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        } else {
            button("Close", false);
            getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
        }
        
        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Keys.ESCAPE) {
                    filterOptions.name = "";
                    sortBySelectedMode();
                    return true;
                }
                return false;
            }
        });
    }
    
    public class AddClickListener extends PopTableClickListener {
        public AddClickListener() {
            super(getSkin(), "more");
            
            populate();
        }
        
        private void populate() {
            var table = getPopTable();
            table.clearChildren();
    
            table.pad(10);
            table.defaults().space(10).fillX();
            var hideListener = new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    table.hide();
                }
            };
            
            var textButton = new TextButton("Image File", getSkin(), "new");
            table.add(textButton);
            textButton.addListener(main.getHandListener());
            textButton.addListener(hideListener);
            textButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                    newDrawableDialog();
                }
            });
    
            if (showing9patchButton) {
                table.row();
                textButton = new TextButton("Custom Placeholder", getSkin(), "new");
                table.add(textButton);
                textButton.addListener(main.getHandListener());
                textButton.addListener(hideListener);
                textButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                        customDrawableDialog();
                    }
                });
    
                table.row();
                textButton = new TextButton("Create 9-Patch", getSkin(), "new");
                table.add(textButton);
                textButton.addListener(main.getHandListener());
                textButton.addListener(hideListener);
                textButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                        main.getDesktopWorker().removeFilesDroppedListener(filesDroppedListener);
                        main.getDialogFactory().showDialog9Patch(main.getAtlasData().getDrawablePairs(),
                                new Dialog9Patch.Dialog9PatchListener() {
                                    @Override
                                    public void fileSaved(FileHandle fileHandle) {
                                        if (fileHandle.exists()) {
                                            drawablesSelected(new Array<>(new FileHandle[]{fileHandle}));
                                            main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);
                                        }
                                    }
                            
                                    @Override
                                    public void cancelled() {
                                        main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);
                                    }
                                });
                    }
                });
            }
        }
    }

    @Override
    public Dialog show(Stage stage, Action action) {
        Dialog dialog = super.show(stage, action);
        stage.setScrollFocus(scrollPane);
        validate();
        scrollPane.setScrollY(scrollPosition);
        
        fire(new DialogEvent(DialogEvent.Type.OPEN));
        return dialog;
    }
    
    private void refreshDrawableDisplay() {
        contentTable.clear();
        
        if (drawables.size == 0) {
            Label label = new Label("No drawables have been added!", getSkin());
            if (main.getAtlasData().getDrawables().size > 0) {
                label.setText("No drawables match filter!");
            }
            contentTable.add(label);
        } else {
            if (MathUtils.isZero(zoomSlider.getValue())) {
                refreshDrawableDisplayDetail();
            } else {
                refreshDrawableDisplayNormal();
            }
        }
    }
    
    private void refreshDrawableDisplayDetail() {
        contentTable.pad(5);
        contentTable.defaults().space(3);
        for (var drawable: drawables) {
            Button drawableButton;
            
            if (property != null || customProperty != null) {
                drawableButton = new Button(getSkin(), "color-base");
                drawableButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        result(drawable);
                        hide();
                    }
                });
                drawableButton.addListener(main.getHandListener());
            } else {
                drawableButton = new Button(getSkin(), "color-base-static");
            }
            contentTable.add(drawableButton).fillX();
            contentTable.row();
            
            Table table = new Table();
            drawableButton.add(table).growX();
            
            table.defaults().space(10);
            //preview
            Container bg = new Container();
            bg.setClip(true);
            bg.setBackground(getSkin().getDrawable("white"));
            bg.setColor(drawable.bgColor);
            
            Image image = new Image(main.getAtlasData().getDrawablePairs().get(drawable));
            if (MathUtils.isEqual(zoomSlider.getValue(), 1)) {
                image.setScaling(Scaling.fit);
                bg.fill(false);
            } else {
                image.setScaling(Scaling.stretch);
                bg.fill();
            }
            bg.setActor(image);
            table.add(bg).size(sizes[MathUtils.floor(zoomSlider.getValue())]);
            
            //name
            var label = new Label(drawable.name, getSkin());
            label.setAlignment(Align.left);
            label.setEllipsis("...");
            label.setEllipsis(true);
            table.add(label);
    
            label = new Label(drawable.type.formattedName, getSkin());
            table.add(label).right().expandX().spaceLeft(50);
            
            if (showingOptions) {
                //more button
                var button = new Button(getSkin(),  "more");
                table.add(button);
                button.addListener(main.getHandListener());
                button.addListener(new MoreClickListener(drawable));
                //prevent click from activating parent button.
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        event.setBubbles(false);
                    }
                });
            }
            
            //Tooltip
            var toolTip = new TextTooltip(drawable.name, main.getTooltipManager(), getSkin());
            label.addListener(toolTip);
        }
    }
    
    private void refreshDrawableDisplayNormal() {
        var contentGroup = new HorizontalGroup();
        contentGroup.center().wrap(true).space(5.0f).wrapSpace(5.0f).rowAlign(Align.left);
        contentTable.add(contentGroup).grow();
    
        for (var drawable : drawables) {
            Button drawableButton;
        
            if (property != null || customProperty != null) {
                drawableButton = new Button(getSkin(), "color-base");
                drawableButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        result(drawable);
                        hide();
                    }
                });
                drawableButton.addListener(main.getHandListener());
            } else {
                drawableButton = new Button(getSkin(), "color-base-static");
            }
            contentGroup.addActor(drawableButton);
        
            Table table = new Table();
            drawableButton.add(table).width(sizes[MathUtils.floor(zoomSlider.getValue())]).height(sizes[MathUtils.floor(zoomSlider.getValue())]);
        
            var subTable = new Table();
            table.add(subTable).growX();
        
            var label = new Label(drawable.type == null ? "error" : drawable.type.formattedName, getSkin());
            subTable.add(label);
            
            if (showingOptions) {
                //more button
                var button = new Button(getSkin(),  "more");
                subTable.add(button).right().expandX();
                button.addListener(main.getHandListener());
                button.addListener(new MoreClickListener(drawable));
                //prevent click from activating parent button.
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        event.setBubbles(false);
                    }
                });
            }
        
            //preview
            table.row();
            Container bg = new Container();
            bg.setClip(true);
            bg.setBackground(getSkin().getDrawable("white"));
            bg.setColor(drawable.bgColor);
        
            Image image = new Image(main.getAtlasData().getDrawablePairs().get(drawable));
            if (MathUtils.isEqual(zoomSlider.getValue(), 1)) {
                image.setScaling(Scaling.fit);
                bg.fill(false);
            } else {
                image.setScaling(Scaling.stretch);
                bg.fill();
            }
            bg.setActor(image);
            table.add(bg).grow();
        
            //name
            table.row();
            label = new Label(drawable.name, getSkin());
            label.setEllipsis("...");
            label.setEllipsis(true);
            label.setAlignment(Align.center);
            table.add(label).colspan(6).growX().width(sizes[MathUtils.floor(zoomSlider.getValue())]);
        
            //Tooltip
            Tooltip toolTip = new TextTooltip(drawable.name, main.getTooltipManager(), getSkin());
            label.addListener(toolTip);
        }
    }
    
    private class MoreClickListener extends PopTableClickListener {
        public MoreClickListener(DrawableData drawable) {
            super(getSkin(), "more");
            
            var hideListener = new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    getPopTable().hide();
                    getPopTable().removeAttachToActor();
                }
            };
            
            var root = getPopTable();
    
            root.pad(10);
            root.defaults().expandX().left();
            
            switch (drawable.type) {
                case TEXTURE:
                case NINE_PATCH:
                    //color wheel
                    var button = new ImageTextButton("New Tinted Drawable", getSkin(), "colorwheel");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            newTintedDrawable(drawable);
                        }
                    });
    
                    //swatches
                    button = new ImageTextButton("New Tinted Drawable from Colors", getSkin(), "swatches");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            colorSwatchesDialog(drawable);
                        }
                    });
    
                    //tiles button
                    button = new ImageTextButton("New Tiled Drawable", getSkin(), "tiles");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event,
                                            Actor actor) {
                            DrawableData tiledDrawable = new DrawableData();
                            tiledDrawable.type = DrawableType.TILED;
                            tiledDrawable.name = drawable.name;
                            tiledDrawable.file = drawable.file;
                            tiledDrawable.tiled = true;
                            Vector2 dimensions = Utils.imageDimensions(drawable.file);
                            tiledDrawable.minWidth = dimensions.x;
                            tiledDrawable.minHeight = dimensions.y;
                            tiledDrawableSettingsDialog("New Tiled Drawable", tiledDrawable, true);
                        }
                    });
    
                    //tenpatch button
                    button = new ImageTextButton("New Tenpatch Drawable", getSkin(), "tenpatch");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            var drawableData = new DrawableData();
                            drawableData.type = DrawableType.TENPATCH;
                            drawableData.name = drawable.name;
                            drawableData.file = drawable.file;
                            drawableData.bgColor = drawable.bgColor;
                            drawableData.tenPatchData = new TenPatchData();
            
                            main.getDesktopWorker().removeFilesDroppedListener(filesDroppedListener);
                            main.getDialogFactory().showDialogTenPatch(drawableData, true, new DialogTenPatch.DialogTenPatchListener() {
                                @Override
                                public void selected(DrawableData drawableData) {
                                    main.getProjectData().getAtlasData().getDrawables().add(drawableData);
                                    main.getProjectData().setChangesSaved(false);
                                    gatherDrawables();
                                    main.getAtlasData().produceAtlas();
                                    sortBySelectedMode();
                                    getStage().setScrollFocus(scrollPane);
                                    main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);
                                    refreshDrawableDisplay();
                                }
                
                                @Override
                                public void cancelled() {
                                    main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);
                                    refreshDrawableDisplay();
                                }
                            });
                        }
                    });
                    
                    //settings
                    button = new ImageTextButton("Settings", getSkin(), "settings-small");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            main.getDialogFactory().showDrawableSettingsDialog(getSkin(), getStage(), drawable, (boolean accepted) -> {
                                if (accepted) {
                                    main.getAtlasData().produceAtlas();
                                    refreshDrawableDisplay();
                                }
                            });
                        }
                    });
                    break;
                case TILED:
                    //duplicate
                    button = new ImageTextButton("Duplicate", getSkin(), "duplicate-small");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            var drawableData = new DrawableData();
                            drawableData.set(drawable);
            
                            main.getDialogFactory().showDuplicateDialog("Duplicate Tiled Drawable", "Please enter the name of the duplicated drawable", drawable.name, new DialogFactory.InputDialogListener() {
                                @Override
                                public void confirmed(String text) {
                                    drawableData.name = text;
                                    main.getAtlasData().getDrawables().add(drawableData);
                                    gatherDrawables();
                                    main.getAtlasData().produceAtlas();
                                    sortBySelectedMode();
                                }
                
                                @Override
                                public void cancelled() {
                    
                                }
                            });
                        }
                    });
                    
                    //settings
                    button = new ImageTextButton("Settings", getSkin(), "settings-small");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            tiledDrawableSettingsDialog("Tiled Drawable Settings", drawable, false);
                        }
                    });
                    break;
                case TINTED:
                case TINTED_FROM_COLOR_DATA:
                    //duplicate
                    button = new ImageTextButton("Duplicate", getSkin(), "duplicate-small");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            var drawableData = new DrawableData();
                            drawableData.set(drawable);
            
                            main.getDialogFactory().showDuplicateDialog("Duplicate Tiled Drawable", "Please enter the name of the duplicated drawable", drawable.name, new DialogFactory.InputDialogListener() {
                                @Override
                                public void confirmed(String text) {
                                    drawableData.name = text;
                                    main.getAtlasData().getDrawables().add(drawableData);
                                    gatherDrawables();
                                    main.getAtlasData().produceAtlas();
                                    sortBySelectedMode();
                                }
                
                                @Override
                                public void cancelled() {
                    
                                }
                            });
                        }
                    });
                    
                    //settings
                    button = new ImageTextButton("Settings", getSkin(), "settings-small");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            tintedDrawableSettingsDialog(drawable);
                        }
                    });
                    break;
                case CUSTOM:
                    //settings
                    button = new ImageTextButton("Rename", getSkin(), "settings-small");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            renameCustomDrawableDialog(drawable);
                        }
                    });
                    break;
                case TENPATCH:
                    //settings
                    button = new ImageTextButton("Settings", getSkin(), "settings-small");
                    root.add(button);
                    root.row();
                    button.addListener(hideListener);
                    button.addListener(main.getHandListener());
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            var drawableData = new DrawableData(drawable);
            
                            main.getDesktopWorker().removeFilesDroppedListener(filesDroppedListener);
                            main.getDialogFactory().showDialogTenPatch(drawableData, false, new DialogTenPatch.DialogTenPatchListener() {
                                @Override
                                public void selected(DrawableData drawableData) {
                                    drawable.set(drawableData);
                                    main.getProjectData().setChangesSaved(false);
                                    gatherDrawables();
                                    main.getAtlasData().produceAtlas();
                                    sortBySelectedMode();
                                    getStage().setScrollFocus(scrollPane);
                                    main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);
                                    refreshDrawableDisplay();
                                }
                
                                @Override
                                public void cancelled() {
                                    main.getDesktopWorker().addFilesDroppedListener(filesDroppedListener);
                                    refreshDrawableDisplay();
                                }
                            });
                        }
                    });
    
                    //duplicate button for an existing ten patch
                    button = new ImageTextButton("Duplicate", getSkin(), "duplicate-small");
                    root.add(button);
                    root.row();
                    button.addListener(main.getHandListener());
                    button.addListener(hideListener);
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            var drawableData = new DrawableData();
                            drawableData.set(drawable);
            
                            main.getDialogFactory().showDuplicateDialog("Duplicate Ten Patch Drawable", "Please enter the name of the duplicated ten patch drawable", drawable.name, new DialogFactory.InputDialogListener() {
                                @Override
                                public void confirmed(String text) {
                                    drawableData.name = text;
                                    main.getAtlasData().getDrawables().add(drawableData);
                                    gatherDrawables();
                                    main.getAtlasData().produceAtlas();
                                    sortBySelectedMode();
                                }
                
                                @Override
                                public void cancelled() {
                    
                                }
                            });
                        }
                    });
    
                    break;
            }
    
            //visible
            var button = new ImageTextButton("Visible", getSkin(), "visible");
            button.setProgrammaticChangeEvents(false);
            button.setChecked(drawable.hidden);
            root.add(button);
            root.row();
            button.addListener(main.getHandListener());
            button.addListener(hideListener);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                    drawable.hidden = ((ImageTextButton) actor).isChecked();
                    main.getProjectData().setChangesSaved(false);
    
                    gatherDrawables();
                    sortBySelectedMode();
                }
            });
            
            //delete
            button = new ImageTextButton("Delete", getSkin(), "delete-small");
            root.add(button);
            button.addListener(hideListener);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                    deleteDrawable(drawable);
                }
            });
            button.addListener(main.getHandListener());
        }
    
        @Override
        public void clicked(InputEvent event, float x, float y) {
            super.clicked(event, x, y);
            event.setBubbles(false);
        }
    
        @Override
        public void tableShown(Event event) {
            getStage().setScrollFocus(null);
        }
    }
    
    private void colorSwatchesDialog(DrawableData drawableData) {
        DialogColors dialog = new DialogColors(main, (StyleProperty) null, true, (colorData, pressedCancel) -> {
            if (colorData != null) {
                final DrawableData tintedDrawable = new DrawableData(drawableData.file);
                tintedDrawable.type = DrawableType.TINTED_FROM_COLOR_DATA;
                tintedDrawable.tintName = colorData.getName();

                //Fix background color for new, tinted drawable
                Color temp = Utils.averageEdgeColor(tintedDrawable.file, colorData.color);

                if (Utils.brightness(temp) > .5f) {
                    tintedDrawable.bgColor = Color.BLACK;
                } else {
                    tintedDrawable.bgColor = Color.WHITE;
                }

                final TextField textField = new TextField(drawableData.name, getSkin());
                final TextButton button = new TextButton("OK", getSkin());
                button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                button.addListener(main.getHandListener());
                textField.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event,
                            Actor actor) {
                        button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                    }
                });
                textField.addListener(main.getIbeamListener());

                Dialog approveDialog = new Dialog("TintedDrawable...", getSkin(), "bg") {
                    @Override
                    protected void result(Object object) {
                        if (object instanceof Boolean && (boolean) object) {
                            tintedDrawable.name = textField.getText();
                            main.getAtlasData().getDrawables().add(tintedDrawable);
                            main.getProjectData().setChangesSaved(false);
                        }
                    }

                    @Override
                    public boolean remove() {
                        gatherDrawables();
                        main.getAtlasData().produceAtlas();
                        sortBySelectedMode();
                        getStage().setScrollFocus(scrollPane);
                        return super.remove();
                    }
                };
                approveDialog.addCaptureListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, int keycode2) {
                        if (keycode2 == Input.Keys.ENTER) {
                            if (!button.isDisabled()) {
                                tintedDrawable.name = textField.getText();
                                main.getAtlasData().getDrawables().add(tintedDrawable);
                                main.getProjectData().setChangesSaved(false);
                                approveDialog.hide();
                            }
                        }
                        return false;
                    }
                });

                approveDialog.getTitleTable().padLeft(5.0f);
                approveDialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
                approveDialog.getButtonTable().padBottom(15.0f);

                approveDialog.text("What is the name of the new tinted drawable?");

                Drawable drawable = main.getAtlasData().getDrawablePairs().get(drawableData);
                Drawable preview = null;
                if (drawable instanceof SpriteDrawable) {
                    preview = ((SpriteDrawable) drawable).tint(colorData.color);
                } else if (drawable instanceof NinePatchDrawable) {
                    preview = ((NinePatchDrawable) drawable).tint(colorData.color);
                }
                if (preview != null) {
                    approveDialog.getContentTable().row();
                    Table table = new Table();
                    table.setBackground(preview);
                    approveDialog.getContentTable().add(table);
                }

                approveDialog.getContentTable().row();
                approveDialog.getContentTable().add(textField).growX();

                approveDialog.button(button, true);
                approveDialog.button("Cancel", false);
                approveDialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
                approveDialog.key(Input.Keys.ESCAPE, false);
                approveDialog.show(getStage());
                getStage().setKeyboardFocus(textField);
                textField.selectAll();

                textField.setFocusTraversal(false);
            }
        });
        dialog.setFillParent(true);
        dialog.show(getStage());
        dialog.refreshTable();
    }
    
    private void tintedDrawableSettingsDialog(DrawableData drawable) {
        TextField textField = new TextField("", getSkin());
        var dialog = new Dialog("Rename drawable?", getSkin(), "bg") {
            @Override
            protected void result(Object object) {
                super.result(object);
                
                if (object instanceof Boolean && (boolean) object == true) {
                    applyTintedDrawableSettings(drawable, textField.getText());
                    drawable.minWidth = ((Spinner) findActor("minWidth")).getValueAsInt();
                    drawable.minHeight = ((Spinner) findActor("minHeight")).getValueAsInt();
                    main.getAtlasData().produceAtlas();
                    refreshDrawableDisplay();
                }
                getStage().setScrollFocus(scrollPane);
            }
            
            public void callResult(Object object) {
                result(object);
            }

            @Override
            public Dialog show(Stage stage) {
                Dialog dialog = super.show(stage);
                stage.setKeyboardFocus(textField);
                return dialog;
            }
        };
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        dialog.getContentTable().add(new Label("Please enter a new name for the drawable: ", getSkin()));
        
        dialog.button("OK", true);
        dialog.button("Cancel", false).key(Keys.ESCAPE, false);
        TextButton okButton = (TextButton) dialog.getButtonTable().getCells().first().getActor();
        okButton.addListener(main.getHandListener());
        dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        
        dialog.getContentTable().row();
        textField.setText(drawable.name);
        textField.selectAll();
        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                boolean disable = !DrawableData.validate(textField.getText()) || (checkIfNameExists(textField.getText()) && !drawable.name.equals(textField.getText()));
                okButton.setDisabled(disable);
            }
        });
        textField.setTextFieldListener((TextField textField1, char c) -> {
            if (c == '\n') {
                if (!okButton.isDisabled()) {
                    dialog.callResult(true);
                    dialog.hide();
                }
            }
        });
        textField.addListener(main.getIbeamListener());
        dialog.getContentTable().add(textField);
        
        dialog.getContentTable().row();
        var table = new Table();
        dialog.getContentTable().add(table);
        
        table.defaults().space(3);
        var label = new Label("Set values to -1 to disable.", getSkin());
        table.add(label).colspan(2).padTop(7);
        
        table.row();
        label = new Label("minWidth:", getSkin());
        table.add(label);
        
        var spinner = new Spinner(drawable.minWidth, 1, true, Spinner.Orientation.HORIZONTAL, getSkin());
        spinner.setMinimum(-1);
        spinner.setName("minWidth");
        table.add(spinner).width(100);
        spinner.getButtonMinus().addListener(main.getHandListener());
        spinner.getButtonPlus().addListener(main.getHandListener());
        spinner.getTextField().addListener(main.getIbeamListener());
        
        table.row();
        label = new Label("minHeight:", getSkin());
        table.add(label);
        
        spinner = new Spinner(drawable.minHeight, 1, true, Spinner.Orientation.HORIZONTAL, getSkin());
        spinner.setMinimum(-1);
        spinner.setName("minHeight");
        table.add(spinner).width(100);
        spinner.getButtonMinus().addListener(main.getHandListener());
        spinner.getButtonPlus().addListener(main.getHandListener());
        spinner.getTextField().addListener(main.getIbeamListener());
        
        textField.setFocusTraversal(false);
        
        dialog.show(getStage());
    }
    
    private void applyTintedDrawableSettings(DrawableData drawable, String name) {
        String oldName = drawable.name;
        drawable.name = name;

        main.getUndoableManager().clearUndoables();
        updateStyleValuesForRename(oldName, name);
        
        main.getRootTable().refreshStyleProperties(true);
        main.getAtlasData().produceAtlas();
        main.getRootTable().refreshPreview();
        
        main.getProjectData().setChangesSaved(false);
        
        sortBySelectedMode();
    }
    
    private void tiledDrawableSettingsDialog(String title, DrawableData drawable, boolean newDrawable) {
        final Spinner minWidthSpinner = new Spinner(0.0f, 1.0f, true, Spinner.Orientation.HORIZONTAL, getSkin());
        final Spinner minHeightSpinner = new Spinner(0.0f, 1.0f, true, Spinner.Orientation.HORIZONTAL, getSkin());
        TextField textField = new TextField("", getSkin()) {
            @Override
            public void next(boolean up) {
                if (up) {
                    getStage().setKeyboardFocus(minHeightSpinner.getTextField());
                    minHeightSpinner.getTextField().selectAll();
                } else {
                    getStage().setKeyboardFocus(minWidthSpinner.getTextField());
                    minWidthSpinner.getTextField().selectAll();
                }
            }

        };
        Dialog tileDialog = new Dialog(title, getSkin(), "bg") {
            @Override
            protected void result(Object object) {
                super.result(object);

                if (object instanceof Boolean && (boolean) object == true) {
                    Button button = this.findActor("color-selector");
                    tiledDrawableSettings(drawable, (ColorData) button.getUserObject(), (float) minWidthSpinner.getValue(), (float) minHeightSpinner.getValue(), textField.getText());
                }
                getStage().setScrollFocus(scrollPane);
            }

            @Override
            public Dialog show(Stage stage) {
                Dialog dialog = super.show(stage);
                stage.setKeyboardFocus(textField);
                return dialog;
            }
        };

        tileDialog.getTitleTable().padLeft(5.0f);
        tileDialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        tileDialog.getButtonTable().padBottom(15.0f);

        var label = new Label("Please enter a name for the TiledDrawable: ", getSkin());
        label.setName("name-label");
        tileDialog.getContentTable().add(label);

        tileDialog.button("OK", true);
        tileDialog.button("Cancel", false).key(Keys.ESCAPE, false);
        TextButton okButton = (TextButton) tileDialog.getButtonTable().getCells().first().getActor();
        okButton.addListener(main.getHandListener());
        tileDialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        
        tileDialog.getContentTable().row();
        var table = new Table();
        table.defaults().space(10.0f);
        tileDialog.getContentTable().add(table);
        
        textField.setText(drawable.name);
        textField.selectAll();
        table.add(textField).growX().colspan(2);
        
        table.row();
        label = new Label("Color:", getSkin());
        label.setName("color-label");
        table.add(label).right();
        
        var subTable = new Table();
        subTable.setBackground(getSkin().getDrawable("dark-gray"));
        table.add(subTable).growX().height(35);

        var button = new Button(getSkin(), "color-selector");
        button.setName("color-selector");
        if (drawable.tintName != null) {
            button.setColor(main.getJsonData().getColorByName(drawable.tintName).color);
        } else {
            label = new Label("none", getSkin());
            button.add(label);
        }
        button.setUserObject(main.getJsonData().getColorByName(drawable.tintName));
        subTable.add(button).grow().pad(3);
        button.addListener(main.getHandListener());
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent ce, Actor actor) {
                DialogColors dialog = new DialogColors(main, (StyleProperty) null, true, (colorData, pressedCancel) -> {
                    Button button = tileDialog.findActor("color-selector");
                    if (colorData != null) {
                        button.setColor(colorData.color);
                        button.setUserObject(colorData);
                        button.clearChildren();
                    } else {
                        if (button.getUserObject() != null) {
                            button.setColor(((ColorData) button.getUserObject()).color);
                        } else {
                            button.setColor(Color.WHITE);
                        }
                    }
                    okButton.setDisabled(!validateTiledDrawable(tileDialog, textField.getText(), drawable.name, (ColorData) button.getUserObject(), newDrawable));
                });
                dialog.setFillParent(true);
                dialog.show(getStage());
                dialog.refreshTable();
            }
        });
        
        table.row();
        label = new Label("MinWidth:", getSkin());
        table.add(label).right();
        minWidthSpinner.setValue(drawable.minWidth);
        minWidthSpinner.setMinimum(0.0f);
        table.add(minWidthSpinner).minWidth(150.0f);
        minWidthSpinner.setTransversalPrevious(textField);
        minWidthSpinner.setTransversalNext(minHeightSpinner.getTextField());
        minWidthSpinner.getButtonMinus().addListener(main.getHandListener());
        minWidthSpinner.getButtonPlus().addListener(main.getHandListener());
        minWidthSpinner.getTextField().addListener(main.getIbeamListener());

        table.row();
        label = new Label("MinHeight:", getSkin());
        table.add(label).right();
        minHeightSpinner.setValue(drawable.minHeight);
        minHeightSpinner.setMinimum(0.0f);
        table.add(minHeightSpinner).minWidth(150.0f);
        minHeightSpinner.setTransversalPrevious(minWidthSpinner.getTextField());
        minHeightSpinner.setTransversalNext(textField);
        minHeightSpinner.getButtonMinus().addListener(main.getHandListener());
        minHeightSpinner.getButtonPlus().addListener(main.getHandListener());
        minHeightSpinner.getTextField().addListener(main.getIbeamListener());

        okButton.setDisabled(!validateTiledDrawable(tileDialog, textField.getText(), drawable.name, (ColorData) button.getUserObject(), newDrawable));
        
        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event,
                    Actor actor) {
                
                Button button = tileDialog.findActor("color-selector");
                
                okButton.setDisabled(!validateTiledDrawable(tileDialog, textField.getText(), drawable.name, (ColorData) button.getUserObject(), newDrawable));
                
            }
        });
        textField.setTextFieldListener((TextField textField1, char c) -> {
            if (c == '\n') {
                if (!okButton.isDisabled()) {
                    Button button1 = tileDialog.findActor("color-selector");
                    tiledDrawableSettings(drawable, (ColorData) button1.getUserObject(), (float) minWidthSpinner.getValue(), (float) minHeightSpinner.getValue(), textField1.getText());
                    tileDialog.hide();
                }
            }
        });
        textField.addListener(main.getIbeamListener());

        tileDialog.show(getStage());
    }
    
    private boolean validateTiledDrawable(Dialog dialog, String newName, String oldName, ColorData colorData, boolean newDrawable) {
        boolean returnValue = true;
        String requiredLabelName = null;
                
        if (!DrawableData.validate(newName) || checkIfNameExists(newName)) {
            if (newDrawable || !newName.equals(oldName)) {
                requiredLabelName = "name-label";
                returnValue = false;
            }
            
        } else if (colorData == null) {
            requiredLabelName = "color-label";
            returnValue = false;
        }
        
        var normalStyle = getSkin().get(Label.LabelStyle.class);
        var requiredStyle = getSkin().get("required", Label.LabelStyle.class);
        var actors = new Array<Actor>();
        actors.addAll(dialog.getChildren());
        
        for (int i = 0; i < actors.size; i++) {
            var actor = actors.get(i);
            
            if (actor instanceof Group) {
                actors.addAll(((Group) actor).getChildren());
            }
            
            if (actor instanceof Label) {
                Label label = (Label) actor;
                
                if (label.getStyle().equals(requiredStyle)) {
                    label.setStyle(normalStyle);
                }
                
                if (requiredLabelName != null && label.getName() != null && label.getName().equals(requiredLabelName)) {
                    label.setStyle(requiredStyle);
                }
            }
        }
        
        return returnValue;
    }
    
    private void tiledDrawableSettings(DrawableData drawable, ColorData colorData, float minWidth, float minHeight, String name) {
        drawable.name = name;
        drawable.tintName = colorData.getName();
        drawable.minWidth = minWidth;
        drawable.minHeight = minHeight;
        
        //Fix background color for new, tinted drawable
        Color temp = Utils.averageEdgeColor(drawable.file, colorData.color);

        if (Utils.brightness(temp) > .5f) {
            drawable.bgColor = Color.BLACK;
        } else {
            drawable.bgColor = Color.WHITE;
        }
        
        if (!main.getAtlasData().getDrawables().contains(drawable, false)) {
            main.getAtlasData().getDrawables().add(drawable);
        }
        main.getProjectData().setChangesSaved(false);
        gatherDrawables();
        main.getAtlasData().produceAtlas();
        sortBySelectedMode();
        getStage().setScrollFocus(scrollPane);
    }
    
    private void updateStyleValuesForRename(String oldName, String newName) {
        Values<Array<StyleData>> values = main.getJsonData().getClassStyleMap().values();
        for (Array<StyleData> styles : values) {
            for (StyleData style : styles) {
                for (StyleProperty styleProperty : style.properties.values()) {
                    if (ClassReflection.isAssignableFrom(Drawable.class, styleProperty.type)) {
                        if (styleProperty.value != null && styleProperty.value.equals(oldName)) {
                            styleProperty.value = newName;
                        }
                    }
                }
            }
        }
    }
    
    private void deleteDrawable(DrawableData drawable) {
        if (!drawable.customized && drawable.tint == null && drawable.tintName == null && drawable.tenPatchData == null && checkDuplicateDrawables(drawable.file, 1)) {
            showConfirmDeleteDialog(drawable);
        } else {
            removeRegionFromTenPatches(drawable);
            main.getAtlasData().getDrawables().removeValue(drawable, true);

            for (Array<StyleData> datas : main.getJsonData().getClassStyleMap().values()) {
                for (StyleData data : datas) {
                    for (StyleProperty styleProperty : data.properties.values()) {
                        if (styleProperty != null && styleProperty.type.equals(Drawable.class) && styleProperty.value != null && styleProperty.value.equals(drawable.toString())) {
                            styleProperty.value = null;
                        }
                    }
                }
            }

            main.getRootTable().refreshStyleProperties(true);
            main.getRootTable().refreshPreview();

            main.getUndoableManager().clearUndoables();
            
            main.getProjectData().setChangesSaved(false);

            gatherDrawables();
            sortBySelectedMode();
        }
    }

    /**
     * Shows a dialog to confirm deletion of all TintedDrawables based on the
     * provided drawable data. This is called when the delete button is pressed
     * on a drawable in the drawable list.
     * @param drawable 
     */
    private void showConfirmDeleteDialog(DrawableData drawable) {
        Dialog dialog = new Dialog("Delete duplicates?", getSkin(), "bg"){
            @Override
            protected void result(Object object) {
                if ((boolean) object) {
                    main.getProjectData().setChangesSaved(false);
                    removeDuplicateDrawables(drawable.file);
                    removeRegionFromTenPatches(drawable);
                    gatherDrawables();
                    sortBySelectedMode();
                }
            }
        };
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        dialog.text("Deleting this drawable will also delete one or more derivative drawables.\n"
                + "Delete duplicates?");
        dialog.button("OK", true);
        dialog.button("Cancel", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
        dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(getStage());
    }
    
    private void removeRegionFromTenPatches(DrawableData drawable) {
        for (var data : main.getAtlasData().getDrawables()) {
            if (data.tenPatchData != null) {
                var iter = data.tenPatchData.regionNames.iterator();
                while (iter.hasNext()) {
                    String name = iter.next();
                    if (name.equals(drawable.name)) {
                        iter.remove();
                    }
                }
                
                if (data.tenPatchData.regions != null) {
                    var iter2 = data.tenPatchData.regions.iterator();
                    while (iter2.hasNext()) {
                        var region = iter2.next();
                        if (((TextureAtlas.AtlasRegion)region).name.equals(drawable.name)) {
                            iter2.remove();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Sorts by selected sort order and populates the list.
     */
    private void sortBySelectedMode() {
        gatherDrawables();
        applyFilterOptions();
        
        switch (sortSelectBox.getSelectedIndex()) {
            case 0:
                sortDrawablesAZ();
                break;
            case 1:
                sortDrawablesZA();
                break;
            case 2:
                sortDrawablesOldest();
                break;
            case 3:
                sortDrawablesNewest();
                break;
        }
    }
    
    private void applyFilterOptions() {
        var iter = drawables.iterator();
        while (iter.hasNext()) {
            var drawable = iter.next();
            
            if (!filterOptions.regularExpression) {
                if (!filterOptions.name.equals("") && !drawable.name.contains(filterOptions.name.toLowerCase(Locale.ROOT))) {
                    iter.remove();
                    continue;
                }
            } else {
                if (!drawable.name.matches(filterOptions.name)) {
                    iter.remove();
                }
            }
            
            if (!filterOptions.custom) {
                if (drawable.type == DrawableType.CUSTOM) {
                    iter.remove();
                    continue;
                }
            }
            
            if (!filterOptions.ninePatch) {
                if (drawable.type == DrawableType.NINE_PATCH) {
                    iter.remove();
                    continue;
                }
            }
            
            if (!filterOptions.texture) {
                if (drawable.type == DrawableType.TEXTURE) {
                    iter.remove();
                    continue;
                }
            }
            
            if (!filterOptions.tiled) {
                if (drawable.type == DrawableType.TILED) {
                    iter.remove();
                    continue;
                }
            }
            
            if (!filterOptions.tinted) {
                if (drawable.type == DrawableType.TINTED || drawable.type == DrawableType.TINTED_FROM_COLOR_DATA) {
                    iter.remove();
                    continue;
                }
            }
            
            if (!filterOptions.tenPatch) {
                if (drawable.type == DrawableType.TENPATCH) {
                    iter.remove();
                    continue;
                }
            }
            
            if (!filterOptions.hidden) {
                if (drawable.hidden) {
                    iter.remove();
                }
            }
        }
    }
    
    /**
     * Sorts alphabetically from A to Z.
     */
    private void sortDrawablesAZ() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
        refreshDrawableDisplay();
    }
    
    /**
     * Sorts alphabetically from Z to A.
     */
    private void sortDrawablesZA() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> o1.toString().compareToIgnoreCase(o2.toString()) * -1);
        refreshDrawableDisplay();
    }
    
    /**
     * Sorts by modified date with oldest first.
     */
    private void sortDrawablesOldest() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> {
            if (o1.file.lastModified() < o2.file.lastModified()) {
                return -1;
            } else if (o1.file.lastModified() > o2.file.lastModified()) {
                return 1;
            } else {
                return 0;
            }
        });
        refreshDrawableDisplay();
    }
    
    /**
     * Sorts by modified date with newest first.
     */
    private void sortDrawablesNewest() {
        Sort.instance().sort(drawables, (DrawableData o1, DrawableData o2) -> {
            if (o1.file.lastModified() < o2.file.lastModified()) {
                return 1;
            } else if (o1.file.lastModified() > o2.file.lastModified()) {
                return -1;
            } else {
                return 0;
            }
        });
        refreshDrawableDisplay();
    }
    
    /**
     * Checks if there are any drawables that have the same file name as the specified file.
     * This ignores the file extension.
     * @param handle
     * @param minimum The minimum allowed matches before it's considered a duplicate
     * @return 
     */
    private boolean checkDuplicateDrawables(FileHandle handle, int minimum) {
        int count = 0;
        String name = DrawableData.proper(handle.name());
        for (int i = 0; i < main.getAtlasData().getDrawables().size; i++) {
            DrawableData data = main.getAtlasData().getDrawables().get(i);
            if (data.file != null && name.equals(DrawableData.proper(data.file.name()))) {
                count++;
            }
        }
        
        return count > minimum;
    }
    
    private boolean checkDuplicateDrawables(String name, int minimum) {
        int count = 0;
        for (int i = 0; i < main.getAtlasData().getDrawables().size; i++) {
            DrawableData data = main.getAtlasData().getDrawables().get(i);
            if (data.name != null && name.equals(data.name)) {
                count++;
            }
        }
        
        return count > minimum;
    }
    
    private boolean checkDuplicateFontDrawables(String name, int minimum) {
        int count = 0;
        for (int i = 0; i < main.getAtlasData().getFontDrawables().size; i++) {
            DrawableData data = main.getAtlasData().getFontDrawables().get(i);
            if (data.name != null && name.equals(data.name)) {
                count++;
            }
        }
        
        return count > minimum;
    }
    
    /**
     * Removes any duplicate drawables that share the same file name. This
     * ignores the file extension and also deletes TintedDrawables from the
     * same file. Calls removeDuplicateDrawables(handle, true).
     * @param handle 
     */
    private void removeDuplicateDrawables(FileHandle handle) {
        removeDuplicateDrawables(handle, true);
    }
    
    /**
     * Removes any duplicate drawables that share the same file name. This
     * ignores the file extension and also deletes TintedDrawables from the
     * same file.
     * @param handle 
     */
    private void removeDuplicateDrawables(FileHandle handle, boolean deleteStyleValues) {
        boolean refreshDrawables = false;
        String name = DrawableData.proper(handle.name());
        for (int i = 0; i < main.getAtlasData().getDrawables().size; i++) {
            DrawableData data = main.getAtlasData().getDrawables().get(i);
            if (name.equals(DrawableData.proper(data.file.name()))) {
                main.getAtlasData().getDrawables().removeValue(data, true);
                
                if (deleteStyleValues) {
                    for (Array<StyleData> datas : main.getJsonData().getClassStyleMap().values()) {
                        for (StyleData tempData : datas) {
                            for (StyleProperty prop : tempData.properties.values()) {
                                if (prop != null && prop.type.equals(Drawable.class) && prop.value != null && prop.value.equals(data.toString())) {
                                    prop.value = null;
                                }
                            }
                        }
                    }
                }
                
                refreshDrawables = true;
                i--;
            }
        }
        
        main.getRootTable().refreshStyleProperties(true);
        main.getRootTable().refreshPreview();
        
        if (refreshDrawables) {
            gatherDrawables();
        }
    }
    
    /**
     * Removes any duplicate drawables that share the same name. This does not
     * delete TintedDrawables from the same file.
     */
    private void removeDuplicateDrawables(String name, boolean deleteStyleValues) {
        boolean refreshDrawables = false;
        for (int i = 0; i < main.getAtlasData().getDrawables().size; i++) {
            DrawableData data = main.getAtlasData().getDrawables().get(i);
            if (data.name != null && name.equals(data.name)) {
                main.getAtlasData().getDrawables().removeValue(data, true);
                
                if (deleteStyleValues) {
                    for (Array<StyleData> datas : main.getJsonData().getClassStyleMap().values()) {
                        for (StyleData tempData : datas) {
                            for (StyleProperty prop : tempData.properties.values()) {
                                if (prop != null && prop.type.equals(Drawable.class) && prop.value != null && prop.value.equals(data.toString())) {
                                    prop.value = null;
                                }
                            }
                        }
                    }
                }
                
                refreshDrawables = true;
                i--;
            }
        }
        
        main.getRootTable().refreshStyleProperties(true);
        main.getRootTable().refreshPreview();
        
        if (refreshDrawables) {
            gatherDrawables();
        }
    }
    
    /**
     * Show an setStatusBarError indicating a drawable that exceeds project specifications
     */
    private void showDrawableError() {
        Dialog dialog = new Dialog("Error...", getSkin(), "bg");
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        Label label = new Label("Error while adding new drawables.\nEnsure that image dimensions are\nless than maximums specified in project.\nRolling back changes...", getSkin());
        label.setAlignment(Align.center);
        dialog.text(label);
        
        var textButton = new TextButton("OK", getSkin());
        textButton.addListener(main.getHandListener());
        dialog.button(textButton);
        dialog.show(getStage());
    }
    
    private void newDrawableDialog() {
        main.getDialogFactory().showDialogLoading(() -> {
            String defaultPath = "";

            if (main.getProjectData().getLastDrawablePath() != null) {
                FileHandle fileHandle = new FileHandle(main.getProjectData().getLastDrawablePath());
                if (fileHandle.parent().exists()) {
                    defaultPath = main.getProjectData().getLastDrawablePath();
                }
            }

            String[] filterPatterns = null;
            if (!Utils.isMac()) {
                filterPatterns = new String[]{"*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"};
            }

            List<File> files = main.getDesktopWorker().openMultipleDialog("Choose drawable file(s)...", defaultPath, filterPatterns, "Image files");
            if (files != null && files.size() > 0) {
                Gdx.app.postRunnable(() -> {
                    drawablesSelected(files);
                });
            }
        });
    }
    
    private void customDrawableDialog() {
        Array<DrawableData> backup = new Array<>();
        
        main.getDialogFactory().showCustomDrawableDialog(getSkin(), getStage(), (String name1) -> {
            DrawableData drawable = new DrawableData(name1);
            drawable.type = DrawableType.CUSTOM;
            main.getAtlasData().getDrawables().add(drawable);
            gatherDrawables();
            main.getDialogFactory().showDialogLoading(() -> {
                Gdx.app.postRunnable(() -> {
                    if (!main.getAtlasData().produceAtlas()) {
                        showDrawableError();
                        Gdx.app.log(getClass().getName(), "Attempting to reload drawables backup...");
                        main.getAtlasData().getDrawables().clear();
                        main.getAtlasData().getDrawables().addAll(backup);
                        gatherDrawables();
                        if (main.getAtlasData().produceAtlas()) {
                            Gdx.app.log(getClass().getName(), "Successfully rolled back changes to drawables");
                        } else {
                            Gdx.app.error(getClass().getName(), "Critical failure, could not roll back changes to drawables");
                        }
                    } else {
                        if (main.getProjectData().areResourcesRelative()) {
                            main.getProjectData().makeResourcesRelative();
                        }
                        
                        main.getProjectData().setChangesSaved(false);
                    }
                    
                    sortBySelectedMode();
                });
            });
        });
    }
    
    private void renameCustomDrawableDialog(DrawableData drawableData) {
        main.getDialogFactory().showCustomDrawableDialog(main.getSkin(), main.getStage(), drawableData, (String name1) -> {
            applyTintedDrawableSettings(drawableData, name1);
        });
    }

    /**
     * Called when a selection of drawables has been chosen from the
     * newDrawablesDialog(). Adds the new drawables to the project.
     * @param files 
     */
    private void drawablesSelected(List<File> files) {
        Array<FileHandle> fileHandles = new Array<>();
        
        files.forEach((file) -> {
            fileHandles.add(new FileHandle(file));
        });
        
        drawablesSelected(fileHandles);
    }
    
    private void drawablesSelected(Array<FileHandle> files) {
        main.getAtlasData().atlasCurrent = false;
        Array<DrawableData> backup = new Array<>(main.getAtlasData().getDrawables());
        Array<FileHandle> unhandledFiles = new Array<>();
        Array<FileHandle> filesToProcess = new Array<>();
        
        main.getProjectData().setLastDrawablePath(files.get(0).parent().path() + "/");
        for (FileHandle fileHandle : files) {
            var duplicateDrawable = checkDuplicateDrawables(DrawableData.proper(fileHandle.name()), 0);
            var duplicateFontDrawable = checkDuplicateFontDrawables(DrawableData.proper(fileHandle.name()), 0);
            if (duplicateDrawable || duplicateFontDrawable) {
                unhandledFiles.add(fileHandle);
            } else {
                filesToProcess.add(fileHandle);
            }
        }
        
        if (unhandledFiles.size > 0) {
            showRemoveDuplicatesDialog(unhandledFiles, backup, filesToProcess);
        } else {
            finalizeDrawables(backup, filesToProcess);
        }
    }
    
    /**
     * Shows a dialog to confirm removal of duplicate drawables that have the
     * same name without extension. This is called after selecting new drawables.
     * Does not delete existing style values that point to this drawable.
     * @param unhandledFiles
     * @param backup
     * @param filesToProcess 
     */
    private void showRemoveDuplicatesDialog(Array<FileHandle> unhandledFiles, Array<DrawableData> backup, Array<FileHandle> filesToProcess) {
        Dialog dialog = new Dialog("Delete duplicates?", getSkin(), "bg"){
            @Override
            protected void result(Object object) {
                if ((boolean) object) {
                    for (FileHandle fileHandle : unhandledFiles) {
                        removeDuplicateDrawables(DrawableData.proper(fileHandle.name()), false);
                        if (!checkDuplicateFontDrawables(DrawableData.proper(fileHandle.name()), 0)) {
                            filesToProcess.add(fileHandle);
                        }
                    }
                }
                finalizeDrawables(backup, filesToProcess);
                main.getAtlasData().produceAtlas();
                main.getRootTable().refreshPreview();
            }
        };
        
        dialog.getTitleTable().padLeft(5.0f);
        dialog.getContentTable().padLeft(10.0f).padRight(10.0f).padTop(5.0f);
        dialog.getButtonTable().padBottom(15.0f);
        
        var containsFontDrawable = false;
        for (FileHandle fileHandle : unhandledFiles) {
            if (checkDuplicateFontDrawables(DrawableData.proper(fileHandle.name()), 0)) {
                containsFontDrawable = true;
                break;
            }
        }
        
        if (containsFontDrawable) {
            dialog.text("This operation will overwrite one or more drawables\n"
                    + "Delete duplicates?\n\n"
                    + "Note: drawables that overwrite drawables used by fonts can not be added.");
        } else {
            dialog.text("This operation will overwrite one or more drawables\n"
                    + "Delete duplicates?");
        }
        
        dialog.button("OK", true);
        dialog.button("Cancel", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(main.getHandListener());
        dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(getStage());
    }
    
    /**
     * Adds the drawables to the project.
     * @param backup If there is a failure, the drawable list will be rolled
     * back to the provided backup.
     * @param filesToProcess 
     */
    private void finalizeDrawables(Array<DrawableData> backup, Array<FileHandle> filesToProcess) {
        for (FileHandle file : filesToProcess) {
            DrawableData data = new DrawableData(file);
            if (Utils.isNinePatch(file.name())) {
                data.type = DrawableType.NINE_PATCH;
            } else {
                data.type = DrawableType.TEXTURE;
            }
            if (!checkIfNameExists(data.name)) {
                main.getAtlasData().getDrawables().add(data);
            }
        }        
        
        gatherDrawables();

        main.getDialogFactory().showDialogLoading(() -> {
            Gdx.app.postRunnable(() -> {
                if (!main.getAtlasData().produceAtlas()) {
                    showDrawableError();
                    Gdx.app.log(getClass().getName(), "Attempting to reload drawables backup...");
                    main.getAtlasData().getDrawables().clear();
                    main.getAtlasData().getDrawables().addAll(backup);
                    gatherDrawables();
                    if (main.getAtlasData().produceAtlas()) {
                        Gdx.app.log(getClass().getName(), "Successfully rolled back changes to drawables");
                    } else {
                        Gdx.app.error(getClass().getName(), "Critical failure, could not roll back changes to drawables");
                    }
                } else {
                    if (main.getProjectData().areResourcesRelative()) {
                        main.getProjectData().makeResourcesRelative();
                    }

                    main.getProjectData().setChangesSaved(false);
                }

                sortBySelectedMode();
            });
        });
    }
    
    /**
     * Creates a TintedDrawable based on the provided DrawableData. Prompts
     * user for a Color and name.
     * @param drawableData 
     */
    private void newTintedDrawable(DrawableData drawableData) {
        Color previousColor = Color.WHITE;
        if (drawableData.tint != null) {
            previousColor = drawableData.tint;
        }
        main.getDialogFactory().showDialogColorPicker(previousColor, new DialogColorPicker.ColorListener() {
            @Override
            public void selected(Color color) {
                if (color != null) {
                    final DrawableData tintedDrawable = new DrawableData(drawableData.file);
                    tintedDrawable.type = DrawableType.TINTED;
                    tintedDrawable.tint = color;
                    
                    //Fix background color for new, tinted drawable
                    Color temp = Utils.averageEdgeColor(tintedDrawable.file, tintedDrawable.tint);
                    
                    if (Utils.brightness(temp) > .5f) {
                        tintedDrawable.bgColor = Color.BLACK;
                    } else {
                        tintedDrawable.bgColor = Color.WHITE;
                    }
                    
                    final TextField textField = new TextField(drawableData.name, getSkin());
                    final TextButton button = new TextButton("OK", getSkin());
                    button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                    textField.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                            button.setDisabled(!DrawableData.validate(textField.getText()) || checkIfNameExists(textField.getText()));
                        }
                    });
                    textField.addListener(main.getIbeamListener());

                    Dialog dialog = new Dialog("TintedDrawable...", getSkin(), "bg") {
                        @Override
                        protected void result(Object object) {
                            if (object instanceof Boolean && (boolean) object) {
                                tintedDrawable.name = textField.getText();
                                main.getAtlasData().getDrawables().add(tintedDrawable);
                                main.getProjectData().setChangesSaved(false);
                            }
                        }

                        @Override
                        public boolean remove() {
                            gatherDrawables();
                            main.getAtlasData().produceAtlas();
                            sortBySelectedMode();
                            getStage().setScrollFocus(scrollPane);
                            return super.remove();
                        }
                    };
                    dialog.getTitleTable().getCells().first().padLeft(5.0f);
                    dialog.addCaptureListener(new InputListener() {
                        @Override
                        public boolean keyDown(InputEvent event, int keycode2) {
                            if (keycode2 == Input.Keys.ENTER) {
                                if (!button.isDisabled()) {
                                    tintedDrawable.name = textField.getText();
                                    main.getAtlasData().getDrawables().add(tintedDrawable);
                                    main.getProjectData().setChangesSaved(false);
                                    dialog.hide();
                                }
                            }
                            return false;
                        }
                    });
                    dialog.text("What is the name of the new tinted drawable?");
                    dialog.getContentTable().getCells().first().pad(10.0f);

                    Drawable drawable = main.getAtlasData().getDrawablePairs().get(drawableData);
                    Drawable preview = null;
                    if (drawable instanceof SpriteDrawable) {
                        preview = ((SpriteDrawable) drawable).tint(color);
                    } else if (drawable instanceof NinePatchDrawable) {
                        preview = ((NinePatchDrawable) drawable).tint(color);
                    }
                    if (preview != null) {
                        dialog.getContentTable().row();
                        Table table = new Table();
                        table.setBackground(preview);
                        dialog.getContentTable().add(table);
                    }

                    dialog.getContentTable().row();
                    dialog.getContentTable().add(textField).growX().pad(10.0f);

                    dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
                    dialog.button(button, true);
                    button.addListener(main.getHandListener());
                    dialog.button("Cancel", false);
                    dialog.getButtonTable().getCells().get(1).getActor().addListener(main.getHandListener());
                    dialog.key(Input.Keys.ESCAPE, false);
                    dialog.show(getStage());
                    getStage().setKeyboardFocus(textField);
                    textField.selectAll();
                    textField.setFocusTraversal(false);
                }
            }
        });
    }
    
    private boolean checkIfNameExists(String name) {
        return checkIfDrawableNameExists(name) || checkIfFontDrawableNameExists(name);
    }
    
    /**
     * Returns true if any existing drawable has the indicated name.
     * @param name
     * @return 
     */
    private boolean checkIfDrawableNameExists(String name) {
        boolean returnValue = false;
        
        for (DrawableData drawable : main.getAtlasData().getDrawables()) {
            if (drawable.name.equals(name)) {
                returnValue = true;
                break;
            }
        }
        
        return returnValue;
    }
    
    /**
     * Returns true if any existing drawable has the indicated name.
     * @param name
     * @return 
     */
    private boolean checkIfFontDrawableNameExists(String name) {
        boolean returnValue = false;
        
        for (DrawableData drawable : main.getAtlasData().getFontDrawables()) {
            if (drawable.name.equals(name)) {
                returnValue = true;
                break;
            }
        }
        
        return returnValue;
    }
    
    @Override
    public boolean remove() {
        scrollPosition = scrollPane.getScrollY();

        main.getDesktopWorker().removeFilesDroppedListener(filesDroppedListener);

        try {
            if (!main.getAtlasData().atlasCurrent) {
                FileHandle defaultsFile = Main.appFolder.child("texturepacker/atlas-internal-settings.json");
                main.getAtlasData().writeAtlas(defaultsFile);
                main.getAtlasData().atlasCurrent = true;
            }
        } catch (Exception e) {
            Gdx.app.error(getClass().getName(), "Error creating atlas upon drawable dialog exit", e);
            main.getDialogFactory().showDialogError("Atlas Error...", "Error creating atlas upon drawable dialog exit.\n\nOpen log?");
        }

        fire(new DialogEvent(DialogEvent.Type.CLOSE));
        return super.remove();
    }
    
    @Override
    protected void result(Object object) {
        instance = null;
        if (object != null) {
            if (object instanceof DrawableData) {
                main.getProjectData().setChangesSaved(false);
                DrawableData drawable = (DrawableData) object;

                Undoable undoable;
                if (property != null) {
                    undoable = new DrawableUndoable(main.getRootTable(), main.getAtlasData(),
                                    property, property.value, drawable.name);
                } else {
                    undoable = new UndoableManager.CustomDrawableUndoable(main, customProperty, drawable.name);
                }
                main.getUndoableManager().addUndoable(undoable, true);
                
                if (listener != null) {
                    listener.confirmed(drawable, this);
                }
            } else if (object instanceof Boolean) {
                if (property != null) {
                    if ((boolean) object) {
                        main.getProjectData().setChangesSaved(false);
                        DrawableUndoable undoable =
                                new DrawableUndoable(main.getRootTable(), main.getAtlasData(),
                                        property, property.value, null);
                        main.getUndoableManager().addUndoable(undoable, true);
                        
                        if (listener != null) {
                            listener.emptied(this);
                        }
                    } else {
                        boolean hasDrawable = false;
                        for (DrawableData drawable : main.getAtlasData().getDrawables()) {
                            if (drawable.name.equals(property.value)) {
                                hasDrawable = true;
                                break;
                            }
                        }

                        if (!hasDrawable) {
                            main.getProjectData().setChangesSaved(false);
                            main.getUndoableManager().clearUndoables();
                            property.value = null;
                            main.getRootTable().refreshStyleProperties(true);
                        }
                        
                        if (listener != null) {
                            listener.cancelled(this);
                        }
                    }
                } else if (customProperty != null) {
                    if ((boolean) object) {
                        main.getProjectData().setChangesSaved(false);
                        CustomDrawableUndoable undoable = new CustomDrawableUndoable(main, customProperty, null);
                        main.getUndoableManager().addUndoable(undoable, true);
                        
                        if (listener != null) {
                            listener.emptied(this);
                        }
                    } else {
                        boolean hasDrawable = false;
                        for (DrawableData drawable : main.getAtlasData().getDrawables()) {
                            if (drawable.name.equals(customProperty.getValue())) {
                                hasDrawable = true;
                                break;
                            }
                        }

                        if (!hasDrawable) {
                            main.getProjectData().setChangesSaved(false);
                            main.getUndoableManager().clearUndoables();
                            customProperty.setValue(null);
                            main.getRootTable().refreshStyleProperties(true);
                        }
                        
                        if (listener != null) {
                            listener.cancelled(this);
                        }
                    }
                } else {
                    if (listener != null) {
                        listener.cancelled(this);
                    }
                }
            }
        } else {
            if (listener != null) {
                listener.cancelled(this);
            }
        }
    
        main.getAtlasData().produceAtlas();
        main.getRootTable().refreshPreview();
    }

    public boolean isShowing9patchButton() {
        return showing9patchButton;
    }

    public void setShowing9patchButton(boolean showing9patchButton) {
        this.showing9patchButton = showing9patchButton;
        populate();
    }
    
    public boolean isShowingOptions() {
        return showingOptions;
    }
    
    public void setShowingOptions(boolean showingOptions) {
        this.showingOptions = showingOptions;
        populate();
    }
    
    public FilterOptions getFilterOptions() {
        return filterOptions;
    }
    
    public void setFilterOptions(FilterOptions filterOptions) {
        this.filterOptions = filterOptions;
        populate();
    }
    
    public static class FilterOptions {
        public boolean texture = true;
        public boolean ninePatch = true;
        public boolean tinted = true;
        public boolean tiled = true;
        public boolean custom = true;
        public boolean tenPatch = true;
        public boolean hidden = false;
        public boolean regularExpression = false;
        public String name = "";

        void set(FilterOptions filterOptions) {
            texture = filterOptions.texture;
            ninePatch = filterOptions.ninePatch;
            tinted = filterOptions.tinted;
            tiled = filterOptions.tiled;
            custom = filterOptions.custom;
            tenPatch = filterOptions.tenPatch;
            regularExpression = filterOptions.regularExpression;
            name = filterOptions.name;
            hidden = filterOptions.hidden;
        }
    }
    
    private static class FilterInputListener extends InputListener {
        private DialogDrawables dialog;
        private String name;
        
        public FilterInputListener(DialogDrawables dialog) {
            super();
            
            this.dialog = dialog;
            name = "";
        }

        @Override
        public boolean keyTyped(InputEvent event, char character) {
            //not enter
            if (character != 10) {
                Label label = dialog.findActor("filter-label");
                if (!label.isVisible()) {
                    name = "";
                }

                var filterOptions = dialog.filterOptions;
                filterOptions.regularExpression = false;
                //backspace
                if (character == 8) {
                    if (name.length() > 0) {
                        name = name.substring(0, name.length() - 1);
                    }
                } else {
                    name += character;
                }
                filterOptions.name = name;

                Button button = dialog.findActor("filter");
                button.setChecked(true);

                label.setText(name);
                label.clearActions();
                label.addAction(Actions.sequence(Actions.visible(true), Actions.fadeIn(.25f), Actions.delay(2.0f), Actions.fadeOut(.25f), Actions.visible(false)));

                dialog.sortBySelectedMode();
            }
            return super.keyTyped(event, character);
        }
        
    }
}
