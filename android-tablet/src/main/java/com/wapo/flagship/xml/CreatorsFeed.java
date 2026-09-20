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

public class CreatorsFeed implements IComicsFeed, Serializable {
    private static final String TAG = CreatorsFeed.class.getName();
    private static final String ChannelTag = "channel";
    private static final String ItemTag = "item";
    private static final String TitleTag = "title";
    private static final String EnclosureTag = "enclosure";
    private static final String PubDateTag = "pubDate";

    private static HashMap<String, Utils.Action2<Object, String>> Tags = new HashMap<String, Utils.Action2<Object, String>>();
    private static SimpleDateFormat PubDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
    private String _title;
    private List<IComicsFeedItem> _items = new ArrayList<IComicsFeedItem>();

    static {
        Tags.put(TitleTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((CreatorsFeed)target)._title = value;
            }
        });
        Tags.put(EnclosureTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((FeedItem)target)._url = value;
            }
        });
        Tags.put(PubDateTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                FeedItem item = (FeedItem)target;
                item._pubDate = null;
                try {
                    item._pubDate = PubDateFormat.parse(value);
                } catch (ParseException e) {
                    Logger.w(TAG, Utils.exceptionToString(e));
                }
            }
        });
    }

    public static CreatorsFeed parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        SAXParser parser = factory.newSAXParser();
        CreatorsFeed result = new CreatorsFeed();
        parser.parse(is, result.getHandler());
        return result;
    }

    private DefaultHandler getHandler() {
        return new ContentHandler();
    }

    @Override
    public String getTitle() {
        return _title;
    }

    @Override
    public List<IComicsFeedItem> getItems() {
        return _items;
    }

    private class FeedItem implements IComicsFeedItem, Serializable {
        private String _url;
        private Date _pubDate;

        @Override
        public String getUrl() {
            return _url;
        }

        @Override
        public Date getPubDate() {
            return _pubDate;
        }
    }

    private class ContentHandler extends DefaultHandler {
        private boolean _isChannel = false;
        private boolean _isProcessing = false;
        private StringBuilder _accumulator = new StringBuilder();
        private boolean _isComplete = false;
        private FeedItem _item;

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

            if (_item != null && EnclosureTag.equals(qName)) {
                String url = attributes.getValue("url");
                if (url != null && Tags.containsKey(EnclosureTag)) {
                    Tags.get(EnclosureTag).run(_item, url);
                }
                return;
            }

            if (_item != null && PubDateTag.equals(qName)) {
                setProcessing();
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (_isComplete) {
                return;
            }

            if (ChannelTag.equals(qName)) {
                _isComplete = true;
            }

            if (_item != null && ItemTag.equals(qName)) {
                _isChannel = true;
                _item = null;
            }

            if (_isProcessing) {
                if (Tags.containsKey(qName)) {
                    Tags.get(qName).run(_item != null ? _item : CreatorsFeed.this, _accumulator.toString());
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
