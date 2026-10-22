package com.fingertip.baselib.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fingertip.baselib.top.TopApplication

//版本3：加了 book_copy_progress 表。缓存数据丢了都能从服务端重拉，直接清库重建
@Database(
    entities = [CatalogEntity::class, ChapterEntity::class, ReadProgressEntity::class,
        CopyProgressEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BuddhaDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var instance: BuddhaDatabase? = null

        fun get(): BuddhaDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    TopApplication.instance,
                    BuddhaDatabase::class.java,
                    "buddha.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
