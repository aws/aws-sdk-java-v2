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

package software.amazon.awssdk.services.lambda.invoke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.utils.Base64Utils;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * A factory for objects that implement a user-supplied interface by invoking a remote Lambda
 * function.
 * <p>
 * <pre class="brush: java">
 * public class Request {
 *     // Standard POJO stuff here modeling the input your Lambda function
 *     // expects.
 * }
 *
 * public class Result {
 *     // More standard POJO stuff here modeling the output your Lambda
 *     // function produces.
 * }
 *
 * public interface LambdaFunctions {
 *
 *     &#064;LambdaFunction
 *     Result doSomeStuff(Request request);
 * }
 * LambdaFunctions functions = LambdaInvokerFactory.builder()
 *                             .lambdaClient(AWSLambdaSyncClientBuilder.standard()
 *                                  .credentialsProvider(new ProfileCredentialsProvider("myprofile"))
 *                                  .build())
 *                             .build(LambdaFunctions.class);
 * Request request = new Request(...);
 * Result result = functions.doSomeStuff(request);
 * </pre>
 */
@ReviewBeforeRelease("Clean up or delete.")
public final class LambdaInvokerFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    private LambdaInvokerFactory() {
    }

    /**
     * Creates a new Lambda invoker implementing the given interface and wrapping the given {@code AWSLambda} client.
     *
     * @param interfaceClass the interface to implement
     * @param awsLambda      the lambda client to use for making remote calls
     * @deprecated Use {@link LambdaInvokerFactory#builder()} to configure invoker factory.
     */
    @Deprecated
    public static <T> T build(Class<T> interfaceClass, LambdaAsyncClient awsLambda) {
        return build(interfaceClass, awsLambda, new LambdaInvokerFactoryConfig());
    }

    /**
     * Creates a new Lambda invoker implementing the given interface and wrapping the given {@code AWSLambda} client.
     *
     * @param interfaceClass the interface to implement
     * @param awsLambda      the lambda client to use for making remote calls
     * @param config         configuration for the LambdaInvokerFactory
     * @deprecated Use {@link LambdaInvokerFactory#builder()} to configure invoker factory.
     */
    @Deprecated
    public static <T> T build(Class<T> interfaceClass, LambdaAsyncClient awsLambda, LambdaInvokerFactoryConfig config) {
        final Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                                                    new Class<?>[] {interfaceClass},
                                                    new LambdaInvocationHandler(interfaceClass, awsLambda, config));

        return interfaceClass.cast(proxy);
    }

    /**
     * @return An instance of {@link Builder} to configure an invoker factory and build proxies for
     *     invoking remote lambda functions.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private LambdaFunctionNameResolver functionNameResolver;
        private String functionAlias;
        private String functionVersion;
        private LambdaAsyncClient lambda;

        /**
         * Sets a new Function name resolver to override the default behavior.
         *
         * @param functionNameResolver Implementation of {@link LambdaFunctionNameResolver}
         * @return The current object for method chaining.
         */
        public Builder lambdaFunctionNameResolver(
                LambdaFunctionNameResolver functionNameResolver) {
            this.functionNameResolver = functionNameResolver;
            return this;
        }

        private LambdaFunctionNameResolver resolveFunctionNameResolver() {
            return functionNameResolver == null ? new DefaultLambdaFunctionNameResolver() :
                   functionNameResolver;
        }

        /**
         * Sets the function alias to invoke. See <a href="http://docs.aws.amazon.com/lambda/latest/dg/versioning-aliases.html">Versioning
         * & Aliases</a> for more information on aliases.
         *
         * @return This current object for method chaining.
         */
        public Builder functionAlias(String functionAlias) {
            this.functionAlias = functionAlias;
            return this;
        }

        /**
         * Sets the function version to invoke. See <a href="http://docs.aws.amazon.com/lambda/latest/dg/versioning-aliases.html">Versioning
         * & Aliases</a> for more information on function versions.
         *
         * @return This current object for method chaining.
         */
        public Builder functionVersion(String functionVersion) {
            this.functionVersion = functionVersion;
            return this;
        }

        /**
         * Sets the client to use to call AWS Lambda. If not set a default client is used.
         *
         * @param lambda Client instance to use.
         * @return This current object for method chaining.
         */
        public Builder lambdaClient(LambdaAsyncClient lambda) {
            this.lambda = lambda;
            return this;
        }

        private LambdaAsyncClient resolveLambdaClient() {
            return lambda == null ? LambdaAsyncClient.builder().build() : lambda;
        }

        /**
         * Build a remote proxy of the given interface to make calls to AWS Lambda.
         *
         * @param interfaceClass Interface class to proxy.
         * @param <T>            Interface type.
         * @return This current object for method chaining.
         */
        public <T> T build(Class<T> interfaceClass) {
            return LambdaInvokerFactory.build(interfaceClass, resolveLambdaClient(), getConfiguration());
        }

        private LambdaInvokerFactoryConfig getConfiguration() {
            return new LambdaInvokerFactoryConfig(resolveFunctionNameResolver(), functionAlias, functionVersion);
        }
    }

    private static class LambdaInvocationHandler implements InvocationHandler {

        private final LambdaAsyncClient awsLambda;
        private final Logger log;
        private final LambdaInvokerFactoryConfig config;

        LambdaInvocationHandler(Class<?> interfaceClass, LambdaAsyncClient awsLambda, LambdaInvokerFactoryConfig config) {
            this.awsLambda = awsLambda;
            this.log = LoggerFactory.getLogger(interfaceClass);
            this.config = config;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString")) {
                return this.toString();
            }

            LambdaFunction annotation = validateInterfaceMethod(method, args);
            InvokeRequest invokeRequest = buildInvokeRequest(method, annotation, args == null ? null : args[0]);
            InvokeResponse invokeResult = awsLambda.invoke(invokeRequest).get();
            return processInvokeResponse(method, invokeResult);
        }

        /**
         * Verifies that the given method is annotated appropriately.
         */
        private LambdaFunction validateInterfaceMethod(Method method, Object[] args) {

            LambdaFunction annotation = method.getAnnotation(LambdaFunction.class);

            if (annotation == null) {
                throw new LambdaSerializationException("No LambdaFunction annotation for method " + method.getName());
            }

            if (annotation.invocationType() != InvocationType.RequestResponse && annotation.logType() != LogType.None) {
                throw new LambdaSerializationException("InvocationType must be RequestResponse if LogType " + "is set");
            }

            if (args != null && args.length > 1) {
                throw new LambdaSerializationException("LambdaFunctions take either 0 or 1 arguments");
            }

            return annotation;
        }

        /**
         * Builds an InvokeRequest from the given method, its {@code LambdaFunction} annotation, and
         * the input parameter (if any).
         */
        private InvokeRequest buildInvokeRequest(Method method, LambdaFunction annotation, Object input) {

            InvokeRequest.Builder invokeRequestBuilder = InvokeRequest.builder();

            String functionName = config.getLambdaFunctionNameResolver().getFunctionName(method, annotation, config);

            invokeRequestBuilder.functionName(functionName);
            if (hasQualifier()) {
                invokeRequestBuilder.qualifier(getQualifier());
            }
            invokeRequestBuilder.invocationType(annotation.invocationType());
            invokeRequestBuilder.logType(annotation.logType());

            if (input != null) {
                try {

                    String payload = MAPPER.writer().writeValueAsString(input);
                    if (log.isDebugEnabled()) {
                        log.debug("Serialized request object to '" + payload + "'");
                    }
                    invokeRequestBuilder.payload(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));

                } catch (JsonProcessingException ex) {
                    throw new LambdaSerializationException("Failed to serialize request object to JSON", ex);
                }
            }

            return invokeRequestBuilder.build();
        }

        private boolean hasQualifier() {
            return getQualifier() != null;
        }

        private String getQualifier() {
            return config.getFunctionAlias() == null ? config.getFunctionVersion() :
                   config.getFunctionAlias();
        }


        /**
         * Process the result of invoking a remote function. If the response includes server-side
         * logs, dump them into our logs; if it includes a server-side error indication, parse it
         * into a corresponding {@code Exception} type, otherwise parse the result payload into a
         * Java object suitable for returning from this method.
         */
        private Object processInvokeResponse(Method method, InvokeResponse invokeResult) throws Throwable {

            if (invokeResult.logResult() != null && log.isInfoEnabled()) {
                try {

                    String decoded = new String(Base64Utils.decode(invokeResult.logResult()), StringUtils.UTF8);

                    log.info(method.getName() + " log:\n\t" + decoded.replaceAll("\n", "\n\t"));

                } catch (Exception ex) {
                    log.warn("Error decoding log result '" + invokeResult.logResult() + "'", ex);
                }
            }

            String functionError = invokeResult.functionError();

            if (functionError == null) {
                // Success.
                return getObjectFromPayload(method, invokeResult);
            } else {
                throw getExceptionFromPayload(method, invokeResult);
            }
        }

        /**
         * Reads a Java object suitable for returning from the given method from the payload of the
         * given {@code InvokeResponse} (or returns {@code null} if the method has no return value or
         * the response contains no payload).
         *
         * @throws LambdaSerializationException
         *             on error deserializing
         */
        private Object getObjectFromPayload(Method method, InvokeResponse invokeResult) {

            try {

                return getObjectFromPayload(method.getGenericReturnType(), invokeResult.payload());

            } catch (IOException ex) {
                throw new LambdaSerializationException("Failed to parse Lambda function result", ex);
            }
        }

        /**
         * Unmarshall the exception from the response payload. The invoker factory supports unmarshalling into custom exceptions
         * that are declared on the method signature (in the interface the invoker factory proxies).
         *
         * @param method       Method being proxied
         * @param invokeResult Result from AWS Lambda.
         * @return Exception to throw back to the caller. May either be a custom exception declared in the interface, a generic
         *     exception unmarshalled from the payload, or a very generic exception if we can't unmarshall the payload.
         */
        private Throwable getExceptionFromPayload(Method method, InvokeResponse invokeResult) {
            try {
                LambdaFunctionException error = getObjectFromPayload(LambdaFunctionException.class, invokeResult.payload());
                error.setFunctionError(invokeResult.functionError());
                error.fillInStackTrace(method.getDeclaringClass());

                return getExceptionToThrow(method, error);
            } catch (Exception ex) {
                log.warn("Error parsing exception information from response payload", ex);
                return new LambdaFunctionException("Unexpected error executing Lambda function",
                                                   invokeResult.functionError());
            }
        }

        /**
         * Get the correct exception to throw back to the caller.
         *
         * @param method Interface method we are proxying
         * @param error  Unmarshalled error payload
         * @return A custom exception if the error matches any thrown by the interface method or the original {@link
         *     LambdaFunctionException} if none matches.
         */
        private Throwable getExceptionToThrow(Method method, LambdaFunctionException error) {
            final String type = error.getType();
            final Constructor<?> constructor = findConstructor(findCustomExceptionClass(method, type));

            if (constructor != null) {
                try {
                    final Throwable toReturn = (Throwable) constructor.newInstance(error.getMessage());
                    toReturn.setStackTrace(error.getStackTrace());
                    return toReturn;
                } catch (Exception ex) {
                    log.warn("Error constructing custom exception", ex);
                }
            }
            return error;
        }

        /**
         * Search the method throws clause to find an exception that matches the error type.
         *
         * @param method We only consider exception types that are explicitly declared in the interface for the proxied method.
         * @param type   Error type returned by AWS Lambda.
         * @return Custom exception class to create or null to use default exception.
         */
        private Class<?> findCustomExceptionClass(Method method, String type) {
            if (type != null) {
                for (Class<?> exceptionType : method.getExceptionTypes()) {
                    if (exceptionType.getName().equals(type) || exceptionType.getSimpleName().equals(type)) {
                        return exceptionType;
                    }
                }
            }
            return null;
        }

        /**
         * For custom exceptions we expect to find a accessible constructor that takes a String parameter (for the error message)
         *
         * @param type Exception class
         * @return Applicable constructor or null if not found.
         */
        private Constructor<?> findConstructor(Class<?> type) {
            if (type == null) {
                return null;
            }
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params != null && params.length == 1 && String.class.equals(params[0])) {
                    return constructor;
                }
            }
            return null;
        }

        private <T> T getObjectFromPayload(Class<T> type, ByteBuffer payload) throws IOException {
            return type.cast(getObjectFromPayload((Type) type, payload));
        }

        private Object getObjectFromPayload(Type type, ByteBuffer payload) throws IOException {
            if (type == void.class || payload.remaining() == 0) {
                return null;
            }

            JavaType javaType = MAPPER.getTypeFactory().constructType(type);

            return MAPPER.reader(javaType).readValue(BinaryUtils.copyAllBytesFrom(payload));
        }
    }
}
