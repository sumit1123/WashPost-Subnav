// package com.wapo.flagship.features.wear
//
// import android.content.BroadcastReceiver
// import android.content.Context
// import com.wapo.android.commons.util.LogUtil
// import androidx.localbroadcastmanager.content.LocalBroadcastManager
// import com.google.android.gms.wearable.*
// import com.wapo.flagship.AppContext
// import com.washingtonpost.android.R
// import java.lang.ref.WeakReference
// import java.util.ArrayList
//
// /**
// * Created by kattim on 2/28/18.
// */
// class WearListener() :
//    CapabilityClient.OnCapabilityChangedListener {
//
//    private var connectedWearNodesWithApp: Set<Node>? = null
//    private var allConnectedWearNodes: List<Node>? = null
//
//    private var contextRef:WeakReference<Context>? = null
//
//    fun activityCreated(context: Context) {
//        contextRef = WeakReference(context)
//    }
//
//    fun activityResumed() {
//
//        contextRef?.get()?.let {
//            Wearable.getCapabilityClient(it)
//                .addListener(this, VERIFY_WEAR_APP_CAPABILITY_NAME)
//        }
//
//        // detect all connected wear nodes with the wear app.
//        findAllConnectedWearsWithApp()
//
//        // detect all connected wear nodes in the network.
//        findAllConnectedWears()
//    }
//
//    fun activityPaused() {
//        contextRef?.get()?.let {
//            Wearable.getCapabilityClient(it)
//                .removeListener(this)
//        }
//    }
//
//    /**
//     *  Called when either:
//     *  1. Wear app is installed/uninstalled on a connected Wear, or
//     *  2. A Wear with the installed wear app is connected/disconnected.
//     *
//     */
//    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
//        LogUtil.d(TAG, "onCapabilityChanged(): $capabilityInfo")
//        connectedWearNodesWithApp = capabilityInfo.nodes
//
//        findAllConnectedWears()
//    }
//
//    private fun findAllConnectedWearsWithApp() {
//        LogUtil.d(TAG, "findAllConnectedWearsWithApp()")
//
//        contextRef?.get()?.let {
//            Wearable.getCapabilityClient(it)
//                .getCapability(VERIFY_WEAR_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
//                .addOnSuccessListener { capabilityInfo ->
//                    LogUtil.d(TAG, "Get capability info successfully: $capabilityInfo")
//                    connectedWearNodesWithApp = capabilityInfo.nodes
//                    verifyNodeAndUpdateUI()
//                }
//                .addOnFailureListener { e ->
//                    LogUtil.d(TAG, "Failed CapabilityClient: $e")
//                    connectedWearNodesWithApp = null
//                    verifyNodeAndUpdateUI()
//                }
//        }
//    }
//
//    private fun findAllConnectedWears() {
//        LogUtil.d(TAG, "findAllConnectedWears()")
//
//        contextRef?.get()?.let {
//            Wearable.getNodeClient(it).connectedNodes
//                .addOnSuccessListener { nodeList ->
//                    allConnectedWearNodes = nodeList
//                    verifyNodeAndUpdateUI()
//                }
//                .addOnFailureListener { e ->
//                    LogUtil.d(TAG, "Failed NodeClient: $e")
//                    allConnectedWearNodes = null
//                    verifyNodeAndUpdateUI()
//                }
//        }
//    }
//
//    private fun verifyNodeAndUpdateUI() {
//        // Create a List of Nodes (Wear devices) without app.
//        val message: String
//
//        contextRef?.get()?.let { context ->
//            if (connectedWearNodesWithApp == null || allConnectedWearNodes == null) {
//                LogUtil.d(TAG, "Waiting on Results for both connected nodes and nodes with app")
//                return
//            } else if (connectedWearNodesWithApp!!.isEmpty()) {
//                message = context.getString(R.string.install_wear_app_message)
//                LogUtil.d(TAG, context.getString(R.string.missing_wear_app))
//            } else if (connectedWearNodesWithApp!!.size < allConnectedWearNodes!!.size) {
//                message = context.getString(R.string.install_wear_app_message)
//                LogUtil.d(
//                    TAG,
//                    String.format(
//                        context.getString(R.string.wear_installed_some),
//                        connectedWearNodesWithApp
//                    )
//                )
//            } else {
//                message = context.getString(R.string.has_wear_app)
//                LogUtil.d(
//                    TAG,
//                    String.format(
//                        context.getString(R.string.wear_installed_all),
//                        connectedWearNodesWithApp
//                    )
//                )
//            }
//
//            val nodesWithoutApp = allConnectedWearNodes!!
//                .filterNotTo(ArrayList<Node>()) { connectedWearNodesWithApp!!.contains(it) }
//
//            if (connectedWearNodesWithApp!!.isNotEmpty() || nodesWithoutApp.isNotEmpty()) {
//                if (AppContext.shouldShowWearMessage()) {
//                    AppContext.setPrefShouldShowWearMessage(false)
//                    WearDialog.getDialog(context, message, nodesWithoutApp).show()
//                } else {
//
//                }
//            } else {
//                LogUtil.d(TAG, context.getString(R.string.missing_wear_device))
//            }
//        }
//    }
//
//    companion object {
//        private const val TAG = "WearListener"
//        private const val VERIFY_WEAR_APP_CAPABILITY_NAME = "verify_wash_post_wear_app"
//    }
// }
