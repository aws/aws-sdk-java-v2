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

package software.amazon.awssdk.core.exception;


import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Extension of {@link SdkClientException} thrown when EC2 Instance Metadata Service (IMDS)
 * returns a non-successful response (4XX codes).
 * This exception includes HTTP status codes from IMDS responses  to enable detailed error handling.
 * <p>Clients can use {@link #statusCode()} to access the specific HTTP status code
 * for granular error handling and logging.
 */


@SdkPublicApi
public class Ec2MetadataClientException extends SdkClientException{

    private final int statusCode;

    private Ec2MetadataClientException(BuilderImpl builder) {
        super(builder);
        this.statusCode = builder.statusCode;
    }

    /**
     * @return The HTTP status code returned by the IMDS service.
     */
    public int statusCode() {
        return statusCode;
    }


    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkClientException.Builder {
        Builder statusCode(int statusCode);

        @Override
        Ec2MetadataClientException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private int statusCode;

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public Ec2MetadataClientException build() {
            return new Ec2MetadataClientException(this);
        }
    }
}
