package no.nav.tms.min.side.proxy

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


internal class AppResolverTest {

    @Test
    fun clientId() {
        AppResolver("dev-gcp").apply {
            variablesFor("appnavn").assert {
                this.clientId shouldBe "dev-gcp:min-side:appnavn"
                this.baseUrl shouldBe "http://appnavn"
            }
        }

        AppResolver("prod-gcp")
            .apply {
                variablesFor(application = "annet-namespace-app", namespace = "test").assert {
                    this.clientId shouldBe "prod-gcp:test:annet-namespace-app"
                    this.baseUrl shouldBe "http://annet-namespace-app.test"
                }
                variablesFor(application = "nn", namespace = "jjj",environment = "dev-fss").assert {
                    this.clientId shouldBe "dev-fss:jjj:nn"
                    this.baseUrl shouldBe "http://nn.jjj"
                }
            }

        shouldNotThrow<Exception> {
            AppResolver("dev-fss")
            AppResolver("prod-fss")

        }
        shouldThrow<IllegalArgumentException> {
            AppResolver("ukjent miljø")
        }

        shouldThrow<IllegalArgumentException> {
            AppResolver("prod-fss").variablesFor(application = "app", environment = "ikke-kjent")
        }

    }
}