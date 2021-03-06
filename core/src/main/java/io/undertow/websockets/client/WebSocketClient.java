package io.undertow.websockets.client;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSocketVersion;
import org.xnio.ChannelListener;
import org.xnio.FutureResult;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.http.HttpUpgrade;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * The Web socket client.
 *
 * @author Stuart Douglas
 */
public class WebSocketClient {


    public static IoFuture<WebSocketChannel> connect(XnioWorker worker, final Pool<ByteBuffer> bufferPool, final OptionMap optionMap, final URI uri, WebSocketVersion version) {
        final FutureResult<WebSocketChannel> ioFuture = new FutureResult<WebSocketChannel>();
        final URI newUri;
        try {
            newUri = new URI(uri.getScheme().equals("wss") ? "https" : "http", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath().isEmpty() ? "/" : uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final WebSocketClientHandshake handshake = WebSocketClientHandshake.create(WebSocketVersion.V13, newUri);
        final Map<String, String> headers = handshake.createHeaders();
        IoFuture<StreamConnection> result = HttpUpgrade.performUpgrade(worker, null, newUri, headers, new ChannelListener<StreamConnection>() {
            @Override
            public void handleEvent(StreamConnection channel) {
                WebSocketChannel result = handshake.createChannel(channel, newUri.toString(), bufferPool);
                ioFuture.setResult(result);
            }
        }, null, optionMap, handshake.handshakeChecker(newUri, headers));
        result.addNotifier(new IoFuture.Notifier<StreamConnection, Object>() {
            @Override
            public void notify(IoFuture<? extends StreamConnection> res, Object attachment) {
                if (res.getStatus() == IoFuture.Status.FAILED) {
                    ioFuture.setException(res.getException());
                }
            }
        }, null);
        return ioFuture.getIoFuture();
    }


    private WebSocketClient() {

    }
}
