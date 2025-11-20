package com.tufanakcay.androidwebview; // SESUAIKAN

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
            String adUnitId = "ca-app-pub-3940256099942544/2247696110"; // ID Native Ad Test

            AdLoader adLoader = new AdLoader.Builder(context, adUnitId)
                .forNativeAd(nativeAd -> {
                    String adHtml = inflateNativeAdToHtmlString(nativeAd);
                    String js = "javascript:document.getElementById('" + placeholderId + "_web').innerHTML = '" + 
                                escapeJavaScript(adHtml) + "';"; 
                    webView.evaluateJavascript(js, null);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e("AdPlacer", "Native Ad failed to load: " + adError.getMessage());
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

        // Style HTML sederhana untuk Native Ad (pastikan ini sesuai dengan CSS HTML Anda)
        return 
               "<div style='margin: 20px 20px;'>" + 
               "<a href='#' onclick=\"window.location.href='https://play.google.com/store/apps/';\" style='text-decoration: none; color: inherit; display: block;'>" + 
               "<div style='background-color: #fff; border-radius: 12px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); padding: 15px 15px; display: flex; align-items: center;'>" + 
               "<div style='flex-grow: 1;'>" + 
               "<div style='font-weight: bold; font-size: 1.2em; color: #000; margin-bottom: 5px;'>" + headline + "</div>" +
               "<div style='font-size: 0.9em; color: #555;'>" + body + "</div>" + 
               "</div>" +
               "<span style='background-color: #4CAF50; color: white; padding: 5px 10px; border-radius: 5px; font-weight: bold; margin-left: 10px;'>" + callToAction + "</span>" +
               "</div>" +
               "</a>" + 
               "</div>";
    }
}
