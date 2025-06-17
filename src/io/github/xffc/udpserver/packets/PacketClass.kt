package io.github.xffc.udpserver.packets

import io.ktor.network.sockets.SocketAddress
import kotlinx.io.Source
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class PacketClass<T : Packet>(
    val id: Int,
    packet: KClass<T>
) {
    internal val constructor = packet.primaryConstructor!!

    fun build(source: Source, address: SocketAddress): T =
        constructor.call(address, *read(source))

    protected open fun read(source: Source): Array<Any> = emptyArray()
}