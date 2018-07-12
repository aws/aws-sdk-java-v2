/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.util;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * No-op response metadata cache used when a client instance is configured
 * without response metadata caching enabled.
 */
@SdkInternalApi
public class NullResponseMetadataCache implements MetadataCache {

    @Override
    public void add(Object obj, SdkResponseMetadata metadata) {
        // deliberately left blank
    }

    @Override
    public SdkResponseMetadata get(Object obj) {
        throw SdkClientException.builder().message("Response metadata caching is not enabled").build();
    }

}
