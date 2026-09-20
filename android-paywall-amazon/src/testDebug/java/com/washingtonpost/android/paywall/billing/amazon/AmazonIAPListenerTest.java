package com.washingtonpost.android.paywall.billing.amazon;

import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by elamgodilj on 10/17/17.
 */
public class AmazonIAPListenerTest {

    @Test
    public void onPurchaseUpdatesResponse() throws Exception {
        // expired 6 months free trial subscription sample
        String sample = "{\n" +
                "    \"userData\": {\n" +
                "        \"marketplace\": \"US\",\n" +
                "        \"userId\": \"I5lrUe-MHD6e4FumZ0F2qvU_eZBUizL6zJ3aJdKzf-g=\"\n" +
                "    },\n" +
                "    \"receipts\": [\n" +
                "        {\n" +
                "            \"productType\": \"SUBSCRIPTION\",\n" +
                "            \"purchaseDate\": \"Apr 10, 2015 9:30:37 PM\",\n" +
                "            \"cancelDate\": \"Oct 10, 2015 9:30:37 PM\",\n" +
                "            \"receiptId\": \"IpZWp1UPHOLwPdvLup05RVaWbBpbcwtMQ_KMLULs-FU=:3:11\",\n" +
                "            \"productId\": \"M6-R\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"requestId\": {\n" +
                "        \"encodedId\": \"e895ff56-a349-4b36-af76-17c72ccd1f83\"\n" +
                "    },\n" +
                "    \"requestStatus\": \"SUCCESSFUL\",\n" +
                "    \"hasMore\": false\n" +
                "}";
        final PurchaseUpdatesResponse purchaseUpdatesResponse = new Gson().fromJson(sample, PurchaseUpdatesResponse.class);

        System.out.println(purchaseUpdatesResponse.getRequestStatus());
        assertNotNull(purchaseUpdatesResponse);
    }
}