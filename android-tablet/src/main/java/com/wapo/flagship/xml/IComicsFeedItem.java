package com.wapo.flagship.xml;

import java.io.Serializable;
import java.util.Date;

public interface IComicsFeedItem extends Serializable {
    String getUrl();
    Date getPubDate();
}
