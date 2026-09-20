///*
// * Copyright (c) 2018. The Washington Post. All rights reserved.
// */
//
//package com.wapo.flagship.features.articles.recycler.holders;
//
//import android.content.Context;
//import androidx.annotation.NonNull;
//import androidx.core.content.ContextCompat;
//
//import android.view.View;
//import android.widget.TextView;
//
//import com.wapo.flagship.features.articles.R;
//import com.wapo.flagship.features.articles.models.LiveBlogArticleModelItem;
//import com.wapo.flagship.features.articles.recycler.AdapterHelper;
//import com.wapo.flagship.features.articles.recycler.ArticleContentHolder;
//import com.wapo.flagship.features.articles.recycler.ArticleItemsClick;
//import com.wapo.flagship.features.articles.recycler.ArticleItemsClickProvider;
//import com.wapo.text.WpTextUtil;
//import com.wapo.view.ProportionalLayout;
//import com.washingtonpost.android.androidlive.liveblog.view.LiveBlogView;
//import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView;
//
///**
// * Created by Jayesh Elamgodil on 5/3/18.
// */
//public class LiveBlogArticleViewHolder extends ArticleContentHolder {
//
//    private TextView tv_title, tv_headline, tv_deck;
//    private NetworkAnimatedImageView imageView;
//    private ProportionalLayout imageLayout;
//    private TextView moreUpdatesView;
//    private LiveBlogView liveBlogView;
//
//
//    public LiveBlogArticleViewHolder(View itemView) {
//        super(itemView);
//        tv_title = itemView.findViewById(R.id.tv_live_blog_title);
//        tv_headline = itemView.findViewById(R.id.tv_live_blog_headline);
//        tv_deck = itemView.findViewById(R.id.tv_live_blog_deck);
//        imageLayout = itemView.findViewById(R.id.layout_live_blog_image);
//        imageView = itemView.findViewById(R.id.iv_live_blog_image);
//        moreUpdatesView = itemView.findViewById(R.id.tv_live_blog_more_updates);
//        liveBlogView = itemView.findViewById(R.id.live_blog_view);
//    }
//
//    @Override
//    public void bind(Object item, int position, final AdapterHelper helper) {
//        super.bind(item, position, helper);
//        if (!(item instanceof LiveBlogArticleModelItem)) return;
//
//        final LiveBlogArticleModelItem modelItem = (LiveBlogArticleModelItem) item;
//
//        final String title = modelItem.getTitle();
//        final String headline = modelItem.getHeadline();
//        final String deck = modelItem.getDeck();
//        final String contentURL = modelItem.getContentURL().endsWith("/") ? modelItem.getContentURL() : modelItem.getContentURL() + "/";
//
//        Context context = itemView.getContext();
//        int textColor = ContextCompat.getColor(itemView.getContext(), helper.isNightMode() ? R.color.live_blog_text_color_night : R.color.live_blog_text_color);
//        if (tv_title != null) {
//            tv_title.setTextColor(textColor);
//            if (title != null) {
//                tv_title.setVisibility(View.VISIBLE);
//                tv_title.setText(WpTextUtil.getSpannableStringBuilder(context, title, R.style.article_live_blog_title, helper.isNightMode()));
//            } else {
//                tv_title.setVisibility(View.GONE);
//            }
//        }
//        if (tv_headline != null) {
//            tv_headline.setTextColor(textColor);
//            if (headline != null) {
//                tv_headline.setVisibility(View.VISIBLE);
//                tv_headline.setText(WpTextUtil.getSpannableStringBuilder(context, headline, R.style.article_live_blog_headline, helper.isNightMode()));
//            } else {
//                tv_headline.setVisibility(View.GONE);
//            }
//        }
//        if (tv_deck != null) {
//            tv_deck.setTextColor(textColor);
//            if (deck != null) {
//                tv_deck.setVisibility(View.VISIBLE);
//                tv_deck.setText(WpTextUtil.getSpannableStringBuilder(context, deck, R.style.article_live_blog_deck, helper.isNightMode()));
//            } else {
//                tv_deck.setVisibility(View.GONE);
//            }
//        }
//        if (moreUpdatesView != null) {
//            String moreUpdates = context.getString(R.string.live_blog_more_updates);
//            if (moreUpdates != null) {
//                moreUpdatesView.setText(WpTextUtil.getSpannableStringBuilder(context, moreUpdates, R.style.article_live_blog_more_updates, helper.isNightMode()));
//            }
//            moreUpdatesView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    onLiveBlogClick(contentURL, helper.getArticleItemsClickProvider());
//                }
//            });
//        }
//
//        final String imageURL = modelItem.getImageURL();
//        final int imageWidth = modelItem.getImageWidth();
//        final int imageHeight = modelItem.getImageHeight();
//        if (imageURL != null) {
//            if (imageWidth > 0 && imageHeight > 0) {
//                if (imageLayout != null) {
//                    if (imageLayout instanceof ProportionalLayout) {
//                        imageLayout.setVisibility(View.VISIBLE);
//                        imageLayout.setAspectRatio((float) imageWidth / imageHeight);
//                    } else {
//                        imageLayout.setVisibility(View.GONE);
//                    }
//                }
//            }
//            if (imageView instanceof NetworkAnimatedImageView) {
//                imageView.setImageUrl(imageURL, helper.getImageLoader());
//            }
//        }
//
//        if (liveBlogView instanceof LiveBlogView) {
//            //liveBlogView.setData(modelItem.getPrimetimeURL(), helper.getLiveBlogServiceURL(), modelItem.getMaxEntries(),
//            liveBlogView.setData(modelItem.getPrimetimeURL(), "https://d26i4oyvu57sjt.cloudfront.net/content/?after=LIVE_BLOG_TIMESTAMP&select=cms_date,cms_title,cms_modified,transformed_content.slug&limit=LIVE_BLOG_MAX_ENTRIES&src_url=", modelItem.getMaxEntries(),
//                    R.layout.article_live_blog_item, helper.isNightMode(), R.style.article_live_blog_item_title_style,
//                    R.style.article_live_blog_item_date_style, new LiveBlogView.LiveBlogItemClickListener() {
//                        @Override
//                        public void onLiveBlogItemClick(@NonNull String link) {
//                            String liveBlogItemURL = contentURL + link;
//                            onLiveBlogClick(liveBlogItemURL, helper.getArticleItemsClickProvider());
//                        }
//                    });
//        }
//    }
//
//    private void onLiveBlogClick(String url, ArticleItemsClickProvider articleItemsClickProvider) {
//        final ArticleItemsClick articleItemsClick = articleItemsClickProvider.getArticleItemsClick();
//        if (articleItemsClick != null) {
//            //articleItemsClick.onLiveBlogClick(url);
//        }
//    }
//}
