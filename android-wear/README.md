# android-wear

WaPo Android Wear (Wear 2.0)

<b>Project Specifications</b>

<b>Getting Article Data from JSONapp</b>

Classes: ArticleAsyncTask, AsyncResponse

ArticleAsynctask is a AsyncTask that makes a HttpUrlConnection with the JSONapp url. It is used to gather a content from an article and receive
a list of articles in a given section. AsyncResponse is an interface that has a method called processFinish, which is called after the
ArticleAsyncTask has finished parsing the JSON data from the JSONapp url.

Example:
WearArticleActivity implements AsyncResponse. It also creates an ArticleAsyncTask with the article url that it needs to open. After the JSON file of the
article is done processing in the ArticleAsyncTask, the ArticleAsyncTask will call upon WearArticleActivity's processFinish method and pass in the parsed JSON
as a parameter. Then, the processFinish method in WearArticleActivity will manipulate the string and output article content to the view.

<b>Complications</b>

Complications are any features on the Watch Face that displays some information other than time. For the Washington Post app, there are
two types of complications: headlines complication that display the top story headline of the day, and alerts complication that display
the number of notifications. They are packaged as a part of the wear app in the manifest as services (HeadlinesProviderService and
AlertsProviderService).

Complications are defined as services, and extends ComplicationProviderService class. When extending off of ComplicationProviderService,
the class needs to override OnComplicationUpdate method that is called whenever the complication is updated. There are multiple meta-data fields that you can
define for complications, such as the update time (in seconds. For refresh on tap, set to 0 and implement another toggle receiver).
There are different types of complications such as LONG_TEXT for long text complication, SHORT_TEXT for short text complication, and more.
Complication services do not have any control of how the data will be displayed in different Watch Faces. They can only send data
to the Watch Face, and it is up to the Watch Face to specify how to render the complications.

1. Headline
The headline complication displays the title of the first story from the Top Stories section. It updates every 300 seconds automatically.
When the user taps on the complication, the app starts WearArticleActivity that displays the first six paragraphs of the article. If there is
no network connection, then the complication will display a message "please connect to a network." If a user connects to a network and taps
the complication, the complication will update to display the top story headline.

2. Alerts
Displays the amount of notifications that a user has.

<b>Communication between Wear and Mobile</b>

Classes Involved: MobileNotifSender, DataLayerListenerService, (Mobile) WearDataLayerListenerService

Background:

Wear 2.0 can connect to a network by itself without having bluetooth connection to a mobile device.
Wear and Mobile devices can send datas and messages to each other using the Wearable Data Layer API,
which is part of the Google Play services.

Data Items - A DataItem "provides data storage with automatic syncing between the handheld and wearable"

The communication between the WaPo mobile and wear app is possible through Data Items and listeners
associated with the Data Layer that listens to Data Item changes.

One important thing to note: services that listen to the Data Layer API will not be notified unless the
DataItem is different. That is, if the mobile app puts the same DataItem more than once into the Data Layer,
a listener service in the wear app will only be called once. To avoid this, one hack is to put unique data such as
current request time.

Specifics:

1. Notification Fragment (MobileNotifSender in android-wear)

When the user presses a notification icon to go to the Alerts section in the Wearable Navigation Drawer, the app will call
requestNotification method of MobileNotifSender class. MobileNotifSender will create a thread that creates a connection
with the data layer. As the connection does not happen instantaneously, it is important to use blockingConnect to connect
to the GoogleApiClient (data layer). BlockingConnect uses the thread that it is on to create a connection with the GoogleApiClient,
so it is important to make sure that it is not on the main ui thread, as that may create an exception. Once the GoogleApiClient
is connected, the wearable sends a message to the data layer with a unique message (byte array) requesting notifications, and
a unique prefix ("/DataLayerNotif") that will allow listener on the mobile app to discern certain updates in the data layer.

2. Article Reading/Saving (DataLayerListenerService in android-wear and WearDataLayerListenerService in android-tablet)

When the user swipes up to bring up the Wearable Action Drawer in the ArticleActivity, the user is presented with two options:
read article more on mobile, or save the article to read later. When the user presses on one of the button, the wearable app sends
a message to the data layer that either tells mobile to open or save article. The data layer communication occurs through the
MobileNotifSender class, which is described in the previous paragraph). When the WearDataLayerListenerService receives this message,
it starts an intent which communicates with a local service inside MainActivity called WearReceiver. From WearReceiver, it either opens up
the article or saves the article.

<b>Wear UI on WaPo Wearable app</b>

For full screen content, there are some complexities with building UIs for the Android Wear. There are two different shapes of watches,
Round and Square. The UIs for Android Wear app must be compatible for both shapes. BoxInsetLayout solves the problem. BoxInsetLayout puts
content into a box for a round screen and does not affect how the content is displayed for the square screens. You can define which sides
of the content will be boxed in with the property, layout_box and setting it to either top, right, left, bottom, or all.

The Wear apps also have a Wear Navigation Drawer, which is a drawer that can be pulled from the top of the screen, and a Wear Action Drawer,
which can be pulled from the bottom of the screen. Navigation Drawers contain navigation to different fragments or activities, while
Action Drawers contain certain actions that can be done on the activity. These are defined in the xml files. Due to the scrollable
properties of the drawers, it is important to establish the differences between scrolling the drawer and the main view (one possible
option is through NestedScrollView).

<b>To-Dos</b>

1) Alerts complication
2) Backstack Fragment
3) Direct to playstore
4) Image sometimes doesn't load for articles
5) Extra Section text in navigation drawer should change after section is chosen in settings fragment