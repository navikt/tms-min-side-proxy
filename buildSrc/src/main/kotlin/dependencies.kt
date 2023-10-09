import default.DependencyGroup
import default.KotlinDefaults

object Caffeine: DependencyGroup {
    override val groupId = "com.github.ben-manes.caffeine"
    override val version = "3.1.8"

    val caffeine = dependency("caffeine")
}

object KotlinTest: KotlinDefaults {
    val junit = dependency("kotlin-test-junit")
}
