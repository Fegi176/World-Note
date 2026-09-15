plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(17) }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.commonmark:commonmark:0.24.0")
    testImplementation("junit:junit:4.13.2")
}
tasks.register<JavaExec>("makeTestWorld") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.nodenote.core.Fixtures")
    args(providers.gradleProperty("fixtureScale").getOrElse("typical"), providers.gradleProperty("fixtureOutput").getOrElse("../.tooling/fixture.json"))
}
