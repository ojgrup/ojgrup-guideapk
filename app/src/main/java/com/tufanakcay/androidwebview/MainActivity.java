package com.tufanakcay.androidwebview; 
// PASTIKAN PACKAGE INI SESUAI DENGAN YANG ANDA GUNAKAN

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.view.KeyEvent;
import androidx.annotation.NonNull;

// Imports AdMob
import com.google.android.gms.ads.MobileAds; 
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class MainActivity extends AppCompatActivity {

    private WebView webViewMenu; 
    private WebView webViewDetail;
    private LinearLayout menuLayout;
    private AdView adViewTopBanner;
    
    private InterstitialAd mInterstitialAd; 
    private int backPressCount = 0; // Penghitung Interstitial (2 kali back)

    // Logika Iklan Open App Dihapus karena tidak ada MyApplication/AppOpenManager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu); 
        webViewDetail = findViewById(R.id.webViewDetail);
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);
        
        // 🔥 KRITIS: Inisialisasi MobileAds di sini (Langsung di MainActivity)
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.i("AdMob", "AdMob Initialized in MainActivity. Loading Ads...");
                
                // 1. Load Banner
                loadBannerAd();
                
                // 2. Load Interstitial
                loadInterstitialAd(); 
            }
        });

        setupWebViewMenu(webViewMenu, "file:///android_asset/1/index.html"); 
        setupWebViewDetail(webViewDetail); 
    }

    private void loadBannerAd() {
        adViewTopBanner.loadAd(new AdRequest.Builder().build());
        
        // AdListener untuk Banner (Menghilangkan Blank Putih jika gagal)
        adViewTopBanner.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdMob", "Banner GAGAL dimuat. Error: " + loadAdError.getMessage());
                // Jika gagal, SEMBUNYIKAN AdView
                adViewTopBanner.setVisibility(View.GONE); 
            }

            @Override
            public void onAdLoaded() {
                Log.i("AdMob", "Banner BERHASIL dimuat.");
                // Jika berhasil, PASTIKAN terlihat
                adViewTopBanner.setVisibility(View.VISIBLE); 
            }
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", // ID Interstitial Test
            adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.i("AdMob", "Interstitial Ad loaded.");
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                    Log.e("AdMob", "Interstitial Ad failed to load: " + loadAdError.getMessage());
                }
            });
    }

    // --- Logika WebView (Disederhanakan) ---

    private void setupWebViewMenu(WebView wv, String url) {
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("myapp://")) {
                    String detailUrl = url.replace("myapp://", "file:///android_asset/2/");
                    loadDetail(detailUrl + ".html");
                    return true;
                }
                return false;
            }
        });
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // 🔥 Native Ad: Pastikan AdPlacer (file terpisah) sudah disalin!
        wv.addJavascriptInterface(new AdPlacer(this, wv), "AndroidAds"); 
        wv.loadUrl(url);
    }
    
    private void setupWebViewDetail(WebView wv) {
        wv.setWebViewClient(new WebViewClient());
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wv.setVisibility(View.GONE);
    }

    private void loadDetail(String url) {
        webViewDetail.loadUrl(url);
        menuLayout.setVisibility(View.GONE);
        webViewDetail.setVisibility(View.VISIBLE);
        backPressCount = 0; // Reset counter saat masuk detail view
    }
    
    // --- Logika Tombol Back Interstitial ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Cek apakah tombol BACK ditekan DAN kita berada di halaman detail
        if (keyCode == KeyEvent.KEYCODE_BACK && webViewDetail.getVisibility() == View.VISIBLE) {
            
            backPressCount++; // TAMBAH HITUNGAN BACK PRESS

            if (backPressCount >= 2) { // JIKA SUDAH 2 KALI ATAU LEBIH
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                    
                    backPressCount = 0; // RESET HITUNGAN
                    
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Setelah Iklan ditutup, kembali ke menu
                            menuLayout.setVisibility(View.VISIBLE);
                            webViewDetail.setVisibility(View.GONE);
                            loadInterstitialAd(); // Muat ulang untuk berikutnya
                        }
                    });
                } else {
                    // Jika iklan tidak siap pada hitungan ke-2
                    menuLayout.setVisibility(View.VISIBLE);
                    webViewDetail.setVisibility(View.GONE);
                    loadInterstitialAd(); // Muat ulang
                    backPressCount = 0; // RESET HITUNGAN
                }
            } else {
                // Ini adalah hitungan ke-1, kunci ditekan, tapi kita tidak kembali dulu
            }
            return true; // Mengkonsumsi tombol back
        }
        return super.onKeyDown(keyCode, event);
    }
}
