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

package software.amazon.awssdk.codegen.emitters;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.xmlServiceModels;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.poet.crac.WarmUpProviderSpec;

public class WarmUpProviderRegistrationTaskTest {

    private static final String SPI_PATH =
        "META-INF/services/software.amazon.awssdk.core.crac.SdkWarmUpProvider";

    private static final String QUERY_PROVIDER = new WarmUpProviderSpec(queryServiceModels()).className().toString();
    private static final String XML_PROVIDER = new WarmUpProviderSpec(xmlServiceModels()).className().toString();

    @TempDir
    Path resourcesDir;

    @Test
    public void writesProviderClassName() throws IOException {
        new WarmUpProviderRegistrationTask(resourcesDir.toString(), QUERY_PROVIDER).compute();

        List<String> lines = Files.readAllLines(resourcesDir.resolve(SPI_PATH), StandardCharsets.UTF_8);
        assertThat(lines).contains(QUERY_PROVIDER);
    }

    @Test
    public void appendsSecondProviderWithoutClobbering() throws IOException {
        new WarmUpProviderRegistrationTask(resourcesDir.toString(), QUERY_PROVIDER).compute();
        new WarmUpProviderRegistrationTask(resourcesDir.toString(), XML_PROVIDER).compute();

        List<String> lines = Files.readAllLines(resourcesDir.resolve(SPI_PATH), StandardCharsets.UTF_8);
        assertThat(lines).contains(QUERY_PROVIDER).contains(XML_PROVIDER);
    }

    @Test
    public void doesNotDuplicateOnSecondIdenticalRun() throws IOException {
        new WarmUpProviderRegistrationTask(resourcesDir.toString(), QUERY_PROVIDER).compute();
        new WarmUpProviderRegistrationTask(resourcesDir.toString(), QUERY_PROVIDER).compute();

        List<String> lines = Files.readAllLines(resourcesDir.resolve(SPI_PATH), StandardCharsets.UTF_8);
        long count = lines.stream().filter(l -> l.equals(QUERY_PROVIDER)).count();
        assertThat(count).isEqualTo(1);
    }
}
