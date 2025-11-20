package com.yourpackage.yourappname; 
// Sesuaikan dengan package Anda

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.android.gms.ads.MobileAds; 
import android.widget.LinearLayout;
import android.view.KeyEvent;

public class MainActivity extends AppCompatActivity {

    private WebView webViewMenu; 
    private WebView webViewDetail;
    private LinearLayout menuLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi AdMob
        MobileAds.initialize(this, initializationStatus -> {});

        // Inisialisasi View
        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu); 
        webViewDetail = findViewById(R.id.webViewDetail);

        // Muat WebView Menu dan Detail
        setupWebViewMenu(webViewMenu, "file:///android_asset/1/index.html"); 
        setupWebViewDetail(webViewDetail); 
    }

    private void setupWebViewMenu(WebView wv, String url) {
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        
        wv.setBackgroundColor(0x00000000); 

        // 🔥 Tambahkan JavaScript Interface
        wv.addJavascriptInterface(new AdPlacer(this, wv), "AndroidAds");

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    webViewDetail.loadUrl(url); 
                    menuLayout.setVisibility(View.GONE);
                    webViewDetail.setVisibility(View.VISIBLE);
                    return true; 
                }
                return false; 
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 🔥 Panggil JavaScript Interface untuk meminta Iklan Native (4 Kali)
                if (url.endsWith("index.html")) {
                    view.loadUrl("javascript:AndroidAds.requestNativeAd('native_ad_placeholder_1');");
                    view.loadUrl("javascript:AndroidAds.requestNativeAd('native_ad_placeholder_2');");
                    view.loadUrl("javascript:AndroidAds.requestNativeAd('native_ad_placeholder_3');");
                    view.loadUrl("javascript:AndroidAds.requestNativeAd('native_ad_placeholder_4');");
                }
            }
        }); 
        
        wv.loadUrl(url);
    }
    
    private void setupWebViewDetail(WebView wv) {
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // ... Tambahkan pengaturan lain
        
        wv.setWebViewClient(new WebViewClient());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webViewDetail.getVisibility() == View.VISIBLE) {
            webViewDetail.setVisibility(View.GONE);
            menuLayout.setVisibility(View.VISIBLE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
