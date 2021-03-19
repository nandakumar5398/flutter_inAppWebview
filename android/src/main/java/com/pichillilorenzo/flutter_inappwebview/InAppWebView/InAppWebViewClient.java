package com.pichillilorenzo.flutter_inappwebview.InAppWebView;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.pichillilorenzo.flutter_inappwebview.CredentialDatabase.Credential;
import com.pichillilorenzo.flutter_inappwebview.CredentialDatabase.CredentialDatabase;
import com.pichillilorenzo.flutter_inappwebview.InAppBrowser.InAppBrowserActivity;
import com.pichillilorenzo.flutter_inappwebview.JavaScriptBridgeInterface;
import com.pichillilorenzo.flutter_inappwebview.Util;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import android.util.Base64;

import io.flutter.plugin.common.MethodChannel;

public class InAppWebViewClient extends WebViewClient {

  protected static final String LOG_TAG = "IAWebViewClient";
  private FlutterWebView flutterWebView;
  private InAppBrowserActivity inAppBrowserActivity;
  public MethodChannel channel;
  private static int previousAuthRequestFailureCount = 0;
  private static List<Credential> credentialsProposed = null;
  String htmlData = "<!DOCTYPE html><html><head><title>Maintenance</title><style type='text/css'>html, body{font-family: 'Montserrat', sans-serif !important;}.server{text-align: center;width: 80%;margin: 10% auto;}html{height: 100%;background-color: #FBFBFB;}.server p{color: #9E9E9E !important;font-size: 14px !important;}img{width: 180px;}a{color: #3F51B5 !important;}</style></head><body><div class='server'><img src=\"data:image/png;base64,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\" alt=\"Issue Occurred\" /><h2 style='color: #9E9E9E;font-weight: normal;'>Under Maintenance</h2><p>Unable to process your request please try again by restarting the application !!!</p><p>please contact <a href='mailto:info@tmsforce.com' target='_top'>info@tmsforce.com</a> for further clarifications</p></div></body></html>";
  String encodehtmlData = Base64.encodeToString(htmlData.getBytes(), Base64.NO_PADDING);


  public InAppWebViewClient(Object obj) {
    super();
    if (obj instanceof InAppBrowserActivity)
      this.inAppBrowserActivity = (InAppBrowserActivity) obj;
    else if (obj instanceof FlutterWebView)
      this.flutterWebView = (FlutterWebView) obj;
    this.channel = (this.inAppBrowserActivity != null) ? this.inAppBrowserActivity.channel : this.flutterWebView.channel;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    InAppWebView webView = (InAppWebView) view;
    if (webView.options.useShouldOverrideUrlLoading) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        onShouldOverrideUrlLoading(
                webView,
                request.getUrl().toString(),
                request.getMethod(),
                request.getRequestHeaders(),
                request.isForMainFrame(),
                request.hasGesture(),
                request.isRedirect());
      } else {
        onShouldOverrideUrlLoading(
                webView,
                request.getUrl().toString(),
                request.getMethod(),
                request.getRequestHeaders(),
                request.isForMainFrame(),
                request.hasGesture(),
                false);
      }
      if (webView.regexToCancelSubFramesLoadingCompiled != null) {
        if (request.isForMainFrame())
          return true;
        else {
          Matcher m = webView.regexToCancelSubFramesLoadingCompiled.matcher(request.getUrl().toString());
          if (m.matches())
            return true;
          else
            return false;
        }
      } else {
        // There isn't any way to load an URL for a frame that is not the main frame,
        // so if the request is not for the main frame, the navigation is allowed.
        return request.isForMainFrame();
      }
    }
    return false;
  }

  @Override
  public boolean shouldOverrideUrlLoading(WebView webView, String url) {
    InAppWebView inAppWebView = (InAppWebView) webView;
    if (inAppWebView.options.useShouldOverrideUrlLoading) {
      onShouldOverrideUrlLoading(inAppWebView, url, "GET", null,true, false, false);
      return true;
    }
    return false;
  }

  public void onShouldOverrideUrlLoading(final InAppWebView webView, final String url, final String method, final Map<String, String> headers,
                                         final boolean isForMainFrame, boolean hasGesture, boolean isRedirect) {
    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", url);
    obj.put("method", method);
    obj.put("headers", headers);
    obj.put("isForMainFrame", isForMainFrame);
    obj.put("androidHasGesture", hasGesture);
    obj.put("androidIsRedirect", isRedirect);
    obj.put("iosWKNavigationType", null);
    channel.invokeMethod("shouldOverrideUrlLoading", obj, new MethodChannel.Result() {
      @Override
      public void success(Object response) {
        if (response != null) {
          Map<String, Object> responseMap = (Map<String, Object>) response;
          Integer action = (Integer) responseMap.get("action");
          if (action != null) {
            switch (action) {
              case 1:
                if (isForMainFrame) {
                  // There isn't any way to load an URL for a frame that is not the main frame,
                  // so call this only on main frame.
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    webView.loadUrl(url, headers);
                  else
                    webView.loadUrl(url);
                }
                return;
              case 0:
              default:
                return;
            }
          }
        }
      }

      @Override
      public void error(String s, String s1, Object o) {
        Log.d(LOG_TAG, "ERROR: " + s + " " + s1);
      }

      @Override
      public void notImplemented() {

      }
    });
  }

  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {

    InAppWebView webView = (InAppWebView) view;

    String js = InAppWebView.consoleLogJS.replaceAll("[\r\n]+", "");
    js += JavaScriptBridgeInterface.flutterInAppBroserJSClass.replaceAll("[\r\n]+", "");
    if (webView.options.useShouldInterceptAjaxRequest) {
      js += InAppWebView.interceptAjaxRequestsJS.replaceAll("[\r\n]+", "");
    }
    if (webView.options.useShouldInterceptFetchRequest) {
      js += InAppWebView.interceptFetchRequestsJS.replaceAll("[\r\n]+", "");
    }
    if (webView.options.useOnLoadResource) {
      js += InAppWebView.resourceObserverJS.replaceAll("[\r\n]+", "");
    }
    js += InAppWebView.checkGlobalKeyDownEventToHideContextMenuJS.replaceAll("[\r\n]+", "");
    js += InAppWebView.onWindowFocusEventJS.replaceAll("[\r\n]+", "");
    js += InAppWebView.onWindowBlurEventJS.replaceAll("[\r\n]+", "");
    js += InAppWebView.printJS.replaceAll("[\r\n]+", "");

    js = InAppWebView.scriptsWrapperJS
            .replace("$PLACEHOLDER_VALUE", js)
            .replaceAll("[\r\n]+", "");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      webView.evaluateJavascript(js, (ValueCallback<String>) null);
    } else {
      webView.loadUrl("javascript:" + js);
    }

    super.onPageStarted(view, url, favicon);

    webView.isLoading = true;
    if (inAppBrowserActivity != null && inAppBrowserActivity.searchView != null && !url.equals(inAppBrowserActivity.searchView.getQuery().toString())) {
      inAppBrowserActivity.searchView.setQuery(url, false);
    }

    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", url);
    channel.invokeMethod("onLoadStart", obj);
  }


  public void onPageFinished(WebView view, String url) {
    final InAppWebView webView = (InAppWebView) view;

    super.onPageFinished(view, url);

    webView.isLoading = false;
    previousAuthRequestFailureCount = 0;
    credentialsProposed = null;

    // CB-10395 InAppBrowserManager's WebView not storing cookies reliable to local device storage
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().flush();
    } else {
      CookieSyncManager.getInstance().sync();
    }

    String js = InAppWebView.platformReadyJS.replaceAll("[\r\n]+", "");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      webView.evaluateJavascript(js, (ValueCallback<String>) null);
    } else {
      webView.loadUrl("javascript:" + js);
    }

    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", url);
    channel.invokeMethod("onLoadStop", obj);
  }

  @Override
  public void doUpdateVisitedHistory (WebView view, String url, boolean isReload) {
    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", url);
    obj.put("androidIsReload", isReload);
    channel.invokeMethod("onUpdateVisitedHistory", obj);

    super.doUpdateVisitedHistory(view, url, isReload);
  }

  @Override
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

    final InAppWebView webView = (InAppWebView) view;

    if (webView.options.disableDefaultErrorPage) {
      webView.stopLoading();
      webView.loadUrl("about:blank");
    }

    webView.isLoading = false;
    previousAuthRequestFailureCount = 0;
    credentialsProposed = null;

    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", failingUrl);
    obj.put("code", errorCode);
    obj.put("message", description);
    //view.loadData(htmlData, "text/html", "UTF-8");
    view.loadData(encodehtmlData, "text/html", "base64");
    channel.invokeMethod("onLoadError", obj);

    super.onReceivedError(view, errorCode, description, failingUrl);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public void onReceivedHttpError (WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
    super.onReceivedHttpError(view, request, errorResponse);
    if(request.isForMainFrame()) {
      Map<String, Object> obj = new HashMap<>();
      if (inAppBrowserActivity != null)
        obj.put("uuid", inAppBrowserActivity.uuid);
      obj.put("url", request.getUrl().toString());
      obj.put("statusCode", errorResponse.getStatusCode());
      obj.put("description", errorResponse.getReasonPhrase());
      view.loadData(encodehtmlData, "text/html", "base64");
      //view.loadData(htmlData, "text/html", "UTF-8");
      channel.invokeMethod("onLoadHttpError", obj);
    }
  }

  /**
   * On received http auth request.
   */
  @Override
  public void onReceivedHttpAuthRequest(final WebView view, final HttpAuthHandler handler, final String host, final String realm) {

    URL url;
    try {
      url = new URL(view.getUrl());
    } catch (MalformedURLException e) {
      e.printStackTrace();

      credentialsProposed = null;
      previousAuthRequestFailureCount = 0;

      handler.cancel();
      return;
    }

    final String protocol = url.getProtocol();
    final int port = url.getPort();

    previousAuthRequestFailureCount++;

    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("host", host);
    obj.put("protocol", protocol);
    obj.put("realm", realm);
    obj.put("port", port);
    obj.put("previousFailureCount", previousAuthRequestFailureCount);

    channel.invokeMethod("onReceivedHttpAuthRequest", obj, new MethodChannel.Result() {
      @Override
      public void success(Object response) {
        if (response != null) {
          Map<String, Object> responseMap = (Map<String, Object>) response;
          Integer action = (Integer) responseMap.get("action");
          if (action != null) {
            switch (action) {
              case 1:
                String username = (String) responseMap.get("username");
                String password = (String) responseMap.get("password");
                Boolean permanentPersistence = (Boolean) responseMap.get("permanentPersistence");
                if (permanentPersistence != null && permanentPersistence && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  CredentialDatabase.getInstance(view.getContext()).setHttpAuthCredential(host, protocol, realm, port, username, password);
                }
                handler.proceed(username, password);
                return;
              case 2:
                if (credentialsProposed == null)
                  credentialsProposed = CredentialDatabase.getInstance(view.getContext()).getHttpAuthCredentials(host, protocol, realm, port);
                if (credentialsProposed.size() > 0) {
                  Credential credential = credentialsProposed.remove(0);
                  handler.proceed(credential.username, credential.password);
                } else {
                  handler.cancel();
                }
                // used custom CredentialDatabase!
                // handler.useHttpAuthUsernamePassword();
                return;
              case 0:
              default:
                credentialsProposed = null;
                previousAuthRequestFailureCount = 0;
                handler.cancel();
                return;
            }
          }
        }

        InAppWebViewClient.super.onReceivedHttpAuthRequest(view, handler, host, realm);
      }

      @Override
      public void error(String s, String s1, Object o) {
        Log.e(LOG_TAG, s + ", " + s1);
      }

      @Override
      public void notImplemented() {
        InAppWebViewClient.super.onReceivedHttpAuthRequest(view, handler, host, realm);
      }
    });
  }

  @Override
  public void onReceivedSslError(final WebView view, final SslErrorHandler handler, final SslError error) {
    URL url;
    try {
      url = new URL(error.getUrl());
    } catch (MalformedURLException e) {
      e.printStackTrace();
      handler.cancel();
      return;
    }

    final String host = url.getHost();
    final String protocol = url.getProtocol();
    final String realm = null;
    final int port = url.getPort();

    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("host", host);
    obj.put("protocol", protocol);
    obj.put("realm", realm);
    obj.put("port", port);
    obj.put("androidError", error.getPrimaryError());
    obj.put("iosError", null);
    obj.put("sslCertificate", InAppWebView.getCertificateMap(error.getCertificate()));

    String message;
    switch (error.getPrimaryError()) {
      case SslError.SSL_DATE_INVALID:
        message = "The date of the certificate is invalid";
        break;
      case SslError.SSL_EXPIRED:
        message = "The certificate has expired";
        break;
      case SslError.SSL_IDMISMATCH:
        message = "Hostname mismatch";
        break;
      default:
      case SslError.SSL_INVALID:
        message = "A generic error occurred";
        break;
      case SslError.SSL_NOTYETVALID:
        message = "The certificate is not yet valid";
        break;
      case SslError.SSL_UNTRUSTED:
        message = "The certificate authority is not trusted";
        break;
    }
    obj.put("message", message);

    channel.invokeMethod("onReceivedServerTrustAuthRequest", obj, new MethodChannel.Result() {
      @Override
      public void success(Object response) {
        if (response != null) {
          Map<String, Object> responseMap = (Map<String, Object>) response;
          Integer action = (Integer) responseMap.get("action");
          if (action != null) {
            switch (action) {
              case 1:
                handler.proceed();
                return;
              case 0:
              default:
                handler.cancel();
                return;
            }
          }
        }

        InAppWebViewClient.super.onReceivedSslError(view, handler, error);
      }

      @Override
      public void error(String s, String s1, Object o) {
        Log.e(LOG_TAG, s + ", " + s1);
      }

      @Override
      public void notImplemented() {
        InAppWebViewClient.super.onReceivedSslError(view, handler, error);
      }
    });
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onReceivedClientCertRequest(final WebView view, final ClientCertRequest request) {

    URL url;
    try {
      url = new URL(view.getUrl());
    } catch (MalformedURLException e) {
      e.printStackTrace();
      request.cancel();
      return;
    }

    final String protocol = url.getProtocol();
    final String realm = null;

    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("host", request.getHost());
    obj.put("protocol", protocol);
    obj.put("realm", realm);
    obj.put("port", request.getPort());

    channel.invokeMethod("onReceivedClientCertRequest", obj, new MethodChannel.Result() {
      @Override
      public void success(Object response) {
        if (response != null) {
          Map<String, Object> responseMap = (Map<String, Object>) response;
          Integer action = (Integer) responseMap.get("action");
          if (action != null) {
            switch (action) {
              case 1:
                {
                  InAppWebView webView = (InAppWebView) view;
                  String certificatePath = (String) responseMap.get("certificatePath");
                  String certificatePassword = (String) responseMap.get("certificatePassword");
                  String androidKeyStoreType = (String) responseMap.get("androidKeyStoreType");
                  Util.PrivateKeyAndCertificates privateKeyAndCertificates = Util.loadPrivateKeyAndCertificate(certificatePath, certificatePassword, androidKeyStoreType);
                  request.proceed(privateKeyAndCertificates.privateKey, privateKeyAndCertificates.certificates);
                }
                return;
              case 2:
                request.ignore();
                return;
              case 0:
              default:
                request.cancel();
                return;
            }
          }
        }

        InAppWebViewClient.super.onReceivedClientCertRequest(view, request);
      }

      @Override
      public void error(String s, String s1, Object o) {
        Log.e(LOG_TAG, s + ", " + s1);
      }

      @Override
      public void notImplemented() {
        InAppWebViewClient.super.onReceivedClientCertRequest(view, request);
      }
    });
  }

  @Override
  public void onScaleChanged(WebView view, float oldScale, float newScale) {
    super.onScaleChanged(view, oldScale, newScale);
    final InAppWebView webView = (InAppWebView) view;
    webView.scale = newScale;

    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("oldScale", oldScale);
    obj.put("newScale", newScale);
    channel.invokeMethod("onScaleChanged", obj);
  }

  @RequiresApi(api = Build.VERSION_CODES.O_MR1)
  @Override
  public void onSafeBrowsingHit(final WebView view, final WebResourceRequest request, final int threatType, final SafeBrowsingResponse callback) {
    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", request.getUrl().toString());
    obj.put("threatType", threatType);

    channel.invokeMethod("onSafeBrowsingHit", obj, new MethodChannel.Result() {
      @Override
      public void success(Object response) {
        if (response != null) {
          Map<String, Object> responseMap = (Map<String, Object>) response;
          Boolean report = (Boolean) responseMap.get("report");
          Integer action = (Integer) responseMap.get("action");

          report = report != null ? report : true;

          if (action != null) {
            switch (action) {
              case 0:
                callback.backToSafety(report);
                return;
              case 1:
                callback.proceed(report);
                return;
              case 2:
              default:
                callback.showInterstitial(report);
                return;
            }
          }
        }

        InAppWebViewClient.super.onSafeBrowsingHit(view, request, threatType, callback);
      }

      @Override
      public void error(String s, String s1, Object o) {
        Log.e(LOG_TAG, s + ", " + s1);
      }

      @Override
      public void notImplemented() {
        InAppWebViewClient.super.onSafeBrowsingHit(view, request, threatType, callback);
      }
    });
  }

  @Override
  public WebResourceResponse shouldInterceptRequest(WebView view, final String url) {

    final InAppWebView webView = (InAppWebView) view;

    if (webView.options.useShouldInterceptRequest) {
      WebResourceResponse onShouldInterceptResponse = onShouldInterceptRequest(url);
      if (onShouldInterceptResponse != null) {
        return onShouldInterceptResponse;
      }
    }

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException uriExpection) {
      String[] urlSplitted = url.split(":");
      String scheme = urlSplitted[0];
      try {
        URL tempUrl = new URL(url.replace(scheme, "https"));
        uri = new URI(scheme, tempUrl.getUserInfo(), tempUrl.getHost(), tempUrl.getPort(), tempUrl.getPath(), tempUrl.getQuery(), tempUrl.getRef());
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    String scheme = uri.getScheme();

    if (webView.options.resourceCustomSchemes != null && webView.options.resourceCustomSchemes.contains(scheme)) {
      final Map<String, Object> obj = new HashMap<>();
      if (inAppBrowserActivity != null)
        obj.put("uuid", inAppBrowserActivity.uuid);
      obj.put("url", url);
      obj.put("scheme", scheme);

      Util.WaitFlutterResult flutterResult;
      try {
        flutterResult = Util.invokeMethodAndWait(channel, "onLoadResourceCustomScheme", obj);
      } catch (InterruptedException e) {
        e.printStackTrace();
        return null;
      }

      if (flutterResult.error != null) {
        Log.e(LOG_TAG, flutterResult.error);
      }
      else if (flutterResult.result != null) {
        Map<String, Object> res = (Map<String, Object>) flutterResult.result;
        WebResourceResponse response = null;
        try {
          response = webView.contentBlockerHandler.checkUrl(webView, url, res.get("content-type").toString());
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (response != null)
          return response;
        byte[] data = (byte[]) res.get("data");
        return new WebResourceResponse(res.get("content-type").toString(), res.get("content-encoding").toString(), new ByteArrayInputStream(data));
      }
    }

    WebResourceResponse response = null;
    if (webView.contentBlockerHandler.getRuleList().size() > 0) {
      try {
        response = webView.contentBlockerHandler.checkUrl(webView, url);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return response;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
    final InAppWebView webView = (InAppWebView) view;

    String url = request.getUrl().toString();

    if (webView.options.useShouldInterceptRequest) {
      WebResourceResponse onShouldInterceptResponse = onShouldInterceptRequest(request);
      if (onShouldInterceptResponse != null) {
        return onShouldInterceptResponse;
      }
    }

    return shouldInterceptRequest(view, url);
  }

  public WebResourceResponse onShouldInterceptRequest(Object request) {
    String url = request instanceof String ? (String) request : null;
    String method = "GET";
    Map<String, String> headers = null;
    Boolean hasGesture = false;
    Boolean isForMainFrame = true;
    Boolean isRedirect = false;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request instanceof WebResourceRequest) {
      WebResourceRequest webResourceRequest = (WebResourceRequest) request;
      url = webResourceRequest.getUrl().toString();
      headers = webResourceRequest.getRequestHeaders();
      hasGesture = webResourceRequest.hasGesture();
      isForMainFrame = webResourceRequest.isForMainFrame();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        isRedirect = webResourceRequest.isRedirect();
      }
    }

    final Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", url);
    obj.put("method", method);
    obj.put("headers", headers);
    obj.put("isForMainFrame", isForMainFrame);
    obj.put("hasGesture", hasGesture);
    obj.put("isRedirect", isRedirect);

    Util.WaitFlutterResult flutterResult;
    try {
      flutterResult = Util.invokeMethodAndWait(channel, "shouldInterceptRequest", obj);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return null;
    }

    if (flutterResult.error != null) {
      Log.e(LOG_TAG, flutterResult.error);
    }
    else if (flutterResult.result != null) {
      Map<String, Object> res = (Map<String, Object>) flutterResult.result;
      String contentType = (String) res.get("contentType");
      String contentEncoding = (String) res.get("contentEncoding");
      byte[] data = (byte[]) res.get("data");
      Map<String, String> responseHeaders = (Map<String, String>) res.get("headers");
      Integer statusCode = (Integer) res.get("statusCode");
      String reasonPhrase = (String) res.get("reasonPhrase");

      ByteArrayInputStream inputStream = (data != null) ? new ByteArrayInputStream(data) : null;

      if ((responseHeaders == null && statusCode == null && reasonPhrase == null) || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        return new WebResourceResponse(contentType, contentEncoding, inputStream);
      } else {
        return new WebResourceResponse(contentType, contentEncoding, statusCode, reasonPhrase, responseHeaders, inputStream);
      }
    }

    return null;
  }

  @Override
  public void onFormResubmission (final WebView view, final Message dontResend, final Message resend) {
    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", view.getUrl());

    channel.invokeMethod("onFormResubmission", obj, new MethodChannel.Result() {

      @Override
      public void success(@Nullable Object response) {
        if (response != null) {
          Map<String, Object> responseMap = (Map<String, Object>) response;
          Integer action = (Integer) responseMap.get("action");
          if (action != null) {
            switch (action) {
              case 0:
                resend.sendToTarget();
                return;
              case 1:
              default:
                dontResend.sendToTarget();
                return;
            }
          }
        }

        InAppWebViewClient.super.onFormResubmission(view, dontResend, resend);
      }

      @Override
      public void error(String errorCode, @Nullable String errorMessage, @Nullable Object errorDetails) {
        Log.d(LOG_TAG, "ERROR: " + errorCode + " " + errorMessage);
      }

      @Override
      public void notImplemented() {
        InAppWebViewClient.super.onFormResubmission(view, dontResend, resend);
      }
    });
  }

  @Override
  public void onPageCommitVisible(WebView view, String url) {
    super.onPageCommitVisible(view, url);
    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("url", url);
    channel.invokeMethod("onPageCommitVisible", obj);
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
    final InAppWebView webView = (InAppWebView) view;

    if (webView.options.useOnRenderProcessGone) {
      Boolean didCrash = detail.didCrash();
      Integer rendererPriorityAtExit = detail.rendererPriorityAtExit();

      Map<String, Object> obj = new HashMap<>();
      if (inAppBrowserActivity != null)
        obj.put("uuid", inAppBrowserActivity.uuid);
      obj.put("didCrash", didCrash);
      obj.put("rendererPriorityAtExit", rendererPriorityAtExit);

      channel.invokeMethod("onRenderProcessGone", obj);

      return true;
    }

    return super.onRenderProcessGone(view, detail);
  }

  @Override
  public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
    Map<String, Object> obj = new HashMap<>();
    if (inAppBrowserActivity != null)
      obj.put("uuid", inAppBrowserActivity.uuid);
    obj.put("realm", realm);
    obj.put("account", account);
    obj.put("args", args);

    channel.invokeMethod("onReceivedLoginRequest", obj);
  }

  @Override
  public void onUnhandledKeyEvent(WebView view, KeyEvent event) {

  }

  public void dispose() {
    channel.setMethodCallHandler(null);
    if (inAppBrowserActivity != null) {
      inAppBrowserActivity = null;
    }
    if (flutterWebView != null) {
      flutterWebView = null;
    }
  }
}
