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

package software.amazon.awssdk.protocols.json.internal;

import com.fasterxml.jackson.core.JsonFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.json.BaseAwsStructuredJsonFactory;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

/**
 * Creates generators and protocol handlers for plain text JSON wire format.
 */
@SdkInternalApi
public final class AwsStructuredPlainJsonFactory {

    /**
     * Recommended to share JsonFactory instances per http://wiki.fasterxml
     * .com/JacksonBestPracticesPerformance
     */
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    public static final BaseAwsStructuredJsonFactory SDK_JSON_FACTORY = new BaseAwsStructuredJsonFactory(JSON_FACTORY) {
        @Override
        protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                       String contentType) {
            return new SdkJsonGenerator(jsonFactory, contentType);
        }

        @Override
        public JsonFactory getJsonFactory() {
            return JSON_FACTORY;
        }
    };

    protected AwsStructuredPlainJsonFactory() {
    }
}
