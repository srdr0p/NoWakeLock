package com.js.nowakelock.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.js.nowakelock.data.db.entity.AppInfo

@Dao
interface AppInfoDao {

    @Query("select * from appInfo")
    fun loadAllAppInfos(): LiveData<List<AppInfo>>

    @Query("select * from appInfo where packageName = :packageName")
    suspend fun loadAppInfo(packageName: String): AppInfo

    @Query("select packageName from appInfo")
    suspend fun loadPackageNames(): List<String>

    @Query("select packageName from appInfo")
    fun loadPackageNames2(): LiveData<List<String>>

    @Query("update appInfo set count = count+1 where packageName = :packageName")
    suspend fun upCount(packageName: String)

    @Query("update appInfo set blockCount = blockCount+1 where packageName = :packageName")
    suspend fun upBlockCount(packageName: String)

    @Query("update appInfo set count = :count where packageName = :packageName")
    suspend fun upCount(packageName: String, count: Int)

    @Query("update appInfo set blockCount = :blockCount where packageName = :packageName")
    suspend fun upBlockCount(packageName: String, blockCount: Int)

    @Query("update appInfo set count = 0 where packageName = :packageName")
    suspend fun rstCount(packageName: String)

    @Query("update appInfo set blockCount = 0 where packageName = :packageName")
    suspend fun rstBlockCount(packageName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appInfos: MutableCollection<AppInfo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appInfo: AppInfo)

    @Delete
    suspend fun deleteAll(appInfos: MutableCollection<AppInfo>)

    @Delete
    suspend fun delete(appInfo: AppInfo)
}