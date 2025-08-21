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

package software.amazon.awssdk.transfer.s3.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

class PresignedDownloadRequestTest {

    @Test
    void build_withoutPresignedUrlDownloadRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> PresignedDownloadRequest.builder()
                                                        .responseTransformer(AsyncResponseTransformer.toBytes())
                                                        .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("presignedUrlDownloadRequest");
    }

    @Test
    void builder_withoutResponseTransformer_returnsUntypedBuilder() {
        PresignedDownloadRequest.UntypedBuilder untypedBuilder = PresignedDownloadRequest.builder()
                                                                                        .presignedUrlDownloadRequest(createPresignedRequest());

        assertThat(untypedBuilder).isNotNull();
    }

    @Test
    void build_withResponseTransformer_createsRequestCorrectly() {
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> responseTransformer =
            AsyncResponseTransformer.toBytes();
        PresignedUrlDownloadRequest presignedRequest = createPresignedRequest();

        PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> request = 
            PresignedDownloadRequest.builder()
                                   .presignedUrlDownloadRequest(presignedRequest)
                                   .responseTransformer(responseTransformer)
                                   .build();

        assertThat(request.responseTransformer()).isEqualTo(responseTransformer);
        assertThat(request.presignedUrlDownloadRequest()).isEqualTo(presignedRequest);
    }

    @Test
    void presignedUrlDownloadRequest_withConsumerBuilder_buildsCorrectly() {
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> responseTransformer =
            AsyncResponseTransformer.toBytes();

        PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> request = 
            PresignedDownloadRequest.builder()
                                   .presignedUrlDownloadRequest(b -> b.presignedUrl(createTestUrl()))
                                   .responseTransformer(responseTransformer)
                                   .build();

        assertThat(request.presignedUrlDownloadRequest()).isNotNull();
        assertThat(request.presignedUrlDownloadRequest().presignedUrl()).isEqualTo(createTestUrl());
    }

    @Test
    void responseTransformer_withNull_throwsNullPointerException() {
        assertThatThrownBy(() -> PresignedDownloadRequest.builder()
                                                        .presignedUrlDownloadRequest(createPresignedRequest())
                                                        .responseTransformer(null)
                                                        .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("responseTransformer");
    }

    @Test
    void builder_untypedToTypedTransition_worksCorrectly() {
        PresignedDownloadRequest.UntypedBuilder untypedBuilder = PresignedDownloadRequest.builder()
            .presignedUrlDownloadRequest(createPresignedRequest());

        PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> request = 
            untypedBuilder.responseTransformer(AsyncResponseTransformer.toBytes())
                         .build();

        assertThat(request.responseTransformer()).isNotNull();
        assertThat(request.presignedUrlDownloadRequest()).isNotNull();
    }

    @Test
    void toBuilder_copiesAllFields() {
        PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> original = 
            PresignedDownloadRequest.builder()
                                   .presignedUrlDownloadRequest(createPresignedRequest())
                                   .responseTransformer(AsyncResponseTransformer.toBytes())
                                   .build();

        PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> copy = original.toBuilder().build();

        assertThat(copy.responseTransformer()).isEqualTo(original.responseTransformer());
        assertThat(copy.presignedUrlDownloadRequest()).isEqualTo(original.presignedUrlDownloadRequest());
    }

    @Test
    void equalsHashCodeTest() {
        EqualsVerifier.forClass(PresignedDownloadRequest.class)
                      .withNonnullFields("responseTransformer", "presignedUrlDownloadRequest")
                      .verify();
    }

    private PresignedUrlDownloadRequest createPresignedRequest() {
        return PresignedUrlDownloadRequest.builder()
                                         .presignedUrl(createTestUrl())
                                         .build();
    }

    private URL createTestUrl() {
        try {
            return new URL("https://example.com/test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}