package io.flutter.plugins.googlemobileads;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Map;

import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugins.googlemobileads.nativetemplates.FlutterNativeTemplateStyle;

public class FlutterNativeAdEx extends FlutterAd {

    private static final String TAG = "FlutterNativeAdEx";

    @NonNull private final AdInstanceManager manager;
    @NonNull private final String adUnitId;
    @Nullable private GoogleMobileAdsPlugin.NativeAdFactory adFactory;
    @NonNull private final FlutterAdLoader flutterAdLoader;
    @Nullable private FlutterAdRequest request;
    @Nullable private FlutterAdManagerAdRequest adManagerRequest;
    @Nullable private Map<String, Object> customOptions;
    @Nullable private NativeAdView nativeAdView;
    @Nullable private final FlutterNativeAdOptions nativeAdOptions;
    @Nullable private FlutterNativeTemplateStyle nativeTemplateStyle;
    @Nullable private TemplateView templateView;
    @NonNull private final Context context;
    private NativeAd platformNativeAd;
    private FrameLayout defaultNativeAdView;


    static class Builder {
        @Nullable
        private AdInstanceManager manager;
        @Nullable private String adUnitId;
        @Nullable private FlutterAdRequest request;
        @Nullable private FlutterAdManagerAdRequest adManagerRequest;
        @Nullable private Integer id;
        @Nullable private FlutterNativeAdOptions nativeAdOptions;
        @Nullable private FlutterAdLoader flutterAdLoader;
        @NonNull
        private final Context context;

        Builder(Context context) {
            this.context = context;
        }

        @CanIgnoreReturnValue
        public FlutterNativeAdEx.Builder setId(int id) {
            this.id = id;
            return this;
        }

        @CanIgnoreReturnValue
        public FlutterNativeAdEx.Builder setManager(@NonNull AdInstanceManager manager) {
            this.manager = manager;
            return this;
        }

        @CanIgnoreReturnValue
        public FlutterNativeAdEx.Builder setAdUnitId(@NonNull String adUnitId) {
            this.adUnitId = adUnitId;
            return this;
        }

        @CanIgnoreReturnValue
        public FlutterNativeAdEx.Builder setRequest(@NonNull FlutterAdRequest request) {
            this.request = request;
            return this;
        }

        @CanIgnoreReturnValue
        public FlutterNativeAdEx.Builder setAdManagerRequest(@NonNull FlutterAdManagerAdRequest request) {
            this.adManagerRequest = request;
            return this;
        }

        @CanIgnoreReturnValue
        public FlutterNativeAdEx.Builder setNativeAdOptions(@Nullable FlutterNativeAdOptions nativeAdOptions) {
            this.nativeAdOptions = nativeAdOptions;
            return this;
        }

        @CanIgnoreReturnValue
        public FlutterNativeAdEx.Builder setFlutterAdLoader(@NonNull FlutterAdLoader flutterAdLoader) {
            this.flutterAdLoader = flutterAdLoader;
            return this;
        }

        FlutterNativeAdEx build() {
            if (manager == null) {
                throw new IllegalStateException("AdInstanceManager cannot be null.");
            } else if (adUnitId == null) {
                throw new IllegalStateException("AdUnitId cannot be null.");
            } else if (request == null && adManagerRequest == null) {
                throw new IllegalStateException("adRequest or addManagerRequest must be non-null.");
            }

            final FlutterNativeAdEx nativeAd;
            if (request == null) {
                nativeAd =
                        new FlutterNativeAdEx(
                                context,
                                id,
                                manager,
                                adUnitId,
                                adManagerRequest,
                                flutterAdLoader,
                                nativeAdOptions
                                );
            } else {
                nativeAd =
                        new FlutterNativeAdEx(
                                context,
                                id,
                                manager,
                                adUnitId,
                                request,
                                flutterAdLoader,
                                nativeAdOptions
                                );
            }
            return nativeAd;
        }
    }

    protected FlutterNativeAdEx(
            @NonNull Context context,
            int adId,
            @NonNull AdInstanceManager manager,
            @NonNull String adUnitId,
            @NonNull FlutterAdRequest request,
            @NonNull FlutterAdLoader flutterAdLoader,
            @Nullable FlutterNativeAdOptions nativeAdOptions
            ) {
        super(adId);
        this.context = context;
        this.manager = manager;
        this.adUnitId = adUnitId;
        this.request = request;
        this.flutterAdLoader = flutterAdLoader;
        this.nativeAdOptions = nativeAdOptions;
    }

    protected FlutterNativeAdEx(
            @NonNull Context context,
            int adId,
            @NonNull AdInstanceManager manager,
            @NonNull String adUnitId,
            @NonNull FlutterAdManagerAdRequest adManagerRequest,
            @NonNull FlutterAdLoader flutterAdLoader,
            @Nullable FlutterNativeAdOptions nativeAdOptions
            ) {
        super(adId);
        this.context = context;
        this.manager = manager;
        this.adUnitId = adUnitId;
        this.adManagerRequest = adManagerRequest;
        this.flutterAdLoader = flutterAdLoader;
        this.nativeAdOptions = nativeAdOptions;
    }

    @Override
    void load() {
        final NativeAd.OnNativeAdLoadedListener loadedListener = new FlutterNativeAdExLoadedListener(this);
        final AdListener adListener = new FlutterNativeAdListener(adId, manager);
        // Note we delegate loading the ad to FlutterAdLoader mainly for testing purposes.
        // As of 20.0.0 of GMA, mockito is unable to mock AdLoader.
        final NativeAdOptions options =
                this.nativeAdOptions == null
                        ? new NativeAdOptions.Builder().build()
                        : nativeAdOptions.asNativeAdOptions();
        if (request != null) {
            flutterAdLoader.loadNativeAd(
                    adUnitId, loadedListener, options, adListener, request.asAdRequest(adUnitId));
        } else if (adManagerRequest != null) {
            AdManagerAdRequest adManagerAdRequest = adManagerRequest.asAdManagerAdRequest(adUnitId);
            flutterAdLoader.loadAdManagerNativeAd(
                    adUnitId, loadedListener, options, adListener, adManagerAdRequest);
        } else {
            Log.e(TAG, "A null or invalid ad request was provided.");
        }
    }

    @Override
    @Nullable
    public PlatformView getPlatformView() {
        if (nativeAdView != null) {
            return new FlutterPlatformView(nativeAdView);
        } else if (templateView != null) {
            return new FlutterPlatformView(templateView);
        }
        else{
            if (defaultNativeAdView == null) {
                defaultNativeAdView = new FrameLayout(context);
                defaultNativeAdView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            }
            return new FlutterPlatformView(defaultNativeAdView);
        }
    }

    void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
//        if (nativeTemplateStyle != null) {
//            templateView = nativeTemplateStyle.asTemplateView(context);
//            templateView.setNativeAd(nativeAd);
//        } else {
//            nativeAdView = adFactory.createNativeAd(nativeAd, customOptions);
//        }
        platformNativeAd = nativeAd;
        nativeAd.setOnPaidEventListener(new FlutterPaidEventListener(manager, this));
        manager.onAdLoaded(adId, nativeAd.getResponseInfo());
    }

    public void setNativeAdUI(
            @NonNull GoogleMobileAdsPlugin.NativeAdFactory adFactory,
            @Nullable Map<String, Object> customOptions,
            @Nullable FlutterNativeTemplateStyle nativeTemplateStyle) {
        if (platformNativeAd == null) {
            Log.e(TAG, "Error showing native ad - the native ad wasn't loaded yet.");
            return;
        }
        if (nativeTemplateStyle != null) {
            templateView = nativeTemplateStyle.asTemplateView(context);
            templateView.setNativeAd(platformNativeAd);
        } else {
            nativeAdView = adFactory.createNativeAd(platformNativeAd, customOptions);
        }
    }


    @Override
    void dispose() {
        if (nativeAdView != null) {
            nativeAdView.destroy();
            nativeAdView = null;
        }
        if (templateView != null) {
            templateView.destroyNativeAd();
            templateView = null;
        }
        if (defaultNativeAdView != null) {
            defaultNativeAdView.removeAllViews();
            defaultNativeAdView = null;
        }
        if (platformNativeAd != null) {
            platformNativeAd.destroy();
            platformNativeAd = null;
        }
    }
}
