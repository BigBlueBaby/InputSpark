package com.inputspark.services.impl

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.Service
import com.inputspark.model.CustomRule
import com.inputspark.model.InputMethodType
import com.inputspark.model.PluginConfig
import com.inputspark.model.SceneType
import com.inputspark.services.ConfigurationManager
import java.awt.Color

/**
 * 配置管理实现类
 * 使用 IntelliJ 平台提供的持久化状态组件存储配置
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
@State(
    name = "InputSparkConfig",
    storages = [Storage("InputSpark.xml")]
)
@Service(Service.Level.APP)
class ConfigurationManagerImpl : ConfigurationManager, PersistentStateComponent<PluginConfig> {

    private var config = PluginConfig()

    override fun getState(): PluginConfig {
        return config
    }

    override fun loadState(state: PluginConfig) {
        // XML反序列化时可能会丢失默认值，这里做安全合并或直接使用
        this.config = state
    }

    override fun getCursorColorConfig(): Map<InputMethodType, Color> {
        return config.cursorColors.entries.associate { (k, v) ->
            InputMethodType.valueOf(k) to Color.decode(v)
        }
    }

    override fun getSceneConfig(): Map<SceneType, Boolean> {
        return config.sceneConfig.entries.associate { (k, v) ->
            SceneType.valueOf(k) to v
        }
    }

    override fun getCustomRules(): List<CustomRule> {
        return config.customRules
    }

    override fun saveConfiguration(config: PluginConfig) {
        this.config = config
    }

    override fun getConfig(): PluginConfig {
        return config
    }
}
