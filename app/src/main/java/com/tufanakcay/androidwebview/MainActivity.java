package com.tufanakcay.androidwebview;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler; // Import untuk Handler
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

    // --- Konstanta ---
    // GANTI ID Iklan ini dengan ID Anda setelah pengujian!
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // ID Test
    private static final String SPLASH_URL = "file:///android_asset/1/sflash.html"; // URL Splash Screen
    private static final String MENU_URL = "file:///android_asset/1/index.html"; // URL Menu Utama

    // --- Deklarasi Variabel ---
    private WebView webViewMenu;
    private WebView webViewDetail;
    private WebView webViewSplash; // Tambahkan WebView untuk Splash
    private LinearLayout menuLayout;
    private AdView adViewTopBanner;

    private InterstitialAd mInterstitialAd;
    private int backPressCount = 0; // Penghitung Interstitial (2 kali back)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu);
        webViewDetail = findViewById(R.id.webViewDetail);
        webViewSplash = findViewById(R.id.webViewSplash); // Inisialisasi Splash WebView (Perlu di activity_main.xml)
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);

        // 1. Inisialisasi AdMob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.i("AdMob", "AdMob Initialized. Loading Ads...");
                loadBannerAd();
                loadInterstitialAd();
            }
        });

        // 2. Tampilkan Splash Screen
        setupWebViewSplash(webViewSplash, SPLASH_URL); // Memuat sflash.html

        // 3. Persiapan WebView Menu dan Detail
        setupWebViewMenu(webViewMenu, MENU_URL);
        setupWebViewDetail(webViewDetail);

        // 4. Atur Penundaan (Delay) untuk menyembunyikan Splash dan menampilkan Menu
        // Tujuannya agar splash screen terlihat selama 3 detik.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Sembunyikan Splash setelah durasi yang ditentukan
                webViewSplash.setVisibility(View.GONE);
                // Tampilkan Menu (ini akan terlihat karena menuLayout defaultnya terlihat)
                menuLayout.setVisibility(View.VISIBLE);
            }
        }, 3000); // Tunda selama 3000 milidetik = 3 detik
    }

    private void loadBannerAd() {
        // ... (Kode loadBannerAd tidak berubah, sudah benar) ...
        adViewTopBanner.loadAd(new AdRequest.Builder().build());

        adViewTopBanner.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdMob", "Banner GAGAL dimuat. Error: " + loadAdError.getMessage());
                adViewTopBanner.setVisibility(View.GONE);
            }

            @Override
            public void onAdLoaded() {
                Log.i("AdMob", "Banner BERHASIL dimuat.");
                adViewTopBanner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadInterstitialAd() {
        // ... (Kode loadInterstitialAd menggunakan konstanta) ...
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID,
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

    // --- Logika WebView ---

    // Fungsi Baru: Setup untuk Splash Screen
    private void setupWebViewSplash(WebView wv, String url) {
        wv.setWebViewClient(new WebViewClient());
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Pastikan Splash terlihat dari awal
        wv.setVisibility(View.VISIBLE);
        wv.loadUrl(url);
    }
    
    // Fungsi setupWebViewMenu dan setupWebViewDetail tetap sama
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
        wv.addJavascriptInterface(new AdPlacer(this, wv), "AndroidAds");
        wv.loadUrl(url); // Sekarang memuat MENU_URL (index.html)
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
        backPressCount = 0;
    }

    // --- Logika Tombol Back ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // Kasus 0: Jangan lakukan apa-apa jika Splash Screen masih terlihat
            if (webViewSplash.getVisibility() == View.VISIBLE) {
                return true; // Mengkonsumsi tombol back
            }

            // Kasus 1: Kita berada di Halaman Detail (webViewDetail)
            if (webViewDetail.getVisibility() == View.VISIBLE) {

                backPressCount++; // TAMBAH HITUNGAN BACK PRESS

                if (backPressCount >= 2) {
                    if (mInterstitialAd != null) {
                        mInterstitialAd.show(this);
                        backPressCount = 0;

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Setelah Iklan ditutup, kembali ke menu
                                webViewDetail.setVisibility(View.GONE);
                                menuLayout.setVisibility(View.VISIBLE);
                                loadInterstitialAd(); // Load iklan baru untuk tampilan berikutnya
                            }
                        });
                    } else {
                        // Jika iklan tidak siap pada hitungan ke-2, langsung kembali ke menu
                        webViewDetail.setVisibility(View.GONE);
                        menuLayout.setVisibility(View.VISIBLE);
                        loadInterstitialAd();
                        backPressCount = 0;
                    }
                }
                // Konsumsi tombol back di Halaman Detail, meskipun baru hitungan ke-1
                return true;
            }

            // Kasus 2: Kita berada di Menu Utama (menuLayout)
            if (menuLayout.getVisibility() == View.VISIBLE) {
                // Biarkan default Android keluar
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
