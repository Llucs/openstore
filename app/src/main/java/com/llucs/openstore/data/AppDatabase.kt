package com.llucs.openstore.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.llucs.openstore.data.dao.AppDao
import com.llucs.openstore.data.dao.RepoDao
import com.llucs.openstore.data.dao.VersionDao
import com.llucs.openstore.data.entity.AppEntity
import com.llucs.openstore.data.entity.RepoEntity
import com.llucs.openstore.data.entity.VersionEntity

@Database(
    entities = [RepoEntity::class, AppEntity::class, VersionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao
    abstract fun appDao(): AppDao
    abstract fun versionDao(): VersionDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "openstore.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
