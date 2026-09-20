package com.washingtonpost.android.paywall.newdata.model;

import java.util.Arrays;
import java.util.List;

/**
 * Created by kilarib on 3/23/17.
 */
public class GroupLimit {

    public final int groupId;
    public final List sectionIds;
    public final int limit;

    public GroupLimit(int groupId, String[] sectionIds, int limit){
        this.groupId = groupId;
        this.sectionIds = Arrays.asList(sectionIds);
        this.limit = limit;
    }
}
