package com.github.niuhf0452.exile.web.serialization

import com.github.niuhf0452.exile.web.MediaType
import com.github.niuhf0452.exile.web.WebEntitySerializer
import kotlinx.serialization.modules.EmptyModule
import java.util.*

object EntitySerializers {
    private val serializers = ServiceLoader.load(WebEntitySerializer.Factory::class.java)
            .map { it.createSerializer(EmptyModule) }

    fun getSerializer(mediaType: MediaType): WebEntitySerializer? {
        serializers.forEach { s ->
            s.mediaTypes.forEach { m ->
                if (m.isAcceptable(mediaType)) {
                    return s
                }
            }
        }
        return null
    }

    fun acceptSerializer(acceptTypes: List<MediaType>): Pair<MediaType, WebEntitySerializer>? {
        serializers.forEach { s ->
            s.mediaTypes.forEach { m ->
                acceptTypes.forEach { a ->
                    if (a.isAcceptable(m)) {
                        return m to s
                    }
                }
            }
        }
        return null
    }
}