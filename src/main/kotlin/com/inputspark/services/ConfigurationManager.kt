package com.inputspark.services

import com.inputspark.model.CustomRule
import com.inputspark.model.InputMethodType
import com.inputspark.model.PluginConfig
import com.inputspark.model.SceneType
import java.awt.Color

/**
 * 配置管理接口
 * 用于管理插件的各项配置信息的读取和保存
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
interface ConfigurationManager {
    
    /**
     * 获取-光标颜色配置
     * 获取不同输入法对应的光标颜色配置
     *
     * @return 输入法类型到颜色的映射
     */
    fun getCursorColorConfig(): Map<InputMethodType, Color>
    
    /**
     * 获取-场景配置
     * 获取各场景是否启用自动切换的配置
     *
     * @return 场景类型到布尔值的映射
     */
    fun getSceneConfig(): Map<SceneType, Boolean>
    
    /**
     * 获取-自定义规则
     * 获取所有用户定义的切换规则
     *
     * @return 自定义规则列表
     */
    fun getCustomRules(): List<CustomRule>
    
    /**
     * 保存-配置
     * 保存插件的完整配置信息
     *
     * @param config 插件配置对象
     */
    fun saveConfiguration(config: PluginConfig)
    
    /**
     * 获取-当前配置
     * 获取当前的完整配置对象
     *
     * @return 插件配置对象
     */
    fun getConfig(): PluginConfig
}
