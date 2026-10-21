package io.github.jqssun.airplay

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Full HD is a one-time Play purchase. Ownership is cached in Prefs.FULL_HD so the service and
// offline starts see it; writing that pref restarts the server at the new resolution.
object FullHd {
    const val PRODUCT_ID = "full_hd"

    private var client: BillingClient? = null
    private lateinit var prefs: SharedPreferences
    private var details: ProductDetails? = null
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price // localized, e.g. "$6.99" or "169,00 Kč"; null until Play answers

    fun connect(context: Context) {
        if (client != null) return refresh()
        val app = context.applicationContext
        prefs = app.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        client = BillingClient.newBuilder(app)
            // only new purchases arrive here, so this can grant but never revoke
            .setListener { result, purchases ->
                if (result.responseCode == BillingResponseCode.OK && own(purchases.orEmpty())) Prefs.FULL_HD.write(prefs, true)
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
            .apply {
                startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode == BillingResponseCode.OK) refresh()
                    }
                    override fun onBillingServiceDisconnected() {} // auto-reconnects on the next call
                })
            }
    }

    // the full list of owned items: grants, and revokes after a refund
    fun refresh() {
        // still connecting: onBillingSetupFinished refreshes (querying now only spams reconnect attempts)
        val c = client?.takeIf { it.isReady } ?: return
        c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) Prefs.FULL_HD.write(prefs, own(purchases))
        }
        if (details != null) return
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(ProductType.INAPP).build()
        c.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, found ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            details = found.productDetailsList.firstOrNull()
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    fun buy(activity: Activity) {
        val c = client
        val d = details
        if (c == null || d == null) {
            // sideloaded build, no Play Store on this TV, or Play hasn't answered yet
            Toast.makeText(activity, "Google Play isn't available right now. Try again in a moment.", Toast.LENGTH_LONG).show()
            connect(activity)
            return
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d)
        d.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken?.let(params::setOfferToken)
        c.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params.build())).build())
    }

    // true if Full HD is paid for; acknowledges it (unacknowledged purchases are refunded after 3 days)
    private fun own(purchases: List<Purchase>): Boolean {
        val paid = purchases.filter { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        paid.filterNot { it.isAcknowledged }.forEach {
            client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) {}
        }
        return paid.isNotEmpty()
    }
}
