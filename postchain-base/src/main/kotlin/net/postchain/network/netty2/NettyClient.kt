package net.postchain.network.netty2

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import net.postchain.core.Shutdownable
import java.net.SocketAddress
import java.util.concurrent.TimeUnit

class NettyClient : Shutdownable {

    private lateinit var client: Bootstrap
    lateinit var connectFuture: ChannelFuture
    private lateinit var group: EventLoopGroup
    private lateinit var channelHandler: ChannelHandler

    fun setChannelHandler(channelHandler: ChannelHandler) {
        this.channelHandler = channelHandler
    }

    fun connect(peerAddress: SocketAddress) {
        group = NioEventLoopGroup()

        client = Bootstrap()
                .group(group)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline()
                                // inbound
                                .addLast(NettyCodecs.lengthFieldPrepender())
                                // outbound
                                .addLast(NettyCodecs.lengthFieldBasedFrameDecoder())
                                // app
                                .addLast(channelHandler)
                    }
                })

        connectFuture = client.connect(peerAddress).sync()
    }

    override fun shutdown() {
        connectFuture.channel().close()
        connectFuture.channel().closeFuture().sync()
        group.shutdownGracefully(0, 0, TimeUnit.SECONDS).sync()
    }
}