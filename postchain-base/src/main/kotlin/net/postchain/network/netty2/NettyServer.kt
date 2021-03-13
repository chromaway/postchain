// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import mu.KLogging
import java.util.concurrent.TimeUnit

class NettyServer {

    companion object: KLogging()

    private lateinit var server: ServerBootstrap
    private lateinit var bindFuture: ChannelFuture
    private lateinit var createChannelHandler: () -> ChannelHandler
    private lateinit var eventLoopGroup: EventLoopGroup

    fun setChannelHandler(handlerFactory: () -> ChannelHandler) {
        this.createChannelHandler = handlerFactory
    }

    fun run(port: Int) {
        eventLoopGroup = NioEventLoopGroup(1, DefaultThreadFactory("NettyServer"))

        server = ServerBootstrap()
                .group(eventLoopGroup)
                .channel(NioServerSocketChannel::class.java)
//                .option(ChannelOption.SO_BACKLOG, 10)
//                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline()
                                // inbound
                                .addLast(NettyCodecs.lengthFieldPrepender())
                                // outbound
                                .addLast(NettyCodecs.lengthFieldBasedFrameDecoder())
                                // app
                                .addLast(createChannelHandler())
                    }
                })

        bindFuture = server.bind(port).sync()
    }

    fun shutdown() {
        logger.debug("Shutting down NettyServer")
        try {
            eventLoopGroup.shutdownGracefully(0, 2000, TimeUnit.MILLISECONDS).sync()
            logger.debug("Shutting down NettyServer done")
        } catch (t: Throwable) {
            logger.debug("Shutting down NettyServer failed", t)
        }
    }
}