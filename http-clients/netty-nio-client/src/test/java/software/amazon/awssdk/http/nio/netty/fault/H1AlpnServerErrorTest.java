package software.amazon.awssdk.http.nio.netty.fault;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.SdkAsyncHttpClientH1TestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;

public class H1AlpnServerErrorTest extends SdkAsyncHttpClientH1TestSuite {

    @Override
    protected SdkAsyncHttpClient setupClient() {
        return NettyNioAsyncHttpClient.builder()
                                      .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(2).build())
                                      .protocol(Protocol.HTTP1_1)
                                      .protocolNegotiation(ProtocolNegotiation.ALPN)
                                      .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }
}