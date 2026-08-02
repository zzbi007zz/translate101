package com.example.zaloauto.service.accessibility

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * Finds UI elements inside Zalo's accessibility node tree.
 * All methods recycle returned nodes; callers must recycle non-returned nodes.
 * Uses iterative stack-based traversal to avoid StackOverflowError on deep layouts.
 */
class ZaloNodeFinder {

    /**
     * Find a visible node by exact or partial text match.
     */
    fun findByText(
        root: AccessibilityNodeInfo,
        text: String,
        exact: Boolean = false
    ): AccessibilityNodeInfo? {
        val candidates = if (exact) {
            root.findAccessibilityNodeInfosByText(text)
        } else {
            iterativeCollectByText(root, text)
        }
        val found = candidates.firstOrNull { it.isVisibleToUser }
        candidates.forEach { if (it !== found) it.recycle() }
        return found
    }

    private fun iterativeCollectByText(
        root: AccessibilityNodeInfo,
        needle: String
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.push(root)

        val lowerNeedle = needle.lowercase()
        while (stack.isNotEmpty()) {
            val node = stack.pop() ?: continue
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            if (node.childCount == 0) {
                // Leaf node: check and keep if matches
                if (lowerNeedle in text || lowerNeedle in desc) {
                    result.add(node)
                } else {
                    node.recycle()
                }
            } else {
                // Parent node: push children, keep this as potential match too
                if (lowerNeedle in text || lowerNeedle in desc) {
                    result.add(android.view.accessibility.AccessibilityNodeInfo.obtain(node))
                }
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { stack.push(it) }
                }
                node.recycle()
            }
        }
        return result
    }

    /**
     * Find a visible node by resource ID match.
     */
    fun findById(root: AccessibilityNodeInfo, resourceId: String): AccessibilityNodeInfo? {
        val candidates = root.findAccessibilityNodeInfosByViewId(resourceId)
        val found = candidates.firstOrNull { it.isVisibleToUser }
        candidates.forEach { if (it !== found) it.recycle() }
        return found
    }

    /**
     * Find the first visible EditText node in the tree.
     */
    fun findEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return iterativeFindFirst(root) { node ->
            node.isEditable && node.isVisibleToUser
        }
    }

    /**
     * Find a clickable node matching the given content description.
     * Useful for locating the Send button which appears after typing.
     */
    fun findClickableNear(
        root: AccessibilityNodeInfo,
        description: String
    ): AccessibilityNodeInfo? {
        val descLower = description.lowercase()
        return iterativeFindFirst(root) { node ->
            val clickable = node.isClickable && node.isVisibleToUser
            val matches = node.contentDescription?.toString()
                ?.lowercase()?.contains(descLower) == true
            clickable && matches
        }
    }

    /**
     * Poll rootInActiveWindow every [intervalMs] until predicate matches or timeout.
     */
    fun waitForNode(
        getRoot: () -> AccessibilityNodeInfo?,
        timeoutMs: Long = 8000L,
        intervalMs: Long = 200L,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Pair<AccessibilityNodeInfo?, Boolean> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val root = getRoot()
            if (root != null) {
                if (predicate(root)) return Pair(root, true)
                root.recycle()
            }
            SystemClock.sleep(intervalMs)
        }
        return Pair(null, false)
    }

    /**
     * Get rootInActiveWindow with retry.
     */
    fun getRootSafely(
        getRoot: () -> AccessibilityNodeInfo?,
        maxRetries: Int = 3
    ): AccessibilityNodeInfo? {
        repeat(maxRetries) {
            val root = getRoot()
            if (root != null) return root
            SystemClock.sleep(100)
        }
        return null
    }

    /**
     * Iterative depth-first search returning the first node matching predicate.
     * Recycles all non-matching nodes during traversal.
     */
    private fun iterativeFindFirst(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.push(root)

        while (stack.isNotEmpty()) {
            val node = stack.pop() ?: continue
            if (predicate(node)) {
                // Found match — recycle remaining stack
                stack.forEach { it.recycle() }
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.push(it) }
            }
        }
        return null
    }
}
