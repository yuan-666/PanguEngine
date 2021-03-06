package engine.server.network;

import engine.Platform;
import engine.util.Side;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkServer {
    private ChannelFuture future;

    private List<NetworkHandler> handlers = Collections.synchronizedList(new ArrayList<>());

    public void run(int port){
        run(null, port);
    }
    public void run(@Nullable InetAddress address, int port){
        Platform.getLogger().debug(String.format("Start launching netty server at %s:%d", address == null ? "*" : address.getHostAddress(), port));
        var bossGroup = new NioEventLoopGroup();
        var workerGroup = new NioEventLoopGroup();
        future = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        try {
                            ch.config().setOption(ChannelOption.TCP_NODELAY, true);
                        } catch (ChannelException var3) {

                        }
                        ch.pipeline().addLast("decoder", new PacketDecoder())
                                .addLast("encoder", new PacketEncoder());
                        var handler = new NetworkHandler(Side.SERVER);
                        handlers.add(handler);
                        ch.pipeline().addLast("handler", handler);
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true).bind(port).syncUninterruptibly();
        Platform.getLogger().debug(String.format("Launched netty server at %s:%d", address == null ? "*" : address.getHostAddress(), port));
    }

    public SocketAddress runLocal(){
        Platform.getLogger().debug("Start launching netty local server");
        var bossGroup = new NioEventLoopGroup();
        var workerGroup = new NioEventLoopGroup();
        future = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(LocalServerChannel.class)
                .childHandler(new ChannelInitializer<>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        var handler = new NetworkHandler(Side.SERVER);
                        handlers.add(handler);
                        ch.pipeline().addLast("handler", handler);
                    }
                }).bind(LocalAddress.ANY).syncUninterruptibly();
        Platform.getLogger().debug("Launched netty local server at %s:%d");
        return future.channel().localAddress();
    }

    public void tick(){
        for (NetworkHandler handler : handlers) {

        }
    }

    public void shutdown(){
        future.channel().close().syncUninterruptibly();
    }
}
