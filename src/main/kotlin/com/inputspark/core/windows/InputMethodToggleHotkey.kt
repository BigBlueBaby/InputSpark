package com.inputspark.core.windows

import java.awt.event.KeyEvent

/**
 * 输入法切换热键
 * 用于封装设置页中配置的单键或组合键
 *
 * @property displayText 热键展示文本
 * @property keyCodes 热键对应的按键编码列表
 *
 * @author 林龙祥
 * @since 2026-04-28
 */
data class InputMethodToggleHotkey(
    /** 热键展示文本 */
    val displayText: String,
    /** 热键对应的按键编码列表 */
    val keyCodes: List<Int>
)

/**
 * 输入法切换热键解析器
 * 负责将用户在设置页输入的热键文本解析为可执行的按键编码
 *
 * @author 林龙祥
 * @since 2026-04-28
 */
object InputMethodToggleHotkeyParser {

    /** 默认热键文本 */
    const val DEFAULT_HOTKEY_TEXT = "Shift"

    /** 常见别名与标准展示名映射 */
    private val displayAliasMap: Map<String, String> = mapOf(
        "SHIFT" to "Shift",
        "CTRL" to "Ctrl",
        "CONTROL" to "Ctrl",
        "ALT" to "Alt",
        "OPTION" to "Alt",
        "META" to "Meta",
        "WIN" to "Win",
        "WINDOWS" to "Win",
        "CMD" to "Meta",
        "COMMAND" to "Meta",
        "ENTER" to "Enter",
        "RETURN" to "Enter",
        "ESC" to "Esc",
        "ESCAPE" to "Esc",
        "SPACE" to "Space",
        "TAB" to "Tab",
        "CAPSLOCK" to "CapsLock",
        "CAPS" to "CapsLock",
        "BACKSPACE" to "Backspace",
        "DELETE" to "Delete",
        "DEL" to "Delete",
        "INSERT" to "Insert",
        "HOME" to "Home",
        "END" to "End",
        "PAGEUP" to "PageUp",
        "PAGEDOWN" to "PageDown",
        "UP" to "Up",
        "DOWN" to "Down",
        "LEFT" to "Left",
        "RIGHT" to "Right"
    )

    /** 常见别名与 KeyEvent 编码映射 */
    private val keyCodeAliasMap: Map<String, Int> = mapOf(
        "SHIFT" to KeyEvent.VK_SHIFT,
        "CTRL" to KeyEvent.VK_CONTROL,
        "CONTROL" to KeyEvent.VK_CONTROL,
        "ALT" to KeyEvent.VK_ALT,
        "OPTION" to KeyEvent.VK_ALT,
        "META" to KeyEvent.VK_META,
        "WIN" to KeyEvent.VK_WINDOWS,
        "WINDOWS" to KeyEvent.VK_WINDOWS,
        "CMD" to KeyEvent.VK_META,
        "COMMAND" to KeyEvent.VK_META,
        "ENTER" to KeyEvent.VK_ENTER,
        "RETURN" to KeyEvent.VK_ENTER,
        "ESC" to KeyEvent.VK_ESCAPE,
        "ESCAPE" to KeyEvent.VK_ESCAPE,
        "SPACE" to KeyEvent.VK_SPACE,
        "TAB" to KeyEvent.VK_TAB,
        "CAPSLOCK" to KeyEvent.VK_CAPS_LOCK,
        "CAPS" to KeyEvent.VK_CAPS_LOCK,
        "BACKSPACE" to KeyEvent.VK_BACK_SPACE,
        "DELETE" to KeyEvent.VK_DELETE,
        "DEL" to KeyEvent.VK_DELETE,
        "INSERT" to KeyEvent.VK_INSERT,
        "HOME" to KeyEvent.VK_HOME,
        "END" to KeyEvent.VK_END,
        "PAGEUP" to KeyEvent.VK_PAGE_UP,
        "PAGEDOWN" to KeyEvent.VK_PAGE_DOWN,
        "UP" to KeyEvent.VK_UP,
        "DOWN" to KeyEvent.VK_DOWN,
        "LEFT" to KeyEvent.VK_LEFT,
        "RIGHT" to KeyEvent.VK_RIGHT
    )

    /**
     * 解析-热键配置
     * 将用户输入的热键文本解析为标准热键对象
     *
     * @param hotkeyText 用户输入的热键文本
     * @return 解析后的热键对象
     */
    fun parse(hotkeyText: String?): InputMethodToggleHotkey {
        // 原始热键文本：来自设置页或持久化配置，允许为空字符串
        val rawHotkeyText = hotkeyText?.trim().orEmpty()
        // 生效热键文本：为空时回退为默认 Shift，保证旧配置可兼容
        val effectiveHotkeyText = rawHotkeyText.ifBlank { DEFAULT_HOTKEY_TEXT }
        // 热键分段列表：按加号拆分后用于分别解析每个按键
        val hotkeyTokenList = effectiveHotkeyText
            .split("+")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        require(hotkeyTokenList.isNotEmpty()) { "中英文切换按键不能为空" }

        // 展示名列表：用于回显到设置页，统一成标准格式
        val displayTokenList = hotkeyTokenList.map { token ->
            resolveDisplayToken(token)
        }
        // 按键编码列表：供 Robot 依次按下与释放
        val keyCodeList = hotkeyTokenList.map { token ->
            resolveKeyCode(token)
                ?: throw IllegalArgumentException("不支持的按键：$token")
        }
        // 标准展示文本：统一输出为可读的组合键文案
        val displayText = displayTokenList.joinToString(" + ")
        return InputMethodToggleHotkey(displayText, keyCodeList)
    }

    /**
     * 解析-标准展示名
     * 将用户输入的按键别名转换为统一展示文案
     *
     * @param token 单个按键文本
     * @return 标准展示名
     */
    private fun resolveDisplayToken(token: String): String {
        // 标准化后的按键文本：去空格并转大写，方便做别名匹配
        val normalizedToken = normalizeToken(token)
        // 别名命中结果：优先复用预定义的标准名称
        val aliasDisplayText = displayAliasMap[normalizedToken]
        if (aliasDisplayText != null) {
            return aliasDisplayText
        }
        if (normalizedToken.length == 1 && normalizedToken[0].isLetterOrDigit()) {
            return normalizedToken
        }
        if (normalizedToken.startsWith("F") && normalizedToken.drop(1).all { it.isDigit() }) {
            return normalizedToken
        }
        throw IllegalArgumentException("不支持的按键：$token")
    }

    /**
     * 解析-按键编码
     * 将单个按键文本解析为 AWT KeyEvent 编码
     *
     * @param token 单个按键文本
     * @return 按键编码；不支持时返回 null
     */
    private fun resolveKeyCode(token: String): Int? {
        // 标准化后的按键文本：统一用于字典与规则匹配
        val normalizedToken = normalizeToken(token)
        // 别名编码：处理 Shift、Ctrl、Alt、Win 等常见按键
        val aliasKeyCode = keyCodeAliasMap[normalizedToken]
        if (aliasKeyCode != null) {
            return aliasKeyCode
        }
        if (normalizedToken.length == 1 && normalizedToken[0].isLetter()) {
            return KeyEvent.getExtendedKeyCodeForChar(normalizedToken[0].code)
        }
        if (normalizedToken.length == 1 && normalizedToken[0].isDigit()) {
            return KeyEvent.getExtendedKeyCodeForChar(normalizedToken[0].code)
        }
        if (normalizedToken.startsWith("F") && normalizedToken.drop(1).all { it.isDigit() }) {
            // 功能键序号：用于构造 F1-F24 对应的 VK 编码
            val functionIndex = normalizedToken.drop(1).toIntOrNull() ?: return null
            if (functionIndex in 1..24) {
                return KeyEvent.VK_F1 + (functionIndex - 1)
            }
        }
        return null
    }

    /**
     * 标准化-按键文本
     * 去掉空白并统一为大写，避免大小写和空格影响配置识别
     *
     * @param token 单个按键文本
     * @return 标准化结果
     */
    private fun normalizeToken(token: String): String {
        // 去空白后的文本：清理用户输入中的无意义空格
        val compactToken = token.replace(" ", "")
        return compactToken.uppercase()
    }
}
