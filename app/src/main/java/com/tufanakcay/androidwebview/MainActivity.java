package com.tufanakcay.androidwebview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager; 
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.util.Log;
import android.webkit.JavascriptInterface;

// IMPORTS ADMOB
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    
    // Iklan In-Feed 1, 2, 3
    private AdView mAdViewInline1; 
    private FrameLayout adContainerInline1;
    private AdView mAdViewInline2; 
    private FrameLayout adContainerInline2;
    private AdView mAdViewInline3; 
    private FrameLayout adContainerInline3;

    private InterstitialAd mInterstitialAd;
    private int backPressCount = 0;
    private static final int AD_SHOW_THRESHOLD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Membersihkan flag FULLSCREEN yang mungkin konflik
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE); 
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // PENTING: Inisialisasi komponen
        init();
        
        webView.clearCache(true); 
        
        viewUrl("file:///android_asset/index.html"); 

        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob SDK initialized. Interstitial load started.");
            loadInterstitialAd();
        });
    }
    
    private void viewUrl(String url) {
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        
        webView.addJavascriptInterface(new WebAppInterface(), "Android"); 
        
        webView.setWebViewClient(new CustomWebViewClient());
        webView.loadUrl(url);
    }
    
    private void init() {
        // Pastikan ID ini sama persis dengan activity_main.xml
        webView = findViewById(R.id.webView);
        
        adContainerInline1 = findViewById(R.id.ad_container_inline_1);
        mAdViewInline1 = findViewById(R.id.ad_view_inline_1);
        
        adContainerInline2 = findViewById(R.id.ad_container_inline_2);
        mAdViewInline2 = findViewById(R.id.ad_view_inline_2);
        
        adContainerInline3 = findViewById(R.id.ad_container_inline_3);
        mAdViewInline3 = findViewById(R.id.ad_view_inline_3);
    }

    // FUNGSI MUAT IKLAN UTAMA (Dipanggil dari JavaScript)
    public void loadAllInlineAds() {
        Log.d("AdMob", "Loading all inline ads requested by JS.");
        // NULL CHECK untuk mencegah Force Close jika ada ID yang salah di XML
        if (mAdViewInline1 != null) loadInlineAd(mAdViewInline1, adContainerInline1, 1);
        if (mAdViewInline2 != null) loadInlineAd(mAdViewInline2, adContainerInline2, 2);
        if (mAdViewInline3 != null) loadInlineAd(mAdViewInline3, adContainerInline3, 3);
    }
    
    private void loadInlineAd(AdView adView, FrameLayout adContainer, int adIndex) {
        if (adView == null || adContainer == null) {
            Log.e("AdMob", "AdView or AdContainer for index " + adIndex + " is NULL. Skipping load.");
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Panggil JS untuk mendapatkan posisi placeholder
                String jsCode = "javascript:(function(){" +
                    "  var p = document.getElementById('native_ad_placeholder_" + adIndex + "');" +
                    "  if(p && p.offsetParent !== null) {" + 
                    "    var rect = p.getBoundingClientRect();" +
                    "    var y = rect.top + window.scrollY;" +
                    "    Android.setAdPosition(" + adIndex + ", y);" + 
                    "  }" +
                    "})()";
                webView.post(() -> webView.loadUrl(jsCode)); 
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                adContainer.setVisibility(View.GONE);
                Log.e("AdMob", "Inline Ad " + adIndex + " failed to load: " + loadAdError.getMessage());
            }
        });
    }

    // =================================================================
    // KOMUNIKASI JAVASCRIPT KE JAVA
    // =================================================================
    public class WebAppInterface {
        @JavascriptInterface
        public void setAdPosition(int adIndex, int yOffset) {
            webView.post(() -> {
                FrameLayout targetContainer = null;
                if (adIndex == 1) targetContainer = adContainerInline1;
                else if (adIndex == 2) targetContainer = adContainerInline2;
                else if (adIndex == 3) targetContainer = adContainerInline3;

                if (targetContainer != null) {
                    if (targetContainer.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) targetContainer.getLayoutParams();
                        params.topMargin = yOffset; 
                        targetContainer.setLayoutParams(params);
                        targetContainer.setVisibility(View.VISIBLE);
                    } else {
                         Log.e("AdMob", "LayoutParams tidak ditemukan untuk AdContainer " + adIndex);
                    }
                } else {
                     Log.e("AdMob", "AdContainer " + adIndex + " is NULL in Java.");
                }
            });
        }
        
        // Dipanggil oleh JS setelah splash hilang
        @JavascriptInterface
        public void loadAllInlineAds() {
            MainActivity.this.loadAllInlineAds();
        }
    }
    
    // =================================================================
    // LOGIKA IKLAN INTERSTITIAL DAN NAVIGASI
    // =================================================================
    
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", 
            adRequest, new InterstitialAdLoadCallback() { 
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    setInterstitialAdCallback();
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    private void setInterstitialAdCallback() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    webView.goBack();
                    loadInterstitialAd(); // Load iklan berikutnya
                }
            });
        }
    }
    
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("file:///android_asset/")) {
                 view.loadUrl(url);
                 return true;
            }
            return false;
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (webView.canGoBack()) {
                backPressCount++;
                if (backPressCount >= AD_SHOW_THRESHOLD && mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                    backPressCount = 0;
                    return true;
                } else {
                    webView.goBack();
                    return true;
                }
            } else {
                finish(); 
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
