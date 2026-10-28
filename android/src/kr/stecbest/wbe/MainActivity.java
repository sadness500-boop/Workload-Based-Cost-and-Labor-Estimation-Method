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
import android.webkit.GeolocationPermissions;
import android.content.pm.PackageManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Workload-Based Cost and Labor Estimation Method v1.20 — 에스텍 그룹. Produced by RatRae Choi – October 21, 2026 */
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
        s.setGeolocationEnabled(true);   // 내 위치 (GPS)
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

    /* ===== 자동 업데이트: 저장소의 새 화면 파일을 받아 다음부터(받자마자) 그 파일로 실행 ===== */
    private static final String RAW = "https://raw.githubusercontent.com/sadness500-boop/Workload-Based-Cost-and-Labor-Estimation-Method/main/";
    private volatile boolean updating = false;
    private String bundledVer() { try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return "0"; } }
    private File cachedHtml() { return new File(getFilesDir(), "www/index.html"); }
    private static boolean verGt(String a, String b) {
        String[] x = a.replaceAll("[^0-9.]", "").split("\\."), y = b.replaceAll("[^0-9.]", "").split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length && x[i].length() > 0 ? Integer.parseInt(x[i]) : 0, q = i < y.length && y[i].length() > 0 ? Integer.parseInt(y[i]) : 0;
            if (p != q) return p > q;
        }
        return false;
    }
    private String currentVer() { String c = prefs.getString("cachedVer", ""); return (c.length() > 0 && cachedHtml().exists() && verGt(c, bundledVer())) ? c : bundledVer(); }

    private void loadApp() {
        try {
            String c = prefs.getString("cachedVer", "");
            InputStream in = (c.length() > 0 && cachedHtml().exists() && verGt(c, bundledVer())) ? new FileInputStream(cachedHtml()) : getAssets().open("www/index.html");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            web.loadDataWithBaseURL(BASE, bo.toString("UTF-8"), "text/html", "UTF-8", null);
        } catch (Exception e) {
            web.loadUrl(OLD);
        }
        checkUpdate();
    }

    private static byte[] get(String u, int max) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000); c.setReadTimeout(30000); c.setUseCaches(false); c.setInstanceFollowRedirects(true);
        if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
        InputStream in = c.getInputStream(); ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[65536]; int n;
        while ((n = in.read(b)) > 0) { bo.write(b, 0, n); if (bo.size() > max) throw new Exception("too big"); }
        in.close(); c.disconnect(); return bo.toByteArray();
    }

    private void checkUpdate() {
        if (updating) return; updating = true;
        new Thread(new Runnable() { public void run() {
            try {
                String js = new String(get(RAW + "version.json?t=" + System.currentTimeMillis(), 20000), "UTF-8");
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(js);
                if (!m.find()) return;
                final String v = m.group(1);
                if (!verGt(v, currentVer())) return;
                byte[] html = get(RAW + "index.html?t=" + System.currentTimeMillis(), 40 * 1024 * 1024);
                String head = new String(html, 0, Math.min(html.length, 4000), "UTF-8");
                if (html.length < 500000 || head.indexOf("Workload-Based Cost and Labor Estimation Method") < 0) return;
                File f = cachedHtml(); f.getParentFile().mkdirs();
                File tmp = new File(f.getPath() + ".tmp"); FileOutputStream fo = new FileOutputStream(tmp); fo.write(html); fo.close();
                if (f.exists()) f.delete(); tmp.renameTo(f);
                prefs.edit().putString("cachedVer", v).apply();
                runOnUiThread(new Runnable() { public void run() {
                    Toast.makeText(MainActivity.this, "새 버전 v" + v.replaceAll("^v", "") + " 으로 자동 업데이트했습니다", Toast.LENGTH_LONG).show();
                    updating = false; loadApp();
                } });
                return;
            } catch (Exception ignored) {
            } finally { if (updating) updating = false; }
        } }).start();
    }

    private boolean openOutside(Uri u) {
        String sc = u.getScheme();
        if (sc == null || sc.equals("file") || sc.equals("data") || sc.equals("blob") || sc.equals("about")) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
        return true;
    }

    /** 견적서 불러오기: 기기 파일·구글 드라이브·네이버 MYBOX 등 문서 선택기 */
    private GeolocationPermissions.Callback geoCb; private String geoOrigin;
    private static final int REQ_LOC = 2001;
    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == REQ_LOC && geoCb != null) {
            boolean ok = res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED;
            geoCb.invoke(geoOrigin, ok, false); geoCb = null;
        }
    }

    private class Chrome extends WebChromeClient {
        /** 내 위치: 휴대폰 위치 권한을 물어보고 허용하면 GPS 위치를 알려 줍니다 */
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                geoCb = callback; geoOrigin = origin;
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
            } else callback.invoke(origin, true, false);
        }
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

    /** 엑셀 저장: 다운로드/WBE 폴더 · Things to enjoy 앱 열기 */
    private class Bridge {
        /** 화면에서 새 버전을 알아챘을 때 */
        @JavascriptInterface
        public void updateNow() { runOnUiThread(new Runnable() { public void run() { checkUpdate(); } }); }
        /** 설치된 앱이면 바로 실행 */
        @JavascriptInterface
        public boolean openApp(String pkg) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i == null) return false;
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            } catch (Exception e) { return false; }
        }
        /** 친구 추천: 카톡·문자 등 공유 창 */
        @JavascriptInterface
        public void share(String text) {
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "업무량 기반 비용·인력 산정 프로그램 추천");
                i.putExtra(Intent.EXTRA_TEXT, text);
                Intent c = Intent.createChooser(i, "친구 추천");
                c.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(c);
            } catch (Exception ignored) { }
        }
        /** 설치 파일·웹 주소는 휴대폰 브라우저로 */
        @JavascriptInterface
        public void openUrl(String url) {
            try { Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); } catch (Exception ignored) { }
        }

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
