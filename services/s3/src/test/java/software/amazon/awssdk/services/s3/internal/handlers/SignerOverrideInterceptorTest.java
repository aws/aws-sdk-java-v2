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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;

import java.net.URI;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtS3V4aSigner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class SignerOverrideInterceptorTest {

    private static final String MRAP_ARN = "arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap";
    private static final String AP_ARN = "arn:aws:s3:us-east-1:123456789012:accesspoint:myaccesspoint";
    private static final String OUTPOSTS_ARN = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456"
                                               + ":accesspoint:myaccesspoint";

    private final SignerOverrideInterceptor interceptor = new SignerOverrideInterceptor();

    @Test
    public void multiRegionAccessPointArn_shouldSetInternalSignerOverride() {
        SdkRequest request = interceptor.modifyRequest(createContext(MRAP_ARN, null),
                                                       createExecutionParameters(false));

        assertThat(request.overrideConfiguration()).isPresent();
        assertThat(request.overrideConfiguration().get().signer().get()).isInstanceOf(DefaultAwsCrtS3V4aSigner.class);
    }

    @Test
    public void standardAccessPointArn_shouldNotSetInternalSignerOverride() {
        SdkRequest request = interceptor.modifyRequest(createContext(AP_ARN, null),
                                                       createExecutionParameters(false));
        assertThat(request.overrideConfiguration()).isEmpty();
    }

    @Test
    public void outpostsAccessPointArn_shouldNotSetInternalSignerOverride() {
        SdkRequest request = interceptor.modifyRequest(createContext(OUTPOSTS_ARN, null),
                                                       createExecutionParameters(false));
        assertThat(request.overrideConfiguration()).isEmpty();
    }

    @Test
    public void nonAccessPointArn_shouldNotSetInternalSignerOverride() {
        String bucket = "test-bucket";
        String key = "test-key";
        URI customUri = URI.create(String.format("http://s3-test.com/%s/%s", bucket, key));
        Context.ModifyHttpRequest ctx = context(PutObjectRequest.builder().build(), sdkHttpRequest(customUri));
        SdkRequest request = interceptor.modifyRequest(ctx, createExecutionParameters(false));
        assertThat(request.overrideConfiguration()).isEmpty();
    }

    @Test
    public void multiRegionAccessPointArn_clientOverride_shouldNotChangeSignerOverride() {
        SdkRequest request = interceptor.modifyRequest(createContext(MRAP_ARN, null),
                                                       createExecutionParameters(true));

        assertThat(request.overrideConfiguration()).isEmpty();
    }

    @Test
    public void multiRegionAccessPointArn_requestOverride_shouldNotChangeSignerOverride() {
        SdkRequest request = interceptor.modifyRequest(createContext(MRAP_ARN, AwsS3V4Signer.create()),
                                                       createExecutionParameters(false));

        assertThat(request.overrideConfiguration()).isPresent();
        assertThat(request.overrideConfiguration().get().signer().get()).isInstanceOf(AwsS3V4Signer.class);
    }

    private Context.ModifyRequest createContext(String accessPointArn, Signer requestOverrideSigner) {
        String key = "test-key";
        URI customUri = URI.create(String.format("http://s3-test.com/%s/%s", urlEncode(accessPointArn), key));

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                                                                  .bucket(accessPointArn)
                                                                  .key(key);
        if (requestOverrideSigner != null) {
            requestBuilder.overrideConfiguration(c -> c.signer(requestOverrideSigner));
        }

        return context(requestBuilder.build(), sdkHttpRequest(customUri));
    }

    private ExecutionAttributes createExecutionParameters(boolean clientOverride) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        if (clientOverride) {
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.SIGNER_OVERRIDDEN, true);
        }
        return executionAttributes;
    }

    private SdkHttpRequest sdkHttpRequest(URI customUri) {
        return SdkHttpFullRequest.builder()
                                 .protocol(customUri.getScheme())
                                 .host(customUri.getHost())
                                 .port(customUri.getPort())
                                 .method(SdkHttpMethod.GET)
                                 .encodedPath(customUri.getPath())
                                 .build();
    }

    private Context.ModifyHttpRequest context(SdkRequest request, SdkHttpRequest sdkHttpRequest) {
        return new Context.ModifyHttpRequest() {
            @Override
            public SdkHttpRequest httpRequest() {
                return sdkHttpRequest;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return null;
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return null;
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }
}
