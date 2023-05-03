package no.nav.tms.min.side.proxy

import no.nav.tms.min.side.proxy.AppResolver.KnownEnvironment.Companion.envCheck

class AppResolver(private val environment: String) {

    init {
        environment.envCheck()
    }

    fun variablesFor(
        application: String,
        namespace: String? = null,
        environment: String? = null
    ): Pair<String, String> {
        val knownEnv = environment?.envCheck() ?: this.environment
        return Pair(
            "$knownEnv:${namespace ?: "min-side"}:$application",
            "http://$application${namespace?.let { ".$it" } ?: ""}"
        )
    }

    private enum class KnownEnvironment(private val envString: String) {

        PROD_FSS("prod-fss"),
        DEV_FSS("dev-fss"),
        DEV_GCP("dev-gcp"),
        PROD_GCP("prod-gcp");

        companion object {
            fun String.envCheck(): String = this.also {
                if (KnownEnvironment.values().none { it.envString == this }) {
                    throw IllegalArgumentException("$this er ikke et kjent miljø")
                }
            }
        }
    }
}

val Pair<String, String>.clientId: String
    get() = first

val Pair<String, String>.baseUrl: String
    get() = second
