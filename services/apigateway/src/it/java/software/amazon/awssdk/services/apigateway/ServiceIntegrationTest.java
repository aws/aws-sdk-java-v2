/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.apigateway.model.CreateApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.CreateApiKeyResponse;
import software.amazon.awssdk.services.apigateway.model.CreateResourceRequest;
import software.amazon.awssdk.services.apigateway.model.CreateResourceResponse;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiResponse;
import software.amazon.awssdk.services.apigateway.model.DeleteRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.GetApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.GetApiKeyResponse;
import software.amazon.awssdk.services.apigateway.model.GetResourceRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourceResponse;
import software.amazon.awssdk.services.apigateway.model.GetResourcesRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourcesResponse;
import software.amazon.awssdk.services.apigateway.model.GetRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApiResponse;
import software.amazon.awssdk.services.apigateway.model.IntegrationType;
import software.amazon.awssdk.services.apigateway.model.Op;
import software.amazon.awssdk.services.apigateway.model.PatchOperation;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationRequest;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationResponse;
import software.amazon.awssdk.services.apigateway.model.PutMethodRequest;
import software.amazon.awssdk.services.apigateway.model.PutMethodResponse;
import software.amazon.awssdk.services.apigateway.model.Resource;
import software.amazon.awssdk.services.apigateway.model.UpdateApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.UpdateResourceRequest;
import software.amazon.awssdk.services.apigateway.model.UpdateRestApiRequest;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String NAME = "java-sdk-integration-"
                                       + System.currentTimeMillis();
    private static final String DESCRIPTION = "fooDesc";

    private static String restApiId = null;

    @BeforeClass
    public static void createRestApi() {
        CreateRestApiResponse createRestApiResult = apiGateway.createRestApi(
                CreateRestApiRequest.builder().name(NAME)
                                          .description(DESCRIPTION).build());

        assertNotNull(createRestApiResult);
        assertNotNull(createRestApiResult.description());
        assertNotNull(createRestApiResult.id());
        assertNotNull(createRestApiResult.name());
        assertNotNull(createRestApiResult.createdDate());
        assertEquals(createRestApiResult.name(), NAME);
        assertEquals(createRestApiResult.description(), DESCRIPTION);

        restApiId = createRestApiResult.id();
    }

    @AfterClass
    public static void deleteRestApiKey() {
        if (restApiId != null) {
            apiGateway.deleteRestApi(DeleteRestApiRequest.builder().restApiId(restApiId).build());
        }
    }

    @Test
    public void testUpdateRetrieveRestApi() {
        PatchOperation patch = PatchOperation.builder().op(Op.REPLACE)
                                                   .path("/description").value("updatedDesc").build();
        apiGateway.updateRestApi(UpdateRestApiRequest.builder().restApiId(restApiId)
                                                           .patchOperations(patch).build());

        GetRestApiResponse getRestApiResult = apiGateway
                .getRestApi(GetRestApiRequest.builder().restApiId(restApiId).build());

        assertNotNull(getRestApiResult);
        assertNotNull(getRestApiResult.description());
        assertNotNull(getRestApiResult.id());
        assertNotNull(getRestApiResult.name());
        assertNotNull(getRestApiResult.createdDate());
        assertEquals(getRestApiResult.name(), NAME);
        assertEquals(getRestApiResult.description(), "updatedDesc");
    }

    @Test
    public void testCreateUpdateRetrieveApiKey() {
        CreateApiKeyResponse createApiKeyResult = apiGateway
                .createApiKey(CreateApiKeyRequest.builder().name(NAME)
                                                       .description(DESCRIPTION).build());

        assertNotNull(createApiKeyResult);
        assertNotNull(createApiKeyResult.description());
        assertNotNull(createApiKeyResult.id());
        assertNotNull(createApiKeyResult.name());
        assertNotNull(createApiKeyResult.createdDate());
        assertNotNull(createApiKeyResult.enabled());
        assertNotNull(createApiKeyResult.lastUpdatedDate());
        assertNotNull(createApiKeyResult.stageKeys());

        String apiKeyId = createApiKeyResult.id();
        assertEquals(createApiKeyResult.name(), NAME);
        assertEquals(createApiKeyResult.description(), DESCRIPTION);

        PatchOperation patch = PatchOperation.builder().op(Op.REPLACE)
                                                   .path("/description").value("updatedDesc").build();
        apiGateway.updateApiKey(UpdateApiKeyRequest.builder().apiKey(apiKeyId)
                                                         .patchOperations(patch).build());

        GetApiKeyResponse getApiKeyResult = apiGateway
                .getApiKey(GetApiKeyRequest.builder().apiKey(apiKeyId).build());

        assertNotNull(getApiKeyResult);
        assertNotNull(getApiKeyResult.description());
        assertNotNull(getApiKeyResult.id());
        assertNotNull(getApiKeyResult.name());
        assertNotNull(getApiKeyResult.createdDate());
        assertNotNull(getApiKeyResult.enabled());
        assertNotNull(getApiKeyResult.lastUpdatedDate());
        assertNotNull(getApiKeyResult.stageKeys());
        assertEquals(getApiKeyResult.id(), apiKeyId);
        assertEquals(getApiKeyResult.name(), NAME);
        assertEquals(getApiKeyResult.description(), "updatedDesc");
    }

    @Test
    public void testResourceOperations() {
        GetResourcesResponse resourcesResult = apiGateway
                .getResources(GetResourcesRequest.builder()
                                      .restApiId(restApiId).build());
        List<Resource> resources = resourcesResult.items();
        assertEquals(resources.size(), 1);
        Resource rootResource = resources.get(0);
        assertNotNull(rootResource);
        assertEquals(rootResource.path(), "/");
        String rootResourceId = rootResource.id();

        CreateResourceResponse createResourceResult = apiGateway
                .createResource(CreateResourceRequest.builder()
                                        .restApiId(restApiId)
                                        .pathPart("fooPath")
                                        .parentId(rootResourceId).build());
        assertNotNull(createResourceResult);
        assertNotNull(createResourceResult.id());
        assertNotNull(createResourceResult.parentId());
        assertNotNull(createResourceResult.path());
        assertNotNull(createResourceResult.pathPart());
        assertEquals(createResourceResult.pathPart(), "fooPath");
        assertEquals(createResourceResult.parentId(), rootResourceId);

        PatchOperation patch = PatchOperation.builder().op(Op.REPLACE)
                                                   .path("/pathPart").value("updatedPath").build();
        apiGateway.updateResource(UpdateResourceRequest.builder()
                                          .restApiId(restApiId)
                                          .resourceId(createResourceResult.id())
                                          .patchOperations(patch).build());

        GetResourceResponse getResourceResult = apiGateway
                .getResource(GetResourceRequest.builder()
                                     .restApiId(restApiId)
                                     .resourceId(createResourceResult.id()).build());
        assertNotNull(getResourceResult);
        assertNotNull(getResourceResult.id());
        assertNotNull(getResourceResult.parentId());
        assertNotNull(getResourceResult.path());
        assertNotNull(getResourceResult.pathPart());
        assertEquals(getResourceResult.pathPart(), "updatedPath");
        assertEquals(getResourceResult.parentId(), rootResourceId);

        PutMethodResponse putMethodResult = apiGateway
                .putMethod(PutMethodRequest.builder().restApiId(restApiId)
                                                 .resourceId(createResourceResult.id())
                                                 .authorizationType("AWS_IAM").httpMethod("PUT").build());
        assertNotNull(putMethodResult);
        assertNotNull(putMethodResult.authorizationType());
        assertNotNull(putMethodResult.apiKeyRequired());
        assertNotNull(putMethodResult.httpMethod());
        assertEquals(putMethodResult.authorizationType(), "AWS_IAM");
        assertEquals(putMethodResult.httpMethod(), "PUT");

        PutIntegrationResponse putIntegrationResult = apiGateway
                .putIntegration(PutIntegrationRequest.builder()
                                        .restApiId(restApiId)
                                        .resourceId(createResourceResult.id())
                                        .httpMethod("PUT").type(IntegrationType.MOCK)
                                        .uri("http://foo.bar")
                                        .integrationHttpMethod("GET").build());
        assertNotNull(putIntegrationResult);
        assertNotNull(putIntegrationResult.cacheNamespace());
        assertNotNull(putIntegrationResult.type());
        assertEquals(putIntegrationResult.type(),
                            IntegrationType.MOCK);
    }
}
