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

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class JsonContentResolverFactory {
    /**
     * Content type resolver implementation for Ion-enabled services.
     */
    public static final JsonContentTypeResolver ION_BINARY = new JsonContentTypeResolverImpl("application/x-amz-ion-");

    /**
     * Content type resolver implementation for debugging Ion-enabled services.
     */
    public static final JsonContentTypeResolver ION_TEXT = new JsonContentTypeResolverImpl("text/x-amz-ion-");

    /**
     * Content type resolver implementation for AWS_CBOR enabled services.
     */
    public static final JsonContentTypeResolver AWS_CBOR = new JsonContentTypeResolverImpl("application/x-amz-cbor-");

    /**
     * Content type resolver implementation for plain text AWS_JSON services.
     */
    public static final JsonContentTypeResolver AWS_JSON = new JsonContentTypeResolverImpl("application/x-amz-json-");

    private JsonContentResolverFactory() {
    }
}
