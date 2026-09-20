package com.wapo.text;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.view.Gravity.LEFT;
import static android.view.Gravity.TOP;

public class FontSizeDialog extends Dialog implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final int DEFAULT_SMALL_MULTIPLIER = -2;
    private static final int DEFAULT_MEDIUM_MULTIPLIER = 0;
    private static final int DEFAULT_LARGE_MULTIPLIER = 2;
    private static final int DEFAULT_EXTRA_LARGE_MULTIPLIER = 4;

    private int _left;
    private int _top;
    private int _width;
    private int _height;
    private FontSizeDialog.OnFontSizeChangedListener _listener;
    private SeekBar _seekBar;
    private View _larger;
    private View _smaller;
    private int _progress;
    private boolean isPhone;
    private int currentSelectionPos = 1;
    private ListView listView;
    private FontSizeListAdapter adapter;
    private Handler uiHandler;
    private static final long DISMISS_DELAY = 200;
    private List<FontSizes> fontSizeItems;
    private int fontChangeStep;
    private int originFontSize;

    private class FontSizes {

        private int fontSizeInt;
        private String fontSizeString;

        FontSizes(int fontSizeInt, String fontSizeString) {
            this.fontSizeInt = fontSizeInt;
            this.fontSizeString = fontSizeString;
        }

        public int getFontSizeInt() {
            return fontSizeInt;
        }

        public String getFontSizeString() {
            return fontSizeString;
        }
    }

    public FontSizeDialog(Context context, int theme, boolean isPhone, @NonNull FontSizeDialog.OnFontSizeChangedListener listener, int fontSize) {
        super(context, theme);
        context = getContext();
        this.isPhone = isPhone;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this._listener = listener;
        uiHandler = new Handler();
        originFontSize = fontSize;
        fontSizeItems = new ArrayList<>();

        FontSizeMultiplier fontSizeMultiplier = new FontSizeMultiplier(context);

        fontSizeItems.add(new FontSizes(fontSizeMultiplier.getFontSizeMultiplier(0), context.getString(R.string.font_size_multiplier_1)));
        fontSizeItems.add(new FontSizes(fontSizeMultiplier.getFontSizeMultiplier(1), context.getString(R.string.font_size_multiplier_2)));
        fontSizeItems.add(new FontSizes(fontSizeMultiplier.getFontSizeMultiplier(2), context.getString(R.string.font_size_multiplier_3)));
        fontSizeItems.add(new FontSizes(fontSizeMultiplier.getFontSizeMultiplier(3), context.getString(R.string.font_size_multiplier_4)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        final LayoutInflater inflater = getLayoutInflater();

        if (!isPhone) {
            final View view = inflater.inflate(R.layout.font_size_buttons, null, false);
            setContentView(view, new ViewGroup.LayoutParams(_width, _height));

            _seekBar = (SeekBar) view.findViewById(R.id.font_size_bar);
            _larger = view.findViewById(R.id.larger_font_label);
            _smaller = view.findViewById(R.id.smaller_font_label);

            changeFontSize(0, true);

            _larger.setOnClickListener(this);
            _smaller.setOnClickListener(this);
            _seekBar.setOnSeekBarChangeListener(this);

            final Window window = getWindow();
            final WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = LEFT | TOP;
            lp.x = _left;
            lp.y = _top;
        } else {
            determineCurrentPosition();
            final View view = inflater.inflate(R.layout.dialog_font_size, null, false);
            adapter = new FontSizeListAdapter(fontSizeItems, currentSelectionPos, this);
            listView = (ListView) view.findViewById(R.id.list);
            listView.setAdapter(adapter);
            setContentView(view);
        }
    }

    public void determineCurrentPosition() {
        int temp = originFontSize;
        for (int i = 0; i < fontSizeItems.size(); i++) {
            if (temp == fontSizeItems.get(i).getFontSizeInt()) {
                currentSelectionPos = i;
                break;
            }
        }
    }

    public String determineCurrentPositionString() {
        determineCurrentPosition();
        return fontSizeItems.get(currentSelectionPos).getFontSizeString();
    }

    public FontSizeDialog configureSizes(int left, int top, int width, int height) {
        _left = left;
        _top = top;
        _width = width;
        _height = height;
        return this;
    }

    @Override
    public void onClick(View v) {

        if (R.id.larger_font_label == v.getId()) {
            changeFontSize(1, true);
            return;
        }
        if (R.id.smaller_font_label == v.getId()) {
            changeFontSize(-1, true);
            return;
        }
    }

    private void changeFontSize(int step, boolean updateSeekBar) {
        int fontSize = (originFontSize + step) % 4;

        final boolean smallEnabled = fontSize > 0;
        final boolean largeEnabled = fontSize < 3;

        _smaller.setEnabled(smallEnabled);
        _larger.setEnabled(largeEnabled);

        if (updateSeekBar) {
            _seekBar.setProgress(fontSize);
        }

        originFontSize = fontSize;

        if (step != 0) {
            _listener.onFontSizeChanged(fontSize);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        changeFontSize(progress - _progress, false);
        _progress = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        _progress = seekBar.getProgress();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}


    private void changeFontSize() {
        if(_listener == null) {
            return;
        }
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                _listener.onFontSizeChanged(fontChangeStep);
                dismiss();
            }
        }, DISMISS_DELAY);
    }

    public void setFontChangeStep(int fontChangeStep) {
        this.fontChangeStep = fontChangeStep;
    }




    public interface OnFontSizeChangedListener {
        void onFontSizeChanged(int value);
    }

    private static class FontSizeListAdapter extends BaseAdapter {

        private float originalSize;
        private List<FontSizes> fontSizeItems;
        private int currentSelectionPos = 1;
        private WeakReference<FontSizeDialog> dialogRef;

        public FontSizeListAdapter(List<FontSizes> fontSizeItems, int currentSelectionPos, FontSizeDialog dialog) {
            this.fontSizeItems = fontSizeItems;
            this.currentSelectionPos = currentSelectionPos;
            this.dialogRef = new WeakReference<>(dialog);
        }

        @Override
        public int getCount() {
            return fontSizeItems == null ? 0 : fontSizeItems.size();
        }

        @Override
        public Object getItem(int position) {
            return fontSizeItems == null ? null : fontSizeItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.font_size_row_item, parent, false);
            }
            TextView textView = (TextView) convertView.findViewById(R.id.text);
            if (originalSize == 0) {
                originalSize = textView.getTextSize();
            }
            String text = fontSizeItems.get(position).getFontSizeString();
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(new WpTextAppearanceSpan(convertView.getContext(), R.style.FontSizeDialogStyle), 0, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new LocalFontAdjustment(fontSizeItems.get(position).getFontSizeInt()), 0, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannableString);
            ImageView selectorIcon = (ImageView) convertView.findViewById(R.id.selector_icon);
            if (position == currentSelectionPos){
                selectorIcon.setImageResource(R.drawable.radio_button_on);
            } else {
                selectorIcon.setImageResource(R.drawable.radio_button_off);
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentSelectionPos != position) {
                        if (fontSizeItems == null) return;
                        if (dialogRef == null || dialogRef.get() == null) {
                            return;
                        }

                        dialogRef.get().setFontChangeStep(fontSizeItems.get(position).getFontSizeInt());
                        currentSelectionPos = position;
                        notifyDataSetChanged();
                        dialogRef.get().changeFontSize();
                        dialogRef.get().dismiss();
                    }
                }
            });
            return convertView;
        }
    }

    public static class FontSizeMultiplier {

        private final int[] multipliers = new int[4];

        public FontSizeMultiplier(Context context) {
            multipliers[0] = DEFAULT_SMALL_MULTIPLIER;
            multipliers[1] = DEFAULT_MEDIUM_MULTIPLIER;
            multipliers[2] = DEFAULT_LARGE_MULTIPLIER;
            multipliers[3] = DEFAULT_EXTRA_LARGE_MULTIPLIER;

            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.dialogTheme, outValue, true);

            TypedArray a = context.getTheme().obtainStyledAttributes(
                    outValue.data,
                    R.styleable.FontSizeDialog
            );

            try {
                final int multiplierStyle = a.getResourceId(R.styleable.FontSizeDialog_multiplier, R.style.MultiplierStyle);
                final int[] multiplier = R.styleable.Multiplier;
                TypedArray aM = context.getTheme().obtainStyledAttributes(
                        multiplierStyle,
                        multiplier
                );
                try {
                    multipliers[0] = aM.getInteger(R.styleable.Multiplier_font_multiplier_small, multiplier[0]);
                    multipliers[1] = aM.getInteger(R.styleable.Multiplier_font_multiplier_medium, multipliers[1]);
                    multipliers[2] = aM.getInteger(R.styleable.Multiplier_font_multiplier_large, multipliers[2]);
                    multipliers[3] = aM.getInteger(R.styleable.Multiplier_font_multiplier_extra_large, multipliers[3]);
                } finally {
                    aM.recycle();
                }

            } finally {
                a.recycle();
            }
        }

        public int getFontSizeMultiplier(int index) {
            if (index < 0 || index >= multipliers.length) {
                return DEFAULT_MEDIUM_MULTIPLIER;
            }
            return multipliers[index];
        }

        public int getMultiplierIndex(int fontSizeMultiplier) {
            for (int i = 0; i < multipliers.length; i++) {
                if (multipliers[i] == fontSizeMultiplier) {
                    return i;
                }
            }
            return 0;
        }
    }

    /**
     * The same as {@link GlobalFontAdjustmentSpan} but uses probided local fontSize instead of global one
     */
    public static class LocalFontAdjustment extends MetricAffectingSpan {

        private final int fontSize;

        public LocalFontAdjustment(int fontSize) {
            this.fontSize = fontSize;
        }

        @Override
        public void updateMeasureState(TextPaint p) {
            p.setTextSize(p.getTextSize() + fontSize * 1.6f);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            updateMeasureState(tp);
        }
    }
}
