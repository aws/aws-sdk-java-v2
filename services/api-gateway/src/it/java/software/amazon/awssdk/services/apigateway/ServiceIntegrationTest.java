/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.List;
import junit.framework.Assert;
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

        Assert.assertNotNull(createRestApiResult);
        Assert.assertNotNull(createRestApiResult.description());
        Assert.assertNotNull(createRestApiResult.id());
        Assert.assertNotNull(createRestApiResult.name());
        Assert.assertNotNull(createRestApiResult.createdDate());
        Assert.assertEquals(createRestApiResult.name(), NAME);
        Assert.assertEquals(createRestApiResult.description(), DESCRIPTION);

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

        Assert.assertNotNull(getRestApiResult);
        Assert.assertNotNull(getRestApiResult.description());
        Assert.assertNotNull(getRestApiResult.id());
        Assert.assertNotNull(getRestApiResult.name());
        Assert.assertNotNull(getRestApiResult.createdDate());
        Assert.assertEquals(getRestApiResult.name(), NAME);
        Assert.assertEquals(getRestApiResult.description(), "updatedDesc");
    }

    @Test
    public void testCreateUpdateRetrieveApiKey() {
        CreateApiKeyResponse createApiKeyResult = apiGateway
                .createApiKey(CreateApiKeyRequest.builder().name(NAME)
                                                       .description(DESCRIPTION).build());

        Assert.assertNotNull(createApiKeyResult);
        Assert.assertNotNull(createApiKeyResult.description());
        Assert.assertNotNull(createApiKeyResult.id());
        Assert.assertNotNull(createApiKeyResult.name());
        Assert.assertNotNull(createApiKeyResult.createdDate());
        Assert.assertNotNull(createApiKeyResult.enabled());
        Assert.assertNotNull(createApiKeyResult.lastUpdatedDate());
        Assert.assertNotNull(createApiKeyResult.stageKeys());

        String apiKeyId = createApiKeyResult.id();
        Assert.assertEquals(createApiKeyResult.name(), NAME);
        Assert.assertEquals(createApiKeyResult.description(), DESCRIPTION);

        PatchOperation patch = PatchOperation.builder().op(Op.REPLACE)
                                                   .path("/description").value("updatedDesc").build();
        apiGateway.updateApiKey(UpdateApiKeyRequest.builder().apiKey(apiKeyId)
                                                         .patchOperations(patch).build());

        GetApiKeyResponse getApiKeyResult = apiGateway
                .getApiKey(GetApiKeyRequest.builder().apiKey(apiKeyId).build());

        Assert.assertNotNull(getApiKeyResult);
        Assert.assertNotNull(getApiKeyResult.description());
        Assert.assertNotNull(getApiKeyResult.id());
        Assert.assertNotNull(getApiKeyResult.name());
        Assert.assertNotNull(getApiKeyResult.createdDate());
        Assert.assertNotNull(getApiKeyResult.enabled());
        Assert.assertNotNull(getApiKeyResult.lastUpdatedDate());
        Assert.assertNotNull(getApiKeyResult.stageKeys());
        Assert.assertEquals(getApiKeyResult.id(), apiKeyId);
        Assert.assertEquals(getApiKeyResult.name(), NAME);
        Assert.assertEquals(getApiKeyResult.description(), "updatedDesc");
    }

    @Test
    public void testResourceOperations() {
        GetResourcesResponse resourcesResult = apiGateway
                .getResources(GetResourcesRequest.builder()
                                      .restApiId(restApiId).build());
        List<Resource> resources = resourcesResult.items();
        Assert.assertEquals(resources.size(), 1);
        Resource rootResource = resources.get(0);
        Assert.assertNotNull(rootResource);
        Assert.assertEquals(rootResource.path(), "/");
        String rootResourceId = rootResource.id();

        CreateResourceResponse createResourceResult = apiGateway
                .createResource(CreateResourceRequest.builder()
                                        .restApiId(restApiId)
                                        .pathPart("fooPath")
                                        .parentId(rootResourceId).build());
        Assert.assertNotNull(createResourceResult);
        Assert.assertNotNull(createResourceResult.id());
        Assert.assertNotNull(createResourceResult.parentId());
        Assert.assertNotNull(createResourceResult.path());
        Assert.assertNotNull(createResourceResult.pathPart());
        Assert.assertEquals(createResourceResult.pathPart(), "fooPath");
        Assert.assertEquals(createResourceResult.parentId(), rootResourceId);

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
        Assert.assertNotNull(getResourceResult);
        Assert.assertNotNull(getResourceResult.id());
        Assert.assertNotNull(getResourceResult.parentId());
        Assert.assertNotNull(getResourceResult.path());
        Assert.assertNotNull(getResourceResult.pathPart());
        Assert.assertEquals(getResourceResult.pathPart(), "updatedPath");
        Assert.assertEquals(getResourceResult.parentId(), rootResourceId);

        PutMethodResponse putMethodResult = apiGateway
                .putMethod(PutMethodRequest.builder().restApiId(restApiId)
                                                 .resourceId(createResourceResult.id())
                                                 .authorizationType("AWS_IAM").httpMethod("PUT").build());
        Assert.assertNotNull(putMethodResult);
        Assert.assertNotNull(putMethodResult.authorizationType());
        Assert.assertNotNull(putMethodResult.apiKeyRequired());
        Assert.assertNotNull(putMethodResult.httpMethod());
        Assert.assertEquals(putMethodResult.authorizationType(), "AWS_IAM");
        Assert.assertEquals(putMethodResult.httpMethod(), "PUT");

        PutIntegrationResponse putIntegrationResult = apiGateway
                .putIntegration(PutIntegrationRequest.builder()
                                        .restApiId(restApiId)
                                        .resourceId(createResourceResult.id())
                                        .httpMethod("PUT").type(IntegrationType.MOCK)
                                        .uri("http://foo.bar")
                                        .integrationHttpMethod("GET").build());
        Assert.assertNotNull(putIntegrationResult);
        Assert.assertNotNull(putIntegrationResult.cacheNamespace());
        Assert.assertNotNull(putIntegrationResult.type());
        Assert.assertEquals(putIntegrationResult.type(),
                            IntegrationType.MOCK);
    }
}
