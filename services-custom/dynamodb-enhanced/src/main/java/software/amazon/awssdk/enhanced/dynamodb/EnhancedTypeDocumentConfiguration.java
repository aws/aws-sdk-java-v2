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

package software.amazon.awssdk.enhanced.dynamodb;


import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration for {@link EnhancedType} of document type
 */
@SdkPublicApi
public final class EnhancedTypeDocumentConfiguration implements ToCopyableBuilder<EnhancedTypeDocumentConfiguration.Builder,
    EnhancedTypeDocumentConfiguration> {
    private final boolean preserveEmptyObject;

    public EnhancedTypeDocumentConfiguration(Builder builder) {
        this.preserveEmptyObject = builder.preserveEmptyObject != null && builder.preserveEmptyObject;
    }

    /**
     * @return whether to initialize the associated {@link EnhancedType} as empty class when
     * mapping it to a Java object
     */
    public boolean preserveEmptyObject() {
        return preserveEmptyObject;
    }

    @Override
    public Builder toBuilder() {
        return builder().preserveEmptyObject(preserveEmptyObject);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements CopyableBuilder<Builder, EnhancedTypeDocumentConfiguration> {
        private Boolean preserveEmptyObject;

        private Builder() {
        }

        /**
         * Specifies whether to initialize the associated {@link EnhancedType} as empty class when
         * mapping it to a Java object
         */
        public Builder preserveEmptyObject(Boolean preserveEmptyObject) {
            this.preserveEmptyObject = preserveEmptyObject;
            return this;
        }

        @Override
        public EnhancedTypeDocumentConfiguration build() {
            return new EnhancedTypeDocumentConfiguration(this);
        }
    }
}
