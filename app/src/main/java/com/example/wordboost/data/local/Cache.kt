package com.example.wordboost.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "translations", indices = [Index(value = ["translated"], unique = false)])
data class CacheEntity(
    @PrimaryKey val original: String,
    val translated: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Dao та Database залишаються поки без змін на цьому кроці
@Dao
interface CacheDao {
    @Query("SELECT * FROM translations WHERE original = :text OR translated = :text LIMIT 1")
    fun getCacheEntry(text: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTranslation(originalText: String, translatedText: String) {
        insert(CacheEntity(original = originalText, translated = translatedText))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: CacheEntity)

    @Query("UPDATE translations SET timestamp = :newTimestamp WHERE original = :originalText")
    fun updateTimestamp(originalText: String, newTimestamp: Long)

    // !!! Змініть метод очищення для видалення записів старіших за певну відмітку часу !!!
    @Query("DELETE FROM translations WHERE timestamp < :thresholdTime")
    fun clearOldEntries(thresholdTime: Long)
}

@Database(entities = [CacheEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wordboost-db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}