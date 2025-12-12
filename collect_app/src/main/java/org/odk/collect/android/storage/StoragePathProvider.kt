package org.odk.collect.android.storage

import android.os.Environment
import org.odk.collect.android.application.Collect
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.utilities.FileUtils
import org.odk.collect.projects.ProjectDependencyFactory
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.shared.PathUtils
import timber.log.Timber
import java.io.File

class StoragePathProvider(
    private val projectsDataService: ProjectsDataService = DaggerUtils.getComponent(Collect.getInstance()).currentProjectProvider(),
    private val projectsRepository: ProjectsRepository = DaggerUtils.getComponent(Collect.getInstance()).projectsRepository(),
    val odkRootDirPath: String = Collect.getInstance().getExternalFilesDir(null)!!.absolutePath
) : ProjectDependencyFactory<StoragePaths> {

    @JvmOverloads
    @Deprecated(message = "Use create() instead")
    fun getProjectRootDirPath(projectId: String? = null): String {
        val uuid = projectId ?: projectsDataService.requireCurrentProject().uuid
        val path = getOdkDirPath(StorageSubdirectory.PROJECTS) + File.separator + uuid

        if (!File(path).exists()) {
            File(path).mkdirs()

            try {
                val sanitizedProjectName = PathUtils.getPathSafeFileName(projectsRepository.get(uuid)!!.name)
                File(path + File.separator + sanitizedProjectName).createNewFile()
            } catch (e: Exception) {
                Timber.e(
                    Error(
                        FileUtils.getFilenameError(
                            projectsRepository.get(uuid)!!.name
                        )
                    )
                )
            }
        }

        return path
    }

    @JvmOverloads
    @Deprecated(message = "Use create() instead")
    fun getOdkDirPath(subdirectory: StorageSubdirectory, projectId: String? = null): String {
        val path = when (subdirectory) {
            StorageSubdirectory.PROJECTS,
            StorageSubdirectory.SHARED_LAYERS -> odkRootDirPath + File.separator + subdirectory.directoryName
            StorageSubdirectory.FORMS,
            StorageSubdirectory.INSTANCES,
            StorageSubdirectory.CACHE,
            StorageSubdirectory.METADATA,
            StorageSubdirectory.LAYERS,
            StorageSubdirectory.SETTINGS -> getProjectRootDirPath(projectId) + File.separator + subdirectory.directoryName
        }

        if (!File(path).exists()) {
            File(path).mkdirs()
        }

        return path
    }

    @Deprecated(
        message = "Should use specific temp file or create a new file in StorageSubdirectory.CACHE instead",
        ReplaceWith(
            "getOdkDirPath(StorageSubdirectory.CACHE) + File.separator + \"tmp.jpg\""
        )
    )
    fun getTmpImageFilePath(): String {
        return getOdkDirPath(StorageSubdirectory.CACHE) + File.separator + "tmp.jpg"
    }

    @Deprecated(
        message = "Should use specific temp file or create a new file in StorageSubdirectory.CACHE instead",
        ReplaceWith(
            "getOdkDirPath(StorageSubdirectory.CACHE) + File.separator + \"tmp.mp4\""
        )
    )
    fun getTmpVideoFilePath(): String {
        return getOdkDirPath(StorageSubdirectory.CACHE) + File.separator + "tmp.mp4"
    }

    // Smap-specific methods

    /**
     * Get unscoped storage root directory path (legacy storage for fieldTask).
     * Returns the path to /sdcard/fieldTask
     */
    fun getUnscopedStorageRootDirPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath + File.separator + "fieldTask"
    }

    /**
     * Get unscoped storage directory path for a specific subdirectory.
     */
    fun getUnscopedStorageDirPath(subdirectory: StorageSubdirectory): String {
        return getUnscopedStorageRootDirPath() + File.separator + subdirectory.directoryName
    }

    /**
     * Get directory path for a specific subdirectory.
     * This is a convenience method that maps to getOdkDirPath for compatibility.
     */
    @JvmOverloads
    fun getDirPath(subdirectory: StorageSubdirectory, projectId: String? = null): String {
        return getOdkDirPath(subdirectory, projectId)
    }

    /**
     * Get the storage root directory path (scoped storage).
     */
    fun getStorageRootDirPath(): String {
        return odkRootDirPath
    }

    /**
     * Get custom splash screen image path.
     */
    fun getCustomSplashScreenImagePath(): String {
        return getStorageRootDirPath() + File.separator + "customSplashScreenImage.jpg"
    }

    /**
     * Get instance database path (relative path).
     */
    @JvmOverloads
    fun getInstanceDbPath(filePath: String, projectId: String? = null): String {
        return getDbPath(getDirPath(StorageSubdirectory.INSTANCES, projectId), filePath)
    }

    /**
     * Get absolute instance file path.
     */
    @JvmOverloads
    fun getAbsoluteInstanceFilePath(filePath: String, projectId: String? = null): String {
        return PathUtils.getAbsoluteFilePath(getDirPath(StorageSubdirectory.INSTANCES, projectId), filePath)
    }

    /**
     * Get absolute form file path.
     */
    @JvmOverloads
    fun getAbsoluteFormFilePath(filePath: String, projectId: String? = null): String {
        return PathUtils.getAbsoluteFilePath(getDirPath(StorageSubdirectory.FORMS, projectId), filePath)
    }

    /**
     * Get form database path (relative path).
     */
    @JvmOverloads
    fun getFormDbPath(filePath: String, projectId: String? = null): String {
        return getDbPath(getDirPath(StorageSubdirectory.FORMS, projectId), filePath)
    }

    /**
     * Get cache database path (relative path).
     */
    @JvmOverloads
    fun getCacheDbPath(filePath: String, projectId: String? = null): String {
        return getDbPath(getDirPath(StorageSubdirectory.CACHE, projectId), filePath)
    }

    /**
     * Get absolute cache file path.
     */
    @JvmOverloads
    fun getAbsoluteCacheFilePath(filePath: String, projectId: String? = null): String {
        return PathUtils.getAbsoluteFilePath(getDirPath(StorageSubdirectory.CACHE, projectId), filePath)
    }

    /**
     * Get database path (converts absolute to relative if needed).
     */
    private fun getDbPath(dirPath: String, filePath: String): String {
        val relativeFilePath: String
        if (filePath.startsWith(dirPath)) {
            relativeFilePath = PathUtils.getRelativeFilePath(dirPath, filePath)
        } else {
            relativeFilePath = filePath
        }
        return relativeFilePath
    }

    /**
     * Get relative map layer path.
     * Handles both scoped and unscoped storage paths, including legacy /sdcard/fieldTask/layers path.
     */
    @Suppress("PMD.DoNotHardCodeSDCard")
    @JvmOverloads
    fun getRelativeMapLayerPath(path: String?, projectId: String? = null): String? {
        if (path == null) {
            return null
        }
        // Handle legacy hardcoded path
        if (path.startsWith("/sdcard/fieldTask/layers")) {
            return path.substring("/sdcard/fieldTask/layers".length + 1)
        } else if (path.startsWith(getUnscopedStorageDirPath(StorageSubdirectory.LAYERS))) {
            return path.substring(getUnscopedStorageDirPath(StorageSubdirectory.LAYERS).length + 1)
        } else if (path.startsWith(getDirPath(StorageSubdirectory.LAYERS, projectId))) {
            return path.substring(getDirPath(StorageSubdirectory.LAYERS, projectId).length + 1)
        }
        return path
    }

    /**
     * Get absolute offline map layer path.
     */
    @JvmOverloads
    fun getAbsoluteOfflineMapLayerPath(path: String?, projectId: String? = null): String? {
        if (path == null) {
            return null
        }
        return getDirPath(StorageSubdirectory.LAYERS, projectId) + File.separator + getRelativeMapLayerPath(path, projectId)
    }

    // End Smap-specific methods

    override fun create(projectId: String): StoragePaths {
        return StoragePaths(getProjectRootDirPath(projectId),
            getOdkDirPath(StorageSubdirectory.FORMS, projectId),
            getOdkDirPath(StorageSubdirectory.INSTANCES, projectId),
            getOdkDirPath(StorageSubdirectory.CACHE, projectId),
            getOdkDirPath(StorageSubdirectory.METADATA, projectId),
            getOdkDirPath(StorageSubdirectory.SETTINGS, projectId),
            getOdkDirPath(StorageSubdirectory.LAYERS, projectId)
        )
    }
}
