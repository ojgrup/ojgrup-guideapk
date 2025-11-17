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
    
    // DEKLARASI TIGA UNIT IKLAN IN-FEED
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
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE); 
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        init();
        
        webView.clearCache(true); 
        viewUrl(); 

        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob SDK initialized. Starting ad loads.");
            loadAllInlineAds(); // FUNGSI BARU
            loadInterstitialAd();
        });
    }

    private void init() {
        webView = findViewById(R.id.webView);
        
        // INISIALISASI TIGA UNIT IKLAN IN-FEED
        mAdViewInline1 = findViewById(R.id.ad_view_inline_1); 
        adContainerInline1 = findViewById(R.id.ad_container_inline_1);
        mAdViewInline2 = findViewById(R.id.ad_view_inline_2); 
        adContainerInline2 = findViewById(R.id.ad_container_inline_2);
        mAdViewInline3 = findViewById(R.id.ad_view_inline_3); 
        adContainerInline3 = findViewById(R.id.ad_container_inline_3);
    }

    private void viewUrl() {
        String localAssetUrl = "file:///android_asset/index.html";
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        
        webView.addJavascriptInterface(new WebAppInterface(), "Android"); 
        
        webView.setWebViewClient(new CustomWebViewClient());
        webView.loadUrl(localAssetUrl);
    }
    
    // FUNGSI BARU: MUAT SEMUA IKLAN INLINE
    private void loadAllInlineAds() {
        // Muat Iklan 1
        loadInlineAd(mAdViewInline1, adContainerInline1, 1);
        // Muat Iklan 2
        loadInlineAd(mAdViewInline2, adContainerInline2, 2);
        // Muat Iklan 3
        loadInlineAd(mAdViewInline3, adContainerInline3, 3);
    }
    
    private void loadInlineAd(AdView adView, FrameLayout adContainer, int adIndex) {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Setelah Iklan dimuat, panggil JS untuk menemukan posisi placeholder
                // Kita akan mencari ID: 'native_ad_placeholder_1', 'native_ad_placeholder_2', dst.
                String jsCode = "javascript:(function(){" +
                    "  var p = document.getElementById('native_ad_placeholder_" + adIndex + "');" +
                    "  if(p) {" +
                    "    var rect = p.getBoundingClientRect();" +
                    "    var y = rect.top + window.scrollY;" +
                    "    Android.setAdPosition(" + adIndex + ", y);" + // Kirim Indeks dan Posisi Y ke Java
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
    // KOMUNIKASI JAVASCRIPT KE JAVA (SEKARANG MENERIMA INDEX)
    // =================================================================
    public class WebAppInterface {
        @JavascriptInterface
        public void setAdPosition(int adIndex, int yOffset) {
            Log.d("AdPosition", "Ad index " + adIndex + " position received: " + yOffset);
            
            webView.post(() -> {
                FrameLayout targetContainer = null;
                
                // Pilih container yang benar berdasarkan indeks dari JS
                if (adIndex == 1) targetContainer = adContainerInline1;
                else if (adIndex == 2) targetContainer = adContainerInline2;
                else if (adIndex == 3) targetContainer = adContainerInline3;

                if (targetContainer != null) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) targetContainer.getLayoutParams();
                    // Gunakan Y-offset yang diterima
                    params.topMargin = yOffset; 
                    targetContainer.setLayoutParams(params);
                    targetContainer.setVisibility(View.VISIBLE);
                }
            });
        }
    }
    
    // =================================================================
    // LOGIKA ADMOB INTERSTITIAL & BACK BUTTON (TIDAK BERUBAH)
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
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
