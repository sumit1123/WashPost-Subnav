package com.wapo.flagship.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.Toast;

import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.features.comics.ComicsActivity;
import com.wapo.flagship.features.shared.fragments.TopBarFragment;
import com.washingtonpost.android.BuildConfig;
import com.washingtonpost.android.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: maxx
 * Date: 2/9/13
 * Time: 12:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class UIUtil {
    private static boolean D = BuildConfig.DEBUG;

    private static final Map<String,Integer> heroList = new HashMap<String,Integer>();
    private static final Map<String,Integer> panelHeroList = new HashMap<String,Integer>();
    private static double scaleFactor= Double.NaN;


    public static Bitmap scaleBitmap(Bitmap data, int maxWidth, int maxHeight) {

        Bitmap bitmap;
        int actualWidth = data.getWidth();
        int actualHeight = data.getHeight();

        int desiredWidth = getResizedDimension(maxWidth, maxHeight,
                actualWidth, actualHeight);
        int desiredHeight = getResizedDimension(maxHeight, maxWidth,
                actualHeight, actualWidth);

        bitmap = Bitmap.createScaledBitmap(data, desiredWidth, desiredHeight, true);

        return bitmap;
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary Maximum size of the primary dimension (i.e. width for
     *        max width), or zero to maintain aspect ratio with secondary
     *        dimension
     * @param maxSecondary Maximum size of the secondary dimension, or zero to
     *        maintain aspect ratio with primary dimension
     * @param actualPrimary Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     */
    public static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
                                           int actualSecondary) {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }


    public static float dip2Px(int dip, Context ctx) {
        Resources r = ctx.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    public static boolean isPIPSupported() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    }


    public static DisplayMetrics displayMetrics(Context ctx) {
        WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        return dm;
    }


    public static boolean isPortrait(Context ctx) {
       return Configuration.ORIENTATION_PORTRAIT == ctx.getResources().getConfiguration().orientation;
    }


    public static Point sizeInDp(Context ctx) {
        DisplayMetrics outMetrics = ctx.getResources().getDisplayMetrics();
        float density  = outMetrics.density;
        float dpHeight = outMetrics.heightPixels / density;
        float dpWidth  = outMetrics.widthPixels / density;
        return new Point((int)dpWidth, (int)dpHeight);
    }

    public static boolean isPhone(Context cxt)
    {
        return cxt.getResources().getBoolean(R.bool.is_phone);
    }

    public static boolean isNonHero(final double width, final double height, Context context)
    {
        initializeHeroList(context);
        String widthHeight = width+":"+height;
        return !heroList.containsKey(widthHeight);
    }

    public static int getPanelHeroHeight(final double width, final double height, Context context)
    {
        initializePanelHeroList(context);
        String widthHeight = width+":"+height;
        String portraitDefault = "portrait_default";

        return panelHeroList.containsKey(widthHeight) ?
                panelHeroList.get(widthHeight) :
                panelHeroList.get(portraitDefault);
    }

    public static void initializeHeroList(Context context)
    {
            if (heroList.isEmpty())
            {
                heroList.put("3.0:2.0", context.getResources().getInteger(R.integer.portrait_shark));
                heroList.put("2.0:2.0",context.getResources().getInteger(R.integer.portrait_squid));
                heroList.put("3.0:1.0",context.getResources().getInteger(R.integer.portrait_dolphin));
                heroList.put("portrait_default",context.getResources().getInteger(R.integer.portrait_default));

            }
    }

    public static void initializePanelHeroList(Context context)
    {
        if (panelHeroList.isEmpty())
        {
            panelHeroList.put("3.0:2.0",context.getResources().getInteger(R.integer.portrait_shark_panel));
            panelHeroList.put("2.0:2.0",context.getResources().getInteger(R.integer.portrait_squid_panel));
            panelHeroList.put("3.0:1.0",context.getResources().getInteger(R.integer.portrait_dolphin_panel));
            panelHeroList.put("portrait_default",context.getResources().getInteger(R.integer.portrait_default_panel));

        }
    }

    public static void startActivityWithAnimation(Intent intent, Activity activity) {
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }

    public static void startActivityForResultWithAnimation(Intent intent, Activity activity, int requestCode) {
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }

    public static void startComicsActivity(Activity activity, String comicsAuthorNumberParam) {
        Intent i = new Intent(activity, ComicsActivity.class);
        i.putExtra(TopBarFragment.SectionDisplayName, activity.getResources().getString(R.string.comics).toUpperCase());
        i.putExtra(ComicsActivity.ComicsAuthorNumberParam, comicsAuthorNumberParam);
        UIUtil.startActivityForResultWithAnimation(i, activity, 1122);
    }

    public static void loadImageViewFromAsset(Activity context, ImageView imageView, String asset) throws IOException {
        int width = imageView.getWidth();
        int height = imageView.getHeight();

        if (width == 0) {
            width = imageView.getMeasuredWidth();
        }
        if (width == 0) {
            width = context.getWindowManager().getDefaultDisplay().getWidth();
        }

        if (height == 0) {
            height = imageView.getMeasuredHeight();
        }
        if (height == 0) {
            height = context.getWindowManager().getDefaultDisplay().getHeight();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Resources res = context.getResources();
        BitmapFactory.decodeStream(res.getAssets().open(asset), null, options);
        float k = Math.max((float) width / 2f / options.outWidth, (float) height / 2f / options.outHeight);
        options.inJustDecodeBounds = false;
        if (k < 1 && k > 0) {
            options.inScaled = true;
            options.inSampleSize = (int)Math.round(1 / k + 0.25);
        }

        Bitmap background = BitmapFactory.decodeStream(res.getAssets().open(asset), null, options);
        if (background != null) {
            Matrix m = new Matrix();
            if (background.getWidth() > 0 && background.getHeight() > 0) {
                k = Math.max((float)width / background.getWidth(), (float)height / background.getHeight());
            }
            m.setScale(k, k);
            imageView.setImageBitmap(background);
            imageView.setImageMatrix(m);
        }
    }

    public static void getDisplaySize(Context context, Point point) {
        if (point == null || context == null) {
            return;
        }

        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        display.getSize(point);
    }

    public static double getScaleFactor(Context context) {
        if (scaleFactor == Double.NaN) {
            Resources res = context.getResources();
            scaleFactor = (double)Math.min(
                    res.getDisplayMetrics().heightPixels,
                    res.getDisplayMetrics().widthPixels) / res.getDimensionPixelSize(R.dimen.sf_base_style_landscape_height);
        }
        return scaleFactor;
    }

    public static float dipOrDpToFloat(String value) {
        if (value.indexOf("dp") != -1) {
            value = value.replace("dp", "");
        }
        else {
            value = value.replace("dip", "");
        }
        return Float.parseFloat(value);
    }


    public static int getRelativeTop(View myView) {
        Rect bounds = new Rect();
        myView.getGlobalVisibleRect(bounds);
        return bounds.top;
    }

    public static int getRelativeLeft(View myView) {
//	    if (myView.getParent() == myView.getRootView())
        if(myView.getId() == android.R.id.content)
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) myView.getParent());
    }

    public static class ExpandShrinkAnim extends Animation {

        int targetHeight;
        int startHeight;
        View view;

        public ExpandShrinkAnim(View view, int startHeight, int targetHeight) {
            this.view = view;
            this.targetHeight = targetHeight;
            this.startHeight = startHeight;
            //LogUtil.e("Section", "Start height " + startHeight + " TargetHeight " + targetHeight);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (view != null && view.getLayoutParams() != null) {
                view.getLayoutParams().height = (int) (startHeight + targetHeight * interpolatedTime);
                view.requestLayout();
            }
        }

        @Override
        public void initialize(int width, int height, int parentWidth,
                               int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

    }

    public static void showToast(String message) {
        Toast.makeText(FlagshipApplication.getInstance(), message, Toast.LENGTH_SHORT).show();
    }

    static int[] location = new int[2];
    @MainThread
    public static Rect getViewScreenRect(@NonNull View view, @Nullable Rect rect) {
        if (D) {
            if (!Utils.isMainThread()) {
                throw new RuntimeException("This method is expected to be called on the main thread only");
            }
        }

        if (rect == null) {
            rect = new Rect();
        }

        float scaleX = 1f;
        float scaleY = 1f;

        View parent = view;
        while (parent != null) {
            scaleX *= parent.getScaleX();
            scaleY *= parent.getScaleY();

            if (parent.getParent() instanceof View) {
                parent = (View) parent.getParent();
            } else {
                parent = null;
            }
        }

        view.getLocationOnScreen(location);
        rect.set(
                location[0],
                location[1],
                Math.round(location[0] + view.getWidth() * scaleX),
                Math.round(location[1] + view.getHeight() * scaleY)
        );

        return rect;
    }

    /**
     * method to return background color drawable selector for MaterialButton on pre-lollipop devices.
     */
    public static StateListDrawable makeSelectorWithColorResource(@NonNull Context context,
                                                                  @ColorRes int colorResourceId) {
        int color = ContextCompat.getColor(context, colorResourceId);
        StateListDrawable res = new StateListDrawable();
        res.setExitFadeDuration(300);
        res.setAlpha(60);
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(color));
        res.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return res;
    }

    public static Bitmap vectorToBitmap(Context context, @DrawableRes int resVector) {
        Drawable drawable = AppCompatResources.getDrawable(context, resVector);
        Bitmap b = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        drawable.setBounds(0, 0, c.getWidth(), c.getHeight());
        drawable.draw(c);
        return b;
    }
}
