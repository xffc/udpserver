package io.github.xffc.udpserver.packets

import io.ktor.network.sockets.SocketAddress
import kotlinx.io.Buffer
import kotlin.reflect.full.companionObjectInstance

interface Packet {
    val address: SocketAddress

    fun write(buffer: Buffer)

    fun getClass(): PacketClass<*> =
        this::class.companionObjectInstance as PacketClass<*>

    companion object {
        val registry = mutableListOf<PacketClass<*>>()
    }
}
