package verifyui.gradle.check

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Rule-based checks over the renders produced by the generated VerifyUiTest.
 *
 * Input:  <componentDir>/<file>.semantics.json (+ <file>.png, or <file>.error.txt on render failure)
 *         where <file> is "<state>" or "<state>__<variant>" (variant: font_2x, rtl, dark).
 * Output: <componentDir>/report.json
 *
 * Pure Kotlin + JDK so it runs inside the Gradle daemon with no extra dependencies.
 */
object Checker {

    const val MIN_TOUCH_DP = 48.0
    private const val CLIP_TOLERANCE_DP = 1.0
    private const val RTL_TOLERANCE_DP = 8.0
    private const val CONTRAST_NORMAL = 4.5
    private const val CONTRAST_LARGE = 3.0

    val FIX_HINTS = mapOf(
        "render_failed" to "The preview threw while rendering. Read the error; previews must not need a ViewModel, " +
            "Hilt or navigation. Pass plain data to a stateless composable instead.",
        "touch_target" to "Make the touch target at least 48x48dp: Modifier.minimumInteractiveComponentSize(), " +
            "use IconButton/Button, or move .clickable outside the padding so the hit area grows.",
        "missing_label" to "Give the control an accessible label: contentDescription on the Icon/Image, or " +
            "Modifier.semantics { contentDescription = \"...\" } on the clickable. Say what it does, e.g. \"Remove Masala Dosa\".",
        "collapsed" to "The element was squeezed to zero size. A sibling without a weight is taking all the space: " +
            "put Modifier.weight(1f) on the flexible text instead of on a Spacer.",
        "text_clipped" to "Text needs more space than it was given. Replace fixed height()/width() on the text or its " +
            "parent with heightIn(min = ...) / widthIn(...) so it can grow with font scale.",
        "text_truncated" to "Text is ellipsized. Allow more lines (maxLines) or confirm truncation is intended.",
        "toggle_semantics" to "The row toggles a switch/checkbox but exposes no role or state. Replace .clickable with " +
            "Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = ...) and pass onCheckedChange = null to the Switch.",
        "rtl_not_mirrored" to "Layout does not mirror in RTL. Replace absolutePadding / Arrangement.Absolute / " +
            "Alignment.Absolute / left/right offsets with start/end-based equivalents.",
        "low_contrast" to "Text contrast is below WCAG AA. Use a MaterialTheme.colorScheme role (onSurface, " +
            "onSurfaceVariant, onPrimary...) instead of a hard-coded Color. If the text has no explicit color, " +
            "check the preview wraps the content in Surface { } like the app's Scaffold does.",
    )

    // ------------------------------------------------------------------ model

    class TextLayout(val lines: Int, val ellipsized: Boolean, val needed: DoubleArray?, val given: DoubleArray?)

    class Node(
        val id: Int,
        val parent: Int?,
        val bounds: DoubleArray,
        val unclipped: DoubleArray,
        /** Outer size of the layout node, including e.g. minimumInteractiveComponentSize. */
        val layout: DoubleArray?,
        val text: String?,
        val contentDescription: String?,
        val role: String?,
        val toggleState: String?,
        val clickable: Boolean,
        val textLayout: TextLayout?,
        val scrollable: Boolean = false,
    ) {
        val ownLabel: String? get() = text ?: contentDescription
    }

    class Dump(
        val file: String,
        val fontScale: Double,
        val density: Double,
        val nodes: List<Node>,
        val png: File,
    ) {
        val base: String get() = file.substringBefore("__")
        val variant: String get() = file.substringAfter("__", "")
        val root: Node get() = nodes.first()
        private val children: Map<Int, List<Node>> = nodes.filter { it.parent != null }.groupBy { it.parent!! }

        fun descendants(n: Node): List<Node> {
            val out = mutableListOf<Node>()
            val stack = ArrayDeque(children[n.id].orEmpty())
            while (stack.isNotEmpty()) {
                val c = stack.removeLast()
                out += c
                stack.addAll(children[c.id].orEmpty())
            }
            return out
        }

        private val byId = nodes.associateBy { it.id }

        /** True if the node sits inside a scroll container (LazyColumn, verticalScroll, ...). */
        fun inScrollContainer(n: Node): Boolean {
            var p = n.parent?.let { byId[it] }
            while (p != null) {
                if (p.scrollable) return true
                p = p.parent?.let { byId[it] }
            }
            return false
        }

        fun labelOf(n: Node): String? = n.ownLabel ?: descendants(n).firstNotNullOfOrNull { it.ownLabel }
    }

    class Finding(
        val rule: String,
        val severity: String,
        val state: String,
        val label: String?,
        val where: String,
        val boundsDp: DoubleArray?,
        val message: String,
    ) {
        /** Identity of the element across states: positions move with RTL and font scale, labels/anchors do not. */
        val key: String
            get() = label ?: Regex("of '(.+)'$").find(where)?.groupValues?.get(1)?.let { "unlabeled near $it" } ?: where
    }

    // ------------------------------------------------------------------ geometry

    private fun w(b: DoubleArray) = b[2] - b[0]
    private fun h(b: DoubleArray) = b[3] - b[1]
    private fun cx(b: DoubleArray) = (b[0] + b[2]) / 2
    private fun cy(b: DoubleArray) = (b[1] + b[3]) / 2
    private fun fmt(d: Double) = "%.0f".format(d)

    private fun describe(d: Dump, n: Node): String {
        val b = n.unclipped
        val size = "${fmt(w(b))}x${fmt(h(b))}dp"
        d.labelOf(n)?.let { lbl ->
            val kind = if (n.clickable) "clickable" else "text"
            return "$kind '$lbl' ($size)"
        }
        val rowTexts = d.nodes.filter { it.text != null && abs(cy(it.unclipped) - cy(b)) < 16 }
        val anchor = rowTexts.minByOrNull { abs(cx(it.unclipped) - cx(b)) }?.let {
            val side = if (cx(b) > cx(it.unclipped)) "right of" else "left of"
            ", $side '${it.text}'"
        } ?: ""
        return "unlabeled clickable ($size) at x=${fmt(b[0])},y=${fmt(b[1])}dp$anchor"
    }

    /**
     * Material components reach 48dp through minimumInteractiveComponentSize, which grows the layout
     * node symmetrically around a smaller semantics node. Accept that expansion (<= 4dp per side),
     * but not arbitrary padding around a small clickable.
     */
    private fun touchSize(n: Node): Pair<Double, Double> {
        val sw = w(n.unclipped)
        val sh = h(n.unclipped)
        val l = n.layout ?: return sw to sh
        fun grow(semantic: Double, outer: Double) = if (outer >= semantic && outer - semantic <= 8.5) outer else semantic
        return grow(sw, l[0]) to grow(sh, l[1])
    }

    private fun finding(rule: String, severity: String, d: Dump, n: Node, message: String) =
        Finding(rule, severity, d.file, d.labelOf(n), describe(d, n), n.bounds, message)

    // ------------------------------------------------------------------ per-state rules

    private fun isRoot(d: Dump, n: Node): Boolean =
        n.bounds.contentEquals(d.root.bounds) && (n.parent == null || n.parent == d.root.id)

    fun checkDump(d: Dump): List<Finding> {
        val out = mutableListOf<Finding>()
        for (n in d.nodes) {
            if (isRoot(d, n)) continue
            val b = n.bounds
            val ub = n.unclipped
            val visible = w(b) > 0.5 && h(b) > 0.5

            if ((n.clickable || n.text != null) && (w(ub) < 1 || h(ub) < 1)) {
                out += finding("collapsed", "error", d, n,
                    "${describe(d, n)} was measured at zero size; it is squeezed out of the layout.")
                continue
            }
            if ((n.clickable || n.text != null) && !visible) {
                // Laid out at a real size but not visible: scrolled out of view is fine, clipped away is not.
                if (!d.inScrollContainer(n)) {
                    out += finding("text_clipped", "error", d, n,
                        "${describe(d, n)} is fully hidden by a parent that clips it.")
                }
                continue
            }

            if (n.clickable) {
                val (tw, th) = touchSize(n)
                if (tw < MIN_TOUCH_DP - 0.5 || th < MIN_TOUCH_DP - 0.5) {
                    out += finding("touch_target", "error", d, n,
                        "${describe(d, n)} is too small to tap: ${fmt(tw)}x${fmt(th)}dp touch target, minimum is ${fmt(MIN_TOUCH_DP)}x${fmt(MIN_TOUCH_DP)}dp.")
                }
                if (d.labelOf(n) == null) {
                    out += finding("missing_label", "error", d, n,
                        "${describe(d, n)} has no text or contentDescription; TalkBack cannot announce it.")
                }
                if (n.role == null && d.descendants(n).any { it.toggleState != null }) {
                    out += finding("toggle_semantics", "error", d, n,
                        "${describe(d, n)} toggles a child switch/checkbox but has no Role or toggle state.")
                }
            }

            val tl = n.textLayout
            if (tl?.needed != null && tl.given != null) {
                val (nw, nh) = tl.needed[0] to tl.needed[1]
                val (gw, gh) = tl.given[0] to tl.given[1]
                if (nh > gh + CLIP_TOLERANCE_DP || nw > gw + CLIP_TOLERANCE_DP) {
                    out += finding("text_clipped", "error", d, n,
                        "Text '${n.text}' needs ${fmt(nw)}x${fmt(nh)}dp but got ${fmt(gw)}x${fmt(gh)}dp.")
                }
            }
            if (tl?.ellipsized == true) {
                out += finding("text_truncated", "warning", d, n,
                    "Text '${n.text}' is ellipsized to ${tl.lines} line(s).")
            }
            if (n.text != null && visible &&
                (h(ub) - h(b) > CLIP_TOLERANCE_DP || w(ub) - w(b) > CLIP_TOLERANCE_DP)
            ) {
                out += finding("text_clipped", "error", d, n,
                    "Text '${n.text}' is clipped by its parent (${fmt(w(b))}x${fmt(h(b))}dp visible of ${fmt(w(ub))}x${fmt(h(ub))}dp).")
            }
        }
        out += checkContrast(d)
        return out
    }

    // ------------------------------------------------------------------ contrast

    private fun luminance(rgb: Int): Double {
        fun ch(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(rgb shr 16 and 0xFF) + 0.7152 * ch(rgb shr 8 and 0xFF) + 0.0722 * ch(rgb and 0xFF)
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun hex(rgb: Int) = "#%06X".format(rgb and 0xFFFFFF)

    private fun checkContrast(d: Dump): List<Finding> {
        if (!d.png.exists()) return emptyList()
        val img: BufferedImage = ImageIO.read(d.png) ?: return emptyList()
        val out = mutableListOf<Finding>()
        for (n in d.nodes) {
            if (n.text == null) continue
            val b = n.bounds
            val x0 = max(0, (b[0] * d.density).toInt())
            val y0 = max(0, (b[1] * d.density).toInt())
            val x1 = min(img.width, (b[2] * d.density).toInt())
            val y1 = min(img.height, (b[3] * d.density).toInt())
            if (x1 - x0 < 3 || y1 - y0 < 3) continue
            val counts = HashMap<Int, Int>()
            for (y in y0 until y1) for (x in x0 until x1) {
                val p = img.getRGB(x, y) and 0xFFFFFF
                counts[p] = (counts[p] ?: 0) + 1
            }
            val bg = counts.maxByOrNull { it.value }!!.key
            val fg = counts.keys.maxByOrNull { contrast(it, bg) }!!
            val ratio = contrast(fg, bg)
            val tl = n.textLayout
            val lineHeightDp = (tl?.needed?.get(1) ?: h(b)) / max(1, tl?.lines ?: 1)
            val large = lineHeightDp / d.fontScale >= 26.0 // >= ~18sp text (28sp line height)
            val threshold = if (large) CONTRAST_LARGE else CONTRAST_NORMAL
            if (ratio < threshold) {
                out += finding("low_contrast", "error", d, n,
                    "Text '${n.text}' contrast is ${"%.2f".format(ratio)}:1 (needs $threshold:1); ${hex(fg)} on ${hex(bg)}.")
            }
        }
        return out
    }

    // ------------------------------------------------------------------ cross-state rules

    /** Paired by traversal order: the semantics tree has the same shape in LTR and RTL. */
    fun checkRtl(ltr: Dump, rtl: Dump): List<Finding> {
        if (ltr.nodes.size != rtl.nodes.size) return emptyList()
        val rootW = w(ltr.root.bounds)
        val stuck = mutableListOf<Triple<Node, Node, Double>>()
        for ((a, b) in ltr.nodes.zip(rtl.nodes)) {
            if (a.text == null && !a.clickable) continue
            val ab = a.unclipped
            val bb = b.unclipped
            if (w(ab) > rootW * 0.6 || abs(cx(ab) - rootW / 2) < RTL_TOLERANCE_DP) continue
            val expected = rootW - cx(ab)
            if (abs(cx(bb) - expected) > RTL_TOLERANCE_DP && abs(cx(bb) - cx(ab)) < RTL_TOLERANCE_DP) {
                stuck += Triple(a, b, expected)
            }
        }
        if (stuck.isEmpty()) return emptyList()
        val (a, b, expected) = stuck.first()
        val first = ltr.labelOf(a)
        val others = stuck.drop(1).map { ltr.labelOf(it.first) ?: "unlabeled" }.distinct().filter { it != first }
            .take(3).joinToString(", ") { "'$it'" }
        val also = if (others.isEmpty()) "" else " Also not mirrored: $others."
        return listOf(
            Finding("rtl_not_mirrored", "error", rtl.file, ltr.labelOf(a), describe(ltr, a), b.bounds,
                "${describe(ltr, a)} stays at x=${fmt(cx(b.unclipped))}dp in RTL; expected about x=${fmt(expected)}dp.$also"),
        )
    }

    // ------------------------------------------------------------------ component report

    class Result(val component: String, val status: String, val errors: Int, val warnings: Int, val findings: List<Map<String, Any?>>)

    fun checkComponent(dir: File): Result {
        val component = dir.name
        val dumps = dir.listFiles { f -> f.name.endsWith(".semantics.json") }.orEmpty()
            .sortedBy { it.name }
            .map { parseDump(it) }
        val renderErrors = dir.listFiles { f -> f.name.endsWith(".error.txt") }.orEmpty().sortedBy { it.name }

        val raw = mutableListOf<Finding>()
        for (e in renderErrors) {
            val state = e.name.removeSuffix(".error.txt")
            val firstLines = e.readText().lineSequence().take(6).joinToString("\n")
            raw += Finding("render_failed", "error", state, null, "preview '$state'", null, "Preview failed to render:\n$firstLines")
        }
        dumps.forEach { raw += checkDump(it) }
        for (d in dumps.filter { it.variant.isEmpty() }) {
            dumps.firstOrNull { it.base == d.base && it.variant == "rtl" }?.let { raw += checkRtl(d, it) }
        }

        // Same rule on the same element across states -> one finding listing the states.
        val grouped = LinkedHashMap<Pair<String, String>, MutableMap<String, Any?>>()
        for (f in raw) {
            val k = f.rule to f.key
            val g = grouped.getOrPut(k) {
                linkedMapOf(
                    "rule" to f.rule,
                    "severity" to f.severity,
                    "states" to mutableListOf<String>(),
                    "element" to f.where,
                    "label" to f.label,
                    "bounds_dp" to f.boundsDp?.toList(),
                    "message" to f.message,
                    "fix_hint" to FIX_HINTS[f.rule],
                )
            }
            @Suppress("UNCHECKED_CAST")
            val states = g["states"] as MutableList<String>
            if (f.state !in states) states += f.state
        }
        val findings = grouped.values.sortedWith(compareBy({ if (it["severity"] == "error") 0 else 1 }, { it["rule"] as String }))
        val errors = findings.count { it["severity"] == "error" }
        val warnings = findings.size - errors
        val status = if (errors > 0) "fail" else "pass"

        val states = (dumps.map { it.file } + renderErrors.map { it.name.removeSuffix(".error.txt") }).sorted()
        val report = linkedMapOf<String, Any?>(
            "component" to component,
            "status" to status,
            "summary" to linkedMapOf("errors" to errors, "warnings" to warnings, "states" to states),
            "images" to dumps.associate { it.file to it.png.absolutePath },
            "findings" to findings,
        )
        File(dir, "report.json").writeText(Json.write(report) + "\n")
        return Result(component, status, errors, warnings, findings)
    }

    fun summary(r: Result): String = buildString {
        append("${r.component}: ${r.status.uppercase()} (${r.errors} errors, ${r.warnings} warnings)\n")
        for (f in r.findings) {
            @Suppress("UNCHECKED_CAST")
            val states = (f["states"] as List<String>).joinToString(", ")
            append("  [${f["severity"]}] ${(f["rule"] as String).padEnd(17)} ${states.padEnd(30)} ${f["message"]}\n")
        }
    }

    // ------------------------------------------------------------------ parsing

    @Suppress("UNCHECKED_CAST")
    fun parseDump(file: File): Dump {
        val json = Json.parse(file.readText()) as Map<String, Any?>
        val meta = json["meta"] as Map<String, Any?>
        val nodes = (json["nodes"] as List<Map<String, Any?>>).map { n ->
            fun arr(key: String) = (n[key] as List<Any?>?)?.map { (it as Number).toDouble() }?.toDoubleArray()
            val tl = (n["textLayout"] as Map<String, Any?>?)?.let { t ->
                TextLayout(
                    lines = (t["lines"] as Number).toInt(),
                    ellipsized = t["ellipsized"] == true,
                    needed = (t["needed"] as List<Any?>?)?.map { (it as Number).toDouble() }?.toDoubleArray(),
                    given = (t["given"] as List<Any?>?)?.map { (it as Number).toDouble() }?.toDoubleArray(),
                )
            }
            Node(
                id = (n["id"] as Number).toInt(),
                parent = (n["parent"] as Number?)?.toInt(),
                bounds = arr("bounds")!!,
                unclipped = arr("unclipped")!!,
                layout = arr("layout"),
                text = n["text"] as String?,
                contentDescription = n["contentDescription"] as String?,
                role = n["role"] as String?,
                toggleState = n["toggleState"] as String?,
                clickable = n["clickable"] == true,
                textLayout = tl,
                scrollable = n["scrollable"] == true,
            )
        }
        val name = file.name.removeSuffix(".semantics.json")
        return Dump(
            file = name,
            fontScale = (meta["fontScale"] as Number?)?.toDouble() ?: 1.0,
            density = (meta["density"] as Number).toDouble(),
            nodes = nodes,
            png = File(file.parentFile, "$name.png"),
        )
    }
}
