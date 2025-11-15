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
import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import android.util.Log; // Diperlukan untuk Log.d/Log.e

public class MainActivity extends AppCompatActivity {

    WebView webView;
    private AdView mAdView; // Variabel AdMob Banner
    private FrameLayout adContainer;
    
    private InterstitialAd mInterstitialAd; // Variabel AdMob Interstitial
    private int backPressCount = 0; 
    private static final int AD_SHOW_THRESHOLD = 2; // Tampilkan iklan setiap 2 kali tombol back ditekan

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // INISIALISASI ADMOB SDK (Wajib)
        MobileAds.initialize(this, initializationStatus -> {
            // SDK siap
        });

        init();
        viewUrl();
        loadBannerAd();      // Muat iklan Banner
        loadInterstitialAd(); // Muat iklan Interstitial pertama kali
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
                injectAdPlaceholder(); 
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                adContainer.setVisibility(View.GONE);
                removeAdPlaceholder(); 
            }
        });
    }

    // Menyuntikkan ruang kosong ke HTML agar Banner Native di atas terlihat
    private void injectAdPlaceholder() {
        final int adHeightDp = 50; 
        float density = getResources().getDisplayMetrics().density;
        int adHeightPx = (int) (adHeightDp * density); 
        
        // JavaScript mencari ID="admob_placeholder" di HTML dan memberinya tinggi
        String jsCode = "javascript:" +
            "var placeholder = document.getElementById('admob_placeholder');" +
            "if (placeholder) {" +
            "   placeholder.style.height = '" + adHeightPx + "px';" + 
            "   placeholder.style.marginBottom = '20px';" + 
            "}";
        webView.loadUrl(jsCode);
    }
    
    // Menghapus ruang kosong jika iklan gagal dimuat
    private void removeAdPlaceholder() {
        String jsCode = "javascript:" +
            "var placeholder = document.getElementById('admob_placeholder');" +
            "if (placeholder) {" +
            "   placeholder.style.display = 'none';" +
            "}";
        webView.loadUrl(jsCode);
    }
    
    // =================================================================
    // LOGIKA ADMOB INTERSTITIAL
    // =================================================================
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        // GANTI DENGAN ID UNIT IKLAN INTERSTITIAL ANDA
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", 
            adRequest, new AdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.d("AdMob", "Interstitial Ad Loaded.");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                    Log.e("AdMob", "Interstitial Ad Failed to Load: " + loadAdError.getMessage());
                }
            });
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
            // Izinkan link lain dibuka di browser luar (misalnya link eksternal atau tel/email)
            return false;
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
             super.onPageFinished(view, url);
             // Memastikan placeholder iklan disuntikkan setiap kali halaman utama dimuat ulang
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
                    // Tampilkan Interstitial Ad
                    mInterstitialAd.show(MainActivity.this);
                    
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Setelah iklan ditutup, lakukan aksi aslinya (kembali)
                            webView.goBack();
                            // Muat ulang iklan untuk siklus berikutnya
                            loadInterstitialAd(); 
                        }
                    });
                    
                    backPressCount = 0; 
                    return true; // Menahan event 'back' sampai iklan ditutup
                    
                } else {
                    // Jika iklan tidak tersedia atau threshold belum tercapai, kembali normal
                    webView.goBack();
                    return true;
                }
            }
        }
        
        // Jika tidak bisa kembali di WebView, biarkan sistem menangani (keluar aplikasi)
        return super.onKeyDown(keyCode, event);
    }
}
