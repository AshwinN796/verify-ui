package verifyui.gradle

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withGroovyBuilder
import verifyui.gradle.check.Checker
import java.io.File

/**
 * verify-ui: apply to an Android app/library module that uses Compose.
 *
 *   plugins { id("io.github.ashwinn796.verify-ui") }
 *
 *   ./gradlew :app:verifyUi -PverifyUi.composable=OrderSummaryCard
 *
 * Renders every @Preview of that composable (the default preview also at 2x font, RTL and dark),
 * then writes build/verify-ui/<Composable>/report.json.
 */
abstract class VerifyUiExtension {
    /** Build variant whose unit tests render the previews. */
    abstract val variant: Property<String>

    /** Packages scanned for @Preview functions. Defaults to android.namespace. */
    abstract val scanPackages: ListProperty<String>

    /** Android SDK Robolectric renders with. Must be one Robolectric supports. */
    abstract val robolectricSdk: Property<Int>

    abstract val robolectricVersion: Property<String>
    abstract val roborazziVersion: Property<String>
    abstract val previewScannerVersion: Property<String>
    abstract val androidxTestJunitVersion: Property<String>
}

class VerifyUiPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("verifyUi", VerifyUiExtension::class.java).apply {
            variant.convention("debug")
            robolectricSdk.convention(36)
            robolectricVersion.convention("4.16.1")
            roborazziVersion.convention("1.58.0")
            previewScannerVersion.convention("0.9.1")
            androidxTestJunitVersion.convention("1.3.0")
        }
        var configured = false
        for (id in listOf("com.android.application", "com.android.library")) {
            project.pluginManager.withPlugin(id) {
                if (!configured) {
                    configured = true
                    configureAndroid(project, ext)
                }
            }
        }
        project.afterEvaluate {
            if (!configured) {
                throw GradleException(
                    "verify-ui: apply io.github.ashwinn796.verify-ui to an Android application or library module " +
                        "(the module with com.android.application or com.android.library), not to ${project.path}.",
                )
            }
        }
    }

    private fun configureAndroid(project: Project, ext: VerifyUiExtension) {
        val android = project.extensions.getByName("android")

        // 1. Test dependencies. Compose test artifacts take their version from the module's Compose BOM.
        project.dependencies.apply {
            addProvider("testImplementation", ext.robolectricVersion.map { "org.robolectric:robolectric:$it" })
            addProvider("testImplementation", ext.roborazziVersion.map { "io.github.takahirom.roborazzi:roborazzi:$it" })
            addProvider("testImplementation", ext.roborazziVersion.map { "io.github.takahirom.roborazzi:roborazzi-compose:$it" })
            addProvider(
                "testImplementation",
                ext.previewScannerVersion.map { "io.github.sergio-sastre.ComposablePreviewScanner:android:$it" },
            )
            addProvider("testImplementation", ext.androidxTestJunitVersion.map { "androidx.test.ext:junit:$it" })
            add("testImplementation", "androidx.compose.ui:ui-test-junit4")
            add("debugImplementation", "androidx.compose.ui:ui-test-manifest")
        }

        // 2. Robolectric needs merged Android resources in unit tests.
        val testOptions = android.withGroovyBuilder { getProperty("testOptions") }!!
        val unitTests = testOptions.withGroovyBuilder { getProperty("unitTests") }!!
        unitTests.withGroovyBuilder { setProperty("includeAndroidResources", true) }

        // 3. Generated test sources: the renderer + one parameterized test over all previews.
        val genDir = project.layout.buildDirectory.dir("generated/verifyui/test")
        val generate = project.tasks.register("verifyUiGenerateRuntime", GenerateRuntimeTask::class.java) {
            outputDir.set(genDir)
            sdk.set(ext.robolectricSdk)
        }
        // AGP 9 (built-in Kotlin) ignores legacy sourceSets.test.java.srcDir for generated code,
        // so register the directory on every variant's unit-test component via the variant API.
        val genPath = genDir.get().asFile.apply { mkdirs() }.absolutePath
        val components = project.extensions.getByName("androidComponents")
        val allVariants = components.withGroovyBuilder { "selector"() }!!.withGroovyBuilder { "all"() }!!
        components.withGroovyBuilder {
            "onVariants"(allVariants, Action<Any> { addUnitTestSources(this, genPath) })
        }
        project.tasks.configureEach {
            if (name != generate.name && name.contains("UnitTest")) dependsOn(generate)
        }

        // 4. Test task: our generated test only runs when verifyUi is requested; otherwise it is excluded.
        val requested = project.gradle.startParameter.taskNames.any {
            it == "verifyUi" || it.endsWith(":verifyUi")
        }
        val composableProp = project.providers.gradleProperty("verifyUi.composable").orElse("")
        val outRoot = project.layout.buildDirectory.dir("verify-ui")
        val testTaskName = ext.variant.map { "test${it.replaceFirstChar(Char::uppercase)}UnitTest" }

        project.tasks.withType(Test::class.java).configureEach {
            if (requested && name == testTaskName.get()) {
                val namespace = android.withGroovyBuilder { getProperty("namespace") } as String?
                val packages = ext.scanPackages.getOrElse(emptyList()).ifEmpty { listOfNotNull(namespace) }
                if (packages.isEmpty()) {
                    throw GradleException("verify-ui: set android.namespace or verifyUi.scanPackages.")
                }
                val target = composableProp.get()
                val cleanDir = outRoot.get().asFile.let { if (target.isEmpty()) it else File(it, target) }.absolutePath

                filter.includeTestsMatching("verifyui.runtime.VerifyUiTest")
                systemProperty("verifyui.composable", target)
                systemProperty("verifyui.packages", packages.joinToString(","))
                systemProperty("verifyui.outputDir", outRoot.get().asFile.absolutePath)
                systemProperty("roborazzi.test.record", "true")
                outputs.upToDateWhen { false }
                doFirst { File(cleanDir).deleteRecursively() }
            } else {
                filter.excludeTestsMatching("verifyui.runtime.*")
            }
        }

        // 5. verifyUi: render, then check and report.
        project.tasks.register("verifyUi", VerifyUiReportTask::class.java) {
            group = "verification"
            description = "Renders @Previews of -PverifyUi.composable=<Name> in all variants and writes report.json."
            dependsOn(testTaskName)
            composable.set(composableProp)
            outputRoot.set(outRoot)
            outputs.upToDateWhen { false }
        }
    }
}

private fun addUnitTestSources(variant: Any, dir: String) {
    // AGP 8.x/9.x: variant.unitTest; newer AGP: variant.hostTests["UnitTest"].
    val unitTest = runCatching { variant.withGroovyBuilder { getProperty("unitTest") } }.getOrNull()
        ?: runCatching {
            (variant.withGroovyBuilder { getProperty("hostTests") } as Map<*, *>)
                .entries.firstOrNull { it.key.toString().contains("Unit", ignoreCase = true) }?.value
        }.getOrNull()
        ?: return // unit tests disabled for this variant
    val sources = unitTest.withGroovyBuilder { getProperty("sources") }!!
    val flat = runCatching { sources.withGroovyBuilder { getProperty("kotlin") } }.getOrNull()
        ?: sources.withGroovyBuilder { getProperty("java") }
        ?: throw GradleException("verify-ui: cannot find unit-test source directories on ${variant}")
    flat.withGroovyBuilder { "addStaticSourceDirectory"(dir) }
}

/** Copies the runtime sources bundled in the plugin jar into the module's test source set. */
abstract class GenerateRuntimeTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val sdk: Property<Int>

    @get:Input
    val runtimeVersion: String = RUNTIME_VERSION

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile.resolve("verifyui/runtime")
        dir.deleteRecursively()
        dir.mkdirs()
        for (name in RUNTIME_FILES) {
            val text = javaClass.getResourceAsStream("/verifyui-runtime/$name.txt")
                ?.bufferedReader()?.use { it.readText() }
                ?: throw GradleException("verify-ui: missing bundled runtime file $name")
            File(dir, name).writeText(text.replace("__ROBOLECTRIC_SDK__", sdk.get().toString()))
        }
    }

    companion object {
        const val RUNTIME_VERSION = "0.1.0"
        val RUNTIME_FILES = listOf("VerifyUiTest.kt", "Renderer.kt", "SemanticsDump.kt")
    }
}

/** Runs the checker over the renders and prints a summary the agent can act on. */
abstract class VerifyUiReportTask : DefaultTask() {
    @get:Input
    abstract val composable: Property<String>

    @get:Internal
    abstract val outputRoot: DirectoryProperty

    @TaskAction
    fun report() {
        val root = outputRoot.get().asFile
        val target = composable.get()
        val noPreviews = File(root, "_no_previews.txt")
        if (noPreviews.exists()) {
            noPreviews.delete()
            logger.lifecycle(
                "\nverify-ui: no @Preview found for '${target.ifEmpty { "*" }}'.\n" +
                    "Add @Preview functions named ${target}Preview, ${target}LongTextPreview, ... " +
                    "in the composable's file or anywhere under the scanned packages.",
            )
            return
        }
        val dirs = if (target.isNotEmpty()) listOf(File(root, target))
        else root.listFiles { f -> f.isDirectory }.orEmpty().sortedBy { it.name }

        val results = dirs.filter { it.isDirectory }.map { Checker.checkComponent(it) }
        if (results.isEmpty()) {
            logger.lifecycle("\nverify-ui: nothing was rendered under $root")
            return
        }
        val out = StringBuilder("\n")
        for (r in results) {
            out.append(Checker.summary(r))
            out.append("  report: ${File(File(root, r.component), "report.json").absolutePath}\n\n")
        }
        logger.lifecycle(out.toString())
    }
}
