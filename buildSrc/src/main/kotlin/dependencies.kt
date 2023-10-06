import default.DependencyGroup
import default.KotlinDefaults

object Caffeine: DependencyGroup {
    override val groupId = "com.github.ben-manes.caffeine"
    override val version = "3.1.8"

    val caffeine = dependency("caffeine")
}

object GraphQL: DependencyGroup {
    override val groupId get() = "com.expediagroup"
    override val version = "6.3.5"
    val pluginId get() = "com.expediagroup.graphql"

    val kotlinClient get() = dependency("graphql-kotlin-client")

    val kotlinKtorClient get() = dependency("graphql-kotlin-ktor-client")
}

object KotlinTest: KotlinDefaults {
    val junit = dependency("kotlin-test-junit")
}
