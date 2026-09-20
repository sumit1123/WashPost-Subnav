///*
// * Copyright (c) 2019. The Washington Post. All rights reserved.
// */
//
//package com.wapo.flagship.features.articles.recycler.holders;
//
//import android.text.SpannableStringBuilder;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.wapo.flagship.features.articles.R;
//import com.wapo.flagship.features.articles.models.EmbeddedModelItem;
//import com.wapo.flagship.features.articles.recycler.AdapterHelper;
//import com.wapo.flagship.features.articles.recycler.ArticleContentHolder;
//import com.wapo.view.ProportionalLayout;
//import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//
///**
// * Created by Jayesh Elamgodil on 5/1/18.
// */
//public class EmbeddedContentViewHolder extends ArticleContentHolder {
//
//    TextView headlineText, blurbText, byLineText;
//    ProportionalLayout mediaSlot;
//    NetworkAnimatedImageView imageView;
//    Button openCloseButton, shareButton;
//    @NonNull
//    private final SimpleDateFormat simpleDateFormat;
//    @Nullable
//    private final EmbeddedContentClickListener embeddedContentClickListener;
//    boolean canDisplayDate;
//
//    public EmbeddedContentViewHolder(View itemView, boolean canDisplayDate, SimpleDateFormat simpleDateFormat, EmbeddedContentClickListener embeddedContentClickListener) {
//        super(itemView);
//        this.canDisplayDate = canDisplayDate;
//        this.simpleDateFormat = simpleDateFormat == null ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) : simpleDateFormat;
//        this.embeddedContentClickListener = embeddedContentClickListener;
//        blurbText = itemView.findViewById(R.id.tv_embedded_blurb);
//        headlineText = itemView.findViewById(R.id.tv_embedded_headline);
//        byLineText = itemView.findViewById(R.id.tv_embedded_byline);
//        mediaSlot = itemView.findViewById(R.id.pl_article_embedded_media_slot);
//        imageView = itemView.findViewById(R.id.article_media_image);
//        openCloseButton = itemView.findViewById(R.id.b_embedded_read);
//        shareButton = itemView.findViewById(R.id.b_embedded_share);
//    }
//
//    @Override
//    public void bind(Object item, final int position, final AdapterHelper helper) {
//        super.bind(item, position, helper);
//        if (!(item instanceof EmbeddedModelItem)) return;
//        final EmbeddedModelItem modelItem = (EmbeddedModelItem) item;
//        String deck = modelItem.getDeck();
//        String headline = modelItem.getHeadline();
//        String byline = modelItem.getByline();
//
//        if (headlineText != null) {
//            if (headline == null) {
//                headlineText.setVisibility(View.INVISIBLE);
//            } else {
//                headlineText.setVisibility(View.VISIBLE);
//                headlineText.setText(headline);
//            }
//        }
//
//        if (blurbText != null) {
//            if (deck == null) {
//                blurbText.setVisibility(View.INVISIBLE);
//            } else {
//                blurbText.setVisibility(View.VISIBLE);
//                blurbText.setText(deck);
//            }
//        }
//
//        if (byLineText != null) {
//            if (byline == null) {
//                byLineText.setVisibility(View.INVISIBLE);
//            } else {
//                byLineText.setVisibility(View.VISIBLE);
//                byLineText.setText(byline);
//            }
//
//            Date pubDate = helper.getArticle().getLmt();
//            final boolean showDisplayDate = pubDate != null && (canDisplayDate || helper.getArticle().isShowDisplayDate());
//
//            if (byline != null || showDisplayDate) {
//                final SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
//                if (byline != null) {
//                    stringBuilder.append(byline.toUpperCase());
//                }
//                if (showDisplayDate) {
//                    if (stringBuilder.length() > 0) stringBuilder.append(" | ");
//                    try {
//                        String formattedDate = simpleDateFormat.format(pubDate);
//                        if (formattedDate != null) {
//                            stringBuilder.append(formattedDate.toUpperCase());
//                        }
//                    } catch (Exception e) {
//                    }
//
//                }
//
//                byLineText.setText(stringBuilder);
//                byLineText.setVisibility(View.VISIBLE);
//            } else {
//                byLineText.setVisibility(View.VISIBLE);
//            }
//        }
//
//        final String imageURL = modelItem.getImageURL();
//        if (imageURL == null) {
//            if (mediaSlot != null) {
//                mediaSlot.setVisibility(View.GONE);
//            }
//        } else {
//            Integer imageWidth = modelItem.getImageWidth();
//            Integer imageHeight = modelItem.getImageHeight();
//            if (imageWidth != null && imageWidth > 0 && imageHeight != 0 && imageHeight != 0) {
//                if (mediaSlot != null) {
//                    mediaSlot.setVisibility(View.VISIBLE);
//                    mediaSlot.setAspectRatio((float) imageWidth / imageHeight);
//                }
//                if (imageView != null) {
//                    imageView.setVisibility(View.VISIBLE);
//                    imageView.setImageUrl(imageURL, helper.getImageLoader());
//                }
//            }
//        }
//
//        if (openCloseButton != null) {
//            openCloseButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    v.setSelected(!v.isSelected());
//                    if (embeddedContentClickListener != null) {
//                        embeddedContentClickListener.onOpenCloseClick(position, modelItem.getUrl());
//                    }
//                }
//            });
//        }
//        if (shareButton != null) {
//            shareButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (embeddedContentClickListener != null) {
//                        embeddedContentClickListener.onShareClick(modelItem.getUrl());
//                    }
//                }
//            });
//        }
//    }
//
//    @Override
//    public void unbind() {
//        super.unbind();
//
//    }
//
//    public interface EmbeddedContentClickListener {
//        void onOpenCloseClick(int position, String url);
//        void onShareClick(String shareUrl);
//    }
//}