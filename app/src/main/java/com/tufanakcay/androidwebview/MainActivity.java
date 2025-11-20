package com.tufanakcay.androidwebview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager; 
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.view.LayoutInflater;
import android.graphics.Color;

// 🔥 PERBAIKAN KRITIS: MENGUBAH LOKASI AdLoader
import com.google.android.gms.ads.AdLoader; // <--- DIUBAH DARI .adloader.AdLoader
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd; 
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

// 🔥 IMPORTS TAMBAHAN UNTUK MEMBACA FILE HTML DARI ASSETS
import android.content.res.AssetManager;
import java.io.InputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    // VARIABEL VIEW ANDROID
    private WebView webView1, webView2, webView3;
    private FrameLayout nativeAdPlaceholder1, nativeAdPlaceholder2, nativeAdPlaceholder3;
    private AdView adViewTopBanner; 

    // VARIABEL IKLAN ADMOB
    private InterstitialAd mInterstitialAd;
    private AppOpenAd appOpenAd = null; 
    
    // ID IKLAN TEST ADMOB (HARAP GANTI DENGAN ID ASLI ANDA)
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; 
    private static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"; 
    private static final String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"; 

    // VARIABEL KONTROL
    private int backPressCount = 0;
    private static final int AD_SHOW_THRESHOLD = 2; 
    private boolean isAdShown = false; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // PENGATURAN STATUS BAR SOLID (Putih)
        /*
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.WHITE); 
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        */
        setContentView(R.layout.activity_main);

        // 1. Inisialisasi AdMob
        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob initialized successfully.");
            loadInterstitialAd(); 
            
            if (!isAdShown) {
                loadAppOpenAd();
            }
        });

        // 2. Inisialisasi Views
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);
        
        webView1 = findViewById(R.id.webView1);
        webView2 = findViewById(R.id.webView2);
        webView3 = findViewById(R.id.webView3);
        
        nativeAdPlaceholder1 = findViewById(R.id.native_ad_placeholder_1);
        nativeAdPlaceholder2 = findViewById(R.id.native_ad_placeholder_2);
        nativeAdPlaceholder3 = findViewById(R.id.native_ad_placeholder_3);

        // 3. Muat Iklan Banner
        loadTopBannerAd();

        // 4. Konfigurasi WebView dan Muat Fragment HTML
        setupWebView(webView1, "https://builder.ojgrup.com/apkguide/index.php");
       /*
        setupWebView(webView2, "file:///android_asset/fragment_2.html");
        setupWebView(webView3, "file:///android_asset/fragment_3.html"); 
        */
        // 5. Muat 3 Iklan Native ke Placeholder Asli
        loadNativeAd(nativeAdPlaceholder1);
        loadNativeAd(nativeAdPlaceholder2);
        loadNativeAd(nativeAdPlaceholder3);
    }
    
    // Fungsi bantuan untuk konfigurasi WebView
    private void setupWebView(WebView wv, String url) {
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        // Menggunakan LayoutAlgorithm yang stabil
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); 
        
        // 🔥 PERBAIKAN: Implementasi WebViewClient untuk membaca file secara manual
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                
                // 1. Jika tautan adalah tautan eksternal (HTTP/HTTPS)
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    // Biarkan WebView menanganinya (return false)
                    return false; 
                    
                } else if (url.endsWith(".html") || url.endsWith(".htm")) {
                    // 2. Jika tautan adalah file HTML lokal (misalnya: guide1.html)
                    
                    try {
                        // Dapatkan AssetManager
                        AssetManager assetManager = getAssets();
                        
                        // Buka file yang ditargetkan (url adalah nama file, contoh: "guide1.html")
                        InputStream inputStream = assetManager.open(url);
                        int size = inputStream.available();
                        byte[] buffer = new byte[size];
                        inputStream.read(buffer);
                        inputStream.close();
                        
                        String htmlContent = new String(buffer, "UTF-8");
                        
                        // Muat konten HTML baru dengan Base URL yang benar
                        view.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null);
                        Log.d("WebView", "Successfully loaded local file: " + url);
                        return true; // Pemuatan berhasil ditangani oleh aplikasi
                        
                    } catch (IOException e) {
                        // Log error dan biarkan WebView menampilkan pesan error default (return false)
                        Log.e("WebView", "Gagal memuat file lokal: " + url, e);
                        return false; 
                    }
                    
                } else {
                    // 3. Jika tautan tidak dikenal atau tautan relatif tanpa ekstensi
                    // Cegah WebView memuatnya untuk menghindari layar kosong (about:blank)
                    return true;
                }
            }
        }); 
        
        wv.loadUrl(url);
    }
    
    // =======================================================
    // FUNGSI IKLAN ADMOB
    // =======================================================

    private void loadTopBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewTopBanner.loadAd(adRequest);
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    private void loadAppOpenAd() {
        if (appOpenAd != null) return;

        AppOpenAd.load(
            this,
            APP_OPEN_AD_UNIT_ID,
            new AdRequest.Builder().build(),
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, 
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    appOpenAd = ad;
                    showAppOpenAdIfReady();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    appOpenAd = null;
                }
            }
        );
    }
    
    private void showAppOpenAdIfReady() {
        if (appOpenAd != null && !isAdShown) {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isAdShown = true; 
                    loadAppOpenAd(); 
                }
            });
            appOpenAd.show(this);
        }
    }
    
    // =======================================================
    // FUNGSI IKLAN NATIVE (Menggunakan View Asli)
    // =======================================================

    private void loadNativeAd(final FrameLayout placeholder) {
        // AdLoader sekarang sudah benar karena perbaikan import di atas
        AdLoader adLoader = new AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
            .forNativeAd(nativeAd -> {
                displayNativeAd(nativeAd, placeholder);
            })
            .withAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    Log.e("NativeAd", "Native Ad failed to load: " + adError.getMessage());
                    placeholder.setVisibility(View.GONE);
                }
            })
            .build();
            
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void displayNativeAd(NativeAd nativeAd, FrameLayout placeholder) {
        // Inflate layout Native Ad View Anda dari R.layout.native_ad_template
        NativeAdView adView = (NativeAdView) LayoutInflater.from(this)
            .inflate(R.layout.native_ad_template, null); 
            
        try {
            // Set View untuk Headline, Body, CTA, dan Icon
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_app_icon));
            
            // Isi data
            ((TextView)adView.getHeadlineView()).setText(nativeAd.getHeadline());
            
            // Logika untuk Body
            if (nativeAd.getBody() != null) {
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
                adView.getBodyView().setVisibility(View.VISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            }

            // Logika untuk CTA
            if (nativeAd.getCallToAction() != null) {
                ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
                adView.getCallToActionView().setVisibility(View.VISIBLE);
            } else {
                adView.getCallToActionView().setVisibility(View.INVISIBLE);
            }

            // Logika untuk Icon
            if (nativeAd.getIcon() != null) {
                ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            } else {
                adView.getIconView().setVisibility(View.GONE);
            }
            
            // Daftarkan NativeAd ke NativeAdView
            adView.setNativeAd(nativeAd);
            
            // Tambahkan View Iklan Native ke dalam FrameLayout placeholder
            placeholder.removeAllViews();
            placeholder.addView(adView);
            placeholder.setVisibility(View.VISIBLE);
            
        } catch (Exception e) {
            Log.e("NativeAd", "Gagal mengisi data iklan native: " + e.getMessage());
            placeholder.setVisibility(View.GONE);
        }
    }
    
    // =======================================================
    // BACK BUTTON
    // =======================================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            
            backPressCount++;
            if (backPressCount >= AD_SHOW_THRESHOLD && mInterstitialAd != null) {
                mInterstitialAd.show(MainActivity.this);
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        finish(); // Keluar setelah Iklan
                        loadInterstitialAd(); 
                    }
                });
                backPressCount = 0;
                return true;
            } else {
                finish(); // Keluar tanpa Iklan
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
