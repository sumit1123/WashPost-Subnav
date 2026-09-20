package com.wapo.view.share;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ActivitiesAdapter extends BaseAdapter {
    private List<ListItem> _items = new ArrayList<ListItem>();
    private WeakReference<Context> _context;
    private LayoutInflater _inflater;
    private int _layoutId;

    public ActivitiesAdapter(Context context, int textViewLayoutId, Intent[] intents) {
        _context = new WeakReference<Context>(context);
        _layoutId = textViewLayoutId;
        HashMap<ComponentName, ListItem> map = new HashMap<ComponentName, ListItem>();
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return;
        }
        IconResizer resizer = new IconResizer(_context.get());
        int len = intents.length;
        for (int i = 0; i < len; i++) {
            for (ResolveInfo ri : pm.queryIntentActivities(intents[i], PackageManager.MATCH_DEFAULT_ONLY)) {
                if (ri == null || ri.activityInfo == null) {
                    continue;
                }
                ComponentName cm = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
                if (map.containsKey(cm)) {
                    map.get(cm).groups.add(i);
                } else {
                    Intent launchIntent = (Intent) intents[i].clone();
                    launchIntent.setComponent(cm);
                    CharSequence label = ri.activityInfo.loadLabel(pm);
                    Drawable drawable = resizer.createIconThumbnail(ri.activityInfo.loadIcon(pm));
                    if (drawable == null) continue;
                    ListItem item = new ListItem(
                            drawable,
                            label == null ? null : label.toString(),
                            launchIntent
                    );
                    item.groups.add(i);
                    map.put(cm, item);
                }
            }
        }
        _items.addAll(map.values());
        Collections.sort(_items);
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public ListItem getItem(int position) {
        return _items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        TextView itemView = null;
        if (view instanceof TextView) {
            itemView = (TextView) view;
        } else {
            LayoutInflater inflater = getInflater();
            if (inflater != null) itemView = (TextView) inflater.inflate(_layoutId, parent, false);
        }

        assert itemView != null;

        ListItem item = getItem(position);

        itemView.setText(item.label);
        itemView.setCompoundDrawablesWithIntrinsicBounds(null, item.icon, null, null);

        return itemView;
    }

    public void setContext(Context context) {
        _inflater = null;
        _context = new WeakReference<Context>(context);
    }

    private LayoutInflater getInflater() {
        if (_inflater == null) {
            if (_context == null || _context.get() == null) return null;
            _inflater = LayoutInflater.from(_context.get());
        }
        return _inflater;
    }

    public static class ListItem implements Comparable<ListItem> {
        final public Drawable icon;
        final public String label;
        final public Intent intent;
        final public HashSet<Integer> groups = new HashSet<Integer>();

        private ListItem(Drawable icon, String label, Intent intent) {
            this.icon = icon;
            this.label = label;
            this.intent = intent;
        }

        @Override
        public int compareTo(ListItem another) {
            return label.compareTo(another.label);
        }
    }

    //
    // Code is borrowed from android.app.LauncherActivity.
    public class IconResizer {
        private final Resources mResources;
        private int mIconWidth = -1;
        private int mIconHeight = -1;

        private final Rect mOldBounds = new Rect();
        private Canvas mCanvas = new Canvas();

        public IconResizer(Context context) {
            mCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                    Paint.FILTER_BITMAP_FLAG));

            mResources = context.getResources();
            mIconWidth = mIconHeight = (int) mResources.getDimension(
                    android.R.dimen.app_icon_size);
        }

        public Drawable createIconThumbnail(Drawable icon) {
            int width = mIconWidth;
            int height = mIconHeight;

            final int iconWidth = icon.getIntrinsicWidth();
            final int iconHeight = icon.getIntrinsicHeight();

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            }

            if (width > 0 && height > 0) {
                if (width < iconWidth || height < iconHeight) {
                    final float ratio = (float) iconWidth / iconHeight;

                    if (iconWidth > iconHeight) {
                        height = (int) (width / ratio);
                    } else if (iconHeight > iconWidth) {
                        width = (int) (height * ratio);
                    }

                    final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                            Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                    final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                    final Canvas canvas = mCanvas;
                    canvas.setBitmap(thumb);
                    // Copy the old bounds to restore them later
                    // If we were to do oldBounds = icon.getBounds(),
                    // the call to setBounds() that follows would
                    // change the same instance and we would lose the
                    // old bounds
                    mOldBounds.set(icon.getBounds());
                    final int x = (mIconWidth - width) / 2;
                    final int y = (mIconHeight - height) / 2;
                    icon.setBounds(x, y, x + width, y + height);
                    icon.draw(canvas);
                    icon.setBounds(mOldBounds);
                    icon = new BitmapDrawable(mResources, thumb);
                    //canvas.setBitmap(null);
                } else if (iconWidth < width && iconHeight < height) {
                    icon = new BitmapDrawable(mResources,scaleIcon(icon, mIconWidth, mIconHeight));
                }
            }

            return icon;
        }

        private Bitmap scaleIcon(Drawable paramDrawable, int width, int height)
        {
            return (paramDrawable instanceof BitmapDrawable) ?
                    Bitmap.createScaledBitmap(((BitmapDrawable)paramDrawable).getBitmap(), width, height, true) :
                    null;
        }

    }
}