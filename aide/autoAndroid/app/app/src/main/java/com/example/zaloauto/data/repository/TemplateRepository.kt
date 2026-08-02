package com.example.zaloauto.data.repository

import com.example.zaloauto.data.db.TemplateDao
import com.example.zaloauto.data.db.TemplateEntity
import kotlinx.coroutines.flow.Flow

class TemplateRepository(private val templateDao: TemplateDao) {

    fun getAllFlow(): Flow<List<TemplateEntity>> = templateDao.getAllFlow()

    suspend fun getById(id: Long): TemplateEntity? = templateDao.getById(id)

    suspend fun insert(name: String, content: String): Long {
        val entity = TemplateEntity(name = name, content = content)
        return templateDao.insert(entity)
    }

    suspend fun update(template: TemplateEntity) {
        templateDao.update(template)
    }

    suspend fun delete(id: Long) {
        templateDao.delete(id)
    }
}
