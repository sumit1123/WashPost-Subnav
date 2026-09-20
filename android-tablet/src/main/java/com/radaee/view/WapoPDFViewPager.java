package com.radaee.view;

/**
 * Created by elamgodilj on 8/2/17.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.radaee.pdf.Document;
import com.wapo.android.commons.util.Logger;
import com.washingtonpost.android.R;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

/**
 * Created by radaee on 2015/5/14.
 */
public class WapoPDFViewPager extends ViewPager implements PDFLayout.LayoutListener {
    public static final String TAG = PDFViewPager.class.getSimpleName();
    private WeakReference<WapoPDFViewPager.WapoReaderListener> listener;
    private OnPageChangeListener pageChangeListener;
    private PDFPageAdapter m_adt;
    private int m_fit_type;
    private PDFViewPagerHandler m_hand_ui, m_hand_ui_cache;
    private boolean isOpen;

    public WapoPDFViewPager(Context context) {
        super(context);
    }

    public WapoPDFViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isOpen() {
        return isOpen;
    }

    /**
     * @param
     * @param fit_type page fit mode: 0- fit screen. 1- fit width. 2- fit height.
     */
    public void PDFOpen(String[] pdfLocations, int fit_type, @NotNull WapoPDFViewPager.WapoReaderListener listener, int pageno, String defaultPdfPath) {
        m_fit_type = fit_type;
        if (m_hand_ui == null) {
            m_hand_ui = new PDFViewPagerHandler(WapoPDFViewPager.this, Looper.myLooper());
        }
        if (m_hand_ui_cache == null) {
            m_hand_ui_cache = new PDFViewPagerHandler(WapoPDFViewPager.this, Looper.myLooper());
        }
        if (m_adt == null) {
            m_adt = new PDFPageAdapter(getContext(), pdfLocations, defaultPdfPath, m_hand_ui, m_hand_ui_cache, m_fit_type, listener);
        }
        this.listener = new WeakReference<>(listener);
        this.addOnPageChangeListener(getPageChangeListener());
        this.setAdapter(m_adt);
        setCurrentItem(pageno);
        isOpen = true;
    }

    public void PDFClose() {
        if (m_hand_ui != null) {
            m_hand_ui.removeCallbacksAndMessages(null);
            m_hand_ui = null;
        }
        if (m_hand_ui_cache != null) {
            m_hand_ui_cache.removeCallbacksAndMessages(null);
            m_hand_ui_cache = null;
        }
        isOpen = false;
        // close all child views
        for (int i = 0; i < getChildCount(); i++) {
            View pageView = getChildAt(i);
            if (pageView instanceof WapoPDFPageView) {
                if (((WapoPDFPageView)pageView).vIsOpened()) {
                    ((WapoPDFPageView)pageView).vClose();
                    pageView = null;
                }
            }
        }
        // remove all children
        removeAllViews();
        this.removeOnPageChangeListener(getPageChangeListener());
        this.listener = null;
        this.setAdapter(null);
        pageChangeListener = null;
        if (m_adt != null) {
            if (m_adt.currPages != null) {
                for (int i = 0; i < m_adt.currPages.length; i++) {
                    if (m_adt.currPages[i] == null) continue;
                    View view = findViewWithTag(TAG+i);
                    if (view instanceof WapoPDFPageView) {
                        Logger.d(TAG, "View found at position " + i);
                        if (((WapoPDFPageView)view).vIsOpened()) {
                            Logger.d(TAG, "View found has open document at position " + i);
                            ((WapoPDFPageView)view).vClose();
                            view = null;
                        }
                    }
                }
            }
            m_adt.close();
            m_adt = null;
        }
    }

    public void PDFGotoPage(int i, boolean smoothScroll) {
        setCurrentItem(i, smoothScroll);
    }

    public void PDFReload(int pageNo) {
        if (m_adt == null && m_adt.currPages == null) {
            return;
        }
        Logger.d(TAG, "PDFReload");
        //reload current page
        m_adt.checkAndReloadPage(findViewWithTag(TAG+(pageNo)), pageNo);
        //reload current page - 1
        m_adt.checkAndReloadPage(findViewWithTag(TAG+(pageNo - 1)), (pageNo - 1));
        //reload current page + 2
        m_adt.checkAndReloadPage(findViewWithTag(TAG+(pageNo + 1)), (pageNo + 1));
    }

    private OnPageChangeListener getPageChangeListener() {
        if (pageChangeListener == null) {
            pageChangeListener = new OnPageChangeListener() {
                @Override
                public void onPageScrolled(int i, float v, int i1) {
                }

                @Override
                public void onPageSelected(int i) {
                    listener.get().onPageChanged(i);
                }

                @Override
                public void onPageScrollStateChanged(int i) {
                }
            };
        }
        return pageChangeListener;
    }

    @Override
    protected void finalize() throws Throwable {
        PDFClose();
        super.finalize();
    }

    @Override
    public void OnPageChanged(int pageno) {
    }

    @Override
    public void OnPageRendered(int pageno) {
        pageno = getCurrentItem();
        if (m_adt != null && m_adt.currPages != null) {
            if (pageno >= 0 && pageno < m_adt.currPages.length && m_adt.currPages[pageno] != null) {
                View view = findViewWithTag(m_adt.currPages[pageno]);
                if (view instanceof WapoPDFPageView) {
                    ((WapoPDFPageView)view).vRenderFinish();
                }
            }
        }
    }

    @Override
    public void OnCacheRendered(int pageno) {

    }

    @Override
    public void OnFound(boolean found) {
    }

    @Override
    public void OnPageDisplayed(Canvas canvas, VPage vpage) {
    }

    @Override
    public void OnTimer() {
        int pageno = getCurrentItem();
        if (m_adt != null && m_adt.currPages != null) {
            if (pageno >= 0 && pageno < m_adt.currPages.length && m_adt.currPages[pageno] != null) {
                View view = findViewWithTag(m_adt.currPages[pageno]);
                if (view instanceof WapoPDFPageView) {
                    view.invalidate();
                }
            }
        }
    }

    public interface WapoReaderListener {
        void onOpenURI(final String uri, final boolean firstAttempt);
        void onPageChanged(int pageno);
    }

    private static class PDFViewPagerHandler extends Handler {
        private final WeakReference<PDFLayout.LayoutListener> m_listener;


        public PDFViewPagerHandler(PDFLayout.LayoutListener listener, Looper looper) {
            super(looper);
            this.m_listener = new WeakReference<>(listener);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)//render finished.
            {
                case 0:
                    if (m_listener.get() != null) {
                        m_listener.get().OnPageRendered((msg.obj) != null ?  ((VCache) msg.obj).vGetPageNO() : 0);
                    }
                case 100:
                    if (m_listener.get() != null) {
                        m_listener.get().OnTimer();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private static class PDFPageAdapter extends PagerAdapter {
        private WeakReference<Context> m_ctx;
        private String[] pdfLocations;
        private String defaultPdfPath;
        private VThread m_thread;
        private VThread m_thread_cache;
        private int m_fit_type;
        private WapoPDFViewPager.WapoReaderListener listener;
        private String[] currPages = null;

        public PDFPageAdapter(Context ctx, String[] pdfLocations, String defaultPdfPath, PDFViewPagerHandler m_hand_ui, PDFViewPagerHandler m_hand_ui_cache, int m_fit_type, WapoPDFViewPager.WapoReaderListener listener) {
            m_thread = new VThread(m_hand_ui);
            m_thread.start();
            m_thread_cache = new VThread(m_hand_ui_cache);
            m_thread_cache.start();
            this.pdfLocations = pdfLocations;
            int cnt = pdfLocations.length;
            currPages = new String[cnt];
            m_ctx = new WeakReference<>(ctx);
            this.defaultPdfPath = defaultPdfPath;
            this.m_fit_type = m_fit_type;
            this.listener = listener;
        }

        @Override
        public WapoPDFPageView instantiateItem(android.view.ViewGroup container, int position) {
            Logger.d(TAG, "instantiateItem " + position);
            String path = pdfLocations[position];
            String positionKey = WapoPDFViewPager.TAG + position;
            currPages[position] = positionKey;
            Document doc = new Document();
            doc.Open(path, null);
            int diPageCount = doc.GetPageCount();//if open failed, return 0.
            if (diPageCount == 0) {
                Logger.e(TAG, "instantiateItem open failed. Using defaultPdfPath for " + position);
                doc.Open(defaultPdfPath, null);
                path = defaultPdfPath;
            }
            WapoPDFPageView pageView = new WapoPDFPageView(m_ctx.get(), defaultPdfPath);
            if (!pageView.vIsOpened()) {
                pageView.vOpen(m_thread, m_thread_cache, doc, 0, m_fit_type, listener);
            }
            pageView.setTag(R.id.print_page_tag, path);
            pageView.setTag(positionKey);
            container.addView(pageView);
            pageView.invalidate();
            return pageView;
        }

        @Override
        public int getCount() {
            return pdfLocations.length;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object pageView) {
            Logger.d(TAG, "destroyItem " + position);
            try {
                if (pageView instanceof WapoPDFPageView) {
                    boolean isOpen = ((WapoPDFPageView)pageView).vIsOpened();
                    if (isOpen) {
                        ((WapoPDFPageView)pageView).vFreeCache();
                    }
                    container.removeView(((WapoPDFPageView)pageView));
                    if (isOpen) {
                        ((WapoPDFPageView)pageView).vClose();
                    }
                    currPages[position] = null;
                    pageView = null;
                }
            } catch (Exception e) {
                Logger.e(TAG, "destroyItem " + position, e);
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Page:" + position;
        }


        public void checkAndReloadPage(View pageView, int position) {
            if (!(pageView instanceof WapoPDFPageView)) return;
            Logger.d(TAG, "checkAndReloadPage " + position);
            if (defaultPdfPath.equals(pageView.getTag(R.id.print_page_tag))) {
                // if this page is pointing to the default close it and reload
                ((WapoPDFPageView)pageView).vFreeCache();
                ((WapoPDFPageView)pageView).vClose();

                String path = pdfLocations[position];
                Document doc = new Document();
                doc.Open(path, null);
                int diPageCount = doc.GetPageCount();//if open failed, return 0.
                if (diPageCount == 0) {
                    doc.Open(defaultPdfPath, null);
                    path = defaultPdfPath;
                }
                if (!((WapoPDFPageView)pageView).vIsOpened()) {//lazy loading.
                    ((WapoPDFPageView)pageView).vOpen(m_thread, m_thread_cache, doc, 0, m_fit_type, listener);
                }
                pageView.setTag(R.id.print_page_tag, path);
                pageView.invalidate();
            }
        }

        public void close() {
            Logger.d(TAG, "close adapter ");
            if(m_thread != null) {
                m_thread.destroy();
            }
            if (m_thread_cache != null) {
                m_thread_cache.destroy();
            }
            m_thread = null;
            m_thread_cache = null;
            listener = null;
            currPages = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Exception e) {
            Logger.e(TAG, "Exception in PDFViewPager onInterceptTouchEvent.", e);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (Exception e) {
            Logger.e(TAG, "Exception in PDFViewPager onTouchEvent.", e);
        }
        return false;
    }
}

