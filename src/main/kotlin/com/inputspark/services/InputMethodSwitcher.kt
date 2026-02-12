package com.inputspark.services

import com.inputspark.model.InputMethodType

/**
 * 输入法切换接口
 * 定义输入法切换的核心操作
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
interface InputMethodSwitcher {
    
    /**
     * 切换-英文输入法
     * 将系统输入法切换为英文模式
     *
     * @return 切换是否成功
     */
    fun switchToEnglish(): Boolean
    
    /**
     * 切换-中文输入法
     * 将系统输入法切换为中文模式
     *
     * @return 切换是否成功
     */
    fun switchToChinese(): Boolean
    
    /**
     * 获取-当前输入法
     * 获取当前系统的输入法状态
     *
     * @return 当前输入法类型
     */
    fun getCurrentInputMethod(): InputMethodType
    
    /**
     * 判断-是否可用
     * 检查当前环境是否支持输入法切换
     *
     * @return 是否可用
     */
    fun isAvailable(): Boolean
}
