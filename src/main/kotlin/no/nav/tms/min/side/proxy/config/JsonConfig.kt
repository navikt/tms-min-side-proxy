package no.nav.tms.min.side.proxy.config

import kotlinx.serialization.json.Json

fun jsonConfig(ignoreUnknownKeys: Boolean = false): Json {
    return Json {
        this.ignoreUnknownKeys = ignoreUnknownKeys
        this.encodeDefaults = true
    }
}
