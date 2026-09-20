package com.wapo.flagship.xml;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface IComicsFeed extends Serializable {
    String getTitle();
    List<IComicsFeedItem> getItems();
}
