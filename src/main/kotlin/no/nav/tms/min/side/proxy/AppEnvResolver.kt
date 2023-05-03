package no.nav.tms.min.side.proxy

import no.nav.tms.min.side.proxy.AppResolver.KnownCluster.Companion.clusterCheck

class AppResolver(private val clustername: String) {

    init {
        clustername.clusterCheck()
    }

    fun variablesFor(
        application: String,
        namespace: String? = null,
        clusterName: String? = null,
        baseUrlPostfix: String? = null
    ): Pair<String, String> {
        return Pair(
            "${clusterName?.clusterCheck() ?: this.clustername}:${namespace ?: "min-side"}:$application",
            "http://$application${namespace?.let { ".$it" } ?: ""}${baseUrlPostfix?:""}"
        )
    }

    private enum class KnownCluster(private val envString: String) {
        DEV_GCP("dev-gcp"), PROD_GCP("prod-gcp");

        companion object {
            fun String.clusterCheck(): String = this.also {
                if (KnownCluster.values().none { it.envString == this }) {
                    throw IllegalArgumentException("Appvariabler for $this må settes manuelt i yaml-fil")
                }
            }
        }
    }
}

val Pair<String, String>.clientId: String
    get() = first

val Pair<String, String>.baseUrl: String
    get() = second
