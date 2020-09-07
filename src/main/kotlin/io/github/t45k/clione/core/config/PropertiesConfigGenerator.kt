package io.github.t45k.clione.core.config

import java.util.ResourceBundle

class PropertiesConfigGenerator(fileName: String) : ConfigGenerator() {
    private val properties: ResourceBundle = ResourceBundle.getBundle(fileName)

    override fun getSrcString(): String? = properties.getString("SRC")
    override fun getLangString(): String? = properties.getString("LANG")
    override fun getCloneDetectorString(): String? = properties.getString("CLONE_DETECTOR")
    override fun getGranularityString(): String? = properties.getString("GRANULARITY")
    override fun getSimilarityString(): String? = properties.getString("SIMILARITY")
    override fun getStyleString(): String? = properties.getString("STYLE")
}
