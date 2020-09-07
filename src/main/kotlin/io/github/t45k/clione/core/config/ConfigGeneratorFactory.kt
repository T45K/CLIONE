package io.github.t45k.clione.core.config

class ConfigGeneratorFactory {

    companion object {
        fun fromToml(contents: String): RunningConfig = TomlConfigGenerator(contents).generateConfig()
        fun fromProperties(fileName: String): RunningConfig = PropertiesConfigGenerator(fileName).generateConfig()
    }
}
