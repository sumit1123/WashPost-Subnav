/*
 *
 *  *  Copyright (c) 2018. The Washington Post. All rights reserved.
 *
 */
package com.wapo.android.push;

import java.util.List;
import java.util.UUID;

/**
 * Created by kattim on 6/15/17.
 */

public class ABTestGenerator {

    /**
     * Take in a notification which has 3 possible headlines, titles, and urls and generate selections
     * based on a given display chance for each
     * @param notification
     * @return
     */
    public PushNotification runTest(PushNotification notification) {
        final int A = 0;
        final int B = 1;
        final int C = 2;

        List<PushNotification.SplitPushTestingDetails> splitPushes = notification.getSplitPushTestingDetails();
        final String deviceId = UUID.randomUUID().toString();

        //let the defaults values be selection A
        String selectedHeadline = splitPushes.get(A).headline;
        String selectedTitle = splitPushes.get(A).title;
        String selectedUrl = splitPushes.get(A).url;
        String selectedCategory = splitPushes.get(A).category;

        int uniqueNum = (deviceId.hashCode() < 0 ? deviceId.hashCode() * -1 : deviceId.hashCode()) % 100;
        int indexB = splitPushes.get(A).displayChance + splitPushes.get(B).displayChance;
        if (uniqueNum >= splitPushes.get(A).displayChance && uniqueNum < indexB) {
            selectedHeadline = splitPushes.get(B).headline;
            selectedTitle = splitPushes.get(B).title;
            selectedUrl = splitPushes.get(B).url;
            selectedCategory = splitPushes.get(B).category;
        } else if (splitPushes.size() == 3 && uniqueNum >= indexB) {
            selectedHeadline = splitPushes.get(C).headline;
            selectedTitle = splitPushes.get(C).title;
            selectedUrl = splitPushes.get(C).url;
            selectedCategory = splitPushes.get(C).category;
        }

        notification.setHeadline(selectedHeadline);
        notification.setTitle(selectedTitle);
        notification.setUrl(selectedUrl);
        notification.setCategory(selectedCategory);

        return notification;
    }

}
