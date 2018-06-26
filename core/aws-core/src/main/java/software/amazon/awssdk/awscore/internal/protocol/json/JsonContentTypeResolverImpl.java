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

package software.amazon.awssdk.awscore.internal.protocol.json;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;

/**
 * Prefers an explicit content type if provided. Otherwise computes the correct content type based
 * on the wire format used and the version of the protocol.
 */
@SdkInternalApi
public class JsonContentTypeResolverImpl implements JsonContentTypeResolver {

    private final String prefix;

    JsonContentTypeResolverImpl(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String resolveContentType(JsonClientMetadata clientMetadata, AwsJsonProtocolMetadata protocolMetadata) {
        return Optional.ofNullable(clientMetadata.getContentTypeOverride())
                       .orElse(prefix + protocolMetadata.protocolVersion());
    }
}
