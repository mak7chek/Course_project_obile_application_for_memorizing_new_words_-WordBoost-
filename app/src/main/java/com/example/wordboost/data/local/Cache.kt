package com.example.wordboost.data.local


import androidx.room.*

@Entity(tableName = "translations")
data class CacheEntity(
    @PrimaryKey val original: String,
    val translated: String
)


@Dao
interface CacheDao {
    @Query("SELECT * FROM translations WHERE original = :text LIMIT 1")
    fun getTranslation(text: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTranslation(text: String, translated: String) {
        insert(CacheEntity(original = text, translated = translated))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: CacheEntity)
}

@Database(entities = [CacheEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}