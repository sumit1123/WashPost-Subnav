/*
 * Copyright (c) 2015. The Washington Post. All rights reserved.
 */

package com.wapo.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wapo.text.WpTextAppearanceSpan;

public class EmbeddedGalleryView extends ViewGroup {
    private static final int MEASURE_SPEC_UNSPECIFIED = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    private int embeddedContentPadding = 0;
    private final Rect drawablePadding = new Rect();
    private TextView openButton;
    private int openButtonBackgroundResource;
    private int openButtonTopPadding;
    private SpannableString openButtonOpenText;
    private SpannableString openButtonCloseText;
    private int embeddedGalleryChildrenCount;

    //we init isOpen to true so that the setButtonOpen call with false has an effect in the initViews method
    private boolean isOpen = true;

    public EmbeddedGalleryView(Context context) {
        this(context, null);
    }


    public EmbeddedGalleryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmbeddedGalleryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EmbeddedGalleryView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initViews(attrs, defStyleAttr, defStyleRes);
    }


    @SuppressLint("RtlHardcoded")
    private void initViews(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final Context context = getContext();
        final Resources resources = context.getResources();

        openButtonBackgroundResource = R.drawable.btn_show_photos;

        openButtonOpenText = new SpannableString(context.getString(R.string.embed_gallery_open));
        openButtonOpenText.setSpan(new WpTextAppearanceSpan(context, R.style.article_embedded_show_more_photos), 0, openButtonOpenText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        openButtonCloseText = new SpannableString(context.getString(R.string.embed_gallery_close));
        openButtonCloseText.setSpan(new WpTextAppearanceSpan(context, R.style.article_embedded_hide_photos), 0, openButtonCloseText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        openButton = new TextView(context);
        openButton.setGravity(Gravity.CENTER_HORIZONTAL);
        openButton.setBackgroundResource(openButtonBackgroundResource);
        setOpenCloseButton(false);

        addView(openButton);
        resetDimens(resources);

    }

    private void resetDimens(Resources resources) {
        final int buttonHorizontalPadding = resources.getDimensionPixelSize(R.dimen.embedded_gallery_hor_pad);
        final int buttonVerticalPadding = resources.getDimensionPixelSize(R.dimen.embedded_gallery_ver_pad);
        openButton.setPadding(buttonHorizontalPadding, buttonVerticalPadding, buttonHorizontalPadding, buttonVerticalPadding);
        openButton.setGravity(Gravity.CENTER_HORIZONTAL);

        embeddedContentPadding = 0;
        openButtonTopPadding = 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthPadding = getPaddingLeft() + getPaddingRight() + drawablePadding.left + drawablePadding.right;
        int heightPadding = getPaddingTop() + getPaddingBottom() + drawablePadding.top + drawablePadding.bottom;

        if(MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED){
            throw new UnsupportedOperationException("EmbeddedGalleryView cannot support any heightSpecMode other then UNSPECIFIED");
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int rawWidth = widthMode == MeasureSpec.UNSPECIFIED ? 0 : MeasureSpec.getSize(widthMeasureSpec);
        int width = rawWidth - widthPadding;

        int height = 0;

        height += this.embeddedContentPadding;

        final int widthMinusInnerPadding = width - (this.embeddedContentPadding * 2);
        final int innerWidthMS = MeasureSpec.makeMeasureSpec(widthMinusInnerPadding, MeasureSpec.AT_MOST);

        openButton.measure(innerWidthMS, MEASURE_SPEC_UNSPECIFIED);
        height += openButton.getMeasuredHeight() + openButtonTopPadding;

        setMeasuredDimension(width + widthPadding, height + heightPadding);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int top = getPaddingTop();
        top += drawablePadding.top;

        top += embeddedContentPadding;

        final int embeddedLeft = (this.getMeasuredWidth()/2)-(openButton.getMeasuredWidth()/2);

        top += openButtonTopPadding;
        openButton.layout(embeddedLeft, top, embeddedLeft + openButton.getMeasuredWidth(), top + openButton.getMeasuredHeight());

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void resetViewContent() {
        resetDimens(getContext().getResources());

        setOpenCloseButton(false);
        setOpenClickListener(null);
    }

    public void setOpenCloseButton(boolean isOpen) {
        if(isOpen != this.isOpen) {
            openButton.setText(isOpen ? openButtonCloseText : openButtonOpenText);
            openButton.setVisibility(isOpen ? View.GONE : VISIBLE);
            this.isOpen = isOpen;
            //Trying this in order to force the button to have proper padding.
            resetDimens(getContext().getResources());
        }
    }

    public void setOpenClickListener(OnClickListener onClickListener) {
        openButton.setOnClickListener(onClickListener);
    }

    public void setEmbeddedGalleryChildrenCount(final int count) {
        this.embeddedGalleryChildrenCount = count;

        final Context context = getContext();

        String openButtonString;
        if (count == 1) {
            openButtonString = context.getString(R.string.embed_gallery_open_one);
        } else {
            openButtonString = String.format(context.getString(R.string.embed_gallery_open_numbered), embeddedGalleryChildrenCount);
        }
        openButtonOpenText = new SpannableString(openButtonString);
        openButtonOpenText.setSpan(new WpTextAppearanceSpan(context, R.style.article_embedded_show_more_photos), 0, openButtonOpenText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        openButton.setText(openButtonOpenText);
    }

}
