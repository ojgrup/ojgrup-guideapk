package com.tufanakcay.androidwebview; 

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Build;
import android.graphics.Color;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager; 
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.tufanakcay.androidwebview.R; 
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;


public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private InterstitialAd mInterstitialAd;
    private int backPressCount = 0;
    private static final int AD_SHOW_THRESHOLD = 2; 
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; 

    private FrameLayout[] adContainers = new FrameLayout[3];
    private AdView[] adViews = new AdView[3];
    
    private int statusBarHeight = 0; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. 🔥 PENGATURAN STATUS BAR SOLID (NON-TRANSPARAN)
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS); 
            
            // Mengatur warna Status Bar menjadi Putih Solid (Mengatasi 'bayangan hitam')
            window.setStatusBarColor(Color.WHITE); 
            
            // Opsional: Ikon Status Bar menjadi gelap agar terlihat di latar belakang putih
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        
        setContentView(R.layout.activity_main);

        // 2. Inisialisasi WebView, AdMob, dan Ad Inline Containers (Sama seperti sebelumnya)
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android"); 
        
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/splash.html"); 

        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob initialized successfully.");
            loadInterstitialAd(); 
        });

        adContainers[0] = findViewById(R.id.ad_container_inline_1);
        adContainers[1] = findViewById(R.id.ad_container_inline_2);
        adContainers[2] = findViewById(R.id.ad_container_inline_3);

        adViews[0] = findViewById(R.id.ad_view_inline_1);
        adViews[1] = findViewById(R.id.ad_view_inline_2);
        adViews[2] = findViewById(R.id.ad_view_inline_3);
        
        // 3. BLOK INSETS: Mendapatkan tinggi Status Bar HANYA untuk penempatan iklan
        final FrameLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                
                Insets systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()); 
                statusBarHeight = systemWindowInsets.top; 
                
                // 🔥 Karena Status Bar sudah solid, kita pastikan WebView TIDAK memiliki padding tambahan.
                if (webView.getPaddingTop() != 0) {
                    webView.setPadding(0, 0, 0, 0); 
                }
                
                return insets; 
            });
        }
    }
    
    // =======================================================
    // JAVA INTERFACE & LOGIKA IKLAN INLINE
    // =======================================================
    public class WebAppInterface {
        
        @JavascriptInterface
        public void loadMainContent() {
            webView.post(() -> {
                webView.loadUrl("file:///android_asset/index.html");
                loadAllInlineAds();
            });
        }
        
        @JavascriptInterface
        public void loadAllInlineAds() {
            webView.postDelayed(() -> {
                for (int i = 0; i < 3; i++) {
                    loadInlineAd(adViews[i], adContainers[i], i + 1);
                }
            }, 500); 
        }

        @JavascriptInterface
        public void setAdPosition(int adIndex, int yOffset) {
            webView.post(() -> {
                if (adIndex >= 1 && adIndex <= 3) {
                    FrameLayout targetContainer = adContainers[adIndex - 1];
                    if (targetContainer != null) {
                        if (targetContainer.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) targetContainer.getLayoutParams();
                            
                            // 🔥 PERBAIKAN POSISI AKHIR: Posisi Iklan dihitung relatif ke LAYOUT UTAMA (yang mencakup Status Bar)
                            // Jadi, kita harus menambahkan tinggi Status Bar.
                            params.topMargin = yOffset + statusBarHeight; 
                            
                            targetContainer.setLayoutParams(params);
                            targetContainer.setVisibility(View.VISIBLE);
                            Log.d("AdPos", "Ad " + adIndex + " positioned at Y: " + params.topMargin);
                        }
                    }
                }
            });
        }
    }
    
    // ... loadInlineAd(), loadInterstitialAd(), dan onKeyDown(..) tetap sama ...

    private void loadInlineAd(AdView adView, FrameLayout adContainer, int adIndex) {
        if (adView == null || adContainer == null) {
            Log.e("AdMob", "AdView or Container for index " + adIndex + " is null.");
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                String jsCode = "javascript:(function(){" +
                    "  var p = document.getElementById('native_ad_placeholder_" + adIndex + "');" +
                    "  if(p && p.offsetParent !== null) {" + 
                    "    var rect = p.getBoundingClientRect();" +
                    "    var y = rect.top + window.scrollY;" +
                    "    Android.setAdPosition(" + adIndex + ", Math.round(y));" + 
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

        adView.loadAd(adRequest);
    }
    
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.i("AdMob", "Interstitial Ad was loaded.");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.d("AdMob", "Interstitial Ad failed to load: " + loadAdError.getMessage());
                    mInterstitialAd = null;
                }
            });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (webView.canGoBack()) {
                backPressCount++;
                if (backPressCount >= AD_SHOW_THRESHOLD && mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            webView.goBack();
                            loadInterstitialAd(); 
                        }
                    });
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
