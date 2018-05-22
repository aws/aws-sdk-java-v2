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

package software.amazon.awssdk.awscore.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.protocol.json.SdkJsonGenerator;
import software.amazon.awssdk.core.protocol.json.SdkStructuredPlainJsonFactory;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;

/**
 * Creates generators and protocol handlers for plain text JSON wire format.
 */
@SdkProtectedApi
final class AwsStructuredPlainJsonFactory extends SdkStructuredPlainJsonFactory {

    static final AwsStructuredJsonFactory SDK_JSON_FACTORY = new BaseAwsStructuredJsonFactory(
            JSON_FACTORY, JSON_SCALAR_UNMARSHALLERS) {
        @Override
        protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                       String contentType) {
            return new SdkJsonGenerator(jsonFactory, contentType);
        }
    };

    protected AwsStructuredPlainJsonFactory() {
    }
}
