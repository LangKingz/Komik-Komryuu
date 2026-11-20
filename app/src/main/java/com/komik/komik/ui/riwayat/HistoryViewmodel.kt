package com.komik.komik.ui.riwayat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.data.model.Riwayats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewmodel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseRiwayats.getDatabase(application)
    private val dao = db.riwayatdao()

    val riwayatList: LiveData<List<RiwayatDetail>> = dao.getRiwayatDetail()

    fun deleteRiwayat(riwayatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteRiwayatRelasi(riwayatId)
        }
    }

    fun reinsertRiwayat(riwayats: Riwayats) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertRiwayatRelasi(riwayats)
        }
    }

    fun deleteAllRiwayat() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllRiwayatRelasi()
        }
    }
}
