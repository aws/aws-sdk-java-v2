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

package software.amazon.awssdk.protocol.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.stream.Stream;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.wiremock.WireMockUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Reflection utils to create the client class and invoke operation methods.
 */
public class ClientReflector implements SdkAutoCloseable {

    private final IntermediateModel model;
    private final Metadata metadata;
    private final Object client;
    private final Class<?> interfaceClass;

    public ClientReflector(IntermediateModel model) {
        this.model = model;
        this.metadata = model.getMetadata();
        this.interfaceClass = getInterfaceClass();
        this.client = createClient();
    }

    private Class<?> getInterfaceClass() {
        try {
            return Class.forName(getClientFqcn(metadata.getSyncInterface()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call the operation method on the client with the given request.
     *
     * @param params Params to call the operation with. Usually just the request POJO but might be additional params
     *               for streaming operations.
     * @return Unmarshalled result
     */
    public Object invokeMethod(TestCase testCase, Object... params) throws Exception {
        String operationName = testCase.getWhen().getOperationName();
        Method operationMethod = getOperationMethod(operationName, params);
        return operationMethod.invoke(client, params);
    }

    /**
     * Call the operation (with a streaming output) method on the client with the given request.
     *
     * @param requestObject   POJO request object.
     * @param responseHandler Response handler for an operation with a streaming output.
     * @return Unmarshalled result
     */
    public Object invokeStreamingMethod(TestCase testCase,
                                        Object requestObject,
                                        ResponseTransformer<?, ?> responseHandler) throws Exception {
        String operationName = testCase.getWhen().getOperationName();
        Method operationMethod = getOperationMethod(operationName, requestObject.getClass(), ResponseTransformer.class);
        return operationMethod.invoke(client, requestObject, responseHandler);
    }

    @Override
    public void close() {
        if (client instanceof SdkAutoCloseable) {
            ((SdkAutoCloseable) client).close();
        }
    }

    /**
     * Create the sync client to use in the tests.
     */
    private Object createClient() {
        try {
            // Reflectively create a builder, configure it, and then create the client.
            Object untypedBuilder = interfaceClass.getMethod("builder").invoke(null);
            AwsClientBuilder<?, ?> builder = (AwsClientBuilder<?, ?>) untypedBuilder;
            return builder.credentialsProvider(getMockCredentials())
                          .region(Region.US_EAST_1)
                          .endpointOverride(URI.create(getEndpoint()))
                          .build();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private String getEndpoint() {
        return "http://localhost:" + WireMockUtils.port();
    }

    /**
     * @return Dummy credentials to create client with.
     */
    private StaticCredentialsProvider getMockCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    /**
     * @param simpleClassName Class name to fully qualify.
     * @return Fully qualified name of class in the client's base package.
     */
    private String getClientFqcn(String simpleClassName) {
        return String.format("%s.%s", metadata.getFullClientPackageName(), simpleClassName);
    }

    /**
     * Gets the method for the given operation and parameters. Assumes the classes of the params matches
     * the classes of the declared method parameters (i.e. no inheritance).
     *
     * @return Method object to invoke operation.
     */
    private Method getOperationMethod(String operationName, Object... params) throws Exception {
        Class[] classes = Stream.of(params).map(Object::getClass).toArray(Class[]::new);
        return getOperationMethod(operationName, classes);
    }

    /**
     * @return Method object to invoke operation.
     */
    private Method getOperationMethod(String operationName, Class<?>... classes) throws Exception {
        return interfaceClass.getMethod(getOperationMethodName(operationName), classes);
    }

    /**
     * @return Name of the client method for the given operation.
     */
    private String getOperationMethodName(String operationName) {
        return model.getOperations().get(operationName).getMethodName();
    }

}
