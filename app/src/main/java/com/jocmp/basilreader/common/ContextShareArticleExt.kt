package com.jocmp.basilreader.common

import android.content.Context
import android.content.Intent
import com.jocmp.basil.Article

fun Context.shareArticle(article: Article) {
    val share = Intent.createChooser(Intent().apply {
        type = "text/plain"
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, article.url.toString())
        putExtra(Intent.EXTRA_TITLE, article.title)
    }, null)
    startActivity(share)
}
