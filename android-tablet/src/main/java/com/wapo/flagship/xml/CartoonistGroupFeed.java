package com.wapo.flagship.xml;

import com.wapo.flagship.Utils;
import com.wapo.android.commons.util.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class CartoonistGroupFeed implements IComicsFeed, Serializable {
    private static final String TAG = CartoonistGroupFeed.class.getName();
    private static final HashMap<String, Utils.Action2<Object, String>> Tags;

    private static final String ChannelTag = "channel";
    private static final String ItemTag = "item";
    private static final String TitleTag = "title";
    private static final String PubDateTag = "pubDate";
    private static final String MediaContentTag = "media:content";

    private static final Pattern BigZDateFormatRegEx = Pattern.compile(".+ [+|-]?[0-9]{4}", Pattern.CASE_INSENSITIVE);
    private static SimpleDateFormat PubDateFormatBigZ = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");//Fri, 26 Apr 2013 00:00:00 EDT
    private static SimpleDateFormat PubDateFormatSmallZ = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");//Fri, 26 Apr 2013 00:00:00 -0400


    private String _title;
    private List<IComicsFeedItem> _items = new ArrayList<IComicsFeedItem>();

    static {
        Tags = new HashMap<String, Utils.Action2<Object, String>>();
        Tags.put(TitleTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((CartoonistGroupFeed)target)._title = value;
            }
        });
        Tags.put(PubDateTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                FeedItem item = (FeedItem)target;
                item._pubDate = null;
                if (value == null) {
                    return;
                }

                try {
                    item._pubDate = BigZDateFormatRegEx.matcher(value).matches() ?
                                        PubDateFormatBigZ.parse(value) :
                                        PubDateFormatSmallZ.parse(value);
                } catch (ParseException e) {
                    Logger.w(TAG, Utils.exceptionToString(e));
                }
            }
        });
        Tags.put(MediaContentTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((FeedItem)target)._link = value;
            }
        });
    }

    public static CartoonistGroupFeed parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser parser = factory.newSAXParser();
        CartoonistGroupFeed result = new CartoonistGroupFeed();
        parser.parse(is, result.getHandler());
        return result;
    }

    @Override
    public String getTitle() {
        return _title;
    }

    @Override
    public List<IComicsFeedItem> getItems() {
        return _items;
    }

    private DefaultHandler getHandler() {
        return new ContentHandler();
    }



    private class FeedItem implements IComicsFeedItem, Serializable {
        private Date _pubDate;
        private String _link;

        @Override
        public String getUrl() {
            return _link;
        }

        @Override
        public Date getPubDate() {
            return _pubDate;
        }
    }

    private class ContentHandler extends DefaultHandler {
        final Pattern mediaNsRegEx = Pattern.compile("http://search.yahoo.com/mrss/?", Pattern.CASE_INSENSITIVE);

        private boolean _isProcessing = false;
        private StringBuilder _accumulator = new StringBuilder();
        private boolean _isChannel = false;
        private FeedItem _item = null;
        private boolean _isComplete = false;


        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (_isComplete) {
                return;
            }
            if (ChannelTag.equals(qName)) {
                _isChannel = true;
                return;
            }

            if (_isChannel && TitleTag.equals(qName)) {
                setProcessing();
                return;
            }

            if (_isChannel && ItemTag.equals(qName)) {
                _isChannel = false;
                _item = new FeedItem();
                _items.add(_item);
                return;
            }

            if (_item != null && PubDateTag.equals(qName)) {
                setProcessing();
                return;
            }

            if (_item != null && mediaNsRegEx.matcher(uri).matches()) {
                String [] parts = qName.split(":");
                if ("content".equals(parts.length == 2 ? parts[1] : parts[0])) {
                    String value = attributes.getValue("url");
                    if (value != null && Tags.containsKey(MediaContentTag)) {
                        Tags.get(MediaContentTag).run(_item, value);
                    }
                }
                return;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (_isComplete) {
                return;
            }

            if (_isChannel && ChannelTag.equals(qName)) {
                _isComplete = true;
            }

            if (_item != null && ItemTag.equals(qName)) {
                _item = null;
                _isChannel = true;
            }

            if (_isProcessing) {
                if (Tags.containsKey(qName)) {
                    Tags.get(qName).run(_item != null ? _item : CartoonistGroupFeed.this, _accumulator.toString());
                }
                _isProcessing = false;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (_isComplete) {
                return;
            }

            if (_isProcessing) {
                _accumulator.append(ch, start, length);
            }
        }

        private void setProcessing() {
            _isProcessing = true;
            _accumulator.delete(0, _accumulator.length());
        }
    }
}
