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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a completed download transfer from Amazon S3. It can be used to track
 * the underlying {@link GetObjectResponse}
 *
 * @see S3TransferManager#downloadFile(DownloadFileRequest)
 */
@SdkPublicApi
public final class CompletedFileDownload implements CompletedObjectTransfer {
    private final GetObjectResponse response;

    private CompletedFileDownload(DefaultBuilder builder) {
        this.response = Validate.paramNotNull(builder.response, "response");
    }
    
    @Override
    public GetObjectResponse response() {
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompletedFileDownload that = (CompletedFileDownload) o;

        return Objects.equals(response, that.response);
    }

    @Override
    public int hashCode() {
        return response != null ? response.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedFileDownload")
                       .add("response", response)
                       .build();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder {
        /**
         * Specifies the {@link GetObjectResponse} from {@link S3AsyncClient#getObject}
         *
         * @param response the response
         * @return This builder for method chaining.
         */
        Builder response(GetObjectResponse response);

        /**
         * Builds a {@link CompletedFileUpload} based on the properties supplied to this builder
         * @return An initialized {@link CompletedFileDownload}
         */
        CompletedFileDownload build();
    }

    private static final class DefaultBuilder implements Builder {
        private GetObjectResponse response;

        private DefaultBuilder() {
        }

        @Override
        public Builder response(GetObjectResponse response) {
            this.response = response;
            return this;
        }

        public void setResponse(GetObjectResponse response) {
            response(response);
        }

        public GetObjectResponse getResponse() {
            return response;
        }

        @Override
        public CompletedFileDownload build() {
            return new CompletedFileDownload(this);
        }
    }
}
