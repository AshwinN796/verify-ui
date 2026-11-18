import org.gradle.plugin.compatibility.compatibility

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.2.1"
}

group = "io.github.ashwinn796"
version = "0.2.0"

gradlePlugin {
    website = "https://github.com/AshwinN796/verify-ui/tree/master/verify-ui-gradle-plugin#readme"
    vcsUrl = "https://github.com/AshwinN796/verify-ui.git"
    plugins {
        register("verifyUi") {
            id = "io.github.ashwinn796.verify-ui"
            implementationClass = "verifyui.gradle.VerifyUiPlugin"
            displayName = "verify-ui"
            description = "Renders Jetpack Compose @Previews in every state (2x font, RTL, dark) and checks them " +
                "for layout and accessibility bugs, so AI agents can see the UI they write."
            tags = listOf("android", "jetpack-compose", "screenshot-testing", "accessibility", "ai-agents")
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
