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

public class AmUniversalFeed implements IComicsFeed, Serializable {
    private static final String TAG = AmUniversalFeed.class.getName();

    private static final String TitleTag = "title";
    private static final String PubDateTag = "pubDate";
    private static final String ImageLinkTag = "image_link";
    private static final String ChannelTag = "channel";
    private static final String ItemTag = "item";
    private static final String AssetTag = "asset";
    private static final String AssetTypeTag = "asset_type";
    private static final String ImageColorationTag = "image_coloration";

    private static final  HashMap<String, Utils.Action2<Object, String>> Tags = new HashMap<String, Utils.Action2<Object, String>>();
    private static SimpleDateFormat PubDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");//Fri, 26 Apr 2013 00:00:00 +0000

    private String _title;
    private List<IComicsFeedItem> _items = new ArrayList<IComicsFeedItem>();

    static {
        Tags.put(TitleTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((AmUniversalFeed)target)._title = value;
            }
        });
        Tags.put(PubDateTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                FeedItem item = (FeedItem)target;
                try {
                    item._pubDate = PubDateFormat.parse(value);
                } catch (ParseException e) {
                    Logger.w(TAG, Utils.exceptionToString(e));
                    item._pubDate = null;
                }
            }
        });
        Tags.put(ImageLinkTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((FeedItem)target)._link = value;
            }
        });
        //.equals(qName) || .equals(qName) || .equals(qName)
        Tags.put(AssetTypeTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((Asset)target).assetType = value;
            }
        });
        Tags.put(ImageColorationTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((Asset)target).imageColoration = value;
            }
        });
        Tags.put(ImageLinkTag, new Utils.Action2<Object, String>() {
            @Override
            public void run(Object target, String value) {
                ((Asset)target).imageLink = value;
            }
        });
    }

    public static AmUniversalFeed parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        SAXParser parser = factory.newSAXParser();
        AmUniversalFeed result = new AmUniversalFeed();
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

    private class Asset {
        public String assetType;
        public String imageColoration;
        public String imageLink;
    }

    private class ContentHandler extends DefaultHandler {
        private StringBuilder _accumulator = new StringBuilder();
        private boolean _isProcessing = false;
        private boolean _isChannel = false;
        private boolean _isComplete = false;
        private FeedItem _item;
        private boolean _isLinkComplete = false;
        private final Asset _asset = new Asset();
        private boolean _isAsset = false;

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
                _isAsset = false;
                _item = new FeedItem();
                _isLinkComplete = false;
                _items.add(_item);
                return;
            }

            if (_item != null && PubDateTag.equals(qName)) {
                setProcessing();
                return;
            }

            if (_item != null && !_isLinkComplete && AssetTag.equals(qName)) {
                _isAsset = true;
                _asset.assetType = null;
                _asset.imageLink = null;
                _asset.imageColoration = null;
                return;
            }

            if (_isAsset && (AssetTypeTag.equals(qName) || ImageColorationTag.equals(qName) || ImageLinkTag.equals(qName))) {
                setProcessing();
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (_isComplete) {
                return;
            }

            if (_isChannel && ChannelTag.equals(qName)) {
                _isComplete = true;
                return;
            }

            if (_isAsset  && AssetTag.equals(qName)) {
                if ("webready".equals(_asset.assetType)) {
                    if ("color".equals(_asset.imageColoration)) {
                        _item._link = _asset.imageLink;
                        _isLinkComplete = true;
                    } else if (_item._link == null) {
                        _item._link = _asset.imageLink;
                    }
                }
                _isAsset = false;
                return;
            }

            if (_item != null && ItemTag.equals(qName)) {
                //
                // close item tag
                _isChannel = true;
                _item = null;
                return;
            }

            if (_isProcessing) {
                if (_isAsset) {
                    if (Tags.containsKey(qName)) {
                        Tags.get(qName).run(_asset, _accumulator.toString());
                    }
                } else if (Tags.containsKey(qName)){
                    Tags.get(qName).run(_item != null ? _item : AmUniversalFeed.this, _accumulator.toString());
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
