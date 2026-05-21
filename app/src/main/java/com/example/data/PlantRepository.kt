package com.example.data

import kotlinx.coroutines.flow.Flow

class PlantRepository(private val plantDao: PlantDao) {
    val allPlants: Flow<List<Plant>> = plantDao.getAllPlants()

    suspend fun getPlantById(id: Int): Plant? {
        return plantDao.getPlantById(id)
    }

    suspend fun insertPlant(plant: Plant): Long {
        return plantDao.insertPlant(plant)
    }

    suspend fun updatePlant(plant: Plant) {
        plantDao.updatePlant(plant)
    }

    suspend fun deletePlant(plant: Plant) {
        plantDao.deletePlant(plant)
    }

    suspend fun deletePlantById(id: Int) {
        plantDao.deletePlantById(id)
    }
}
