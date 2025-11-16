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
import android.webkit.JavascriptInterface; // Import untuk komunikasi JS

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
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE); 
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        init();
        
        webView.clearCache(true); 
        viewUrl(); 

        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob SDK initialized. Starting ad loads.");
            loadBannerAdTop();      // Memuat Iklan Banner Atas
            loadBannerAdInline();   // Memuat Iklan In-Feed/Native
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
        
        // KRUSIAL: Tambahkan interface untuk komunikasi JS ke Java (Hanya untuk Iklan In-Feed)
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
                
                // Minta HTML untuk mengirim posisi placeholder
                // Menggunakan hash change untuk memicu kode JS di CustomWebViewClient
                webView.loadUrl("javascript:window.location.hash='#position_trigger';");
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
            // yOffset adalah posisi vertikal placeholder di WebView
            Log.d("AdPosition", "Placeholder Y position received: " + yOffset);
            
            // Perubahan harus dilakukan di UI thread
            webView.post(() -> {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) adContainerInline.getLayoutParams();
                
                // Set margin top iklan agar melayang di atas placeholder di WebView
                // Mengurangi 55dp (tinggi iklan atas) dari yOffset agar posisi tetap akurat 
                // terhadap WebView yang dimulai di bawah iklan atas.
                int offsetAdjustment = (int) (55 * getResources().getDisplayMetrics().density); // Konversi 55dp ke px
                params.topMargin = yOffset - offsetAdjustment;
                
                adContainerInline.setLayoutParams(params);
                
                // Tampilkan iklan
                adContainerInline.setVisibility(View.VISIBLE);
            });
        }
    }
    
    // =================================================================
    // LOGIKA ADMOB INTERSTITIAL & BACK BUTTON
    // =================================================================
    private void loadInterstitialAd() {
        // ... (fungsi sama seperti sebelumnya) ...
    }

    private void setInterstitialAdCallback() {
        // ... (fungsi sama seperti sebelumnya) ...
    }
    
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // KRUSIAL: Tangkap Hash Change untuk Memicu Pengiriman Posisi Iklan
            if (url.endsWith("#position_trigger")) {
                view.loadUrl("javascript:(function(){" +
                    "  var p = document.getElementById('native_ad_placeholder');" +
                    "  if(p) {" +
                    "    var rect = p.getBoundingClientRect();" +
                    "    var y = rect.top + window.scrollY;" +
                    "    Android.setAdPosition(y);" + // Kirim posisi Y ke Java
                    "  }" +
                    "})()");
                return true;
            }
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
