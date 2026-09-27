package com.yokos.bb10launcher.hub

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.Executors

/**
 * Keeps the Hub history in a small SQLite database. Reads happen once at start-up; writes are
 * queued on one background thread so the listener and UI never wait on disk.
 */
class SqliteHubStore(context: Context) : HubStore {
    private val helper = Helper(context.applicationContext)
    private val writer = Executors.newSingleThreadExecutor()

    override fun loadAll(): List<HubItem> {
        val result = ArrayList<HubItem>()
        helper.readableDatabase.query(TABLE, null, null, null, null, null, "$POST_TIME DESC").use { c ->
            val id = c.getColumnIndexOrThrow(ID)
            val key = c.getColumnIndexOrThrow(KEY)
            val pkg = c.getColumnIndexOrThrow(PACKAGE)
            val label = c.getColumnIndexOrThrow(LABEL)
            val title = c.getColumnIndexOrThrow(TITLE)
            val text = c.getColumnIndexOrThrow(TEXT)
            val time = c.getColumnIndexOrThrow(POST_TIME)
            val category = c.getColumnIndexOrThrow(CATEGORY)
            val reply = c.getColumnIndexOrThrow(HAS_REPLY)
            val active = c.getColumnIndexOrThrow(ACTIVE)
            val read = c.getColumnIndexOrThrow(READ)
            while (c.moveToNext()) {
                result += HubItem(
                    id = c.getLong(id),
                    key = c.getString(key),
                    packageName = c.getString(pkg),
                    appLabel = c.getString(label),
                    title = c.getString(title),
                    text = c.getString(text),
                    postTime = c.getLong(time),
                    category = HubCategory.entries.firstOrNull { it.name == c.getString(category) } ?: HubCategory.Default,
                    hasReply = c.getInt(reply) != 0,
                    active = c.getInt(active) != 0,
                    read = c.getInt(read) != 0,
                )
            }
        }
        return result
    }

    override fun upsert(items: Collection<HubItem>) {
        val rows = items.map { item ->
            ContentValues().apply {
                put(ID, item.id)
                put(KEY, item.key)
                put(PACKAGE, item.packageName)
                put(LABEL, item.appLabel)
                put(TITLE, item.title)
                put(TEXT, item.text)
                put(POST_TIME, item.postTime)
                put(CATEGORY, item.category.name)
                put(HAS_REPLY, if (item.hasReply) 1 else 0)
                put(ACTIVE, if (item.active) 1 else 0)
                put(READ, if (item.read) 1 else 0)
            }
        }
        write { db -> rows.forEach { db.insertWithOnConflict(TABLE, null, it, SQLiteDatabase.CONFLICT_REPLACE) } }
    }

    override fun delete(ids: Collection<Long>) {
        val list = ids.toList()
        write { db ->
            // Stay well under SQLite's bound-parameter limit.
            list.chunked(500).forEach { chunk ->
                val placeholders = chunk.joinToString(",") { "?" }
                db.delete(TABLE, "$ID IN ($placeholders)", chunk.map { it.toString() }.toTypedArray())
            }
        }
    }

    private fun write(block: (SQLiteDatabase) -> Unit) {
        writer.execute {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                block(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE (
                    $ID INTEGER PRIMARY KEY,
                    $KEY TEXT NOT NULL,
                    $PACKAGE TEXT NOT NULL,
                    $LABEL TEXT NOT NULL,
                    $TITLE TEXT NOT NULL,
                    $TEXT TEXT NOT NULL,
                    $POST_TIME INTEGER NOT NULL,
                    $CATEGORY TEXT NOT NULL,
                    $HAS_REPLY INTEGER NOT NULL,
                    $ACTIVE INTEGER NOT NULL,
                    $READ INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX idx_hub_time ON $TABLE ($POST_TIME)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private companion object {
        const val DB_NAME = "hub_history.db"
        const val VERSION = 1
        const val TABLE = "hub"
        const val ID = "id"
        const val KEY = "notification_key"
        const val PACKAGE = "package_name"
        const val LABEL = "app_label"
        const val TITLE = "title"
        const val TEXT = "body"
        const val POST_TIME = "post_time"
        const val CATEGORY = "category"
        const val HAS_REPLY = "has_reply"
        const val ACTIVE = "active"
        const val READ = "is_read"
    }
}
