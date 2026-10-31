package com.oathly.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.oathly.app.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One optional purchase: "Oathly Pro" removes ads. Every study feature stays free.
 * Uses Google Play Billing (required by Play policy for digital goods).
 */
class BillingManager(context: Context, private val settings: SettingsStore) : PurchasesUpdatedListener {

    companion object {
        const val PRO_PRODUCT_ID = "oathly_pro"
    }

    enum class Status { IDLE, PENDING, ERROR }

    private val _isPro = MutableStateFlow(settings.proCached)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _status = MutableStateFlow(Status.IDLE)
    val status: StateFlow<Status> = _status.asStateFlow()

    var onProChanged: ((Boolean) -> Unit)? = null

    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        if (client.isReady) { refresh(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
            }
            override fun onBillingServiceDisconnected() { /* reconnect on next use */ }
        })
    }

    private fun refresh() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = purchases.any { PRO_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                setPro(owned)
                purchases.forEach(::acknowledgeIfNeeded)
            }
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            ),
        ).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = queryResult.productDetailsList.firstOrNull { it.productId == PRO_PRODUCT_ID }
                _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    /** Returns false if the store isn't ready (no product loaded yet). */
    fun buy(activity: Activity): Boolean {
        val d = details ?: run { connect(); return false }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        val result = client.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun restore() = connect()

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { p ->
                if (PRO_PRODUCT_ID !in p.products) return@forEach
                when (p.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> { setPro(true); acknowledgeIfNeeded(p); _status.value = Status.IDLE }
                    Purchase.PurchaseState.PENDING -> _status.value = Status.PENDING
                    else -> Unit
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingClient.BillingResponseCode.USER_CANCELED -> _status.value = Status.IDLE
            else -> _status.value = Status.ERROR
        }
    }

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED || p.isAcknowledged) return
        client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build(),
        ) { /* Google refunds unacknowledged purchases after 3 days; retried on next launch */ }
    }

    private fun setPro(value: Boolean) {
        settings.proCached = value
        if (_isPro.value != value) {
            _isPro.value = value
            onProChanged?.invoke(value)
        } else {
            _isPro.value = value
        }
    }
}
