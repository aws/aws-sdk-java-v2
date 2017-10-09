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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents additional metadata included with a response from AWS. Response
 * metadata varies by service, but all services return an AWS request ID that
 * can be used in the event a service call isn't working as expected and you
 * need to work with AWS support to debug an issue.
 * <p>
 * Access to AWS request IDs is also available through the com.amazonaws.request
 * logger in the AWS SDK for Java.
 */
public class AwsResponseMetadata implements SdkResponseMetadata<AwsResponseMetadata.Builder, AwsResponseMetadata> {
    public static final String AWS_REQUEST_ID = "AWS_REQUEST_ID";

    protected final Map<String, String> metadata;

    /**
     * Creates a new ResponseMetadata object from a specified map of raw
     * metadata information.
     *
     * @param metadata
     *            The raw metadata for the new ResponseMetadata object.
     */
    public AwsResponseMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public AwsResponseMetadata(BuilderImpl builder) {
        this.metadata = builder.metadata;
    }

    /**
     * Creates a new ResponseMetadata object from an existing ResponseMetadata
     * object.
     *
     * @param originalResponseMetadata
     *            The ResponseMetadata object from which to create the new
     *            object.
     */
    public AwsResponseMetadata(AwsResponseMetadata originalResponseMetadata) {
        this(originalResponseMetadata.metadata);
    }

    /**
     * Returns the AWS request ID contained in this response metadata object.
     * AWS request IDs can be used in the event a service call isn't working as
     * expected and you need to work with AWS support to debug an issue.
     *
     * @return The AWS request ID contained in this response metadata object.
     */
    public String getRequestId() {
        return metadata.get(AWS_REQUEST_ID);
    }

    @Override
    public String toString() {
        if (metadata == null) {
            return "{}";
        }
        return metadata.toString();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends SdkResponseMetadata.Builder<Builder, AwsResponseMetadata> {
        Builder metadata(Map<String, String> metadata);
    }

    private static class BuilderImpl implements Builder {
        private Map<String, String> metadata;

        private BuilderImpl(AwsResponseMetadata responseMetadata) {
            metadata(responseMetadata.metadata);
        }

        @Override
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public void setMetadata(Map<String, String> metadata) {
            metadata(metadata);
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        @Override
        public AwsResponseMetadata build() {
            return new AwsResponseMetadata(this);
        }
    }
}
