package no.nav.tms.min.side.proxy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


internal class AppResolverTest {

    @Test
    fun variablesForTest() {
        AppResolver("dev-gcp").apply {
            variablesFor("appnavn").assert {
                this.clientId shouldBe "dev-gcp:min-side:appnavn"
                this.baseUrl shouldBe "http://appnavn"
            }
            variablesFor("meldekort-api-q2", namespace = "meldekort", baseUrlPostfix="/meldekort/meldekort-api").assert {
                this.clientId shouldBe "dev-gcp:meldekort:meldekort-api-q2"
                this.baseUrl shouldBe "http://meldekort-api-q2.meldekort/meldekort/meldekort-api"
            }
        }

        AppResolver("prod-gcp")
            .apply {
                variablesFor(application = "annet-namespace-app", namespace = "test").assert {
                    this.clientId shouldBe "prod-gcp:test:annet-namespace-app"
                    this.baseUrl shouldBe "http://annet-namespace-app.test"
                }
                variablesFor(application = "nn", namespace = "jjj",clusterName = "dev-gcp").assert {
                    this.clientId shouldBe "dev-gcp:jjj:nn"
                    this.baseUrl shouldBe "http://nn.jjj"
                }
            }

        shouldThrow<IllegalArgumentException> {
            AppResolver("ukjent miljø")
        }

        shouldThrow<IllegalArgumentException> {
            AppResolver("prod-fss").variablesFor(application = "app", clusterName = "ikke-kjent")
        }

    }
}