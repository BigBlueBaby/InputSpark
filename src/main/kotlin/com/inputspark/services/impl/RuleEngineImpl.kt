package com.inputspark.services.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.inputspark.model.CustomRule
import com.inputspark.model.InputMethodType
import com.inputspark.services.ConfigurationManager
import com.inputspark.services.RuleEngine
import java.util.regex.Pattern

/**
 * 规则引擎实现类
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
@Service(Service.Level.APP)
class RuleEngineImpl : RuleEngine {

    override fun matchRule(text: String): CustomRule? {
        val configManager = service<ConfigurationManager>()
        val rules = configManager.getCustomRules().filter { it.enabled }.sortedByDescending { it.priority }

        for (rule in rules) {
            try {
                if (Pattern.compile(rule.pattern).matcher(text).find()) {
                    return rule
                }
            } catch (e: Exception) {
                // 忽略无效的正则表达式
            }
        }
        return null
    }

    override fun determineTargetInputMethod(text: String, defaultMethod: InputMethodType): InputMethodType {
        val rule = matchRule(text)
        return rule?.targetInputMethod ?: defaultMethod
    }
}
