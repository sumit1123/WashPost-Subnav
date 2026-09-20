package com.washingtonpost.android.comics.model;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class ComicStrip extends ComicItem implements Parcelable {

    public ComicStrip() {

    }

    public static Parcelable.Creator<ComicStrip> CREATOR = new Parcelable.Creator<ComicStrip>() {
        @Override
        public ComicStrip createFromParcel(Parcel source) {
            return new ComicStrip(source);
        }

        @Override
        public ComicStrip[] newArray(int size) {
            return new ComicStrip[0];
        }
    };

    @SerializedName("_id")
    private String Id;
    private String name;
    private String author;
    private String provider;
    private Date published;
    private int width;
    private int height;
    private String url;

    protected ComicStrip(Parcel source) {
        Id = source.readString();
        name = source.readString();
        author = source.readString();
        provider = source.readString();
        published = (java.util.Date) source.readSerializable();
        width = source.readInt();
        height = source.readInt();
        url = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Id);
        dest.writeString(name);
        dest.writeString(author);
        dest.writeString(provider);
        dest.writeSerializable(published);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeString(url);
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "ComicStrip{" +
                "Id='" + Id + '\'' +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", provider='" + provider + '\'' +
                ", published=" + published +
                ", width=" + width +
                ", height=" + height +
                ", url='" + url + '\'' +
                '}';
    }

}
