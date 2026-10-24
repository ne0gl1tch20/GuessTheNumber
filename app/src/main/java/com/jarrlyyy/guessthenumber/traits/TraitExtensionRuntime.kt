package com.jarrlyyy.guessthenumber.traits

import com.jarrlyyy.guessthenumber.scripting.LuaSandbox
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.luaj.vm2.LuaValue

@Serializable
data class TraitExtensionManifest(
    val schemaVersion: Int = 1,
    val traitId: String,
    val modules: List<TraitExtensionModule>
)

@Serializable
data class TraitExtensionModule(
    val id: String,
    val type: String,
    val entrypoint: String,
    val capabilities: Set<String> = emptySet()
)

data class ValidatedTraitExtension(
    val traitId: String,
    val modules: List<ValidatedTraitModule>
) {
    fun module(id: String): ValidatedTraitModule? = modules.firstOrNull { it.descriptor.id == id }
}

data class ValidatedTraitModule(
    val descriptor: TraitExtensionModule,
    val source: String
)

data class TraitModuleDecision(
    val eligible: Boolean,
    val requestedEffects: Set<TraitEffect>
)

/**
 * Validates extension modules before they can be offered to a host.
 * Lua is executed in the existing instruction-budgeted sandbox. JSON is declarative.
 * JavaScript is presentation-only and must be passed to the WebView's restricted extension host,
 * never evaluated in Kotlin or given the native game bridge.
 */
class TraitExtensionRuntime(
    private val json: Json = Json { ignoreUnknownKeys = false }
) {
    fun validate(manifestText: String, files: Map<String, String>, expectedTraitId: String): ValidatedTraitExtension {
        val manifest = json.decodeFromString<TraitExtensionManifest>(manifestText)
        require(manifest.schemaVersion == 1) { "Unsupported extension manifest schema." }
        require(manifest.traitId == expectedTraitId && Regex("[a-z][a-z0-9_.-]{1,63}").matches(manifest.traitId)) {
            "Extension trait ID mismatch."
        }
        require(manifest.modules.isNotEmpty() && manifest.modules.size <= MAX_MODULES) {
            "Extension module count is outside the supported limit."
        }
        require(manifest.modules.map { it.id }.distinct().size == manifest.modules.size) { "Duplicate module ID." }
        val modules = manifest.modules.map { module ->
            require(Regex("[a-z][a-z0-9_-]{0,47}").matches(module.id)) { "Invalid module ID." }
            require(module.entrypoint.length <= MAX_PATH_LENGTH &&
                !module.entrypoint.startsWith("/") &&
                module.entrypoint.split('/').all { it.isNotBlank() && it != "." && it != ".." }) {
                "Invalid module entrypoint."
            }
            val source = files[module.entrypoint] ?: error("Missing module entrypoint: ${module.entrypoint}")
            require(source.isNotBlank() && source.length <= MAX_SOURCE_CHARS) { "Module source is empty or too large." }
            val type = module.type.uppercase()
            val allowedCapabilities = when (type) {
                "LUA" -> setOf("log", "read_snapshot")
                "JSON" -> setOf("read_rules")
                "JAVASCRIPT" -> setOf("render_ui", "show_local_message")
                else -> error("Unsupported module type: ${module.type}")
            }
            require(module.capabilities.all { it in allowedCapabilities }) { "Module requests an unsupported capability." }
            when (type) {
                "LUA" -> {
                    require(module.entrypoint.endsWith(".lua", ignoreCase = true)) { "Lua modules must use .lua entrypoints." }
                    LuaSandbox {}.validate("${manifest.traitId}_${module.id}", source)
                }
                "JSON" -> {
                    require(module.entrypoint.endsWith(".json", ignoreCase = true)) { "JSON modules must use .json entrypoints." }
                    json.parseToJsonElement(source)
                }
                "JAVASCRIPT" -> {
                    require(module.entrypoint.endsWith(".js", ignoreCase = true)) { "JavaScript modules must use .js entrypoints." }
                    require(!FORBIDDEN_JS.containsMatchIn(source)) { "JavaScript module uses a forbidden host or dynamic-code API." }
                    require(source.contains("renderTrait") || source.contains("registerTraitView")) {
                        "JavaScript UI modules must expose renderTrait(api) or registerTraitView(api)."
                    }
                }
            }
            ValidatedTraitModule(module.copy(type = type), source)
        }
        require(modules.map { it.descriptor.entrypoint }.distinct().size == modules.size) {
            "Two modules cannot share an entrypoint."
        }
        return ValidatedTraitExtension(manifest.traitId, modules)
    }

    /** Executes a Lua logic module and accepts only a bounded, declarative result. */
    fun evaluateLua(
        extension: ValidatedTraitExtension,
        moduleId: String,
        snapshot: Map<String, String>,
        allowedEffects: Set<TraitEffect>
    ): TraitModuleDecision {
        val module = extension.module(moduleId) ?: error("Unknown extension module.")
        require(module.descriptor.type == "LUA") { "Requested module is not Lua." }
        require(snapshot.size <= MAX_INPUT_FIELDS) { "Trait snapshot has too many fields." }
        if (snapshot.isNotEmpty()) require("read_snapshot" in module.descriptor.capabilities) {
            "Lua module did not declare the read_snapshot capability."
        }
        val result = LuaSandbox {}.execute(
            "${extension.traitId}_${module.descriptor.id}",
            module.source,
            snapshot
        )
        val eligible = result.get("eligible").takeIf { !it.isnil() }?.toboolean() ?: false
        val effectTable = result.get("effects")
        val requested = linkedSetOf<TraitEffect>()
        if (!effectTable.isnil()) {
            require(effectTable.istable() && effectTable.length() <= TraitDefinition.MAX_EFFECTS) {
                "Lua module returned an invalid effect list."
            }
            for (index in 1..effectTable.length()) {
                val raw = effectTable.get(index).tojstring()
                val effect = runCatching { TraitEffect.valueOf(raw) }.getOrElse {
                    error("Lua module requested an unknown effect.")
                }
                require(effect in allowedEffects) { "Lua module requested an undeclared effect." }
                requested += effect
            }
        }
        return TraitModuleDecision(eligible, requested)
    }

    fun readJsonModule(extension: ValidatedTraitExtension, moduleId: String): JsonObject {
        val module = extension.module(moduleId) ?: error("Unknown extension module.")
        require(module.descriptor.type == "JSON") { "Requested module is not JSON." }
        return json.parseToJsonElement(module.source).jsonObject
    }

    /** JS is returned as data for the sandboxed WebView host, never eval'd by this class. */
    fun javascriptPresentation(extension: ValidatedTraitExtension, moduleId: String): String {
        val module = extension.module(moduleId) ?: error("Unknown extension module.")
        require(module.descriptor.type == "JAVASCRIPT") { "Requested module is not JavaScript." }
        return module.source
    }

    companion object {
        const val MAX_MODULES = 8
        const val MAX_SOURCE_CHARS = 32_768
        const val MAX_INPUT_FIELDS = 64
        const val MAX_PATH_LENGTH = 160
        private val FORBIDDEN_JS = Regex(
            "(?i)\\b(eval|Function|fetch|XMLHttpRequest|WebSocket|importScripts)\\b|\\bwindow\\s*\\.|\\bdocument\\s*\\.|\\bandroid\\s*\\.|\\bJava\\s*\\."
        )
    }
}
