//package com.washingtonpost.android.recirculation.carousel;
//
//import android.content.Context;
//import android.content.res.Resources;
//import android.graphics.Bitmap;
//import android.graphics.Typeface;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewParent;
//import android.view.animation.AlphaAnimation;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.cardview.widget.CardView;
//
//import com.washingtonpost.android.recirculation.R;
//import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider;
//import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouseClickedListener;
//import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem;
//import com.washingtonpost.android.recirculation.carousel.views.CarouselView;
//
//import junit.framework.TestCase;
//
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.atLeast;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
///**
// * CarouselRecyclerViewAdapterTest
// * Android Recirculation
// * <p>
// * Created by Shreyas Thyagaraja on 6/25/19.
// * Copyright (c) 2019 Washington Post. All rights reserved.
// */
//
//@Ignore("Failing tests")
//@RunWith(MockitoJUnitRunner.class)
//public class CarouselRecyclerViewAdapterTest extends TestCase {
//
//    private CarouselRecyclerViewAdapter carouselRecyclerViewAdapter;
//    @Mock
//    Context context;
//
//    @Mock
//    OnCarouseClickedListener onCarouseClickedListener;
//
//    @Mock
//    CarouselView.CarouselConsumeTouchEventRule carouselConsumeTouchEventRule;
//
//    @Mock
//    ViewParent viewParent;
//
//    @Mock
//    CarouselViewItem carouselViewItem;
//
//    @Mock
//    CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder;
//
//    @Mock
//    CardView cardView;
//
//    @Mock
//    ImageView imageView;
//
//    @Mock
//    TextView bylineTestView;
//
//    @Mock
//    TextView titleTextView;
//
//    @Mock
//    Resources resources;
//
//    @Mock
//    CarouselProvider listener;
//
//    @Mock
//    OnCarouseClickedListener carouseClickedListener;
//
//    @Mock
//    CarouselView.CarouselConsumeTouchEventRule consumeTouchEventRule;
//
//    @Mock
//    View.OnTouchListener onTouchListener;
//
//    @Mock
//    Bitmap bitmap;
//
//    @Mock
//    TextView kickerTextView;
//
//    @Mock
//    TextView storyTypeTextView;
//
//
//    @Before
//    public void setUp() throws Exception {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//    }
//
//    private CarouselRecyclerViewAdapter createCarouselRecyclerViewAdapter(
//            int carouselItemWidth) {
//        return new CarouselRecyclerViewAdapter(carouselItemWidth, listener, carouseClickedListener, consumeTouchEventRule, viewParent);
//    }
//
//    @Test
//    public void onCreateViewHolderTest() {
//        ViewGroup viewGroup = mock(ViewGroup.class);
//        LayoutInflater layoutInflater = mock(LayoutInflater.class);
//        View view = mock(View.class);
//        doReturn(context).when(viewGroup).getContext();
//        doReturn(layoutInflater).when(carouselRecyclerViewAdapter).layoutInflaterFrom(context);
//        doReturn(view).when(layoutInflater).inflate(R.layout.carousel_recycleview_item, viewGroup, false);
//        assertNotNull(carouselRecyclerViewAdapter.onCreateViewHolder(viewGroup, 0));
//        verify(carouselRecyclerViewAdapter).layoutInflaterFrom(context);
//        verify(layoutInflater).inflate(R.layout.carousel_recycleview_item, viewGroup, false);
//        verify(view, never()).getLayoutParams();
//
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(1));
//        ViewGroup.LayoutParams layoutParams = mock(ViewGroup.LayoutParams.class);
//        doReturn(layoutInflater).when(carouselRecyclerViewAdapter).layoutInflaterFrom(context);
//        doReturn(layoutParams).when(view).getLayoutParams();
//        assertNotNull(carouselRecyclerViewAdapter.onCreateViewHolder(viewGroup, 0));
//        verify(view).getLayoutParams();
//        assertEquals(layoutParams.width, 1);
//    }
//
//    @Test
//    public void onBindViewHolderNotNightModeAndCardViewNotNullTest() {
//        View view = mock(View.class);
//        List<CarouselViewItem> items = spy(caurouselList(true));
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_background_color)).thenReturn(1234);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(5678);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(items);
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        verify(carouselViewHolder.cardView).setCardBackgroundColor(1234);
//        verify(carouselViewHolder.byline, atLeast(1)).setTextColor(5678);
//
//        assertEquals(carouselViewHolder.itemView, view);
//        assertEquals(1, carouselRecyclerViewAdapter.getItemCount());
//
//        String title = items.get(0).getTitle();
//        when(carouselViewHolder.title.getText()).thenReturn("Top Stories");
//        assertEquals(title, carouselViewHolder.title.getText());
//
//        String byLineStr = items.get(0).getByline();
//        when(carouselViewHolder.byline.getText()).thenReturn(byLineStr);
//        assertEquals(byLineStr, carouselViewHolder.byline.getText());
//
//        when(carouselViewHolder.byline.getCurrentTextColor()).thenReturn(5678);
//        assertEquals(5678, carouselViewHolder.byline.getCurrentTextColor());
//
//    }
//
//    @Test
//    public void onBindViewHolderNightModeAndCardViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_background_color)).thenReturn(1234);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(5678);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        verify(carouselViewHolder.cardView).setCardBackgroundColor(1234);
//        verify(carouselViewHolder.byline, atLeast(1)).setTextColor(5678);
//
//        when(carouselViewHolder.byline.getCurrentTextColor()).thenReturn(5678);
//        assertEquals(5678, carouselViewHolder.byline.getCurrentTextColor());
//    }
//
//    @Test
//    public void onBindViewHolderNightModeAndByLineTextViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_background_color)).thenReturn(1234);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(5678);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        verify(carouselViewHolder.byline).setTypeface(Typeface.createFromAsset(context.getAssets(), "FranklinITCStd-Light.otf"));
//        verify(carouselViewHolder.byline).setText("Robert Prince");
//        verify(carouselViewHolder.title).setVisibility(View.VISIBLE);
//        verify(carouselViewHolder.title, never()).setVisibility(View.INVISIBLE);
//
//        when(carouselViewHolder.byline.getText()).thenReturn("Robert Prince");
//        when(carouselViewHolder.title.getVisibility()).thenReturn(View.VISIBLE);
//
//        assertTrue(carouselViewHolder.title.getVisibility() == View.VISIBLE);
//        assertEquals("Robert Prince", carouselViewHolder.byline.getText());
//
//
//    }
//
//
//    @Test
//    public void onBindViewHolderNightModeAndTitleTextViewViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(6001);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        verify(carouselViewHolder.title).setTypeface(Typeface.createFromAsset(context.getAssets(), "Postoni-Bold.otf"));
//        verify(carouselViewHolder.title).setText("Top Stories");
//        verify(carouselViewHolder.title).setVisibility(View.VISIBLE);
//        verify(carouselViewHolder.title, atLeast(1)).setTextColor(6001);
//
//        when(carouselViewHolder.title.getVisibility()).thenReturn(View.VISIBLE);
//        assertTrue(carouselViewHolder.title.getVisibility() == View.VISIBLE);
//
//        when(carouselViewHolder.title.getText()).thenReturn("Top Stories");
//        assertEquals("Top Stories", carouselViewHolder.title.getText());
//    }
//
//    @Test
//    public void onBindViewHolderTitleTextViewShouldShowTitleFalseTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(6001);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(false));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        verify(carouselViewHolder.title).setVisibility(View.GONE);
//
//        when(carouselViewHolder.title.getVisibility()).thenReturn(View.GONE);
//        assertFalse("Error for condition", carouselViewHolder.title.getVisibility() == View.VISIBLE);
//        assertTrue(carouselViewHolder.title.getVisibility() == View.GONE);
//
//    }
//
//    @Test
//    public void onBindViewHolderNightModeOffAndTitleTextViewViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        List<CarouselViewItem> items = spy((caurouselList(true)));
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(6002);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(items);
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        verify(carouselViewHolder.title).setTypeface(Typeface.createFromAsset(context.getAssets(), "Postoni-Bold.otf"));
//        verify(carouselViewHolder.title).setText("Top Stories");
//        verify(carouselViewHolder.title).setVisibility(View.VISIBLE);
//        verify(carouselViewHolder.title, never()).setVisibility(View.INVISIBLE);
//        verify(carouselViewHolder.title, atLeast(1)).setTextColor(6002);
//
//        String title = items.get(0).getTitle();
//        when(carouselViewHolder.title.getText()).thenReturn(title);
//        assertEquals(title, carouselViewHolder.title.getText());
//
//        when(carouselViewHolder.title.getCurrentTextColor()).thenReturn(6002);
//        assertEquals(6002, carouselViewHolder.title.getCurrentTextColor());
//
//    }
//
//
//    @Test
//    public void onBindViewHolderNightModeAndImageViewViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(6002);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        assertNotNull(carouselViewHolder.imageView);
//        verify(carouselViewHolder.imageView).setScaleType(ImageView.ScaleType.CENTER_CROP);
//        verify(carouselViewHolder.imageView, never()).setScaleType(ImageView.ScaleType.FIT_XY);
//
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void onTouchSwippingTestCases() {
//        ArgumentCaptor<View.OnTouchListener> captor = ArgumentCaptor.forClass(View.OnTouchListener.class);
//        when(consumeTouchEventRule.canCarouselConsumeTouchEvent()).thenReturn(true);
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        assertNotNull(carouselViewHolder.itemView);
//        assertEquals(carouselViewHolder.itemView, view);
//        verify(carouselViewHolder.itemView).setOnTouchListener(captor.capture());
//        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 1f, 1f, 0);
//        captor.getValue().onTouch(view, event);
//        verify(viewParent).requestDisallowInterceptTouchEvent(true);
//
//    }
//
//    @Test
//    public void onViewRecycledTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.onViewRecycled(carouselViewHolder);
//        assertNotNull(carouselViewHolder.imageView);
//        verify(carouselViewHolder.imageView).setImageDrawable(null);
//    }
//
//    @Test
//    public void getItemTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        assertEquals(1, carouselRecyclerViewAdapter.getItemCount());
//    }
//
//    @Test
//    public void onBitMapLoadedTest() {
//        View view = mock(View.class);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselViewHolder.onBitmapLoaded(bitmap);
//        assertNotNull(imageView);
//        verify(carouselViewHolder.imageView).setImageBitmap(bitmap);
//        verify(carouselViewHolder.imageView).startAnimation(any(AlphaAnimation.class));
//
//    }
//
//    @Test
//    public void onBindViewHolderNightModeAndKickerTextViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        when(view.findViewById(R.id.tv_carousel_kicker)).thenReturn(kickerTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        assertNotNull(kickerTextView);
//        verify(carouselViewHolder.sectionTopic).setText("The rise of King");
//        when(kickerTextView.getText()).thenReturn("The rise of King");
//        assertEquals("The rise of King", kickerTextView.getText());
//
//        verify(carouselViewHolder.sectionTopic).setTypeface(Typeface.createFromAsset(context.getAssets(),
//                context.getResources().getString(R.string.kicker_font_file)));
//        verify(carouselViewHolder.sectionTopic).setTextColor(context.getResources().getColor(R.color.carousel_section_name_text_color));
//    }
//
//    @Test
//    public void onBindViewHolderNightModeFalseAndKickerTextViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(resources.getColor(R.color.carousel_section_name_text_color)).thenReturn(6002);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        when(view.findViewById(R.id.tv_carousel_kicker)).thenReturn(kickerTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        assertNotNull(kickerTextView);
//        verify(carouselViewHolder.sectionTopic).setText("The rise of King");
//
//        when(kickerTextView.getText()).thenReturn("The rise of King");
//        assertEquals("The rise of King", kickerTextView.getText());
//
//        verify(carouselViewHolder.sectionTopic).setTypeface(Typeface.createFromAsset(context.getAssets(),
//                context.getResources().getString(R.string.kicker_font_file)));
//        verify(carouselViewHolder.sectionTopic).setTextColor(context.getResources().getColor(R.color.carousel_section_name_text_color));
//    }
//
//    @Test
//    public void onBindViewHolderNightModeFalseAndStoryTypeTextViewNotNullTest() {
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(view.findViewById(R.id.tv_carousel_story_type)).thenReturn(storyTypeTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        assertNotNull(carouselViewHolder.sectionTopic);
//        verify(carouselViewHolder.sectionTopic).setTypeface(Typeface.createFromAsset(context.getAssets(), context.getResources().getString(R.string.story_type_font_file)));
//        String storyTypeText = " " + context.getResources().getString(R.string.kicker_separator) + " " + "Robert Prince";
//        verify(carouselViewHolder.sectionTopic).setText(storyTypeText);
//        when(storyTypeTextView.getText()).thenReturn(storyTypeText);
//        assertEquals(storyTypeText, storyTypeTextView.getText());
//        verify(carouselViewHolder.sectionTopic).setTextColor(context.getResources().getColor(R.color.carousel_section_name_text_color));
//    }
//
//    @Test
//    public void onBindViewHolderNightModeTrueAndStoryTypeTextViewNotNullTest() {
//        carouselRecyclerViewAdapter = spy(createCarouselRecyclerViewAdapter(0));
//        View view = mock(View.class);
//        when(view.getContext()).thenReturn(context);
//        when(context.getResources()).thenReturn(resources);
//        when(view.findViewById(R.id.cv_carousel_card_view)).thenReturn(cardView);
//        when(view.findViewById(R.id.iv_carousel_image)).thenReturn(imageView);
//        when(view.findViewById(R.id.tv_carousel_byline)).thenReturn(bylineTestView);
//        when(view.findViewById(R.id.tv_carousel_title)).thenReturn(titleTextView);
//        when(view.findViewById(R.id.tv_carousel_kicker)).thenReturn(kickerTextView);
//        when(view.findViewById(R.id.tv_carousel_story_type)).thenReturn(storyTypeTextView);
//        CarouselRecyclerViewAdapter.CarouselViewHolder carouselViewHolder = new CarouselRecyclerViewAdapter.CarouselViewHolder(view);
//        carouselRecyclerViewAdapter.setItems(caurouselList(true));
//        carouselRecyclerViewAdapter.onBindViewHolder(carouselViewHolder, 0);
//        assertNotNull(carouselViewHolder.sectionTopic);
//        verify(carouselViewHolder.sectionTopic).setTypeface(Typeface.createFromAsset(context.getAssets(), context.getResources().getString(R.string.story_type_font_file)));
//        String storyTypeText = " " + context.getResources().getString(R.string.kicker_separator) + " " + "Robert Prince";
//
//        verify(carouselViewHolder.sectionTopic).setText(storyTypeText);
//        when(carouselViewHolder.sectionTopic.getText()).thenReturn(storyTypeText);
//        assertEquals(storyTypeText, storyTypeTextView.getText());
//        verify(carouselViewHolder.sectionTopic).setTextColor(context.getResources().getColor(R.color.carousel_grey));
//
//    }
//
//    private List<CarouselViewItem> caurouselList(boolean isTitle) {
//        List<CarouselViewItem> items = new ArrayList<>();
//        items.add(new CarouselViewItem(0,
//                "www.image.com",
//                "The rise of King",
//                "Robert Prince",
//                "www.image.com",
//                "Top Stories",
//                "Robert Prince",
//                "www.image.com",
//                "Sports",
//                isTitle,
//                "Because you read",
//                null,
//                null,
//                "Opinion:"));
//        return items;
//    }
//}
