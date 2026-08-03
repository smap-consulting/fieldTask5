package org.odk.collect.maps.layers

import java.io.File

interface ReferenceLayerRepository {

    fun getAll(): List<ReferenceLayer>
    fun get(id: String): ReferenceLayer?
    fun addLayer(file: File, shared: Boolean)
    fun delete(id: String)
}

data class ReferenceLayer(val id: String, val file: File, val name: String)

/**
 * smap - Layers supplied by the server are kept in their own directory under the shared layers
 * directory.  Layer ids are the path relative to that directory, so a server supplied layer can
 * be recognised by its id.  Users can still add their own layers alongside them, which is the
 * only way to load a file too large to upload to the server.
 */
object ServerLayers {

    const val DIR = "server"

    private val PREFIX = DIR + File.separator

    @JvmStatic
    fun isServerLayer(layerId: String?) = layerId != null && layerId.startsWith(PREFIX)
}
