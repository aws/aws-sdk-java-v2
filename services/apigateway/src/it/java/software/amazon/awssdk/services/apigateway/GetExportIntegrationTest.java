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

package software.amazon.awssdk.services.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.testutils.Waiter;

/**
 * Integration tests for {@link software.amazon.awssdk.services.apigateway.model.GetExportRequest}.
 */
@Ignore // Running during release causing TooManyRequests exception
public class GetExportIntegrationTest extends IntegrationTestBase {
    private static final String STAGE = "Alpha";

    private static SdkBytes restApi;
    private static String restApiId;
    private static String deploymentId;

    @BeforeClass
    public static void setUp() throws IOException {
        IntegrationTestBase.setUp();
        restApi = SdkBytes.fromInputStream(GetExportIntegrationTest.class.getResourceAsStream("/PetStore-Alpha-swagger-apigateway.json"));
        restApiId = apiGateway.importRestApi(r -> r.body(restApi).failOnWarnings(false)).id();
        deploymentId = apiGateway.createDeployment(r -> r.stageName(STAGE).restApiId(restApiId)).id();
    }

    @AfterClass
    public static void teardown() {
        Waiter.run(() -> apiGateway.deleteStage(r -> r.restApiId(restApiId).stageName(STAGE))).orFail();
        Waiter.run(() -> apiGateway.deleteDeployment(r -> r.deploymentId(deploymentId).restApiId(restApiId))).orFail();
        Waiter.run(() -> apiGateway.deleteRestApi(r -> r.restApiId(restApiId))).orFail();
    }

    @Test
    public void exportAsJson() {
        String bodyAsString = getApiExport("application/json");
        assertThat(bodyAsString).startsWith("{").endsWith("}");
    }

    @Test
    public void exportAsYaml() {
        String bodyAsString = getApiExport("application/yaml");
        assertThat(bodyAsString).doesNotStartWith("{").doesNotEndWith("}");
    }

    private String getApiExport(String accepts) {
        return apiGateway.getExport(r -> r.accepts(accepts)
                .exportType("swagger")
                .restApiId(restApiId)
                .stageName(STAGE))
                .body()
                .asUtf8String();
    }
}
