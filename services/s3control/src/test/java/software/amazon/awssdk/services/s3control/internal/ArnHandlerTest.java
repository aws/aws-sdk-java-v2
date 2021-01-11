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

package software.amazon.awssdk.services.s3control.internal;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.services.s3control.internal.HandlerUtils.X_AMZ_ACCOUNT_ID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;

public class ArnHandlerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SdkHttpRequest request;
    private S3ControlConfiguration configuration;
    private ExecutionAttributes executionAttributes;

    private final ArnHandler arnHandler = ArnHandler.getInstance();
    private static final String ACCOUNT_ID = "123456789012";

    @Before
    public void setup() {
        request = SdkHttpFullRequest.builder()
                                    .appendHeader(X_AMZ_ACCOUNT_ID, ACCOUNT_ID)
                                    .protocol(Protocol.HTTPS.toString())
                                    .method(SdkHttpMethod.POST)
                                    .host(S3ControlClient.serviceMetadata().endpointFor(Region.US_WEST_2).toString())
                                    .build();
        configuration = S3ControlConfiguration.builder().build();
        executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, "s3-control");
        executionAttributes.putAttribute(SIGNING_REGION, Region.of("us-west-2"));
    }

    @Test
    public void outpostBucketArn_shouldResolveHost() {
        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket");
        SdkHttpRequest modifiedRequest = arnHandler.resolveHostForArn(request, configuration, arn, executionAttributes);

        assertThat(modifiedRequest.host(), is("s3-outposts.us-west-2.amazonaws.com"));
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME), is("s3-outposts"));
        assertThat(modifiedRequest.headers().get("x-amz-outpost-id").get(0), is("op-01234567890123456"));
        assertThat(modifiedRequest.headers().get("x-amz-account-id").get(0), is(ACCOUNT_ID));
    }

    @Test
    public void outpostAccessPointArn_shouldResolveHost() {
        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint");
        SdkHttpRequest modifiedRequest = arnHandler.resolveHostForArn(request, configuration, arn, executionAttributes);

        assertThat(modifiedRequest.host(), is("s3-outposts.us-west-2.amazonaws.com"));
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME), is("s3-outposts"));
        assertThat(modifiedRequest.headers().get("x-amz-outpost-id").get(0), is("op-01234567890123456"));
        assertThat(modifiedRequest.headers().get("x-amz-account-id").get(0), is(ACCOUNT_ID));
    }

    @Test
    public void outpostArnWithFipsEnabled_shouldThrowException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("FIPS");

        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket");
        arnHandler.resolveHostForArn(request, enableFips(), arn, executionAttributes);
    }

    @Test
    public void outpostArnWithDualstackEnabled_shouldThrowException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Dualstack");

        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket");
        arnHandler.resolveHostForArn(request, enableDualstack(), arn, executionAttributes);
    }

    private S3ControlConfiguration enableDualstack() {
        return S3ControlConfiguration.builder()
                                     .dualstackEnabled(true)
                                     .build();
    }

    private S3ControlConfiguration enableFips() {
        return S3ControlConfiguration.builder()
                                     .fipsModeEnabled(true)
                                     .build();
    }
}
