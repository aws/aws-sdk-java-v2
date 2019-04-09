/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal.model;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.model.GeneratedRequestItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The default implementation of {@link GeneratedRequestItem}.
 */
@SdkInternalApi
@ThreadSafe
public class DefaultGeneratedRequestItem extends DefaultItem<AttributeValue> implements GeneratedRequestItem {
    private DefaultGeneratedRequestItem(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder
            extends DefaultItem.Builder<AttributeValue, Builder>
            implements GeneratedRequestItem.Builder {
        private Builder() {}

        private Builder(DefaultGeneratedRequestItem item) {
            super(item);
        }

        @Override
        public DefaultGeneratedRequestItem build() {
            return new DefaultGeneratedRequestItem(this);
        }
    }
}
