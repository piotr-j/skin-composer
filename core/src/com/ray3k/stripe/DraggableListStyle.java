package com.ray3k.stripe;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class DraggableListStyle {
    public Drawable dividerUp, dividerOver;
    /** Optional **/
    public Drawable background;

    public DraggableListStyle () {
    }

    public DraggableListStyle (DraggableListStyle style) {
        background = style.background;
        dividerUp = style.dividerUp;
        dividerOver = style.dividerOver;
    }
}
