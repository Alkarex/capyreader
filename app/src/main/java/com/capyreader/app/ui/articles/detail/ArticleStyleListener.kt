package com.capyreader.app.ui.articles.detail

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.capyreader.app.common.AppPreferences
import com.capyreader.app.ui.collectChanges
import com.jocmp.capy.articles.FontOption
import com.jocmp.capy.articles.TextSize
import org.koin.compose.koinInject

@Composable
fun ArticleStyleListener(webView: WebView?, appPreferences: AppPreferences = koinInject()) {
    val textSize by appPreferences.readerOptions.textSize.collectChanges()
    val fontFamily by appPreferences.readerOptions.fontFamily.collectChanges()

    LaunchedEffect(fontFamily) {
        if (webView != null) {
            updateFontFamily(webView, fontFamily)
        }
    }

    LaunchedEffect(textSize) {
        if (webView != null) {
            updateTextSize(webView, textSize)
        }
    }
}

private fun updateTextSize(webView: WebView, textSize: TextSize) {
    webView.evaluateJavascript(
        """
        (function() {         
          let slug = "${textSize.slug}";
          let articleBody = document.getElementsByClassName("article__body")[0];
          const classes = articleBody.className.split(" ").filter(c => !c.startsWith("article__body--text-size"));
          
          articleBody.className = classes.join(" ").trim() + " article__body--text-size-" + slug;
        })();
        """.trimIndent()
    ) {
    }
}
private fun updateFontFamily(webView: WebView, fontOption: FontOption) {
    webView.evaluateJavascript(
        """
        (function() {         
          let slug = "${fontOption.slug}";
          let articleBody = document.getElementsByClassName("article__body")[0];
          const classes = articleBody.className.split(" ").filter(c => !c.startsWith("article__body--font"));
          
          articleBody.className = classes.join(" ").trim() + " article__body--font-" + slug;
        })();
        """.trimIndent()
    ) {
    }
}
