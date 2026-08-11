package com.example.kakuyomureader.feature.bookmark.data

import android.content.Context
import android.content.SharedPreferences
import com.example.kakuyomureader.core.model.BookmarkItem
import com.example.kakuyomureader.core.model.DictionaryItem
import com.example.kakuyomureader.core.model.HistoryItem
import com.example.kakuyomureader.core.model.ReadingMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * お気に入り・閲覧履歴・読書設定の永続化リポジトリ
 *
 * [責務]: SharedPreferencesを用いたお気に入り、履歴、読み上げモード等の永続保存・取得・削除を行う。
 * [影響する状態]: 端末ストレージ (SharedPreferences)。
 * [発生しうる例外・エラー]: JSONパースエラー時のフォールバック処理。
 */
class BookmarkHistoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("speak_browser_data", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BOOKMARKS = "key_bookmarks"
        private const val KEY_HISTORY = "key_history"
        private const val KEY_DICTIONARY = "key_dictionary"
        private const val KEY_READING_MODE = "key_reading_mode"
        private const val KEY_PARENT_LEVELS = "key_parent_levels"
        private const val MAX_HISTORY_SIZE = 100
    }

    /**
     * お気に入り一覧を取得する。
     */
    fun getBookmarks(): List<BookmarkItem> {
        val jsonStr = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"
        val list = mutableListOf<BookmarkItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BookmarkItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        title = obj.optString("title", "無題"),
                        url = obj.optString("url", ""),
                        createdAt = obj.optLong("createdAt", 0L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * お気に入りを追加・更新する（同一URLは上書き）。
     */
    fun saveBookmark(title: String, url: String): List<BookmarkItem> {
        val current = getBookmarks().toMutableList()
        current.removeAll { it.url == url }
        val displayTitle = title.ifBlank { url }
        current.add(0, BookmarkItem(title = displayTitle, url = url))
        saveBookmarksList(current)
        return current
    }

    /**
     * お気に入りを削除する。
     */
    fun deleteBookmark(id: String): List<BookmarkItem> {
        val current = getBookmarks().toMutableList()
        current.removeAll { it.id == id }
        saveBookmarksList(current)
        return current
    }

    private fun saveBookmarksList(list: List<BookmarkItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    /**
     * 閲覧履歴一覧を取得する。
     */
    fun getHistory(): List<HistoryItem> {
        val jsonStr = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<HistoryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        title = obj.optString("title", "無題"),
                        url = obj.optString("url", ""),
                        visitedAt = obj.optLong("visitedAt", 0L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * 閲覧履歴を追加する（同一URLは最新に更新、最大100件）。
     */
    fun addHistory(title: String, url: String): List<HistoryItem> {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return getHistory()
        val current = getHistory().toMutableList()
        current.removeAll { it.url == url }
        val displayTitle = title.ifBlank { url }
        current.add(0, HistoryItem(title = displayTitle, url = url))
        if (current.size > MAX_HISTORY_SIZE) {
            current.removeAt(current.size - 1)
        }
        saveHistoryList(current)
        return current
    }

    /**
     * 閲覧履歴を削除する。
     */
    fun deleteHistory(id: String): List<HistoryItem> {
        val current = getHistory().toMutableList()
        current.removeAll { it.id == id }
        saveHistoryList(current)
        return current
    }

    /**
     * 閲覧履歴を全件クリアする。
     */
    fun clearAllHistory(): List<HistoryItem> {
        prefs.edit().remove(KEY_HISTORY).apply()
        return emptyList()
    }

    private fun saveHistoryList(list: List<HistoryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("visitedAt", item.visitedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    /**
     * 読み上げモードの取得と保存
     */
    fun getReadingMode(): ReadingMode {
        val name = prefs.getString(KEY_READING_MODE, ReadingMode.CONTAINER.name)
        return try {
            ReadingMode.valueOf(name ?: ReadingMode.CONTAINER.name)
        } catch (_: Exception) {
            ReadingMode.CONTAINER
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        prefs.edit().putString(KEY_READING_MODE, mode.name).apply()
    }

    /**
     * 親階層遡り設定の取得と保存 (1〜5)
     */
    fun getParentLevels(): Int = prefs.getInt(KEY_PARENT_LEVELS, 2)

    fun setParentLevels(levels: Int) {
        prefs.edit().putInt(KEY_PARENT_LEVELS, levels.coerceIn(1, 5)).apply()
    }

    /**
     * 再生速度の取得と保存
     */
    fun getPlaybackSpeed(): Float = prefs.getFloat("key_playback_speed", 1.0f)

    fun setPlaybackSpeed(rate: Float) {
        prefs.edit().putFloat("key_playback_speed", rate).apply()
    }

    /**
     * ユーザー辞書一覧を取得する。
     */
    fun getDictionary(): List<DictionaryItem> {
        val jsonStr = prefs.getString(KEY_DICTIONARY, "[]") ?: "[]"
        val list = mutableListOf<DictionaryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DictionaryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        surface = obj.optString("surface", ""),
                        reading = obj.optString("reading", ""),
                        createdAt = obj.optLong("createdAt", 0L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * ユーザー辞書アイテムを追加・更新する（同一表記は上書き）。
     */
    fun saveDictionaryItem(surface: String, reading: String): List<DictionaryItem> {
        if (surface.isBlank() || reading.isBlank()) return getDictionary()
        val current = getDictionary().toMutableList()
        current.removeAll { it.surface == surface.trim() }
        current.add(0, DictionaryItem(surface = surface.trim(), reading = reading.trim()))
        saveDictionaryList(current)
        return current
    }

    /**
     * ユーザー辞書アイテムを削除する。
     */
    fun deleteDictionaryItem(id: String): List<DictionaryItem> {
        val current = getDictionary().toMutableList()
        current.removeAll { it.id == id }
        saveDictionaryList(current)
        return current
    }

    private fun saveDictionaryList(list: List<DictionaryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("surface", item.surface)
                put("reading", item.reading)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_DICTIONARY, array.toString()).apply()
    }
}
