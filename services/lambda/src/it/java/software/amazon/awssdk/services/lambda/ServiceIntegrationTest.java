/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.lambda;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingResponse;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.DeleteEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.GetEventSourceMappingResponse;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationResponse;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.testutils.retry.RetryRule;
import software.amazon.awssdk.utils.Base64Utils;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String FUNCTION_NAME = "java-sdk-helloworld-" + System.currentTimeMillis();

    @Rule
    public RetryRule retryRule = new RetryRule(10, 2000, TimeUnit.MILLISECONDS);

    @BeforeClass
    public static void setUpKinesis() {
        IntegrationTestBase.createKinesisStream();
    }

    @Before
    public void uploadFunction() throws IOException {
        // Upload function
        byte[] functionBits;
        InputStream functionZip = new FileInputStream(cloudFuncZip);
        try {
            functionBits = read(functionZip);
        } finally {
            functionZip.close();
        }

        CreateFunctionResponse result = lambda.createFunction(r -> r.description("My cloud function").functionName(FUNCTION_NAME)
                                                                    .code(FunctionCode.builder().zipFile(ByteBuffer.wrap(functionBits)).build())
                                                                    .handler("helloworld.handler")
                                                                    .memorySize(128)
                                                                    .runtime(Runtime.NODEJS4_3)
                                                                    .timeout(10)
                                                                    .role(lambdaServiceRoleArn)).join();

        checkValid_CreateFunctionResponse(result);
    }

    @After
    public void deleteFunction() {
        lambda.deleteFunction(DeleteFunctionRequest.builder().functionName(FUNCTION_NAME).build());
    }


    private static void checkValid_CreateFunctionResponse(CreateFunctionResponse result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.codeSize());
        Assert.assertNotNull(result.description());
        Assert.assertNotNull(result.functionArn());
        Assert.assertNotNull(result.functionName());
        Assert.assertNotNull(result.handler());
        Assert.assertNotNull(result.lastModified());
        Assert.assertNotNull(result.memorySize());
        Assert.assertNotNull(result.role());
        Assert.assertNotNull(result.runtime());
        Assert.assertNotNull(result.timeout());
    }

    private static void checkValid_GetFunctionConfigurationResponse(GetFunctionConfigurationResponse result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.codeSize());
        Assert.assertNotNull(result.description());
        Assert.assertNotNull(result.functionArn());
        Assert.assertNotNull(result.functionName());
        Assert.assertNotNull(result.handler());
        Assert.assertNotNull(result.lastModified());
        Assert.assertNotNull(result.memorySize());
        Assert.assertNotNull(result.role());
        Assert.assertNotNull(result.runtime());
        Assert.assertNotNull(result.timeout());
    }

    private static void checkValid_GetFunctionResponse(GetFunctionResponse result) {
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.code());
        Assert.assertNotNull(result.code().location());
        Assert.assertNotNull(result.code().repositoryType());

        FunctionConfiguration config = result.configuration();
        checkValid_FunctionConfiguration(config);
    }

    private static void checkValid_FunctionConfiguration(FunctionConfiguration config) {

        Assert.assertNotNull(config);

        Assert.assertNotNull(config.codeSize());
        Assert.assertNotNull(config.description());
        Assert.assertNotNull(config.functionArn());
        Assert.assertNotNull(config.functionName());
        Assert.assertNotNull(config.handler());
        Assert.assertNotNull(config.lastModified());
        Assert.assertNotNull(config.memorySize());
        Assert.assertNotNull(config.role());
        Assert.assertNotNull(config.runtime());
        Assert.assertNotNull(config.timeout());
    }

    private static void checkValid_CreateEventSourceMappingResult(CreateEventSourceMappingResponse result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.batchSize());
        Assert.assertNotNull(result.eventSourceArn());
        Assert.assertNotNull(result.functionArn());
        Assert.assertNotNull(result.lastModified());
        Assert.assertNotNull(result.lastProcessingResult());
        Assert.assertNotNull(result.state());
        Assert.assertNotNull(result.stateTransitionReason());
        Assert.assertNotNull(result.uuid());
    }

    @Test
    public void testFunctionOperations() throws IOException {

        // Get function
        GetFunctionResponse getFunc = lambda.getFunction(r -> r.functionName(FUNCTION_NAME)).join();
        checkValid_GetFunctionResponse(getFunc);

        // Get function configuration
        GetFunctionConfigurationResponse getConfig = lambda.getFunctionConfiguration(r -> r.functionName(FUNCTION_NAME)).join();

        checkValid_GetFunctionConfigurationResponse(getConfig);

        // List functions
        ListFunctionsResponse listFunc = lambda.listFunctions(ListFunctionsRequest.builder().build()).join();
        Assert.assertFalse(listFunc.functions().isEmpty());
        for (FunctionConfiguration funcConfig : listFunc.functions()) {
            checkValid_FunctionConfiguration(funcConfig);
        }

        // Invoke the function
        InvokeResponse invokeResult = lambda.invoke(InvokeRequest.builder().functionName(FUNCTION_NAME)
                .invocationType(InvocationType.EVENT).payload(ByteBuffer.wrap("{}".getBytes())).build()).join();

        Assert.assertEquals(202, invokeResult.statusCode().intValue());
        Assert.assertNull(invokeResult.logResult());
        Assert.assertEquals(0, invokeResult.payload().remaining());

        invokeResult = lambda.invoke(InvokeRequest.builder().functionName(FUNCTION_NAME)
                .invocationType(InvocationType.REQUEST_RESPONSE).logType(LogType.TAIL)
                .payload(ByteBuffer.wrap("{}".getBytes())).build()).join();

        Assert.assertEquals(200, invokeResult.statusCode().intValue());

        System.out.println(new String(Base64Utils.decode(invokeResult.logResult()), StringUtils.UTF8));

        Assert.assertEquals("\"Hello World\"", StringUtils.UTF8.decode(invokeResult.payload()).toString());
    }

    @Test
    public void testEventSourceOperations() {

        // AddEventSourceResult
        CreateEventSourceMappingResponse addResult = lambda
                .createEventSourceMapping(CreateEventSourceMappingRequest.builder().functionName(FUNCTION_NAME)
                        .eventSourceArn(streamArn).startingPosition("TRIM_HORIZON").batchSize(100).build()).join();
        checkValid_CreateEventSourceMappingResult(addResult);

        String eventSourceUUID = addResult.uuid();

        // GetEventSource
        GetEventSourceMappingResponse getResult = lambda.getEventSourceMapping(GetEventSourceMappingRequest.builder()
                .uuid(eventSourceUUID).build()).join();

        // RemoveEventSource
        lambda.deleteEventSourceMapping(DeleteEventSourceMappingRequest.builder().uuid(eventSourceUUID).build());
    }

}
