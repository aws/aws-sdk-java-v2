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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;

@RunWith(Parameterized.class)
public class CopySourceInterceptorTest {
    private final CopySourceInterceptor interceptor = new CopySourceInterceptor();

    @Parameters
    public static Collection<String[]> parameters() throws Exception {
        return Arrays.asList(new String[][] {
            {"bucket", "simpleKey", null,
             "bucket/simpleKey"},

            {"bucket", "key/with/slashes", null,
             "bucket/key/with/slashes"},

            {"bucket", "\uD83E\uDEA3", null,
             "bucket/%F0%9F%AA%A3"},

            {"bucket", "specialChars._ +!#$&'()*,:;=?@\"", null,
             "bucket/specialChars._%20%2B%21%23%24%26%27%28%29%2A%2C%3A%3B%3D%3F%40%22"},

            {"bucket", "%20", null,
             "bucket/%2520"},

            {"bucket", "key/with/version", "ZJlqdTGGfnWjRWjm.WtQc5XRTNJn3sz_",
             "bucket/key/with/version?versionId=ZJlqdTGGfnWjRWjm.WtQc5XRTNJn3sz_"},

            {"source-bucke-e00000144705073101keauk155va6smod88ynqbeta0--op-s3", "CT-debug-Info-16", null,
             "source-bucke-e00000144705073101keauk155va6smod88ynqbeta0--op-s3/CT-debug-Info-16"},

            {"source-bucke-e00000144705073101keauk155va6smod88ynqbeta0--op-s3", "CT-debug-Info-16", "123",
             "source-bucke-e00000144705073101keauk155va6smod88ynqbeta0--op-s3/CT-debug-Info-16?versionId=123"},

            {"arn:aws:s3-outposts:us-west-2:123456789012:outpost/my-outpost/bucket/my-bucket", "my-key", null,
             "arn%3Aaws%3As3-outposts%3Aus-west-2%3A123456789012%3Aoutpost/my-outpost/bucket/my-bucket/object/my-key"},

            {"arn:aws:s3-outposts:us-west-2:123456789012:outpost/my-outpost/bucket/my-bucket", "my-key", "123",
             "arn%3Aaws%3As3-outposts%3Aus-west-2%3A123456789012%3Aoutpost/my-outpost/bucket/my-bucket/object/my-key?versionId=123"},

            {"arn:aws:s3:us-west-2:123456789012:accesspoint/my-access-point", "my-key", null,
             "arn%3Aaws%3As3%3Aus-west-2%3A123456789012%3Aaccesspoint/my-access-point/object/my-key"},

            {"arn:aws:s3:us-west-2:123456789012:accesspoint/my-access-point", "my-key", "123",
             "arn%3Aaws%3As3%3Aus-west-2%3A123456789012%3Aaccesspoint/my-access-point/object/my-key?versionId=123"}
        });
    }

    private final String sourceBucket;
    private final String sourceKey;
    private final String sourceVersionId;
    private final String expectedCopySource;

    public CopySourceInterceptorTest(String sourceBucket, String sourceKey, String sourceVersionId, String expectedCopySource) {
        this.sourceBucket = sourceBucket;
        this.sourceKey = sourceKey;
        this.sourceVersionId = sourceVersionId;
        this.expectedCopySource = expectedCopySource;
    }

    @Test
    public void modifyRequest_ConstructsUrlEncodedCopySource_whenCopyObjectRequest() {
        CopyObjectRequest originalRequest = CopyObjectRequest.builder()
                                                             .sourceBucket(sourceBucket)
                                                             .sourceKey(sourceKey)
                                                             .sourceVersionId(sourceVersionId)
                                                             .build();
        CopyObjectRequest modifiedRequest = (CopyObjectRequest) interceptor
            .modifyRequest(() -> originalRequest, new ExecutionAttributes());

        assertThat(modifiedRequest.copySource()).isEqualTo(expectedCopySource);
    }

    @Test
    public void modifyRequest_ConstructsUrlEncodedCopySource_whenUploadPartCopyRequest() {
        UploadPartCopyRequest originalRequest = UploadPartCopyRequest.builder()
                                                                     .sourceBucket(sourceBucket)
                                                                     .sourceKey(sourceKey)
                                                                     .sourceVersionId(sourceVersionId)
                                                                     .build();
        UploadPartCopyRequest modifiedRequest = (UploadPartCopyRequest) interceptor
            .modifyRequest(() -> originalRequest, new ExecutionAttributes());

        assertThat(modifiedRequest.copySource()).isEqualTo(expectedCopySource);
    }

    @Test
    public void modifyRequest_Throws_whenCopySourceUsedWithSourceBucket_withCopyObjectRequest() {
        CopyObjectRequest originalRequest = CopyObjectRequest.builder()
                                                             .sourceBucket(sourceBucket)
                                                             .sourceKey(sourceKey)
                                                             .sourceVersionId(sourceVersionId)
                                                             .copySource("copySource")
                                                             .build();

        assertThatThrownBy(() -> {
            interceptor.modifyRequest(() -> originalRequest, new ExecutionAttributes());
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parameter 'copySource' must not be used in conjunction with 'sourceBucket'");
    }

    @Test
    public void modifyRequest_Throws_whenCopySourceUsedWithSourceBucket_withUploadPartCopyRequest() {
        UploadPartCopyRequest originalRequest = UploadPartCopyRequest.builder()
                                                                     .sourceBucket(sourceBucket)
                                                                     .sourceKey(sourceKey)
                                                                     .sourceVersionId(sourceVersionId)
                                                                     .copySource("copySource")
                                                                     .build();

        assertThatThrownBy(() -> {
            interceptor.modifyRequest(() -> originalRequest, new ExecutionAttributes());
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parameter 'copySource' must not be used in conjunction with 'sourceBucket'");
    }

    @Test
    public void modifyRequest_Throws_whenSourceBucketNotSpecified_withCopyObjectRequest() {
        CopyObjectRequest originalRequest = CopyObjectRequest.builder()
                                                             .sourceKey(sourceKey)
                                                             .sourceVersionId(sourceVersionId)
                                                             .build();

        assertThatThrownBy(() -> {
            interceptor.modifyRequest(() -> originalRequest, new ExecutionAttributes());
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parameter 'sourceBucket' must not be null");
    }

    @Test
    public void modifyRequest_Throws_whenSourceBucketNotSpecified_withUploadPartCopyRequest() {
        UploadPartCopyRequest originalRequest = UploadPartCopyRequest.builder()
                                                                     .sourceKey(sourceKey)
                                                                     .sourceVersionId(sourceVersionId)
                                                                     .build();

        assertThatThrownBy(() -> {
            interceptor.modifyRequest(() -> originalRequest, new ExecutionAttributes());
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parameter 'sourceBucket' must not be null");
    }

    @Test
    public void modifyRequest_Throws_whenSourceKeyNotSpecified_withCopyObjectRequest() {
        CopyObjectRequest originalRequest = CopyObjectRequest.builder()
                                                             .sourceBucket(sourceBucket)
                                                             .sourceVersionId(sourceVersionId)
                                                             .build();

        assertThatThrownBy(() -> {
            interceptor.modifyRequest(() -> originalRequest, new ExecutionAttributes());
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parameter 'sourceKey' must not be null");
    }

    @Test
    public void modifyRequest_Throws_whenSourceKeyNotSpecified_withUploadPartCopyRequest() {
        UploadPartCopyRequest originalRequest = UploadPartCopyRequest.builder()
                                                                     .sourceBucket(sourceBucket)
                                                                     .sourceVersionId(sourceVersionId)
                                                                     .build();

        assertThatThrownBy(() -> {
            interceptor.modifyRequest(() -> originalRequest, new ExecutionAttributes());
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parameter 'sourceKey' must not be null");
    }
}
