plugins {
    `kotlin-dsl`
}

group = "io.github.ashwinn796"
version = "0.1.0"

gradlePlugin {
    website = "https://github.com/AshwinN796/verify-ui"
    vcsUrl = "https://github.com/AshwinN796/verify-ui.git"
    plugins {
        register("verifyUi") {
            id = "io.github.ashwinn796.verify-ui"
            implementationClass = "verifyui.gradle.VerifyUiPlugin"
            tags = listOf("android", "jetpack-compose", "screenshot-testing", "accessibility", "ai-agents")
            displayName = "verify-ui"
            description = "Renders Jetpack Compose @Previews in every state (2x font, RTL, dark) and checks them " +
                "for layout and accessibility bugs, so AI agents can see the UI they write."
        }
    }
}
