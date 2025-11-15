package com.tufanakcay.androidwebview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient; 
// Import yang diperlukan jika menggunakan file lokal (tidak wajib, tapi baik untuk kejelasan)
import java.io.File; 

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
        
        // =================================================================
        // PERUBAHAN PENTING 1: GANTI SUMBER URL KE KONTEN LOKAL
        // =================================================================
        // Baris asli yang memuat dari string.xml (URL eksternal) dihapus/dikomentari:
        // String dynamicUrl = getString(R.string.web_url); 

        // Gunakan URL untuk memuat file index.html dari folder 'assets'
        String localAssetUrl = "file:///android_asset/index.html"; 
        // =================================================================

        
        // 1. Dapatkan Objek WebSettings
        WebSettings webSettings = webView.getSettings();

        // --- PENGATURAN WEBVIEW UNTUK KONTEN LOKAL ---

        // 2. Aktifkan JavaScript (Disarankan jika konten lokal Anda menggunakan JS)
        webSettings.setJavaScriptEnabled(true);
        
        // 3. Aktifkan DOM Storage (Opsional untuk lokal, tapi tidak ada salahnya dipertahankan)
        webSettings.setDomStorageEnabled(true); 

        // 4. Set Cache Mode ke NORMAL (Bagus untuk lokal dan membantu performa)
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        
        // 5. Setting Tampilan (Dipertahankan seperti yang Anda inginkan)
        webSettings.setBuiltInZoomControls(false); 
        webSettings.setDisplayZoomControls(false);
        
        // 6. Set User Agent (Dihapus/dikomentari karena biasanya tidak relevan untuk konten lokal)
        // webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Mobile Safari/537.36");
        
        // 7. Gunakan WebViewClient Kustom untuk penanganan yang lebih baik
        webView.setWebViewClient(new CustomWebViewClient()); 

        // --- AKHIR PENGATURAN ---

        // =================================================================
        // PERUBAHAN PENTING 2: MEMUAT KONTEN LOKAL
        // =================================================================
        webView.loadUrl(localAssetUrl);
        // =================================================================
    }
    
    // Kelas Kustom untuk menangani loading URL di dalam WebView
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Kita harus memastikan link internal (ke halaman lain di assets) 
            // tetap dimuat di dalam WebView.
            // Periksa jika URL adalah file lokal atau link "http/https" (jika ada link eksternal)
            if (url.startsWith("file:///android_asset/") || url.startsWith("http") || url.startsWith("https")) {
                 view.loadUrl(url);
                 return true; 
            }
            // Jika bukan, mungkin link ke aplikasi lain (misalnya email/dialer),
            // kita bisa membiarkannya ditangani oleh sistem (return false).
            return false;
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
             // Opsional: Lakukan sesuatu setelah loading selesai 
             super.onPageFinished(view, url);
        }
    }
}
