package com.tufanakcay.androidwebview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebSettings; // Diperlukan untuk setting tambahan
import android.webkit.WebView;
import android.webkit.WebViewClient; 

public class MainActivity extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        viewUrl();
    }

    private void init() {
        webView = findViewById(R.id.webView);
    }

    private void viewUrl() {

        String dynamicUrl = getString(R.string.web_url); 

        // 1. Dapatkan Objek WebSettings
        WebSettings webSettings = webView.getSettings();

        // --- PENAMBAHAN PENTING UNTUK MENGATASI BLANK BIRU ---

        // 2. Aktifkan JavaScript (Sudah ada, tapi ini adalah best practice)
        webSettings.setJavaScriptEnabled(true);
        
        // 3. Aktifkan DOM Storage (Wajib untuk Local Storage, seperti yang digunakan Firebase)
        webSettings.setDomStorageEnabled(true); 

        // 4. Set Cache Mode ke Default (Memastikan loading yang lebih baik)
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 5. Setting Tampilan (Opsional, tapi disarankan)
        webSettings.setBuiltInZoomControls(false); 
        webSettings.setDisplayZoomControls(false);
        
        // 6. Set User Agent (Opsional: Kadang membantu untuk mendeteksi sebagai browser mobile asli)
        // webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Mobile Safari/537.36");
        
        // 7. Gunakan WebViewClient Kustom untuk penanganan yang lebih baik
        webView.setWebViewClient(new CustomWebViewClient()); 

        // --- AKHIR PENAMBAHAN ---

        webView.loadUrl(dynamicUrl);
    }
    
    // Kelas Kustom untuk menangani loading URL di dalam WebView
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Memastikan semua link internal (termasuk redirect) tetap di dalam WebView
            view.loadUrl(url);
            return true; 
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
             // Opsional: Lakukan sesuatu setelah loading selesai (misalnya menyembunyikan loading spinner)
             super.onPageFinished(view, url);
        }
        
        // CATATAN: Untuk menangani error (termasuk SSL error), method onReceivedError dan onReceivedSslError 
        // harus ditambahkan di sini, tetapi setting di atas sudah sering menyelesaikannya.
    }
}
