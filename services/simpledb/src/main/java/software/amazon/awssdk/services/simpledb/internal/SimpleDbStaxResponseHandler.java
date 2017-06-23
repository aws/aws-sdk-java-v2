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

package software.amazon.awssdk.services.simpledb.internal;

import java.util.Map;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.http.StaxResponseHandler;
import software.amazon.awssdk.runtime.transform.StaxUnmarshallerContext;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.services.simpledb.SimpleDbResponseMetadata;

public class SimpleDbStaxResponseHandler<T> extends StaxResponseHandler<T> {

    public SimpleDbStaxResponseHandler(Unmarshaller<T, StaxUnmarshallerContext> responseUnmarshaller) {
        super(responseUnmarshaller);
    }

    @Override
    protected void registerAdditionalMetadataExpressions(StaxUnmarshallerContext unmarshallerContext) {
        unmarshallerContext.registerMetadataExpression("ResponseMetadata/BoxUsage", 2,
                                                       SimpleDbResponseMetadata.BOX_USAGE);
    }

    @Override
    protected ResponseMetadata getResponseMetadata(Map<String, String> metadata) {
        return new SimpleDbResponseMetadata(super.getResponseMetadata(metadata));
    }
}
