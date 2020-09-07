package io.github.t45k.clione.core.config

import java.util.MissingResourceException
import java.util.ResourceBundle

class PropertiesConfigGenerator(fileName: String) : ConfigGenerator() {
    private val properties: ResourceBundle = ResourceBundle.getBundle(fileName)

    override fun getSrcString(): String? = getValueFromProperty("SRC")
    override fun getLangString(): String? = getValueFromProperty("LANG")
    override fun getCloneDetectorString(): String? = getValueFromProperty("CLONE_DETECTOR")
    override fun getGranularityString(): String? = getValueFromProperty("GRANULARITY")
    override fun getSimilarityInteger(): Int? = getValueFromProperty("SIMILARITY")?.toInt()
    override fun getStyleString(): String? = getValueFromProperty("STYLE")

    private fun getValueFromProperty(key: String): String? =
        try {
            val value = properties.getString(key)
            if (value == "") null else value
        } catch (e: MissingResourceException) {
            null
        }
}
