package com.tufanakcay.androidwebview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

// IKLAN ADMOB IMPORTS
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback; 
import com.google.android.gms.ads.FullScreenContentCallback;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    private AdView mAdView;
    private FrameLayout adContainer;
    
    private InterstitialAd mInterstitialAd;
    private int backPressCount = 0; 
    private static final int AD_SHOW_THRESHOLD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 1. Inisialisasi tampilan
        init();
        // 2. Muat URL WebView
        viewUrl(); 

        // 3. PERBAIKAN STABILITAS: Muat iklan hanya setelah SDK siap
        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob SDK initialized. Starting ad loads.");
            loadBannerAd();      
            loadInterstitialAd();
        });
    }

    private void init() {
        webView = findViewById(R.id.webView); 
        mAdView = findViewById(R.id.ad_view);
        adContainer = findViewById(R.id.ad_container);
    }

    private void viewUrl() {
        String localAssetUrl = "file:///android_asset/index.html"; 
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); 
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        webSettings.setBuiltInZoomControls(false); 
        webSettings.setDisplayZoomControls(false);
        
        webView.setWebViewClient(new CustomWebViewClient()); 
        webView.loadUrl(localAssetUrl);
    }
    
    // =================================================================
    // LOGIKA ADMOB BANNER
    // =================================================================
    private void loadBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                adContainer.setVisibility(View.VISIBLE); 
                injectAdPlaceholder(); // Panggil placeholder
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                adContainer.setVisibility(View.GONE);
                removeAdPlaceholder(); 
            }
        });
    }

    // PERBAIKAN TAMPILAN: Hapus margin-bottom (yang menghasilkan teks "20px")
    private void injectAdPlaceholder() {
        final int adHeightDp = 50; // Tinggi standard AdMob Banner
        float density = getResources().getDisplayMetrics().density;
        int adHeightPx = (int) (adHeightDp * density); 
        
        String jsCode = "javascript:" +
            "var placeholder = document.getElementById('admob_placeholder');" +
            "if (placeholder) {" +
            "   placeholder.style.height = '" + adHeightPx + "px';" + // Hanya suntikkan tinggi
            "}";
        webView.loadUrl(jsCode);
    }
    
    private void removeAdPlaceholder() {
        String jsCode = "javascript:" +
            "var placeholder = document.getElementById('admob_placeholder');" +
            "if (placeholder) {" +
            "   placeholder.style.height = '0px';" +
            "}";
        webView.loadUrl(jsCode);
    }
    
    // =================================================================
    // LOGIKA ADMOB INTERSTITIAL (Tidak diubah, sudah stabil)
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
    // KODE WEBVIEW DAN TOMBOL BACK
    // =================================================================
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("file:///android_asset/")) {
                 view.loadUrl(url);
                 return true; 
            }
            return false;
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
             super.onPageFinished(view, url);
             if (url.contains("index.html") && adContainer.getVisibility() == View.VISIBLE) {
                 injectAdPlaceholder();
             }
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
