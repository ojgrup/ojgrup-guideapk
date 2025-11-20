package com.yourpackage.yourappname; 
// Sesuaikan dengan package Anda

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

public class AppOpenManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"; // ID App Open Test
    private AppOpenAd appOpenAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private final Application myApplication;
    private Activity currentActivity;
    private boolean isShowingAd = false;
    private long loadTime = 0;

    public AppOpenManager(Application myApplication) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    private void fetchAd() {
        if (isAdAvailable()) return;
        loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override public void onAdLoaded(AppOpenAd ad) {
                AppOpenManager.this.appOpenAd = ad;
                AppOpenManager.this.loadTime = (new java.util.Date()).getTime();
            }
            @Override public void onAdFailedToLoad(LoadAdError loadAdError) {
                // ...
            }
        };
        AppOpenAd.load(myApplication, AD_UNIT_ID, getAdRequest(), AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    }

    public void showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable() && wasLoadTimeLessThanNHoursAgo(4)) {
            FullScreenContentCallback fullScreenContentCallback =
                new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() {
                        AppOpenManager.this.appOpenAd = null; isShowingAd = false; fetchAd();
                    }
                    @Override public void onAdFailedToShowFullScreenContent(AdError adError) {
                        isShowingAd = false; AppOpenManager.this.appOpenAd = null; fetchAd();
                    }
                    @Override public void onAdShowedFullScreenContent() {
                        isShowingAd = true;
                    }
                };
            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);
        } else {
            fetchAd();
        }
    }

    private boolean isAdAvailable() { return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4); }
    
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        return ((new java.util.Date()).getTime() - this.loadTime) < (3600000 * numHours);
    }

    private AdRequest getAdRequest() { return new AdRequest.Builder().build(); }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() { showAdIfAvailable(); }

    // Implementasi ActivityLifecycleCallbacks (untuk melacak Activity saat ini)
    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityStarted(Activity activity) { currentActivity = activity; }
    @Override public void onActivityResumed(Activity activity) { currentActivity = activity; }
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) { currentActivity = null; }
}
