package kr.stecbest.wbe;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Workload-Based Cost and Labor Estimation Method v1.13 — 에스텍 그룹. Produced by RatRae Choi – October 21, 2026 */
public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    /** 카카오 지도 키의 등록 도메인과 같은 주소로 화면을 엽니다 (내용은 앱 안의 파일) */
    private static final String BASE = "https://sadness500-boop.github.io/Workload-Based-Cost-and-Labor-Estimation-Method/";
    private static final String OLD = "file:///android_asset/www/index.html";
    private SharedPreferences prefs;
    private int stage = 0;   // 0: 이전 저장값 옮기기, 1: 새 주소로 실행

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(0xFF231470);
        w.setNavigationBarColor(0xFF231470);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        prefs = getSharedPreferences("wbe", MODE_PRIVATE);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                if (stage == 0 && url != null && url.startsWith("file:")) {
                    // v1.10 이전 앱에 저장된 입력값·담당자 정보를 새 주소로 옮깁니다 (최초 1회)
                    v.evaluateJavascript("(function(){try{return JSON.stringify(Object.assign({},localStorage));}catch(e){return '{}';}})()", new ValueCallback<String>() {
                        @Override public void onReceiveValue(String value) {
                            prefs.edit().putString("old", value == null ? "" : value).apply();
                            stage = 1; loadApp();
                        }
                    });
                } else if (stage == 1 && !prefs.getBoolean("migrated", false)) {
                    prefs.edit().putBoolean("migrated", true).apply();
                    String old = prefs.getString("old", "");
                    if (old != null && old.length() > 4 && !old.equals("null") && !old.equals("\"{}\"")) {
                        v.evaluateJavascript("(function(){try{var o=JSON.parse(" + old + ");var n=0;for(var k in o){if(localStorage.getItem(k)===null){localStorage.setItem(k,o[k]);n++;}}return n;}catch(e){return 0;}})()", new ValueCallback<String>() {
                            @Override public void onReceiveValue(String n) { if (n != null && !n.equals("0")) loadApp(); }
                        });
                    }
                }
            }
            /** 앱 다운로드·웹 버전 링크는 휴대폰 브라우저로 엽니다 */
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) { return openOutside(req.getUrl()); }
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView v, String url) { return openOutside(Uri.parse(url)); }
        });
        web.setWebChromeClient(new Chrome());
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setBackgroundColor(0xFFF3F3F8);
        setContentView(web);
        if (prefs.getBoolean("migrated", false)) { stage = 1; loadApp(); }
        else web.loadUrl(OLD);
    }

    private void loadApp() {
        try {
            InputStream in = getAssets().open("www/index.html");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            web.loadDataWithBaseURL(BASE, bo.toString("UTF-8"), "text/html", "UTF-8", null);
        } catch (Exception e) {
            web.loadUrl(OLD);
        }
    }

    private boolean openOutside(Uri u) {
        String sc = u.getScheme();
        if (sc == null || sc.equals("file") || sc.equals("data") || sc.equals("blob") || sc.equals("about")) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
        return true;
    }

    /** 견적서 불러오기: 기기 파일·구글 드라이브·네이버 MYBOX 등 문서 선택기 */
    private class Chrome extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            boolean image = false;
            String[] acc = params != null ? params.getAcceptTypes() : null;
            if (acc != null) for (String a : acc) if (a != null && a.startsWith("image")) image = true;
            if (image) {
                pick.setType("image/*");   // 견적 담당 사진
            } else {
                pick.setType("*/*");
                pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel", "application/octet-stream"});
            }
            try {
                startActivityForResult(Intent.createChooser(pick, image ? "사진 선택" : "견적서 선택"), PICK_FILE);
            } catch (Exception e) {
                fileCallback = null;
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /** 엑셀 저장: 다운로드/WBE 폴더 */
    private class Bridge {
        @JavascriptInterface
        public String saveBase64(String name, String mime, String b64) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
                final Uri uri;
                String where;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
                    v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WBE");
                    uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) return "저장하지 못했습니다";
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(bytes);
                    os.close();
                    where = "다운로드/WBE 폴더에 저장했습니다: " + safe;
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    File f = new File(dir, safe);
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(bytes);
                    fo.close();
                    uri = null;
                    where = "저장했습니다: " + f.getAbsolutePath();
                }
                final String msg = where;
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                        if (uri != null) {
                            try {
                                Intent view = new Intent(Intent.ACTION_VIEW);
                                view.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                                view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(Intent.createChooser(view, "견적서 열기"));
                            } catch (Exception ignored) { }
                        }
                    }
                });
                return where;
            } catch (Exception e) {
                return "저장하지 못했습니다: " + e.getMessage();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
