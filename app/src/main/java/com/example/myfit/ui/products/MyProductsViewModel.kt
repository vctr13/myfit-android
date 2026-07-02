package com.example.myfit.ui.products

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.Product
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MyFitApp.from(application).database.productDao()

    val products: StateFlow<List<Product>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var showDialog    by mutableStateOf(false)
        private set
    var isEditMode    by mutableStateOf(false)
        private set
    var editingProduct by mutableStateOf<Product?>(null)
        private set

    var editName    by mutableStateOf("")
    var editKcal    by mutableStateOf("")
    var editProtein by mutableStateOf("")
    var editFat     by mutableStateOf("")
    var editCarbs   by mutableStateOf("")
    var editFiber   by mutableStateOf("")
    var formError   by mutableStateOf<String?>(null)
        private set

    fun openAddDialog() {
        editingProduct = null
        isEditMode = false
        editName = ""; editKcal = ""; editProtein = ""
        editFat = ""; editCarbs = ""; editFiber = ""
        formError = null
        showDialog = true
    }

    fun openEditDialog(product: Product) {
        editingProduct = product
        isEditMode = true
        editName    = product.name
        editKcal    = "%.1f".format(product.calories)
        editProtein = "%.1f".format(product.protein)
        editFat     = "%.1f".format(product.fat)
        editCarbs   = "%.1f".format(product.carbs)
        editFiber   = product.fiber?.let { "%.1f".format(it) } ?: ""
        formError = null
        showDialog = true
    }

    fun closeDialog() {
        showDialog = false
        editingProduct = null
        isEditMode = false
    }

    fun saveProduct() {
        val name    = editName.trim()
        val kcal    = editKcal.trim().replace(',', '.').toFloatOrNull()
        val protein = editProtein.trim().replace(',', '.').toFloatOrNull()
        val fat     = editFat.trim().replace(',', '.').toFloatOrNull()
        val carbs   = editCarbs.trim().replace(',', '.').toFloatOrNull()
        val fiber   = editFiber.trim().replace(',', '.').toFloatOrNull()

        if (name.isBlank())                 { formError = "Введите название"; return }
        if (kcal == null || kcal < 0)       { formError = "Введите калории"; return }
        if (protein == null || protein < 0) { formError = "Введите белки"; return }
        if (fat == null || fat < 0)         { formError = "Введите жиры"; return }
        if (carbs == null || carbs < 0)     { formError = "Введите углеводы"; return }

        formError = null
        viewModelScope.launch {
            val current = editingProduct
            if (current != null) {
                dao.update(current.copy(
                    name = name, calories = kcal, protein = protein,
                    fat = fat, carbs = carbs, fiber = fiber
                ))
            } else {
                dao.insert(Product(name = name, calories = kcal, protein = protein,
                    fat = fat, carbs = carbs, fiber = fiber))
            }
            closeDialog()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { dao.delete(product) }
    }
}
