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
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
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
     * The fewest service jars we expect to scan (the repo has ~430). Without it, an empty classpath would make the
     * registration check pass with nothing scanned. It is a floor, not an exact count, so it need not change per new
     * service.
     */
    private static final int MINIMUM_EXPECTED_SERVICE_JARS = 400;

    @Test
    void everyServiceJar_registersAWarmUpProvider() throws IOException {
        List<JarFile> serviceJars = serviceJarsOnClasspath();

        List<String> jarsWithoutRegistration = serviceJars.stream()
                                                          .filter(jar -> jar.getEntry(PROVIDER_RESOURCE) == null)
                                                          .map(JarFile::getName)
                                                          .collect(Collectors.toList());

        assertThat(serviceJars)
            .as("service jars found on the classpath")
            .hasSizeGreaterThanOrEqualTo(MINIMUM_EXPECTED_SERVICE_JARS);
        assertThat(jarsWithoutRegistration)
            .as("every service jar must register a generated SdkWarmUpProvider in META-INF/services")
            .isEmpty();
    }

    private static List<JarFile> serviceJarsOnClasspath() throws IOException {
        List<JarFile> serviceJars = new ArrayList<>();
        Enumeration<URL> manifests = classLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            openJar(manifests.nextElement()).filter(WarmUpProviderCompletenessTest::isServiceJar)
                                            .ifPresent(serviceJars::add);
        }
        return serviceJars;
    }

    /**
     * The jar behind a classpath manifest URL, or empty for a non-jar URL (such as a directory on the classpath).
     * The returned {@link JarFile} is shared and cached by the JVM; the classloader owns it, so it must not be closed.
     */
    private static Optional<JarFile> openJar(URL manifest) {
        if (!"jar".equals(manifest.getProtocol())) {
            return Optional.empty();
        }
        try {
            return Optional.of(((JarURLConnection) manifest.openConnection()).getJarFile());
        } catch (IOException e) {
            // Surefire's transient booter jar can be deleted mid-scan; it is never a service jar, so skip it.
            return Optional.empty();
        }
    }

    private static boolean isServiceJar(JarFile jar) {
        return jar.stream()
                  .anyMatch(e -> e.getName().startsWith("software/amazon/awssdk/services/")
                                 && e.getName().endsWith(".class"));
    }

    private static ClassLoader classLoader() {
        return WarmUpProviderCompletenessTest.class.getClassLoader();
    }
}
