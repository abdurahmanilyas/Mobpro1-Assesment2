package com.assesment2.stockmobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [Item::class], version = 1, exportSchema = false)
abstract class ProductDb : RoomDatabase() {

    abstract fun itemDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: ProductDb? = null

        fun getDatabase(context: Context): ProductDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProductDb::class.java,
                    "item_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}