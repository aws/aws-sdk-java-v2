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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.client.KrbClient;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.kerby.kerberos.kerb.type.ticket.TgtTicket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.testutils.FileUtils;

public class NegotiateProxyAuthGeneratorTest {
    private static final String KRB5_PROP = "java.security.krb5.conf";
    private static final String EXECUTOR_THREAD_NAME = "test-proxy-auth";
    private static final ExecutorService executor =
        Executors.newSingleThreadExecutor(r -> new Thread(r, EXECUTOR_THREAD_NAME));
    private static Path tempDir;
    private static Path keytabFile;
    private static Path ccacheFile;
    private static int port;

    private static SimpleKdcServer kdc;
    private static String krb5PropSave;

    private static Configuration config;

    @BeforeAll
    static void setup() throws IOException, KrbException {
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
            config = new Configuration() {
                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    Map<String, String> opts = new HashMap<>();
                    opts.put("useTicketCache", "true");
                    opts.put("ticketCache", ccacheFile.toAbsolutePath().toString());
                    opts.put("doNotPrompt", "true");
                    opts.put("refreshKrb5Config", "true");
                    return new AppConfigurationEntry[] {
                        new AppConfigurationEntry(
                            "com.sun.security.auth.module.Krb5LoginModule",
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, opts)
                    };
                }
            };
        }

    }

    @AfterAll
    static void teardown() throws KrbException {
        executor.shutdownNow();
        if (krb5PropSave != null) {
            System.setProperty(KRB5_PROP, krb5PropSave);
        } else {
            System.clearProperty(KRB5_PROP);
        }
        kdc.stop();
        FileUtils.cleanUpTestDirectory(tempDir);
    }

    @Test
    void generateAuthParams_configValid_successfullyGeneratesToken() {
        NegotiateProxyAuthGenerator authGenerator = new NegotiateProxyAuthGenerator(config, executor);

        URI proxyEndpoint = URI.create("https://localhost:8192");

        assertThat(authGenerator.generateAuthParams(proxyEndpoint).join()).startsWith("YII");
    }

    @Test
    void generateAuthParams_ticketCacheMissing_failsWithActionableMessage() {
        Configuration missingCacheConfig = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, String> opts = new HashMap<>();
                opts.put("useTicketCache", "true");
                opts.put("ticketCache", tempDir.resolve("nonexistent-cache").toAbsolutePath().toString());
                opts.put("doNotPrompt", "true");
                opts.put("refreshKrb5Config", "true");
                return new AppConfigurationEntry[] {
                    new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, opts)
                };
            }
        };

        NegotiateProxyAuthGenerator authGenerator = new NegotiateProxyAuthGenerator(missingCacheConfig, executor);

        assertThatThrownBy(() -> authGenerator.generateAuthParams(URI.create("https://localhost:8192")).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("kinit")
            .hasMessageContaining("ticket cache");
    }

    @Test
    void generateAuthParams_runsOnSuppliedExecutor() {
        AtomicReference<Thread> loginThread = new AtomicReference<>();
        Configuration recordingConfig = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                loginThread.set(Thread.currentThread());
                return config.getAppConfigurationEntry(name);
            }
        };

        new NegotiateProxyAuthGenerator(recordingConfig, executor).generateAuthParams(URI.create("https://localhost:8192"))
                                                                 .join();

        // The blocking Kerberos work must never run on the caller's thread, which in production is a Netty event loop.
        assertThat(loginThread.get()).isNotSameAs(Thread.currentThread());
        assertThat(loginThread.get().getName()).isEqualTo(EXECUTOR_THREAD_NAME);
    }

    @Test
    void constructor_nullExecutor_throws() {
        assertThatThrownBy(() -> new NegotiateProxyAuthGenerator(config, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("executor");
    }

}
