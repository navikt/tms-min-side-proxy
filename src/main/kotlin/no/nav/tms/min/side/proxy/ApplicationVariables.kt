package no.nav.tms.min.side.proxy

interface ApplicationVariables {
    val clientId: String
    val baseUrl: String
}

class EnvAppVariables(
    clustername: String,
    application: String,
    namespace: String? = null,
    baseUrlPostfix: String? = null,
): ApplicationVariables {
    override val clientId = "$clustername:${namespace ?: "min-side"}:$application"
    override val baseUrl = "http://$application${namespace?.let { ".$it" } ?: ""}${baseUrlPostfix ?: ""}"
}