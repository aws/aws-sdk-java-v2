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

package software.amazon.awssdk.http.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.liveEventLoopGroups;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.newEventLoopGroups;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.net.ssl.SSLHandshakeException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Verifies {@code useTrustStoreSystemProperties} against an HTTPS server whose self-signed certificate is not in the OS trust
 * store, so a request succeeds only when the client trusts the trust store named by the JSSE system properties.
 */
abstract class CrtTrustStoreSystemPropertiesTestSuite<T extends SdkAutoCloseable> {
    private static final String TRUST_STORE = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    private static final String TRUST_STORE_PROVIDER = "javax.net.ssl.trustStoreProvider";
    private static final String[] TRUST_STORE_PROPERTIES = {TRUST_STORE, TRUST_STORE_PASSWORD, TRUST_STORE_TYPE,
                                                            TRUST_STORE_PROVIDER};
    private static final String PASSWORD = "changeit";
    private static final String STUB_BODY = "ok";
    // The CRT accepts at most 1024 certificates in a trust store, the s2n default chain limit.
    private static final int MAX_TRUST_STORE_CERTIFICATES = 1024;
    // Well above a loopback handshake (milliseconds) and well below s2n's 10s minimum blinding delay.
    protected static final Duration CONNECTION_ACQUISITION_TIMEOUT = Duration.ofSeconds(2);
    // AWS_ERROR_HTTP_CONNECTION_MANAGER_ACQUISITION_TIMEOUT
    private static final int CONNECTION_ACQUISITION_TIMEOUT_ERROR = 2093;

    private static Path tempDir;
    private static WireMockServer server;
    private static Path serverKeyStore;
    private static Path serverCertTrustStore;
    private static Path serverCertJksTrustStore;
    private static Path unrelatedCertTrustStore;
    private static Path emptyTrustStore;
    private static Path corruptTrustStore;
    private static Path oversizedTrustStore;

    private final Map<String, String> savedProperties = new HashMap<>();

    protected abstract T buildClient(Boolean useTrustStoreSystemProperties);

    protected abstract T buildClientTrustingAllCertificates(Boolean useTrustStoreSystemProperties);

    protected abstract T buildClientWithPrivateEventLoopGroup(Boolean useTrustStoreSystemProperties);

    protected abstract GetResponse executeGet(T client, URI uri) throws Exception;

    @BeforeAll
    static void setUpServer() throws Exception {
        tempDir = Files.createTempDirectory("crt-truststore-test");
        KeyPair serverKeyPair = generateKeyPair();
        X509Certificate serverCert = selfSignedCertificate(serverKeyPair, "localhost", 1);
        X509Certificate unrelatedCert = selfSignedCertificate(generateKeyPair(), "unrelated", 2);

        KeyStore keyStore = newKeyStore("PKCS12");
        keyStore.setKeyEntry("server", serverKeyPair.getPrivate(), PASSWORD.toCharArray(), new Certificate[] {serverCert});
        serverKeyStore = store(keyStore, "server.p12");

        serverCertTrustStore = store(trustStoreOf("PKCS12", serverCert), "server-cert.p12");
        serverCertJksTrustStore = store(trustStoreOf("JKS", serverCert), "server-cert.jks");
        unrelatedCertTrustStore = store(trustStoreOf("PKCS12", unrelatedCert), "unrelated-cert.p12");
        emptyTrustStore = store(trustStoreOf("PKCS12"), "empty.p12");
        corruptTrustStore = Files.write(tempDir.resolve("corrupt.p12"), "not a keystore".getBytes(UTF_8));

        KeyPair bulkKeyPair = generateKeyPair();
        X509Certificate[] bulkCerts = new X509Certificate[MAX_TRUST_STORE_CERTIFICATES + 1];
        for (int i = 0; i < bulkCerts.length; i++) {
            bulkCerts[i] = selfSignedCertificate(bulkKeyPair, "bulk" + i, 100 + i);
        }
        oversizedTrustStore = store(trustStoreOf("PKCS12", bulkCerts), "oversized.p12");

        server = new WireMockServer(wireMockConfig().dynamicPort()
                                                    .dynamicHttpsPort()
                                                    .keystorePath(serverKeyStore.toString())
                                                    .keystorePassword(PASSWORD)
                                                    .keyManagerPassword(PASSWORD)
                                                    .keystoreType("PKCS12"));
        server.start();
        server.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200).withBody(STUB_BODY)));
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        server.stop();
        try (Stream<Path> files = Files.list(tempDir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Files.delete(file);
            }
        }
        Files.delete(tempDir);
    }

    @BeforeEach
    void clearTrustStoreProperties() {
        for (String property : TRUST_STORE_PROPERTIES) {
            savedProperties.put(property, System.getProperty(property));
            System.clearProperty(property);
        }
    }

    @AfterEach
    void restoreTrustStoreProperties() {
        savedProperties.forEach((property, value) -> {
            if (value == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, value);
            }
        });
    }

    @Test
    void useTrustStoreSystemProperties_trustStoreHasServerCertificate_requestSucceeds() throws Exception {
        setTrustStoreProperties(serverCertTrustStore, PASSWORD);

        try (T client = buildClient(true)) {
            assertStubResponse(executeGet(client, serverUri()));
        }
    }

    @Test
    void useTrustStoreSystemProperties_trustStoreHasServerKeyEntry_requestSucceeds() throws Exception {
        setTrustStoreProperties(serverKeyStore, PASSWORD);

        try (T client = buildClient(true)) {
            assertStubResponse(executeGet(client, serverUri()));
        }
    }

    @Test
    void useTrustStoreSystemProperties_trustStoreTypeSet_requestSucceeds() throws Exception {
        setTrustStoreProperties(serverCertJksTrustStore, PASSWORD);
        System.setProperty(TRUST_STORE_TYPE, "JKS");

        try (T client = buildClient(true)) {
            assertStubResponse(executeGet(client, serverUri()));
        }
    }

    @Test
    void useTrustStoreSystemProperties_trustStoreLacksServerCertificate_requestFailsHandshake() throws Exception {
        setTrustStoreProperties(unrelatedCertTrustStore, PASSWORD);

        try (T client = buildClient(true)) {
            assertHandshakeRejected(() -> executeGet(client, serverUri()));
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void useTrustStoreSystemProperties_notEnabled_ignoresTrustStoreProperties(Boolean useTrustStoreSystemProperties)
        throws Exception {
        setTrustStoreProperties(serverCertTrustStore, PASSWORD);

        try (T client = buildClient(useTrustStoreSystemProperties)) {
            assertHandshakeRejected(() -> executeGet(client, serverUri()));
        }
    }

    // These cannot tell the OS trust store apart from another default such as the JDK cacerts, since neither contains the
    // self-signed server certificate. They verify that build() succeeds and no store containing that certificate is loaded.
    @Test
    void useTrustStoreSystemProperties_trustStorePropertyUnset_buildSucceedsAndDoesNotTrustUnlistedCertificate()
        throws Exception {
        assertBuildSucceedsAndDoesNotTrustServerCertificate();
    }

    @Test
    void useTrustStoreSystemProperties_trustStorePropertyEmpty_buildSucceedsAndDoesNotTrustUnlistedCertificate()
        throws Exception {
        System.setProperty(TRUST_STORE, "");

        assertBuildSucceedsAndDoesNotTrustServerCertificate();
    }

    private void assertBuildSucceedsAndDoesNotTrustServerCertificate() throws Exception {
        try (T client = buildClient(true)) {
            assertHandshakeRejected(() -> executeGet(client, serverUri()));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("trustStoreLoadFailures")
    void useTrustStoreSystemProperties_trustStoreLoadFails_buildThrowsIllegalStateExceptionNamingTrustStore(
        String description, String trustStore, String password, String type, String provider,
        Class<? extends Throwable> expectedCause) {
        setTrustStoreProperties(trustStore, password, type, provider);

        assertThatThrownBy(() -> buildClient(true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(TRUST_STORE)
            .hasMessageContaining(trustStore)
            .hasCauseInstanceOf(expectedCause);
    }

    private static Stream<Arguments> trustStoreLoadFailures() {
        return Stream.of(
            Arguments.of("file does not exist", missingTrustStore(), PASSWORD, null, null, CrtRuntimeException.class),
            Arguments.of("wrong password", serverCertTrustStore.toString(), "wrong", null, null, CrtRuntimeException.class),
            Arguments.of("corrupt file", corruptTrustStore.toString(), PASSWORD, null, null, CrtRuntimeException.class),
            Arguments.of("type unavailable", serverCertTrustStore.toString(), PASSWORD, "NO-SUCH-TYPE", null,
                         CrtRuntimeException.class),
            Arguments.of("provider unavailable", serverCertTrustStore.toString(), PASSWORD, null, "NoSuchProvider",
                         CrtRuntimeException.class),
            Arguments.of("NONE", "NONE", null, null, null, IllegalArgumentException.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("trustStoreRejections")
    void useTrustStoreSystemProperties_trustStoreRejected_buildThrowsIllegalStateException(
        String description, String trustStore, Class<? extends Throwable> expectedCause) {
        setTrustStoreProperties(trustStore, PASSWORD, null, null);

        assertThatThrownBy(() -> buildClient(true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(TRUST_STORE)
            .hasCauseInstanceOf(expectedCause);
    }

    private static Stream<Arguments> trustStoreRejections() {
        return Stream.of(
            Arguments.of("whitespace-only path", " ", CrtRuntimeException.class),
            Arguments.of("no certificates", emptyTrustStore.toString(), IllegalArgumentException.class),
            Arguments.of("more certificates than the CRT accepts", oversizedTrustStore.toString(),
                         IllegalArgumentException.class));
    }

    @Test
    void useTrustStoreSystemProperties_trustStoreLoadFails_doesNotLeakPrivateEventLoopGroup() {
        // Creates the shared static default event-loop group up front so the before/after diff only sees private groups.
        buildClient(null).close();
        Set<CrtResource> before = liveEventLoopGroups();
        System.setProperty(TRUST_STORE, missingTrustStore());

        assertThatThrownBy(() -> buildClientWithPrivateEventLoopGroup(true)).isInstanceOf(IllegalStateException.class);

        assertThat(newEventLoopGroups(before)).isEmpty();
    }

    @Test
    void useTrustStoreSystemProperties_trustAllCertificates_doesNotReadTrustStore() throws Exception {
        System.setProperty(TRUST_STORE, missingTrustStore());

        try (T client = buildClientTrustingAllCertificates(true)) {
            assertStubResponse(executeGet(client, serverUri()));
        }
    }

    @Test
    void useTrustStoreSystemProperties_propertiesChangedAfterBuild_keepsTrustStoreReadAtBuild() throws Exception {
        setTrustStoreProperties(serverCertTrustStore, PASSWORD);

        try (T client = buildClient(true)) {
            setTrustStoreProperties(unrelatedCertTrustStore, PASSWORD);

            assertStubResponse(executeGet(client, serverUri()));
        }
    }

    private static void assertStubResponse(GetResponse response) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(STUB_BODY);
    }

    // s2n reports a rejected certificate only after its 10-30s blinding delay, so the acquisition timeout can fire first.
    private static void assertHandshakeRejected(ThrowingCallable request) {
        assertThatThrownBy(request).satisfiesAnyOf(
            e -> assertThat(e).isInstanceOf(SSLHandshakeException.class),
            e -> {
                assertThat(e).isInstanceOf(IOException.class).hasCauseInstanceOf(HttpException.class);
                assertThat(((HttpException) e.getCause()).getErrorCode()).isEqualTo(CONNECTION_ACQUISITION_TIMEOUT_ERROR);
            });
    }

    private static URI serverUri() {
        return URI.create("https://localhost:" + server.httpsPort());
    }

    private static void setTrustStoreProperties(Path trustStore, String password) {
        System.setProperty(TRUST_STORE, trustStore.toString());
        System.setProperty(TRUST_STORE_PASSWORD, password);
    }

    private static String missingTrustStore() {
        return tempDir.resolve("missing.p12").toString();
    }

    private static void setTrustStoreProperties(String trustStore, String password, String type, String provider) {
        System.setProperty(TRUST_STORE, trustStore);
        setIfNotNull(TRUST_STORE_PASSWORD, password);
        setIfNotNull(TRUST_STORE_TYPE, type);
        setIfNotNull(TRUST_STORE_PROVIDER, provider);
    }

    private static void setIfNotNull(String property, String value) {
        if (value != null) {
            System.setProperty(property, value);
        }
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static X509Certificate selfSignedCertificate(KeyPair keyPair, String commonName, long serialNumber)
        throws Exception {
        Instant now = Instant.now();
        X500Name name = new X500Name("CN=" + commonName);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(serialNumber),
                                                                           Date.from(now.minus(1, DAYS)),
                                                                           Date.from(now.plus(1, DAYS)),
                                                                           name, keyPair.getPublic());
        builder.addExtension(Extension.subjectAlternativeName, false,
                             new GeneralNames(new GeneralName[] {new GeneralName(GeneralName.dNSName, "localhost"),
                                                                 new GeneralName(GeneralName.iPAddress, "127.0.0.1")}));
        builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private static KeyStore newKeyStore(String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(null, null);
        return keyStore;
    }

    private static KeyStore trustStoreOf(String type, X509Certificate... certificates) throws Exception {
        KeyStore keyStore = newKeyStore(type);
        for (int i = 0; i < certificates.length; i++) {
            keyStore.setCertificateEntry("cert" + i, certificates[i]);
        }
        return keyStore;
    }

    private static Path store(KeyStore keyStore, String fileName) throws Exception {
        Path file = tempDir.resolve(fileName);
        try (OutputStream out = Files.newOutputStream(file)) {
            keyStore.store(out, PASSWORD.toCharArray());
        }
        return file;
    }

    protected static final class GetResponse {
        private final int statusCode;
        private final String body;

        GetResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        int statusCode() {
            return statusCode;
        }

        String body() {
            return body;
        }
    }
}
