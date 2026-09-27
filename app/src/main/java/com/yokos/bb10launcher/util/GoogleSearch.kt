package com.yokos.bb10launcher.util

import java.net.URLEncoder

/** Google web search. Pure, so the URL building is unit-tested on the JVM. */
object GoogleSearch {
    const val APP_PACKAGE = "com.google.android.googlequicksearchbox"

    fun url(query: String): String =
        "https://www.google.com/search?q=" + URLEncoder.encode(query.trim(), "UTF-8")
}
