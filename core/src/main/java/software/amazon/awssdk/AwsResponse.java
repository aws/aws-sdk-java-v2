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

package software.amazon.awssdk;

/**
 * Base class for all AWS Service responses.
 */
public abstract class AwsResponse<B extends AwsResponse.Builder<B,R, M>,
        R extends AwsResponse<B,R, M>,
        M extends AwsResponseMetadata> extends SdkResponse<B, R, AwsResponseMetadata> {

    protected AwsResponse(B builder) {
        super(builder);
    }

    public interface Builder<B extends AwsResponse.Builder<B,R, M>,
            R extends AwsResponse<B,R, M>,
            M extends AwsResponseMetadata> extends SdkResponse.Builder<B, R, AwsResponseMetadata> {

    }

    protected static abstract class BuilderImpl<B extends AwsResponse.Builder<B,R, M>,
            R extends AwsResponse<B,R, M>,
            M extends AwsResponseMetadata> extends SdkResponse.BuilderImpl<B, R, AwsResponseMetadata> implements Builder<B, R, M> {
        protected BuilderImpl(Class<B> concrete) {
            super(concrete);
        }

        protected BuilderImpl(Class<B> concrete, R response) {
            super(concrete, response);
        }
    }
}
