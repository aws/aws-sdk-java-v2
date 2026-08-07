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

package software.amazon.awssdk.benchmark.endpoints;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointProvider;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.DynamoDbEndpointResolverUtils;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * JMH benchmark for DynamoDB endpoint resolution through the standard pipeline.
 *
 * <p>Test cases (by index):
 * <ul>
 *   <li>0 - Standard regional endpoint (us-east-1, no FIPS, no dual-stack)</li>
 *   <li>1 - FIPS + dual-stack (us-east-1)</li>
 *   <li>2 - Account ID based endpoint, preferred mode (us-east-1)</li>
 *   <li>3 - Account ID based endpoint, preferred mode, cn-north-1 (account endpoints not supported in CN partition)</li>
 *   <li>4 - Custom endpoint override</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class DynamoDbEndpointResolverBenchmark {

    private static final ClientEndpointProvider DEFAULT_ENDPOINT_PROVIDER =
        ClientEndpointProvider.create(URI.create("https://dynamodb.us-east-1.amazonaws.com"), false);

    @Param({"0", "1", "2", "3", "4"})
    private int testCaseIndex;

    private DynamoDbEndpointProvider provider;
    private SdkRequest request;
    private ExecutionAttributes executionAttributes;

    @Setup(Level.Trial)
    public void setup() {
        provider = DynamoDbEndpointProvider.defaultProvider();
        request = GetItemRequest.builder()
                                .tableName("my-table")
                                .build();

        switch (testCaseIndex) {
            case 0: // Standard regional endpoint (us-east-1, no FIPS, no dual-stack)
                setupTestCase(Region.US_EAST_1, false, false, null, AccountIdEndpointMode.PREFERRED);
                break;
            case 1: // FIPS + dual-stack (us-east-1)
                setupTestCase(Region.US_EAST_1, true, true, null, AccountIdEndpointMode.PREFERRED);
                break;
            case 2: // Account ID based endpoint, preferred mode (us-east-1)
                setupTestCase(Region.US_EAST_1, false, false, "111111111111", AccountIdEndpointMode.PREFERRED);
                break;
            case 3: // Account ID based endpoint, preferred mode, cn-north-1
                // CN partition does not support account-ID endpoints; resolver falls back to regional
                setupTestCase(Region.CN_NORTH_1, false, false, "111111111111", AccountIdEndpointMode.PREFERRED);
                break;
            case 4: // Custom endpoint override
                setupTestCase(Region.US_EAST_1, false, false, null, AccountIdEndpointMode.DISABLED,
                              URI.create("https://localhost:8000"));
                break;
            default:
                throw new IllegalArgumentException("Unknown test case index: " + testCaseIndex);
        }
    }

    @Benchmark
    public void resolveEndpoint(Blackhole blackhole) {
        DynamoDbEndpointParams params = DynamoDbEndpointResolverUtils.ruleParams(request, executionAttributes);
        blackhole.consume(provider.resolveEndpoint(params).join());
    }

    private void setupTestCase(Region region, boolean fips, boolean dualStack,
                               String accountId, AccountIdEndpointMode accountIdMode) {
        setupTestCase(region, fips, dualStack, accountId, accountIdMode, null);
    }

    private void setupTestCase(Region region, boolean fips, boolean dualStack,
                               String accountId, AccountIdEndpointMode accountIdMode,
                               URI endpointOverride) {
        AwsCredentialsIdentity credentials;
        if (accountId != null) {
            credentials = AwsBasicCredentials.builder()
                                             .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                                             .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                                             .accountId(accountId)
                                             .build();
        } else {
            credentials = AwsCredentialsIdentity.create("AKIAIOSFODNN7EXAMPLE",
                                                        "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        }

        SelectedAuthScheme<AwsCredentialsIdentity> selectedAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(credentials),
            AwsV4HttpSigner.create(),
            AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build()
        );

        ClientEndpointProvider clientEndpointProvider = endpointOverride != null
            ? ClientEndpointProvider.create(endpointOverride, false)
            : DEFAULT_ENDPOINT_PROVIDER;

        executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(AwsExecutionAttribute.AWS_REGION, region);
        executionAttributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, fips);
        executionAttributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, dualStack);
        executionAttributes.putAttribute(AwsExecutionAttribute.OPERATION_NAME, "GetItem");
        executionAttributes.putAttribute(AwsExecutionAttribute.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE, accountIdMode);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, AttributeMap.empty());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, clientEndpointProvider);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, new BusinessMetricCollection());
    }
}
