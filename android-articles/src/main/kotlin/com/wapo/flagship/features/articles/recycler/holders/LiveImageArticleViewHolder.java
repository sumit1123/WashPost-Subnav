///*
// * Copyright (c) 2018. The Washington Post. All rights reserved.
// */
//
//package com.wapo.flagship.features.articles.recycler.holders;
//
//import android.view.View;
//
//import com.wapo.flagship.features.articles.R;
//import com.wapo.flagship.features.articles.models.MediaItem;
//import com.wapo.flagship.features.articles.recycler.AdapterHelper;
//import com.wapo.flagship.features.articles.recycler.ArticleContentHolder;
//import com.wapo.view.ProportionalLayout;
////import com.washingtonpost.android.androidlive.liveimage.LiveImageView;
//
//
///**
// * Created by Jayesh Elamgodil on 5/9/18.
// */
//public class LiveImageArticleViewHolder extends ArticleContentHolder {
//
//    private ProportionalLayout imageLayout;
//    //private LiveImageView liveImageView;
//
//
//    public LiveImageArticleViewHolder(View itemView) {
//        super(itemView);
//        imageLayout = itemView.findViewById(R.id.pl_live_image);
//        //liveImageView = itemView.findViewById(R.id.iv_live_image);
//    }
//
//    @Override
//    public void bind(Object item, int position, AdapterHelper helper) {
//        super.bind(item, position, helper);
////        if (!(item instanceof MediaItem)) return;
////
////        MediaItem mediaItem = (MediaItem) item;
////        final String imageURL = mediaItem.getSurfaceUrl();
////        final int imageWidth = mediaItem.getImageWidth();
////        final int imageHeight = mediaItem.getImageHeight();
////
////        if (imageURL != null) {
////            LiveImageView.LiveImageBitmapLoadedListener listener = null;
////            if (imageLayout != null) {
////                if (imageWidth > 0 && imageHeight > 0) {
////                    imageLayout.setAspectRatio(imageWidth / imageHeight);
////                } else {
////                    listener = new LiveImageView.LiveImageBitmapLoadedListener() {
////                        private int imageWidth  = 0, imageHeight = 0;
////                        private boolean setAspectRatio;
////                        @Override
////                        public void onBitmapLoaded(int imageWidth, int imageHeight) {
////                            if (this.imageWidth != imageWidth) {
////                                this.imageWidth = imageWidth;
////                                setAspectRatio = true;
////                            }
////                            if (this.imageHeight != imageHeight) {
////                                this.imageHeight = imageHeight;
////                                setAspectRatio = true;
////                            }
////                            if (this.imageWidth > 0 && this.imageHeight > 0 && setAspectRatio) {
////                                setAspectRatio = false;
////                                if (imageLayout != null) {
////                                    imageLayout.setAspectRatio(imageWidth / imageHeight);
////                                }
////                            }
////                        }
////                    };
////                }
////            }
////
////            if (liveImageView != null) {
////                liveImageView.setData(null, listener, imageURL, imageWidth, imageHeight, false);
////            }
////        }
//    }
//}
