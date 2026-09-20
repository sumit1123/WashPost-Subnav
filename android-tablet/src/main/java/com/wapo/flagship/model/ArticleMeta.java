package com.wapo.flagship.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.wapo.flagship.features.articles.ArticleLinkType;

import java.io.Serializable;

/**
 * Created by maxx on 3/5/14.
 */
public class ArticleMeta implements Serializable, Parcelable {

    public static final String UUID_URL_PREFIX = "uuid://";
    public String id;
    public boolean isUuid;
    public ArticleLinkType articleLinkType;
    public long lastModified;
    public boolean bypassCache;
    public String anchorId;
    public String deepestScrollId;
    public long listenDepthSec;

    public ArticleMeta() { }

    public ArticleMeta(String id, boolean isUuid) {
        this(id, isUuid, ArticleLinkType.NONE);
    }

    public ArticleMeta(String id, boolean isUuid, ArticleLinkType articleLinkType) {
        this(id, isUuid, articleLinkType, 0L);
    }

    public ArticleMeta(String id, boolean isUuid, ArticleLinkType articleLinkType, long lastModified) {
        this.id = id;
        this.isUuid = isUuid;
        this.articleLinkType = articleLinkType;
        this.lastModified = lastModified;
    }

    protected ArticleMeta(Parcel in) {
        id = in.readString();
        isUuid = in.readByte() != 0;
        articleLinkType = ArticleLinkType.valueOf(in.readString());
        lastModified = in.readLong();
        bypassCache = in.readInt() != 0;
        anchorId = in.readString();
        deepestScrollId = in.readString();
        listenDepthSec = in.readLong();
    }

    public static final Creator<ArticleMeta> CREATOR = new Creator<ArticleMeta>() {
        @Override
        public ArticleMeta createFromParcel(Parcel in) {
            return new ArticleMeta(in);
        }

        @Override
        public ArticleMeta[] newArray(int size) {
            return new ArticleMeta[size];
        }
    };

    @Override
    public String toString() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleMeta that = (ArticleMeta) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeByte((byte) (isUuid ? 1 : 0));
        dest.writeString((articleLinkType != null)? articleLinkType.name(): ArticleLinkType.NONE.name());
        dest.writeLong(lastModified);
        dest.writeInt(bypassCache ? 1 : 0);
        dest.writeString(anchorId);
        dest.writeString(deepestScrollId);
        dest.writeLong(listenDepthSec);
    }
}
