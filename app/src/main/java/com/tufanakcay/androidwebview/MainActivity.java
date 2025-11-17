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
import android.webkit.JavascriptInterface; // Import KRUSIAL untuk komunikasi JS

// ===================================
// IMPORTS ADMOB
// ===================================
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
    
    // Iklan 1: Banner Atas
    private AdView mAdView;
    private FrameLayout adContainer;
    
    // Iklan 2: In-Feed / Native
    private AdView mAdViewInline; 
    private FrameLayout adContainerInline;

    private InterstitialAd mInterstitialAd;
    private int backPressCount = 0;
    private static final int AD_SHOW_THRESHOLD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Menghilangkan flag fullscreen untuk memastikan layout status bar/iklan atas tampil benar
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE); 
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        init();
        
        // Membersihkan cache WebView 
        webView.clearCache(true); 
        viewUrl(); 

        // Inisialisasi AdMob dan muat iklan
        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob SDK initialized. Starting ad loads.");
            loadBannerAdTop();      
            loadBannerAdInline();   
            loadInterstitialAd();
        });
    }

    private void init() {
        webView = findViewById(R.id.webView);
        
        // Iklan 1: Banner Atas
        mAdView = findViewById(R.id.ad_view);
        adContainer = findViewById(R.id.ad_container);
        
        // Iklan 2: In-Feed / Native
        mAdViewInline = findViewById(R.id.ad_view_inline); 
        adContainerInline = findViewById(R.id.ad_container_inline);
    }

    private void viewUrl() {
        String localAssetUrl = "file:///android_asset/index.html";
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        
        // Tambahkan interface untuk komunikasi JS ke Java
        webView.addJavascriptInterface(new WebAppInterface(), "Android"); 
        
        webView.setWebViewClient(new CustomWebViewClient());
        webView.loadUrl(localAssetUrl);
    }
    
    // =================================================================
    // IKLAN 1: LOGIKA ADMOB BANNER ATAS (TOP)
    // =================================================================
    private void loadBannerAdTop() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("AdMob", "Top Banner Ad Loaded. Showing container.");
                adContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdMob", "Top Banner failed to load: " + loadAdError.getMessage());
                adContainer.setVisibility(View.GONE);
            }
        });
    }

    // =================================================================
    // IKLAN 2: LOGIKA ADMOB IN-FEED / NATIVE
    // =================================================================
    private void loadBannerAdInline() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdViewInline.loadAd(adRequest);
        
        mAdViewInline.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("AdMob", "Inline Ad Loaded. Requesting HTML position.");
                
                // PERBAIKAN: Panggil JS langsung untuk mendapatkan posisi
                String jsCode = "javascript:(function(){" +
                    "  var p = document.getElementById('native_ad_placeholder');" +
                    "  if(p) {" +
                    "    var rect = p.getBoundingClientRect();" +
                    "    var y = rect.top + window.scrollY;" +
                    "    Android.setAdPosition(y);" + // Kirim posisi Y ke Java
                    "  }" +
                    "})()";
                // WebView harus berada di UI thread, jadi kita menggunakan post/run
                webView.post(() -> webView.loadUrl(jsCode)); 
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdMob", "Inline Ad failed to load: " + loadAdError.getMessage());
                adContainerInline.setVisibility(View.GONE);
            }
        });
    }
    
    // =================================================================
    // KOMUNIKASI JAVASCRIPT KE JAVA
    // =================================================================
    public class WebAppInterface {
        @JavascriptInterface
        public void setAdPosition(int yOffset) {
            Log.d("AdPosition", "Placeholder Y position received: " + yOffset);
            
            webView.post(() -> {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) adContainerInline.getLayoutParams();
                
                // Menggunakan Y-offset yang diterima langsung
                params.topMargin = yOffset; 
                
                adContainerInline.setLayoutParams(params);
                
                // Tampilkan iklan
                adContainerInline.setVisibility(View.VISIBLE);
            });
        }
    }
    
    // =================================================================
    // LOGIKA ADMOB INTERSTITIAL
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
                    loadInterstitialAd();
                }
            });
        }
    }
    
    // =================================================================
    // WEBVIEW CLIENT & BACK BUTTON
    // =================================================================
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Memastikan tidak ada logika hash change yang memicu reload konten
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
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
