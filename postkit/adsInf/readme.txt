Module Name: adsInf
Description: Reusable library that provides Google banner Ads.

Flow:

    ----------------------------------------------------------------------------------------------------------
    |    ----------------------------------      (1)  -----------------          -------------------------  |
    |   |    Android app                     |------>|   adsInf        |------>|   DFP - Remote Ad Server   |
    |   |    (WhiteLabel/Classic/Rainbow     |       |   (BannerAds)   |       |  ---------------------     |
    |   |                                    |<------|                 |<------|    Add Inventory    |      |
    |   -------------------------------------    (2)  _________________          ------------------------   |
    ----------------------------------------------------------------------------------------------------------

    (1) Request for PublisherAdView
    (2) return PublisherAdView

BannerAds:

Request Parameters:
        1. adSize : AdSize
        2. adUnitId : String
        3. adKey : String
        4. adKeyPrepend : String
        5. bestFit : enum BestFit (ON_SCREEN_WIDTH/ON_VIEW_WIDTH/NONE)
        6. adRequestTargets : enum AdRequestTargets (All custom targeting keyvalue pairs)

Usage:
1) new BannerAds.Builder(context)
                .setUnitId(string)
                .getAdView();
- Returns default adView - MEDIUM_RECTANGLE.


2) new BannerAds.Builder(context)
                .setUnitId(string)
                .setAdSize(adSize)
                .getAdView();
- Returns adView of size adSize(Standard Banner / Custom).

3) new BannerAds.Builder(context)
                .setUnitId(string)
                .setAdSize(adSize)
			    .setBestFit(ON_SCREEN_WIDTH)
                .getAdView();
- Returns adView of size adSize if device screen width can accommodate width space of adSize.
    Following is the priority to create an adView with adSizes:
    1.Required(if any) / 2.Default(if any) / 3.BestFit(if any) / 4. MEDIUM_RECT

4) new BannerAds.Builder(context)
                .setUnitId(string)
                .setAdSize(adSize)
                .setBestFit(ON_VIEW_WIDTH)
                .getAdView();
- Returns adView of size adSize if parent view width can accommodate width space of adSize.
    Following is the priority to create an adView with adSizes:
    1.Required(if any) / 2.Default(if any) / 3.BestFit(if any) / 4. MEDIUM_RECT.

5) new BannerAds.Builder(context)
                .setUnitId(string)
                .setBestFit(ON_VIEW_WIDTH)
                .getAdView();
- Returns adView of size adSize if parent view width can accommodate width space of adSize.
    Following is the priority to create an adView with adSizes:
    1.Default(if any) / 2.BestFit(if any) / 3. MEDIUM_RECT.

Ad Targets:
6)  BannerAds.AdRequestTargets adTargets = new BannerAds.AdRequestTargets();
    Map<String, List<String>> map = new HashMap<>();
    map.put("pos", Arrays.asList("flex"));
    map.put("pv2", "15135074");
    adTargets.setCustomTargetsMap(map);
    View tGAMAdView = new BannerAds.Builder(context)
                                    .setAdUnitId("adUnitId")
                                    .setAdSize(AdSize.MEDIUM_RECTANGLE)
                                    .build()
                                    .getAdView();

interface AdViewLifeCycleCallbacks:
7) Classes(who owns adViews) can implement this interface and can change adViews states based on Activities/Fragments life cycle states.
    BannerAds class has static methods "BannerAds#pauseAdView(View)", "BannerAds#resumeAdView(View)" and "BannerAds#destroyAdView(View)"
    to handle adViews states.

Moat tracking:
8) BannerAds class has Moat integration code from adsAnalyticsInf library.
    Moat tracking can be enabled while creating an adview:
                        View view = new BannerAds.Builder(context)
                                .setAdKey(item.getAdKey())
                                .setAdSize(AdSize.MEDIUM_RECTANGLE)
                                .setTracking(true)
                                .build()
                                .getAdView();
    BannerAds class will take care of start tracking once "onAdLoaded" method is called.
    But the class who holds an AdView should take care of destroying that adView using BannerAds.destroyAdView static method.
    Also the class who holds an AdView should call AdView's life cycle methods using BannerAds static methods based on Activities/Fragments life cycle states.


Notes:
1) Library supports "DFP – Double Click For Publishers" right now. Other AdView support can be added later if required.




