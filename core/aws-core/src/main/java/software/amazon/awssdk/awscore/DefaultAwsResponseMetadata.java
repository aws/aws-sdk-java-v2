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

import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkStandardLogger;

/**
 * Represents additional metadata included with a response from a service. Response
 * metadata varies by service, but all services return an AWS request ID that
 * can be used in the event a service call isn't working as expected and you
 * need to work with AWS support to debug an issue.
 * <p>
 * Access to AWS request IDs is also available through the {@link SdkStandardLogger#REQUEST_ID_LOGGER}
 * logger.
 */
@SdkProtectedApi
public final class DefaultAwsResponseMetadata extends AwsResponseMetadata {
    /**
     * Creates a new ResponseMetadata object from a specified map of raw
     * metadata information.
     *
     * @param metadata
     *            The raw metadata for the new ResponseMetadata object.
     */
    private DefaultAwsResponseMetadata(Map<String, String> metadata) {
        super(metadata);
    }

    public static DefaultAwsResponseMetadata create(Map<String, String> metadata) {
        return new DefaultAwsResponseMetadata(metadata);
    }
}
