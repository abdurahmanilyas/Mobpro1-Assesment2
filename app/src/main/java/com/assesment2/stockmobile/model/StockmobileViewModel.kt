package com.assesment2.stockmobile

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.work.*
import com.assesment2.stockmobile.data.Item
import com.assesment2.stockmobile.data.ProductDao
import com.assesment2.stockmobile.model.Goods
import com.assesment2.stockmobile.network.ApiStatus
import com.assesment2.stockmobile.network.StockApi
import com.assesment2.stockmobile.network.UpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class InventoryViewModel(private val productDao: ProductDao) : ViewModel() {


    val allItems: LiveData<List<Item>> = productDao.getItems().asLiveData()


    fun isStockAvailable(item: Item): Boolean {
        return (item.quantityInStock > 0)
    }


    fun updateItem(
        itemId: Int,
        itemName: String,
        itemPrice: String,
        itemCount: String
    ) {
        val updatedItem = getUpdatedItemEntry(itemId, itemName, itemPrice, itemCount)
        updateItem(updatedItem)
    }



    private fun updateItem(item: Item) {
        viewModelScope.launch {
            productDao.update(item)
        }
    }


    fun sellItem(item: Item) {
        if (item.quantityInStock > 0) {

            val newItem = item.copy(quantityInStock = item.quantityInStock - 1)
            updateItem(newItem)
        }
    }


    fun addNewItem(itemName: String, itemPrice: String, itemCount: String) {
        val newItem = getNewItemEntry(itemName, itemPrice, itemCount)
        insertItem(newItem)
    }


    private fun insertItem(item: Item) {
        viewModelScope.launch {
            productDao.insert(item)
        }
    }


    fun deleteItem(item: Item) {
        viewModelScope.launch {
            productDao.delete(item)
        }
    }


    fun retrieveItem(id: Int): LiveData<Item> {
        return productDao.getItem(id).asLiveData()
    }


    fun isEntryValid(itemName: String, itemPrice: String, itemCount: String): Boolean {
        if (itemName.isBlank() || itemPrice.isBlank() || itemCount.isBlank()) {
            return false
        }
        return true
    }


    private fun getNewItemEntry(itemName: String, itemPrice: String, itemCount: String): Item {
        return Item(
            itemName = itemName,
            itemPrice = itemPrice.toDouble(),
            quantityInStock = itemCount.toInt()
        )
    }


    private fun getUpdatedItemEntry(
        itemId: Int,
        itemName: String,
        itemPrice: String,
        itemCount: String
    ): Item {
        return Item(
            id = itemId,
            itemName = itemName,
            itemPrice = itemPrice.toDouble(),
            quantityInStock = itemCount.toInt()
        )
    }

    private val data = MutableLiveData<List<Goods>>()
    private val status = MutableLiveData<ApiStatus>()
    init {
        retrieveData()
    }
    private fun retrieveData() {
        viewModelScope.launch (Dispatchers.IO) {
            status.postValue(ApiStatus.LOADING)
            try {
                val result = StockApi.service.getRate()
                Log.d("MainViewModel", "Success: $result")
                data.postValue(StockApi.service.getRate())
                status.postValue(ApiStatus.SUCCESS)
            } catch (e: Exception) {
                Log.d("MainViewModel", "Failure: ${e.message}")
                status.postValue(ApiStatus.FAILED)
            }
        }
    }
    fun getStatus(): LiveData<ApiStatus> = status
    fun getData(): LiveData<List<Goods>> = data

    fun scheduleUpdater(app: Application) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val myRequest = PeriodicWorkRequest.Builder(
            UpdateWorker::class.java,
            15,
            TimeUnit.MINUTES
        ).setConstraints(constraints)
            .addTag("my_id")
            .build()

        WorkManager.getInstance(app)
            .enqueueUniquePeriodicWork(
                "my_id",
                ExistingPeriodicWorkPolicy.KEEP,
                myRequest
            )
    }
}


class InventoryViewModelFactory(private val productDao: ProductDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(productDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

