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

package software.amazon.awssdk.awscore;

import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.ToString;

/**
 * Represents additional metadata included with a response from a service. Response
 * metadata varies by service, but all services return an AWS request ID that
 * can be used in the event a service call isn't working as expected and you
 * need to work with AWS support to debug an issue.
 */
@SdkProtectedApi
public abstract class AwsResponseMetadata {
    private static final String UNKNOWN = "UNKNOWN";

    private final Map<String, String> metadata;

    /**
     * Creates a new ResponseMetadata object from a specified map of raw
     * metadata information.
     *
     * @param metadata
     *            The raw metadata for the new ResponseMetadata object.
     */
    protected AwsResponseMetadata(Map<String, String> metadata) {
        this.metadata = Collections.unmodifiableMap(metadata);
    }

    protected AwsResponseMetadata(AwsResponseMetadata responseMetadata) {
        this(responseMetadata.metadata);
    }

    /**
     * Returns the AWS request ID contained in this response metadata object.
     * AWS request IDs can be used in the event a service call isn't working as
     * expected and you need to work with AWS support to debug an issue.
     *
     * <p>
     * Can be overriden by subclasses to provide service specific requestId
     *
     * @return The AWS request ID contained in this response metadata object.
     */
    public String requestId() {
        return getValue(AWS_REQUEST_ID);
    }

    @Override
    public final String toString() {
        return ToString.builder("AwsResponseMetadata")
                       .add("metadata", metadata.keySet())
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsResponseMetadata that = (AwsResponseMetadata) o;
        return Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(metadata);
    }

    /**
     * Get the value based on the key, returning {@link #UNKNOWN} if the value is null.
     *
     * @param key the key of the value
     * @return the value or {@link #UNKNOWN} if not present.
     */
    protected final String getValue(String key) {
        return Optional.ofNullable(metadata.get(key)).orElse(UNKNOWN);
    }
}
