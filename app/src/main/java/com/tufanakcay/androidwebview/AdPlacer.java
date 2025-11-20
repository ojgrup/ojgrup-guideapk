package com.tufanakcay.androidwebview; 
// Sesuaikan dengan package Anda

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;

public class AdPlacer {
    // ... (Konstruktor dan variabel sama seperti sebelumnya) ...
    private final Activity activity;
    private final WebView webView;
    private final Context context;

    public AdPlacer(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.context = activity.getApplicationContext();
    }
    
    @JavascriptInterface
    public void requestNativeAd(final String placeholderId) {
        activity.runOnUiThread(() -> {
            String adUnitId = "ca-app-pub-3940256099942544/2247696110"; 

            AdLoader adLoader = new AdLoader.Builder(context, adUnitId)
                .forNativeAd(nativeAd -> {
                    String adHtml = inflateNativeAdToHtmlString(nativeAd);
                    
                    // Suntikkan HTML Iklan lengkap dengan spacing ke placeholder 
                    String js = "javascript:document.getElementById('" + placeholderId + "_web').innerHTML = '" + 
                                escapeJavaScript(adHtml) + "';"; 
                    webView.evaluateJavascript(js, null);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e("AdPlacer", "Ad failed to load: " + adError.getMessage());
                        // Jika gagal, biarkan placeholder kosong dan tidak terlihat
                    }
                })
                .build();

            adLoader.loadAd(new AdRequest.Builder().build());
        });
    }

    private String escapeJavaScript(String html) {
        return html.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "");
    }

    private String inflateNativeAdToHtmlString(NativeAd nativeAd) {
        String headline = nativeAd.getHeadline() != null ? nativeAd.getHeadline() : "Iklan Unggulan";
        String body = nativeAd.getBody() != null ? nativeAd.getBody() : "Lihat penawaran menarik ini!";
        String callToAction = nativeAd.getCallToAction() != null ? nativeAd.getCallToAction() : "KLIK DI SINI";

        // 🔥 KOREKSI UTAMA: Semua spacing (margin, padding, box-shadow) dipindahkan ke string HTML ini.
        return 
               "<div style='margin: 10px 20px 20px 20px;'>" + // Margin luar untuk memisahkan dari kartu di atas dan bawah
               "<a href='#' onclick=\"window.location.href='https://play.google.com/store/apps/';\" style='text-decoration: none; color: inherit; display: block;'>" + 
               "<div style='background-color: #fff; border-radius: 12px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); padding: 15px 15px; display: flex; align-items: center;'>" + 
               "<div style='flex-grow: 1;'>" + 
               "<div style='font-weight: bold; font-size: 1.2em; color: #000; margin-bottom: 5px;'>Tes Iklan: Google Ads</div>" + // Ganti dengan headline
               "<div style='font-size: 0.9em; color: #555;'>Stay up to date with your Ads Check how your ads are performing</div>" + // Ganti dengan body
               "</div>" +
               "<span style='background-color: #4CAF50; color: white; padding: 5px 10px; border-radius: 5px; font-weight: bold; margin-left: 10px;'>" + callToAction + "</span>" +
               "</div>" +
               "</a>" + 
               "</div>";
    }
}
