///*
// * Copyright (c) 2019. The Washington Post. All rights reserved.
// */
//
//package com.wapo.flagship.features.articles.recycler.holders;
//
//import android.content.Context;
//import android.content.res.Resources;
//import android.util.DisplayMetrics;
//import android.util.TypedValue;
//import android.view.View;
//
//import com.wapo.flagship.features.articles.models.AnimatedVideoData;
//import com.wapo.flagship.features.articles.models.VideoData;
//import com.wapo.flagship.features.articles.recycler.AdapterHelper;
//import com.wapo.flagship.features.articles.recycler.video.InlinePlayableMediaHolder;
//import com.wapo.flagship.features.posttv.players.legacy.view.GIFPlayerView;
//import com.wapo.view.ProportionalLayout;
//import com.wapo.flagship.features.articles.R;
//
//import static android.util.TypedValue.COMPLEX_UNIT_DIP;
//
//public class AnimatedVideoViewHolder extends InlinePlayableMediaHolder implements GIFPlayerView.GIFPlayerViewListener {
//
//    private GIFPlayerView gifPlayerView;
//    private View progressBar;
//
//    public AnimatedVideoViewHolder(View itemView) {
//        super(itemView);
//    }
//
//    @Override
//    public void bind(Object item, int position, AdapterHelper helper) {
//        if (item instanceof AnimatedVideoData) {
//            final AnimatedVideoData videoData = (AnimatedVideoData) item;
//            if (videoData.videoUrl == null) return;
//            Context context = itemView.getContext();
//            Resources res = context.getResources();
//            DisplayMetrics dimen = res.getDisplayMetrics();
//            int top = videoData.getTopPadding() == null ?
//                    res.getDimensionPixelSize(R.dimen.article_media_top_padding) :
//                    (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, videoData.getTopPadding(), dimen);
//            int bottom = videoData.getBottomPadding() == null ?
//                    res.getDimensionPixelSize(R.dimen.article_media_padding) :
//                    (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, videoData.getBottomPadding(), dimen);
//            itemView.setPadding(this.itemView.getPaddingLeft(), top, this.itemView.getPaddingRight(), bottom);
//            setViewDimensions(videoData.getPreviewWidth(), videoData.getPreviewHeight());
//            //setImagePreviewForVideo(videoData, helper.getImageLoader());
//            //setVideoCaption(videoData, helper);
//            progressBar = itemView.findViewById(R.id.pb_inline_video);
//            gifPlayerView = itemView.findViewById(R.id.gifPlayer);
//            gifPlayerView.setData(videoData.videoUrl, videoData.loop == -1, videoData.autoPlay, this);
//
//        }
//    }
//
//    private void setViewDimensions(int width, int height) {
//        if (width == 0 || height == 0) {
//            //setFixedHeight();
//        }
//        ProportionalLayout pl = itemView.findViewById(R.id.pl_inline_video);
//        if (pl != null) {
//            pl.setAspectRatio((float) width/height);
//        }
//    }
//
//    protected void playVideo(VideoData videoData) {
//    }
//
//    private void toggleThumbnailImageVisibility(boolean isVisible) {
//        if (getImageView() != null) {
//            getImageView().setVisibility(isVisible ? View.VISIBLE : View.GONE);
//        }
//    }
//
//    private void toggleProgressBarVisibility(boolean isVisible) {
//        if (progressBar != null) {
//            progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
//        }
//    }
//
//    // GIFPlayerView.GIFPlayerViewListener
//
//    @Override
//    public void onGIFPlayerReadyToPlay() {
//        toggleThumbnailImageVisibility(false);
//    }
//
//    @Override
//    public void onGiFPlayerClick() {
//
//    }
//
//    @Override
//    public void onGIFPlayerLoading() {
//        toggleProgressBarVisibility(true);
//    }
//
//    @Override
//    public void onGIFPlayerPlaying() {
//        toggleProgressBarVisibility(false);
//    }
//    // GIFPlayerView.GIFPlayerViewListener
//}
