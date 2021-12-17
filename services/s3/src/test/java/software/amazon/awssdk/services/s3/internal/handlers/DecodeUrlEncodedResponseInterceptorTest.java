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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.EncodingType;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Unit tests for {@link DecodeUrlEncodedResponseInterceptor}.
 * <p>
 * See <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html">https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html</a>
 * and <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/v2-RESTBucketGET.html">https://docs.aws.amazon.com/AmazonS3/latest/API/v2-RESTBucketGET.html</a>
 * for information on which parts of the response must are affected by the EncodingType member.
 */
public class DecodeUrlEncodedResponseInterceptorTest {
    private static final String TEST_URL_ENCODED = "foo+%3D+bar+baz+%CE%B1+%CE%B2+%F0%9F%98%8A";
    private static final String TEST_URL_ENCODED_DELIMITER = "foo+%3D+bar+baz+%CE%B1+%CE%B2+%F0%9F%98%8A+delimiter";
    private static final String TEST_URL_ENCODED_NEXT_MARKER = "foo+%3D+bar+baz+%CE%B1+%CE%B2+%F0%9F%98%8A+nextmarker";

    private static final String TEST_URL_ENCODED_MARKER = "foo+%3D+bar+baz+%CE%B1+%CE%B2+%F0%9F%98%8A+marker";
    private static final String TEST_URL_ENCODED_PREFIX = "foo+%3D+bar+baz+%CE%B1+%CE%B2+%F0%9F%98%8A+prefix";
    private static final String TEST_URL_ENCODED_KEY = "foo+%3D+bar+baz+%CE%B1+%CE%B2+%F0%9F%98%8A+key";
    private static final String TEST_URL_ENCODED_START_AFTER = "foo+%3D+bar+baz+%CE%B1+%CE%B2+%F0%9F%98%8A+startafter";

    // foo = bar baz α β 😊
    private static final String TEST_URL_DECODED = "foo = bar baz α β \uD83D\uDE0A";

    private static final DecodeUrlEncodedResponseInterceptor INTERCEPTOR = new DecodeUrlEncodedResponseInterceptor();

    private static final List<S3Object> TEST_CONTENTS = Arrays.asList(
            S3Object.builder().key(TEST_URL_ENCODED).build(),
            S3Object.builder().key(TEST_URL_ENCODED).build(),
            S3Object.builder().key(TEST_URL_ENCODED).build()
    );

    private static final List<CommonPrefix> COMMON_PREFIXES = Arrays.asList(CommonPrefix.builder()
                                                                                       .prefix(TEST_URL_ENCODED_PREFIX)
                                                                                       .build());
    private static final ListObjectsResponse V1_TEST_ENCODED_RESPONSE = ListObjectsResponse.builder()
                                                                                           .encodingType(EncodingType.URL)
                                                                                           .delimiter(TEST_URL_ENCODED_DELIMITER)
                                                                                           .nextMarker(TEST_URL_ENCODED_NEXT_MARKER)
                                                                                           .prefix(TEST_URL_ENCODED_PREFIX)
                                                                                           .marker(TEST_URL_ENCODED_MARKER)
                                                                                           .contents(TEST_CONTENTS)
                                                                                           .commonPrefixes(COMMON_PREFIXES)
                                                                                           .build();

    private static final ListObjectsV2Response V2_TEST_ENCODED_RESPONSE = ListObjectsV2Response.builder()
                                                                                               .encodingType(EncodingType.URL)
                                                                                               .delimiter(TEST_URL_ENCODED_DELIMITER)
                                                                                               .prefix(TEST_URL_ENCODED_PREFIX)
                                                                                               .startAfter(TEST_URL_ENCODED_START_AFTER)
                                                                                               .contents(TEST_CONTENTS)
                                                                                               .commonPrefixes(COMMON_PREFIXES)
                                                                                               .build();

    private static final String TEST_URL_ENCODED_NEXT_KEY_MARKER = TEST_URL_ENCODED + "+nextKeyMarker";
    private static final String TEST_URL_ENCODED_KEY_MARKER = TEST_URL_ENCODED + "+keyMarker";
    private static final ListObjectVersionsResponse TEST_LIST_OBJECT_VERSION_RESPONSE = ListObjectVersionsResponse.builder()
                                                                                                                  .encodingType(EncodingType.URL)
                                                                                                                  .delimiter(TEST_URL_ENCODED_DELIMITER)
                                                                                                                  .prefix(TEST_URL_ENCODED_PREFIX)
                                                                                                                  .keyMarker(TEST_URL_ENCODED_KEY_MARKER)
                                                                                                                  .nextKeyMarker(TEST_URL_ENCODED_NEXT_KEY_MARKER)
                                                                                                                  .commonPrefixes(COMMON_PREFIXES)
                                                                                                                  .versions(ObjectVersion.builder()
                                                                                                                                         .key(TEST_URL_ENCODED_KEY)
                                                                                                                                         .build())
                                                                                                                  .build();


    private static final ListMultipartUploadsResponse TEST_LIST_MULTIPART_UPLOADS_RESPONSE =
        ListMultipartUploadsResponse.builder()
                                    .encodingType(EncodingType.URL)
                                    .delimiter(TEST_URL_ENCODED_DELIMITER)
                                    .prefix(TEST_URL_ENCODED_PREFIX)
                                    .keyMarker(TEST_URL_ENCODED_KEY_MARKER)
                                    .nextKeyMarker(TEST_URL_ENCODED_NEXT_KEY_MARKER)
                                    .uploads(MultipartUpload.builder().key(TEST_URL_ENCODED_KEY).build())
                                    .commonPrefixes(COMMON_PREFIXES)
                                    .build();

    @Test
    public void encodingTypeSet_decodesListObjectsResponseParts() {
        Context.ModifyResponse ctx = newContext(V1_TEST_ENCODED_RESPONSE);

        ListObjectsResponse decoded = (ListObjectsResponse) INTERCEPTOR.modifyResponse(ctx, new ExecutionAttributes());

        assertDecoded(decoded::delimiter, " delimiter");
        assertDecoded(decoded::nextMarker, " nextmarker");
        assertDecoded(decoded::prefix, " prefix");
        assertDecoded(decoded::marker, " marker");
        assertKeysAreDecoded(decoded.contents());
        assertCommonPrefixesAreDecoded(decoded.commonPrefixes());
    }

    @Test
    public void encodingTypeSet_decodesListObjectsV2ResponseParts() {
        Context.ModifyResponse ctx = newContext(V2_TEST_ENCODED_RESPONSE);

        ListObjectsV2Response decoded = (ListObjectsV2Response) INTERCEPTOR.modifyResponse(ctx, new ExecutionAttributes());

        assertDecoded(decoded::delimiter, " delimiter");
        assertDecoded(decoded::prefix, " prefix");
        assertDecoded(decoded::startAfter, " startafter");
        assertKeysAreDecoded(decoded.contents());
        assertCommonPrefixesAreDecoded(decoded.commonPrefixes());
    }

    @Test
    public void encodingTypeSet_decodesListObjectVersionsResponse() {
        Context.ModifyResponse ctx = newContext(TEST_LIST_OBJECT_VERSION_RESPONSE);

        ListObjectVersionsResponse decoded = (ListObjectVersionsResponse) INTERCEPTOR.modifyResponse(ctx, new ExecutionAttributes());

        assertDecoded(decoded::delimiter, " delimiter");
        assertDecoded(decoded::prefix, " prefix");
        assertDecoded(decoded::keyMarker, " keyMarker");
        assertDecoded(decoded::nextKeyMarker, " nextKeyMarker");
        assertCommonPrefixesAreDecoded(decoded.commonPrefixes());
        assertVersionsAreDecoded(decoded.versions());
    }

    @Test
    public void encodingTypeSet_decodesListMultipartUploadsResponse() {
        Context.ModifyResponse ctx = newContext(TEST_LIST_MULTIPART_UPLOADS_RESPONSE);

        ListMultipartUploadsResponse decoded = (ListMultipartUploadsResponse) INTERCEPTOR.modifyResponse(ctx, new ExecutionAttributes());

        assertDecoded(decoded::delimiter, " delimiter");
        assertDecoded(decoded::prefix, " prefix");
        assertDecoded(decoded::keyMarker, " keyMarker");
        assertDecoded(decoded::nextKeyMarker, " nextKeyMarker");
        assertCommonPrefixesAreDecoded(decoded.commonPrefixes());
        assertUploadsAreDecoded(decoded.uploads());
        assertCommonPrefixesAreDecoded(decoded.commonPrefixes());
    }

    @Test
    public void encodingTypeNotSet_doesNotDecodeListObjectsResponseParts() {
        ListObjectsResponse original = V1_TEST_ENCODED_RESPONSE.toBuilder()
                .encodingType((String) null)
                .build();

        Context.ModifyResponse ctx = newContext(original);

        ListObjectsResponse fromInterceptor = (ListObjectsResponse) INTERCEPTOR.modifyResponse(ctx, new ExecutionAttributes());

        assertThat(fromInterceptor).isEqualTo(original);
    }

    @Test
    public void encodingTypeNotSet_doesNotDecodeListObjectsV2ResponseParts() {
        ListObjectsV2Response original = V2_TEST_ENCODED_RESPONSE.toBuilder()
                .encodingType((String) null)
                .build();

        Context.ModifyResponse ctx = newContext(original);

        ListObjectsV2Response fromInterceptor = (ListObjectsV2Response) INTERCEPTOR.modifyResponse(ctx, new ExecutionAttributes());

        assertThat(fromInterceptor).isEqualTo(original);
    }

    @Test
    public void otherResponses_shouldNotModifyResponse() {
        HeadObjectResponse original = HeadObjectResponse.builder().build();
        Context.ModifyResponse ctx = newContext(original);
        SdkResponse sdkResponse = INTERCEPTOR.modifyResponse(ctx, new ExecutionAttributes());
        assertThat(original.hashCode()).isEqualTo(sdkResponse.hashCode());
    }

    private void assertKeysAreDecoded(List<S3Object> objects) {
        objects.forEach(o -> assertDecoded(o::key));
    }

    private void assertCommonPrefixesAreDecoded(List<CommonPrefix> commonPrefixes) {
        commonPrefixes.forEach(c -> assertDecoded(c::prefix, " prefix"));
    }

    private void assertDecoded(Supplier<String> supplier) {
        assertDecoded(supplier, "");
    }

    private void assertDecoded(Supplier<String> supplier, String suffix) {
        assertThat(supplier.get()).isEqualTo(TEST_URL_DECODED + suffix);
    }

    private void assertVersionsAreDecoded(List<ObjectVersion> versions) {
        versions.forEach(v -> assertDecoded(v::key, " key"));
    }

    private void assertUploadsAreDecoded(List<MultipartUpload> uploads) {
        uploads.forEach(u -> assertDecoded(u::key, " key"));
    }


    private static Context.ModifyResponse newContext(SdkResponse response) {
        return new Context.ModifyResponse() {
            @Override
            public Optional<Publisher<ByteBuffer>> responsePublisher() {
                return null;
            }

            @Override
            public Optional<InputStream> responseBody() {
                return null;
            }

            @Override
            public SdkHttpRequest httpRequest() {
                return null;
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
            public SdkResponse response() {
                return response;
            }

            @Override
            public SdkHttpFullResponse httpResponse() {
                return null;
            }

            @Override
            public SdkRequest request() {
                return null;
            }
        };
    }
}
