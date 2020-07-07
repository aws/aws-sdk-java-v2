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

package software.amazon.awssdk.awscore.client.handler;

import static software.amazon.awssdk.utils.CollectionUtils.firstIfPresent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

@SdkProtectedApi
public final class AwsClientHandlerUtils {

    private AwsClientHandlerUtils() {

    }

    static <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionContext createExecutionContext(
        ClientExecutionParams<InputT, OutputT> executionParams,
        SdkClientConfiguration clientConfig,
        ExecutionAttributes executionAttributes) {

        SdkRequest originalRequest = executionParams.getInput();
        AwsCredentialsProvider clientCredentials = clientConfig.option(AwsClientOption.CREDENTIALS_PROVIDER);
        AwsCredentialsProvider credentialsProvider = originalRequest.overrideConfiguration()
                                                                    .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                                                                    .map(c -> (AwsRequestOverrideConfiguration) c)
                                                                    .flatMap(AwsRequestOverrideConfiguration::credentialsProvider)
                                                                    .orElse(clientCredentials);

        long credentialsResolveStart = System.nanoTime();
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        Duration fetchDuration = Duration.ofNanos(System.nanoTime() - credentialsResolveStart);
        MetricCollector metricCollector = resolveMetricCollector(executionParams);
        metricCollector.reportMetric(CoreMetric.CREDENTIALS_FETCH_DURATION, fetchDuration);

        Validate.validState(credentials != null, "Credential providers must never return null.");

        executionAttributes
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG, clientConfig.option(SdkClientOption.SERVICE_CONFIGURATION))
            .putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, credentials)
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME,
                          clientConfig.option(AwsClientOption.SERVICE_SIGNING_NAME))
            .putAttribute(AwsExecutionAttribute.AWS_REGION, clientConfig.option(AwsClientOption.AWS_REGION))
            .putAttribute(AwsExecutionAttribute.ENDPOINT_PREFIX, clientConfig.option(AwsClientOption.ENDPOINT_PREFIX))
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, clientConfig.option(AwsClientOption.SIGNING_REGION))
            .putAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX, executionParams.isFullDuplex())
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, clientConfig.option(SdkClientOption.CLIENT_TYPE))
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, clientConfig.option(SdkClientOption.SERVICE_NAME))
            .putAttribute(SdkExecutionAttribute.OPERATION_NAME, executionParams.getOperationName())
            .putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN,
                          clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN));

        ExecutionInterceptorChain executionInterceptorChain =
                new ExecutionInterceptorChain(clientConfig.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        return ExecutionContext.builder()
                               .interceptorChain(executionInterceptorChain)
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(originalRequest)
                                                                     .asyncRequestBody(executionParams.getAsyncRequestBody())
                                                                     .requestBody(executionParams.getRequestBody())
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .signer(computeSigner(originalRequest, clientConfig))
                               .metricCollector(metricCollector)
                               .build();
    }

    /**
     * Encodes the request into a flow message and then returns bytebuffer from the message.
     *
     * @param request The request to encode
     * @return A bytebuffer representing the given request
     */
    public static ByteBuffer encodeEventStreamRequestToByteBuffer(SdkHttpFullRequest request) {
        Map<String, HeaderValue> headers = request.headers()
                                                  .entrySet()
                                                  .stream()
                                                  .collect(Collectors.toMap(Map.Entry::getKey, e -> HeaderValue.fromString(
                                                      firstIfPresent(e.getValue()))));
        byte[] payload = null;
        if (request.contentStreamProvider().isPresent()) {
            try {
                payload = IoUtils.toByteArray(request.contentStreamProvider().get().newStream());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return new Message(headers, payload).toByteBuffer();
    }

    // Signer at request level gets priority over client config signer
    private static Signer computeSigner(SdkRequest originalRequest,
                                        SdkClientConfiguration clientConfiguration) {
        return originalRequest.overrideConfiguration()
                              .flatMap(RequestOverrideConfiguration::signer)
                              .orElse(clientConfiguration.option(AwsAdvancedClientOption.SIGNER));
    }

    private static MetricCollector resolveMetricCollector(ClientExecutionParams<?, ?> params) {
        MetricCollector metricCollector = params.getMetricCollector();
        if (metricCollector == null) {
            metricCollector = MetricCollector.create("ApiCall");
        }
        return metricCollector;
    }
}
