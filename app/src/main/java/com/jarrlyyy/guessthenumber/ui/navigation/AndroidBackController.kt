package com.jarrlyyy.guessthenumber.ui.navigation

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity

object AndroidBackController {
    @Volatile
    private var activity: ComponentActivity? = null

    private val INSTALL_SCRIPT = """
        (() => {
          if (window.__gtnAndroidBackInstalled) return;
          const stack = ['play'];
          const findTarget = id => document.querySelector('[data-screen-target="' + CSS.escape(id) + '"]');
          document.addEventListener('click', event => {
            const target = event.target && event.target.closest ? event.target.closest('[data-screen-target]') : null;
            if (!target) return;
            const id = target.getAttribute('data-screen-target');
            if (!id) return;
            setTimeout(() => {
              if (stack[stack.length - 1] !== id) stack.push(id);
            }, 0);
          }, true);
          window.gtnAndroidBack = () => {
            if (stack.length <= 1) return false;
            stack.pop();
            const target = findTarget(stack[stack.length - 1]);
            if (!target) return false;
            target.click();
            return true;
          };
          window.__gtnAndroidBackInstalled = true;
        })();
    """.trimIndent()

    fun install(host: ComponentActivity) {
        activity = host
        val root = host.window.decorView
        fun poll() {
            val webView = findWebView(root)
            if (webView != null && webView.progress >= 100) {
                webView.evaluateJavascript(INSTALL_SCRIPT, null)
            } else {
                root.postDelayed(::poll, 100L)
            }
        }
        poll()
    }

    fun handleBack(onUnhandled: () -> Unit) {
        val webView = activity?.let { findWebView(it.window.decorView) }
        if (webView == null || webView.visibility != View.VISIBLE) {
            onUnhandled()
            return
        }
        webView.evaluateJavascript("(window.gtnAndroidBack ? window.gtnAndroidBack() : false)") { result ->
            if (result != "true") onUnhandled()
        }
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view !is ViewGroup) return null
        for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
