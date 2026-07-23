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

package software.amazon.awssdk.warmup.allservices;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * Asserts that every service jar on the classpath registers a generated {@link SdkWarmUpProvider} in its
 * {@code META-INF/services} file.
 *
 * <p>{@link AllServicesWarmUpTest} and {@link SdkWarmUpPrimeAllServicesTest} find providers through
 * {@code ServiceLoader}, which only sees a provider if its service jar has the registration file. If a new service
 * ships without that file, those tests generate no case for it and stay green while the service has no warm-up
 * coverage. This test reads the jars directly, so it catches a missing registration.
 *
 * <p>Jars are enumerated with {@code ClassLoader.getResources}, not {@code java.class.path}: surefire runs the test
 * JVM with a manifest-only booter jar, so {@code java.class.path} lists only {@code surefirebooter*.jar} and scanning
 * it would find nothing.
 */
class WarmUpProviderCompletenessTest {

    private static final String PROVIDER_RESOURCE = "META-INF/services/software.amazon.awssdk.core.crac.SdkWarmUpProvider";

    /**
     * Floor well below the ~430 services on the classpath. Catches a classpath problem that drops most jars, which
     * would otherwise let the test pass without scanning anything.
     */
    private static final int SERVICE_JAR_FLOOR = 400;

    @Test
    void everyServiceJar_registersAWarmUpProvider() throws IOException {
        List<String> serviceJarsWithoutRegistration = new ArrayList<>();
        int serviceJarsScanned = 0;

        Enumeration<URL> manifests = classLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            URL manifest = manifests.nextElement();
            if (!"jar".equals(manifest.getProtocol())) {
                continue;
            }
            // Shared, JVM-cached JarFile - must NOT be closed, the classloader owns it.
            JarFile jar;
            try {
                jar = ((JarURLConnection) manifest.openConnection()).getJarFile();
            } catch (IOException e) {
                // Surefire's transient booter jar can be deleted mid-scan; it is never a service jar, so skip it.
                continue;
            }
            boolean isServiceJar = jar.stream()
                                      .anyMatch(e -> e.getName().startsWith("software/amazon/awssdk/services/")
                                                     && e.getName().endsWith(".class"));
            if (isServiceJar) {
                serviceJarsScanned++;
                if (jar.getEntry(PROVIDER_RESOURCE) == null) {
                    serviceJarsWithoutRegistration.add(jar.getName());
                }
            }
        }

        assertThat(serviceJarsScanned)
            .as("service jars found on the classpath")
            .isGreaterThanOrEqualTo(SERVICE_JAR_FLOOR);
        assertThat(serviceJarsWithoutRegistration)
            .as("every service jar must register a generated SdkWarmUpProvider in META-INF/services")
            .isEmpty();
    }

    private static ClassLoader classLoader() {
        return WarmUpProviderCompletenessTest.class.getClassLoader();
    }
}
