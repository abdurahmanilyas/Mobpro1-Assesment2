package com.assesment2.stockmobile.data

import android.app.Application


class Stockmobile : Application() {
    val database: ProductDb by lazy { ProductDb.getDatabase(this) }
}
