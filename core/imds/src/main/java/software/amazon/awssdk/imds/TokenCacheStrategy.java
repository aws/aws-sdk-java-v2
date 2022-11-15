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

package software.amazon.awssdk.imds;

/**
 * Defines the different caching strategies for the token required when executing Ec2 Metadata requests.
 */
public enum TokenCacheStrategy {
    /**
     * No caching, a token request will be performed for every metadata requests.
     * This is the default behavior.
     */
    NONE,

    /**
     * Cache the token until it expires. When a request for metadata is performed while the token is expired, the Client will
     * block every request and perform a token call before resuming execution of the metadata requests.
     */
    BLOCKING
}
