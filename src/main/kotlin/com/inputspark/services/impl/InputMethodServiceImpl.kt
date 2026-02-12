package com.inputspark.services.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.inputspark.core.macos.MacOSInputMethodSwitcher
import com.inputspark.core.windows.WindowsInputMethodSwitcher
import com.inputspark.model.InputMethodType
import com.inputspark.services.InputMethodSwitcher

/**
 * 输入法服务实现类
 * 负责根据操作系统选择具体的切换器实现
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
@Service(Service.Level.APP)
class InputMethodServiceImpl : InputMethodSwitcher {

    private val switcher: InputMethodSwitcher? = when {
        System.getProperty("os.name").lowercase().contains("win") -> WindowsInputMethodSwitcher()
        System.getProperty("os.name").lowercase().contains("mac") -> MacOSInputMethodSwitcher()
        else -> null
    }

    override fun switchToEnglish(): Boolean {
        return switcher?.switchToEnglish() ?: false
    }

    override fun switchToChinese(): Boolean {
        return switcher?.switchToChinese() ?: false
    }

    override fun getCurrentInputMethod(): InputMethodType {
        return switcher?.getCurrentInputMethod() ?: InputMethodType.ENGLISH
    }

    override fun isAvailable(): Boolean {
        return switcher?.isAvailable() ?: false
    }
}
