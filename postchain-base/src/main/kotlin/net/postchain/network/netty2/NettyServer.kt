package net.postchain.network.netty2

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.postchain.core.Shutdownable

class NettyServer(private val group: EventLoopGroup) : Shutdownable {

    private lateinit var server: ServerBootstrap
    private lateinit var bindFuture: ChannelFuture
    private lateinit var createChannelHandler: () -> ChannelHandler

    fun setChannelHandler(handlerFactory: () -> ChannelHandler) {
        this.createChannelHandler = handlerFactory
    }

    fun run(port: Int) {
        server = ServerBootstrap()
                .group(group)
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

    override fun shutdown() {
        bindFuture.channel().close()
        bindFuture.channel().closeFuture().sync()
    }
}