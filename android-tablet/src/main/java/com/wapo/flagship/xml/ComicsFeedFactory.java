package com.wapo.flagship.xml;

import android.net.Uri;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class ComicsFeedFactory {
    private static final HashMap<String, Func> Feeds = new HashMap<String, Func>();

    static {
        Func creatorsParser = new Func() {
            @Override
            public IComicsFeed run(InputStream is) throws IOException, SAXException, ParserConfigurationException {
                return CreatorsFeed.parse(is);
            }
        };

        Func cartoonistParser = new Func() {
            @Override
            public IComicsFeed run(InputStream is) throws IOException, SAXException, ParserConfigurationException {
                return CartoonistGroupFeed.parse(is);
            }
        };
        Func amUniversalParser = new Func() {
            @Override
            public IComicsFeed run(InputStream is) throws IOException, SAXException, ParserConfigurationException {
                return AmUniversalFeed.parse(is);
            }
        };

        Feeds.put("www.creators.com", creatorsParser);
        Feeds.put("safr.kingfeatures.com", cartoonistParser);
        Feeds.put("cartoonistgroup.com", cartoonistParser);
        Feeds.put("feedsservice.amuniversal.com", amUniversalParser);
    }

    public static IComicsFeed parse(String url, byte [] body) throws IOException, SAXException, ParserConfigurationException {
        return parse(url, new ByteArrayInputStream(body));
    }

    public static IComicsFeed parse(String url, InputStream is) throws IOException, SAXException, ParserConfigurationException {
        Uri uri = Uri.parse(url);
        return Feeds.containsKey(uri.getHost()) ? Feeds.get(uri.getHost()).run(is) : null;
    }



    private interface Func {
        public IComicsFeed run(InputStream is) throws ParserConfigurationException, SAXException, IOException;
    }
}
