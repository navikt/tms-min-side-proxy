package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


internal class EnvAppVariablesTest {

    @Test
    fun variablesForTest() {
        EnvAppVariables("dev-gcp", "appnavn").assert {
            this.clientId shouldBe "dev-gcp:min-side:appnavn"
            this.baseUrl shouldBe "http://appnavn"
        }

        EnvAppVariables(
            clustername = "dev-gcp",
            application = "meldekort-api-q2",
            namespace = "meldekort",
            baseUrlPostfix = "/meldekort/meldekort-api"
        ).assert {
            this.clientId shouldBe "dev-gcp:meldekort:meldekort-api-q2"
            this.baseUrl shouldBe "http://meldekort-api-q2.meldekort/meldekort/meldekort-api"
        }

        EnvAppVariables("prod-gcp", application = "annet-namespace-app", namespace = "test").assert {
            this.clientId shouldBe "prod-gcp:test:annet-namespace-app"
            this.baseUrl shouldBe "http://annet-namespace-app.test"
        }
    }

}