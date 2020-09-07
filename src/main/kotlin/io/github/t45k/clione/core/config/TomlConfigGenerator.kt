package io.github.t45k.clione.core.config

import com.moandjiezana.toml.Toml

class TomlConfigGenerator(input: String) : ConfigGenerator() {
    private val toml: Toml = Toml().read(input)

    override fun getSrcString(): String? = toml.getString("src")
    override fun getLangString(): String? = toml.getString("lang")
    override fun getCloneDetectorString(): String? = toml.getString("clone_detector")
    override fun getGranularityString(): String? = toml.getString("granularity")
    override fun getSimilarityInteger(): Int? = toml.getLong("similarity")?.toInt()
    override fun getStyleString(): String? = toml.getString("style")
}
