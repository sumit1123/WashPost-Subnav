/*
 * Copyright (C) 2014 . The Washington Post. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wapo.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RelativeLayout;

public abstract class MaterialCustomView extends RelativeLayout{

    protected final static String MATERIALDESIGNXML = "http://schemas.android.com/apk/res-auto";
    protected final static String ANDROIDXML = "http://schemas.android.com/apk/res/android";

    protected int minWidth;
    protected int minHeight;

    protected int backgroundColor;
    protected int beforeBackground;
    protected int backgroundResId = -1;

    // Indicate if user touched this view the last time
    public boolean isLastTouch = false;

    public MaterialCustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        onInitDefaultValues();
        //onInitAttributes(attrs);
    }

    protected abstract void onInitDefaultValues();

    // Set atributtes of XML to View
    protected void setAttributes(AttributeSet attrs) {
        setMinimumHeight(dpToPx(minHeight, getResources()));
        setMinimumWidth(dpToPx(minWidth, getResources()));
        if (backgroundResId != -1 && !isInEditMode()) {
            setBackgroundResource(backgroundResId);
        }
        setBackgroundAttributes(attrs);
    }

    protected int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    /**
     * Set background Color
     */
    protected void setBackgroundAttributes(AttributeSet attrs) {
        int bacgroundColor = attrs.getAttributeResourceValue(ANDROIDXML,"background",-1);
        if(bacgroundColor != -1){
            setBackgroundColor(getResources().getColor(bacgroundColor));
        }else{
            // Color by hexadecimal
            int background = attrs.getAttributeIntValue(ANDROIDXML, "background", -1);
            if(background != -1 && !isInEditMode()) {
                setBackgroundColor(background);
            }else {
                setBackgroundColor(backgroundColor);
            }
        }
    }


    /**
     * Make a dark color to press effect
     * @return
     */
    protected int makePressColor(int alpha) {
        int r = (backgroundColor >> 16) & 0xFF;
        int g = (backgroundColor >> 8) & 0xFF;
        int b = (backgroundColor >> 0) & 0xFF;
        r = (r - 30 < 0) ? 0 : r - 30;
        g = (g - 30 < 0) ? 0 : g - 30;
        b = (b - 30 < 0) ? 0 : b - 30;
        return Color.argb(alpha, r, g, b);
    }
}
