package com.wapo.flagship.json;

import java.util.List;

/**
 * Created by kilarib on 3/10/14.
 */
public class VimeoMeta {
    public String cdn_url;
    public int view;
    public Request request;
    public String player_url;
    public Video video;
    public VBuild build;
    public Embed embed;
    public String vimeo_url;
    public User user;


    class Video{

    }
    public class Request{
        public Files files;

    }
    public class Files
    {
        public H264 h264;
        public List<MetaProfile> progressive;

    }
    public class H264
    {

        public MetaProfile mobile;
        public MetaProfile hd;
        public MetaProfile sd;
    }
    public class MetaProfile
    {
        public String profile;
        public String origin;
        public String url;
        public String id;
        public int height;
        public int width;
        public int bitrate;
        public int availability;


    }
    class VBuild{

    }
    class Embed{

    }
    class User{

    }

}
