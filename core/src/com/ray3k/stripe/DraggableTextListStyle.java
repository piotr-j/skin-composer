package com.ray3k.stripe;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class DraggableTextListStyle extends DraggableListStyle {
    public BitmapFont font;
    /** Optional. */
    public Drawable textBackgroundUp, textBackgroundDown, textBackgroundOver, textBackgroundChecked,
            textBackgroundCheckedOver, dragBackgroundUp, validBackgroundUp, invalidBackgroundUp;
    public Color fontColor, downFontColor, overFontColor, checkedFontColor, checkedOverFontColor, dragFontColor,
            validFontColor, invalidFontColor;

    public DraggableTextListStyle () {
    }

    public DraggableTextListStyle (DraggableTextListStyle style) {
        background = style.background;
        dividerUp = style.dividerUp;
        dividerOver = style.dividerOver;
        font = style.font;
        textBackgroundUp = style.textBackgroundUp;
        textBackgroundDown = style.textBackgroundDown;
        textBackgroundOver = style.textBackgroundOver;
        textBackgroundChecked = style.textBackgroundChecked;
        textBackgroundCheckedOver = style.textBackgroundCheckedOver;
        dragBackgroundUp = style.dragBackgroundUp;
        validBackgroundUp = style.validBackgroundUp;
        invalidBackgroundUp = style.invalidBackgroundUp;
        fontColor = style.fontColor;
        downFontColor = style.downFontColor;
        overFontColor = style.overFontColor;
        checkedFontColor = style.checkedFontColor;
        checkedOverFontColor = style.checkedOverFontColor;
        dragFontColor = style.dragFontColor;
        validFontColor = style.validFontColor;
        invalidFontColor = style.invalidFontColor;
    }
}
