package com.inputspark.services

import com.inputspark.model.CustomRule
import com.inputspark.model.InputMethodType

/**
 * 规则引擎接口
 * 用于处理和匹配自定义的输入法切换规则
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
interface RuleEngine {
    
    /**
     * 匹配-规则
     * 根据当前上下文文本匹配适用的规则
     *
     * @param text 当前上下文文本
     * @return 匹配到的规则，如果没有匹配则返回 null
     */
    fun matchRule(text: String): CustomRule?
    
    /**
     * 确定-目标输入法
     * 根据文本内容确定应该切换到的输入法
     *
     * @param text 当前上下文文本
     * @param defaultMethod 默认输入法
     * @return 目标输入法类型
     */
    fun determineTargetInputMethod(text: String, defaultMethod: InputMethodType): InputMethodType
}
