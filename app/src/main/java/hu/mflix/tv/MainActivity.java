package hu.mflix.tv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint({"SetJavaScriptEnabled", "SetWebViewClient"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        // JavaScript és webes tárhely
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Videólejátszás
        settings.setMediaPlaybackRequiresUserGesture(false);

        // TV / nagy képernyő
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Gyorsabb működés
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // Hardveres gyorsítás
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Cookie-k
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // WebView
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // TV fókuszkezelés elindítása
                installTvFocusNavigation();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // TV távirányítóhoz fókusz
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);

        // Ne legyen WebView-keret
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        // MFLIX
        webView.loadUrl("https://mflix.hu");
    }

    /**
     * TV-s fókuszkezelés.
     *
     * Csak a film részleteire mutató linkek lehetnek fókuszolhatók.
     * A cím, évszám és egyéb belső elemek nem kapnak külön fókuszt.
     */
    private void installTvFocusNavigation() {

        String script =
                "(function() {" +

                "  if (window.__mflixTvFocusInstalled) return;" +
                "  window.__mflixTvFocusInstalled = true;" +

                "  function getMovieCards() {" +
                "      return Array.from(document.querySelectorAll(" +
                "          'a[href*=\"#/details?id=\"]'" +
                "      )).filter(function(el) {" +
                "          var r = el.getBoundingClientRect();" +
                "          return r.width > 20 && r.height > 20;" +
                "      });" +
                "  }" +

                "  function updateFocusables() {" +

                "      var movieCards = getMovieCards();" +

                "      if (movieCards.length === 0) return;" +

                "      var allFocusable = document.querySelectorAll(" +
                "          'a, button, input, select, textarea, [tabindex]'" +
                "      );" +

                "      allFocusable.forEach(function(el) {" +
                "          el.setAttribute('tabindex', '-1');" +
                "      });" +

                "      movieCards.forEach(function(card) {" +
                "          card.setAttribute('tabindex', '0');" +

                "          var children = card.querySelectorAll(" +
                "              'a, button, input, select, textarea, [tabindex]'" +
                "          );" +

                "          children.forEach(function(child) {" +
                "              child.setAttribute('tabindex', '-1');" +
                "          });" +
                "      });" +
                "  }" +

                "  function getCurrentCard() {" +

                "      var active = document.activeElement;" +

                "      if (!active) return null;" +

                "      if (active.matches && active.matches('a[href*=\"#/details?id=\"]')) {" +
                "          return active;" +
                "      }" +

                "      var parent = active.closest && active.closest(" +
                "          'a[href*=\"#/details?id=\"]'" +
                "      );" +

                "      return parent || null;" +
                "  }" +

                "  function moveFocus(direction) {" +

                "      var cards = getMovieCards();" +
                "      if (!cards.length) return;" +

                "      var current = getCurrentCard();" +

                "      if (!current) {" +
                "          cards[0].focus();" +
                "          return;" +
                "      }" +

                "      var currentRect = current.getBoundingClientRect();" +
                "      var currentX = currentRect.left + currentRect.width / 2;" +
                "      var currentY = currentRect.top + currentRect.height / 2;" +

                "      var candidates = cards.filter(function(card) {" +

                "          if (card === current) return false;" +

                "          var r = card.getBoundingClientRect();" +
                "          var x = r.left + r.width / 2;" +
                "          var y = r.top + r.height / 2;" +

                "          if (direction === 'left') return x < currentX - 5;" +
                "          if (direction === 'right') return x > currentX + 5;" +
                "          if (direction === 'up') return y < currentY - 5;" +
                "          if (direction === 'down') return y > currentY + 5;" +

                "          return false;" +
                "      });" +

                "      if (!candidates.length) return;" +

                "      var best = null;" +
                "      var bestScore = Infinity;" +

                "      candidates.forEach(function(card) {" +

                "          var r = card.getBoundingClientRect();" +
                "          var x = r.left + r.width / 2;" +
                "          var y = r.top + r.height / 2;" +

                "          var dx = x - currentX;" +
                "          var dy = y - currentY;" +

                "          var primary;" +
                "          var secondary;" +

                "          if (direction === 'left' || direction === 'right') {" +
                "              primary = Math.abs(dx);" +
                "              secondary = Math.abs(dy);" +
                "          } else {" +
                "              primary = Math.abs(dy);" +
                "              secondary = Math.abs(dx);" +
                "          }" +

                "          var score = primary + secondary * 3;" +

                "          if (score < bestScore) {" +
                "              bestScore = score;" +
                "              best = card;" +
                "          }" +
                "      });" +

                "      if (best) {" +
                "          best.focus();" +
                "          best.scrollIntoView({" +
                "              behavior: 'smooth'," +
                "              block: 'nearest'," +
                "              inline: 'nearest'" +
                "          });" +
                "      }" +
                "  }" +

                "  document.addEventListener('keydown', function(event) {" +

                "      var key = event.key;" +

                "      if (" +
                "          key === 'ArrowLeft' ||" +
                "          key === 'ArrowRight' ||" +
                "          key === 'ArrowUp' ||" +
                "          key === 'ArrowDown'" +
                "      ) {" +

                "          var cards = getMovieCards();" +

                "          if (!cards.length) return;" +

                "          event.preventDefault();" +
                "          event.stopPropagation();" +

                "          if (key === 'ArrowLeft') moveFocus('left');" +
                "          if (key === 'ArrowRight') moveFocus('right');" +
                "          if (key === 'ArrowUp') moveFocus('up');" +
                "          if (key === 'ArrowDown') moveFocus('down');" +

                "          return false;" +
                "      }" +

                "  }, true);" +

                "  var observer = new MutationObserver(function() {" +
                "      updateFocusables();" +
                "  });" +

                "  observer.observe(document.body, {" +
                "      childList: true," +
                "      subtree: true" +
                "  });" +

                "  updateFocusables();" +

                "})();";

        webView.evaluateJavascript(script, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {

            int keyCode = event.getKeyCode();

            switch (keyCode) {

                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:

                    // A távirányító gombjait továbbadjuk
                    // a WebView / weboldal számára.
                    return super.dispatchKeyEvent(event);
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
