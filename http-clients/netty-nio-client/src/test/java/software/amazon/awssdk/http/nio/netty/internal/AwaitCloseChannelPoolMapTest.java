/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.nio.netty.internal;


import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kerby.kerberos.kerb.client.KrbClient;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.kerby.kerberos.kerb.type.ticket.TgtTicket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.nio.netty.ProxyAuthScheme;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.RecordingNetworkTrafficListener;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;

public class AwaitCloseChannelPoolMapTest {
    private static final String KRB5_PROP = "java.security.krb5.conf";
    private static final RecordingNetworkTrafficListener recorder = new RecordingNetworkTrafficListener();

    private static WireMockServer mockProxy;

    private static Path tempDir;
    private static Path keytabFile;
    private static Path ccacheFile;
    private static int port;

    private static SimpleKdcServer kdc;
    private static String krb5PropSave;

    private static Configuration negotiateAuthConfig;

    private AwaitCloseChannelPoolMap channelPoolMap;

    @BeforeAll
    public static void setup() throws Exception {
        mockProxy = new WireMockServer(wireMockConfig().dynamicPort().networkTrafficListener(recorder));
        mockProxy.start();

        setupMockKerberos();
    }

    @AfterAll
    public static void teardown() throws Exception {
        if (krb5PropSave != null) {
            System.setProperty(KRB5_PROP, krb5PropSave);
        } else {
            System.clearProperty(KRB5_PROP);
        }
        mockProxy.stop();
        kdc.stop();
    }

    @AfterEach
    public void methodTeardown() {
        if (channelPoolMap != null) {
            channelPoolMap.close();
        }
        channelPoolMap = null;

        recorder.reset();
    }

    private static void setupMockKerberos() throws Exception {
        tempDir = Files.createTempDirectory(null);
        keytabFile = tempDir.resolve("keytab");
        ccacheFile = tempDir.resolve("ccache");

        try (Socket freePort = new Socket()) {
            freePort.setReuseAddress(true);
            freePort.bind(new InetSocketAddress(0));
            port = freePort.getLocalPort();

            kdc = new SimpleKdcServer();
            kdc.setKdcRealm("EXAMPLE.COM");
            kdc.setKdcHost("localhost");
            kdc.setWorkDir(tempDir.toFile());
            kdc.setKdcTcpPort(port);
            kdc.setAllowUdp(false);
            kdc.init();

            krb5PropSave = System.getProperty(KRB5_PROP);

            System.setProperty(KRB5_PROP, tempDir.resolve("krb5.conf").toAbsolutePath().toString());
            kdc.start();

            kdc.createPrincipal("alice@EXAMPLE.COM", "alicePassword");
            kdc.createAndExportPrincipals(keytabFile.toFile(), "HTTP/localhost@EXAMPLE.COM");

            // initialize the ticket cache
            KrbClient krbClient = kdc.getKrbClient();
            TgtTicket tgt = krbClient.requestTgt("alice@EXAMPLE.COM", "alicePassword");
            krbClient.storeTicket(tgt, ccacheFile.toFile());

            // Override config so we look at the testing cache instead of the real system cache
            negotiateAuthConfig = new Configuration() {
                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    Map<String, String> opts = new HashMap<>();
                    opts.put("useTicketCache", "true");
                    opts.put("ticketCache", ccacheFile.toAbsolutePath().toString());
                    opts.put("refreshKrb5Config", "true");
                    opts.put("doNotPrompt", "true");
                    return new AppConfigurationEntry[] {
                        new AppConfigurationEntry(
                            "com.sun.security.auth.module.Krb5LoginModule",
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, opts)
                    };
                }
            };
        }
    }

    @Test
    public void close_underlyingPoolsShouldBeClosed() {
        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                                                 .sdkChannelOptions(new SdkChannelOptions())
                                                 .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                 .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                                 .protocol(Protocol.HTTP1_1)
                                                 .maxStreams(100)
                                                 .sslProvider(SslProvider.OPENSSL)
                                                 .build();

        int numberOfChannelPools = 5;
        List<SimpleChannelPoolAwareChannelPool> channelPools = new ArrayList<>();

        for (int i = 0; i < numberOfChannelPools; i++) {
            channelPools.add(
                channelPoolMap.get(URI.create("http://" + RandomStringUtils.randomAlphabetic(2) + i + "localhost:" + numberOfChannelPools)));
        }

        assertThat(channelPoolMap.pools().size()).isEqualTo(numberOfChannelPools);
        channelPoolMap.close();
        channelPools.forEach(channelPool -> {
            assertThat(channelPool.underlyingSimpleChannelPool().closeFuture()).isDone();
            assertThat(channelPool.underlyingSimpleChannelPool().closeFuture().join()).isTrue();
        });
    }

    @Test
    public void get_callsInjectedBootstrapProviderCorrectly() {
        BootstrapProvider bootstrapProvider = Mockito.spy(
            new BootstrapProvider(SdkEventLoopGroup.builder().build(),
                                  new NettyConfiguration(GLOBAL_HTTP_DEFAULTS),
                                  new SdkChannelOptions()));

        URI targetUri = URI.create("https://some-awesome-service-1234.amazonaws.com:8080");

        AwaitCloseChannelPoolMap.Builder builder =
            AwaitCloseChannelPoolMap.builder()
                                    .sdkChannelOptions(new SdkChannelOptions())
                                    .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                    .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                    .protocol(Protocol.HTTP1_1)
                                    .maxStreams(100)
                                    .sslProvider(SslProvider.OPENSSL);

        channelPoolMap = new AwaitCloseChannelPoolMap(builder, null, bootstrapProvider);
        channelPoolMap.get(targetUri);

        verify(bootstrapProvider).createBootstrap("some-awesome-service-1234.amazonaws.com", 8080, null);
    }

    @Test
    public void get_usingProxy_callsInjectedBootstrapProviderCorrectly() {
        BootstrapProvider bootstrapProvider = Mockito.spy(
            new BootstrapProvider(SdkEventLoopGroup.builder().build(),
                                  new NettyConfiguration(GLOBAL_HTTP_DEFAULTS),
                                  new SdkChannelOptions()));

        URI targetUri = URI.create("https://some-awesome-service-1234.amazonaws.com:8080");
        Map<URI, Boolean> shouldProxyCache = new HashMap<>();
        shouldProxyCache.put(targetUri, true);

        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .host("localhost")
                              .port(mockProxy.port())
                              .build();

        AwaitCloseChannelPoolMap.Builder builder =
            AwaitCloseChannelPoolMap.builder()
                                    .proxyConfiguration(proxyConfiguration)
                                    .sdkChannelOptions(new SdkChannelOptions())
                                    .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                    .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                    .protocol(Protocol.HTTP1_1)
                                    .maxStreams(100)
                                    .sslProvider(SslProvider.OPENSSL);

        channelPoolMap = new AwaitCloseChannelPoolMap(builder, shouldProxyCache, bootstrapProvider);
        channelPoolMap.get(targetUri);

        verify(bootstrapProvider).createBootstrap("localhost", mockProxy.port(), null);
    }

    @Test
    public void usingProxy_usesCachedValueWhenPresent() {
        URI targetUri = URI.create("https://some-awesome-service-1234.amazonaws.com");

        Map<URI, Boolean> shouldProxyCache = new HashMap<>();
        shouldProxyCache.put(targetUri, true);

        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                                                                  .host("localhost")
                                                                  .port(mockProxy.port())
                                                                  // Deliberately set the target host as a non-proxy host to
                                                                  // see if it will check the cache first
                                                                  .nonProxyHosts(Stream.of(targetUri.getHost()).collect(Collectors.toSet()))
                                                                  .build();

        AwaitCloseChannelPoolMap.Builder builder = AwaitCloseChannelPoolMap.builder()
                                                                           .proxyConfiguration(proxyConfiguration)
                                                                           .sdkChannelOptions(new SdkChannelOptions())
                                                                           .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                                           .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                                                           .protocol(Protocol.HTTP1_1)
                                                                           .maxStreams(100)
                                                                           .sslProvider(SslProvider.OPENSSL);

        channelPoolMap = new AwaitCloseChannelPoolMap(builder, shouldProxyCache, null);

        // The target host does not exist so acquiring a channel should fail unless we're configured to connect to
        // the mock proxy host for this URI.
        SimpleChannelPoolAwareChannelPool channelPool = channelPoolMap.newPool(targetUri);
        Future<Channel> channelFuture = channelPool.underlyingSimpleChannelPool().acquire().awaitUninterruptibly();
        assertThat(channelFuture.isSuccess()).isTrue();
        channelPool.release(channelFuture.getNow()).awaitUninterruptibly();
    }

    @Test
    public void usingProxy_noSchemeGiven_defaultsToHttp() {
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                                                                  .host("localhost")
                                                                  .port(mockProxy.port())
                                                                  .build();

        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                                                 .proxyConfiguration(proxyConfiguration)
                                                 .sdkChannelOptions(new SdkChannelOptions())
                                                 .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                 .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                                 .protocol(Protocol.HTTP1_1)
                                                 .maxStreams(100)
                                                 .sslProvider(SslProvider.OPENSSL)
                                                 .build();

        SimpleChannelPoolAwareChannelPool simpleChannelPoolAwareChannelPool = channelPoolMap.newPool(
            URI.create("https://some-awesome-service:443"));

        simpleChannelPoolAwareChannelPool.acquire().awaitUninterruptibly();

        String requests = recorder.requests().toString();

        assertThat(requests).contains("CONNECT some-awesome-service:443");
    }

    @ParameterizedTest
    @MethodSource("proxyAuthTestParams")
    public void usingProxy_authHeaderCorrect(ProxyAuthScheme authScheme, String username, String password,
                                             String proxyAuthHeader) {
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                                                                  .host("localhost")
                                                                  .port(mockProxy.port())
                                                                  .proxyAuthScheme(authScheme)
                                                                  .username(username)
                                                                  .password(password)
                                                                  .build();

        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                                                 .proxyConfiguration(proxyConfiguration)
                                                 .sdkChannelOptions(new SdkChannelOptions())
                                                 .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                 .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                                 .protocol(Protocol.HTTP1_1)
                                                 .maxStreams(100)
                                                 .sslProvider(SslProvider.OPENSSL)
                                                 .negotiateAuthConfig(negotiateAuthConfig)
                                                 .build();

        SimpleChannelPoolAwareChannelPool simpleChannelPoolAwareChannelPool = channelPoolMap.newPool(
            URI.create("https://some-awesome-service:443"));

        simpleChannelPoolAwareChannelPool.acquire().awaitUninterruptibly();

        String requests = recorder.requests().toString();

        assertThat(requests).contains("CONNECT some-awesome-service:443");

        if (proxyAuthHeader == null) {
            assertThat(requests).doesNotContain("proxy-authorization:");
        } else {
            assertThat(requests).contains(String.format("proxy-authorization: %s", proxyAuthHeader));
        }
    }

    @Test
    public void usesProvidedKeyManagersProvider() {
        TlsKeyManagersProvider provider = mock(TlsKeyManagersProvider.class);

        AttributeMap config = AttributeMap.builder()
                                          .put(TLS_KEY_MANAGERS_PROVIDER, provider)
                                          .build();

        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                                                 .sdkChannelOptions(new SdkChannelOptions())
                                                 .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                 .protocol(Protocol.HTTP1_1)
                                                 .configuration(new NettyConfiguration(config.merge(GLOBAL_HTTP_DEFAULTS)))
                                                 .build();

        ChannelPool channelPool = channelPoolMap.newPool(URI.create("https://localhost:" + mockProxy.port()));
        channelPool.acquire().awaitUninterruptibly();
        verify(provider).keyManagers();
    }

    @Test
    public void acquireChannel_autoReadDisabled() {
        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                                                 .sdkChannelOptions(new SdkChannelOptions())
                                                 .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                 .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                                 .protocol(Protocol.HTTP1_1)
                                                 .protocolNegotiation(ProtocolNegotiation.ASSUME_PROTOCOL)
                                                 .maxStreams(100)
                                                 .sslProvider(SslProvider.OPENSSL)
                                                 .build();

        ChannelPool channelPool = channelPoolMap.newPool(URI.create("https://localhost:" + mockProxy.port()));

        Channel channel = channelPool.acquire().awaitUninterruptibly().getNow();

        assertThat(channel.config().isAutoRead()).isFalse();
    }

    @Test
    public void releaseChannel_autoReadEnabled() {
        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                                                 .sdkChannelOptions(new SdkChannelOptions())
                                                 .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                 .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                                 .protocol(Protocol.HTTP1_1)
                                                 .protocolNegotiation(ProtocolNegotiation.ASSUME_PROTOCOL)
                                                 .maxStreams(100)
                                                 .sslProvider(SslProvider.OPENSSL)
                                                 .build();

        ChannelPool channelPool = channelPoolMap.newPool(URI.create("https://localhost:" + mockProxy.port()));

        Channel channel = channelPool.acquire().awaitUninterruptibly().getNow();

        channelPool.release(channel).awaitUninterruptibly();

        assertThat(channel.config().isAutoRead()).isTrue();
    }

    private static Stream<Arguments> proxyAuthTestParams() {
        return Stream.of(
            Arguments.of(null, null, null, null),
            Arguments.of(null, "user", "pass", "Basic dXNlcjpwYXNz"),
            Arguments.of(ProxyAuthScheme.BASIC, "user", "pass", "Basic dXNlcjpwYXNz"),
            Arguments.of(ProxyAuthScheme.NEGOTIATE, null, null, "Negotiate YII"),
            Arguments.of(ProxyAuthScheme.NEGOTIATE, "user", "pass", "Negotiate YII")

        );
    }
}
