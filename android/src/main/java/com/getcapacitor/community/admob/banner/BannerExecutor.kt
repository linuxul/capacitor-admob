package com.getcapacitor.community.admob.banner

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.util.Supplier
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginException
import com.getcapacitor.community.admob.helpers.AdViewIdHelper
import com.getcapacitor.community.admob.helpers.RequestHelper
import com.getcapacitor.community.admob.models.AdMobPluginError
import com.getcapacitor.community.admob.models.AdMobRevenueData
import com.getcapacitor.community.admob.models.AdOptions
import com.getcapacitor.community.admob.models.Executor
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.common.util.BiConsumer

/**
 * Shows, hides and removes the AdMob banner. Its functions are called on the main thread, where AdMob's banner
 * methods run.
 */
public class BannerExecutor(
    contextSupplier: Supplier<Context>,
    activitySupplier: Supplier<Activity?>,
    notifyListenersFunction: BiConsumer<String, JSObject>,
    pluginLogTag: String
) : Executor(contextSupplier, activitySupplier, notifyListenersFunction, pluginLogTag, "BannerExecutor") {
    private val emptyObject = JSObject()
    private var mAdViewLayout: RelativeLayout? = null
    private var mAdView: AdView? = null
    private var mViewGroup: ViewGroup? = null

    /**
     * Resolve the banner parent, waiting for the layout pass that adds it when it is not
     * there yet, and report the outcome to `onResult` on the UI thread.
     *
     * android.R.id.content can still have no child when initialize() runs - for example
     * when the app is relaunched quickly after being closed - and the old code cached that
     * null for the lifetime of the process, so every later showBanner() threw
     * NullPointerException on it. Waiting here lets AdMob.initialize() mean what callers
     * already read it as: ready to show a banner.
     */
    public fun awaitViewGroup(onResult: (Boolean) -> Unit) {
        if (resolveViewGroup() != null) {
            onResult(true)
            return
        }

        val contentView: View? = liveActivity()?.findViewById(android.R.id.content)
        if (contentView !is ViewGroup) {
            Log.w(logTag, "Banner parent unavailable: no usable android.R.id.content")
            onResult(false)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var settled = false
        lateinit var timeout: Runnable

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (settled || resolveViewGroup() == null) {
                    return
                }
                settled = true
                handler.removeCallbacks(timeout)
                removeGlobalLayoutListener(contentView, this)
                onResult(true)
            }
        }

        timeout = Runnable {
            if (settled) {
                return@Runnable
            }
            settled = true
            removeGlobalLayoutListener(contentView, listener)
            Log.w(logTag, "Banner parent never appeared within ${PARENT_TIMEOUT_MS}ms")
            onResult(false)
        }

        contentView.viewTreeObserver.addOnGlobalLayoutListener(listener)

        // A child added between the first attempt and registering the listener would not
        // fire it, so try once more now that we are listening.
        if (resolveViewGroup() != null) {
            settled = true
            removeGlobalLayoutListener(contentView, listener)
            onResult(true)
            return
        }

        handler.postDelayed(timeout, PARENT_TIMEOUT_MS)
    }

    /**
     * The banner's parent, or null while it is unavailable. Never caches a null: the
     * lookup is retried on each use so a later call can succeed once layout has settled.
     */
    private fun resolveViewGroup(): ViewGroup? {
        mViewGroup?.let { return it }

        val content: View? = liveActivity()?.findViewById(android.R.id.content)
        if (content !is ViewGroup) {
            return null
        }
        // Must be content's first child: showBanner builds CoordinatorLayout.LayoutParams,
        // which android.R.id.content (a FrameLayout) rejects with a ClassCastException
        // while measuring.
        val child: View? = content.getChildAt(0)
        if (child is ViewGroup) {
            mViewGroup = child
        }
        return mViewGroup
    }

    /** The current activity, or null when it is gone or on its way out. */
    private fun liveActivity(): Activity? = activitySupplier.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    public fun showBanner(call: PluginCall) {
        val adOptions = AdOptions.AdOptionsFactory.createBannerOptions(call)
        val density = contextSupplier.get().resources.displayMetrics.density

        val defaultWidthPixels = contextSupplier.get().resources.displayMetrics.widthPixels

        // A missing activity was an uncaught NullPointerException here.
        val activity = activitySupplier.get()!!
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getRealMetrics(metrics)
        val realWidthPixels = metrics.widthPixels

        @Suppress("DEPRECATION")
        val fullscreen = (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0

        if (mAdView != null) {
            updateExistingAdView(adOptions)
            // This call used to stay pending when a banner was shown already.
            call.resolve()
            return
        }

        // Why a try catch block?
        try {
            val adView = AdView(contextSupplier.get())
            mAdView = adView

            if (adOptions.adSize != BannerAdSizeEnum.ADAPTIVE_BANNER) {
                adView.setAdSize(adOptions.adSize.size)
            } else {
                // ADAPTIVE BANNER
                adView.setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(contextSupplier.get(), (defaultWidthPixels / density).toInt())
                )
            }

            // Setup AdView Layout
            val adViewLayout = RelativeLayout(contextSupplier.get())
            mAdViewLayout = adViewLayout
            adViewLayout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)
            adViewLayout.setVerticalGravity(Gravity.BOTTOM)

            val adViewLayoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            )

            // TODO: Make an enum like the AdSizeEnum?
            adViewLayoutParams.gravity = when (adOptions.position) {
                "TOP_CENTER" -> Gravity.TOP
                "CENTER" -> Gravity.CENTER
                else -> Gravity.BOTTOM
            }

            adViewLayout.layoutParams = adViewLayoutParams

            val densityMargin = (adOptions.margin * density).toInt()

            // Center Banner Ads
            val adWidth = (adOptions.adSize.size.width * density).toInt()

            val sideMargin = if (adWidth <= 0 || adOptions.adSize == BannerAdSizeEnum.ADAPTIVE_BANNER) {
                if (fullscreen) (realWidthPixels - defaultWidthPixels) / 2 else 0
            } else if (fullscreen) {
                (realWidthPixels - adWidth) / 2
            } else {
                (defaultWidthPixels - adWidth) / 2
            }
            adViewLayoutParams.setMargins(sideMargin, densityMargin, sideMargin, densityMargin)

            // set Safe Area only for Android 15+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                val rootView = activitySupplier.get()!!.window.decorView
                rootView.setOnApplyWindowInsetsListener { _, insets ->
                    @Suppress("DEPRECATION")
                    val bottomInset = insets.systemWindowInsetBottom

                    @Suppress("DEPRECATION")
                    val topInset = insets.systemWindowInsetTop

                    if ("TOP_CENTER" == adOptions.position) {
                        adViewLayoutParams.setMargins(sideMargin, densityMargin + topInset, sideMargin, densityMargin)
                    } else {
                        adViewLayoutParams.setMargins(sideMargin, densityMargin, sideMargin, densityMargin + bottomInset)
                    }

                    mAdViewLayout?.layoutParams = adViewLayoutParams
                    insets
                }
            }

            createNewAdView(adOptions)

            call.resolve()
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    public fun hideBanner(call: PluginCall) {
        if (mAdView == null) {
            throw PluginException("You tried to hide a banner that was never shown")
        }

        mAdViewLayout?.let { adViewLayout ->
            adViewLayout.visibility = View.GONE
            mAdView?.pause()

            val sizeInfo = BannerAdSizeInfo(0, 0)

            notifyListeners(BannerAdPluginEvents.SizeChanged.webEventName, sizeInfo)

            call.resolve()
        }
    }

    public fun resumeBanner(call: PluginCall) {
        val adViewLayout = mAdViewLayout
        val adView = mAdView
        if (adViewLayout != null && adView != null) {
            adViewLayout.visibility = View.VISIBLE
            adView.resume()

            val sizeInfo = BannerAdSizeInfo(adView)
            notifyListeners(BannerAdPluginEvents.SizeChanged.webEventName, sizeInfo)

            Log.d(logTag, "Banner AD Resumed")
        }

        call.resolve()
    }

    public fun removeBanner(call: PluginCall) {
        mAdView?.let { adView ->
            resolveViewGroup()?.removeView(mAdViewLayout)
            mAdViewLayout?.removeView(adView)
            adView.destroy()
            mAdView = null
            Log.d(logTag, "Banner AD Removed")
            val sizeInfo = BannerAdSizeInfo(0, 0)
            notifyListeners(BannerAdPluginEvents.SizeChanged.webEventName, sizeInfo)
        }

        call.resolve()
    }

    private fun updateExistingAdView(adOptions: AdOptions) {
        val adRequest = RequestHelper.createRequest(adOptions)
        mAdView?.loadAd(adRequest)
    }

    /**
     * Follow iOS method Name:
     * https://developers.google.com/admob/ios/banner?hl=ja
     */
    private fun createNewAdView(adOptions: AdOptions) {
        // The AdView created for this call. The listeners below compare it with `mAdView`, which removeBanner or a
        // failed load clears, to ignore the callbacks of a banner that was removed or replaced since.
        val adView = mAdView ?: return

        val adRequest = RequestHelper.createRequest(adOptions)
        // Assign the correct id needed
        AdViewIdHelper.assignIdToAdView(adView, adOptions, adRequest, logTag, contextSupplier.get())
        // Add the AdView to the view hierarchy.
        mAdViewLayout?.addView(adView)
        // Start loading the ad.
        adView.loadAd(adRequest)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (adView !== mAdView) {
                    return
                }
                val sizeInfo = BannerAdSizeInfo(adView)

                notifyListeners(BannerAdPluginEvents.SizeChanged.webEventName, sizeInfo)
                notifyListeners(BannerAdPluginEvents.Loaded.webEventName, emptyObject)
                super.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                if (adView !== mAdView) {
                    // Stale callback from a banner that was already removed or
                    // replaced. Do not touch the current banner or emit teardown
                    // events for a view the JS layer has already discarded.
                    super.onAdFailedToLoad(adError)
                    return
                }

                resolveViewGroup()?.removeView(mAdViewLayout)
                mAdViewLayout?.removeView(adView)
                adView.destroy()
                mAdView = null

                val sizeInfo = BannerAdSizeInfo(0, 0)
                notifyListeners(BannerAdPluginEvents.SizeChanged.webEventName, sizeInfo)

                val adMobPluginError = AdMobPluginError(adError)
                notifyListeners(BannerAdPluginEvents.FailedToLoad.webEventName, adMobPluginError)

                super.onAdFailedToLoad(adError)
            }

            override fun onAdOpened() {
                notifyListeners(BannerAdPluginEvents.Opened.webEventName, emptyObject)
                super.onAdOpened()
            }

            override fun onAdClosed() {
                notifyListeners(BannerAdPluginEvents.Closed.webEventName, emptyObject)
                super.onAdClosed()
            }

            override fun onAdImpression() {
                notifyListeners(BannerAdPluginEvents.AdImpression.webEventName, emptyObject)
                super.onAdImpression()
            }
        }

        adView.setOnPaidEventListener { adValue ->
            if (adView !== mAdView) {
                return@setOnPaidEventListener
            }
            val responseInfo = adView.responseInfo
            val networkName = responseInfo?.mediationAdapterClassName ?: ""
            val impressionId = responseInfo?.responseId ?: ""
            val revenueData = AdMobRevenueData(adValue, adView.adUnitId, networkName, impressionId)
            notifyListeners(BannerAdPluginEvents.AdPaid.webEventName, revenueData)
        }

        // Add AdViewLayout top of the WebView
        val bannerParent = resolveViewGroup()
        if (bannerParent != null) {
            bannerParent.addView(mAdViewLayout)
        } else {
            Log.w(logTag, "Banner not attached: parent unavailable")
        }
    }

    private companion object {
        /**
         * How long to wait for the banner parent to appear before giving up. Deliberately
         * generous: the devices where the parent is late are the slow ones, and a premature
         * timeout now fails AdMob.initialize() outright rather than only the banner.
         */
        private const val PARENT_TIMEOUT_MS = 5000L

        private fun removeGlobalLayoutListener(view: View, listener: ViewTreeObserver.OnGlobalLayoutListener) {
            val observer = view.viewTreeObserver
            if (observer.isAlive) {
                observer.removeOnGlobalLayoutListener(listener)
            }
        }
    }
}
