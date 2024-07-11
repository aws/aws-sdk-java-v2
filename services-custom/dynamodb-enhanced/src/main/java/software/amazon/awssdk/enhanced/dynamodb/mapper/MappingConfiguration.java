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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration Object to define behaviour of DynamoDB operations.
 */

@SdkPublicApi
public final class MappingConfiguration implements
                                  ToCopyableBuilder<MappingConfiguration.Builder, MappingConfiguration> {

    /**
     * @param AttributeMapping toggles between SHALLOW and NESTED to denote the mode of operation for the request
     */
    private final AttributeMapping attributeMapping;

    /**
     * @param ignoreNulls If set to true; any null values in the Java object will not be added to the output map. If set to false;
     * null values in the Java object will be added as {@link AttributeValue} of type 'nul' to the output map.
     */
    private final boolean ignoreNulls;

    private MappingConfiguration(Builder builder) {
        this.attributeMapping = builder.attributeMapping;
        this.ignoreNulls = builder.ignoreNulls;
    }

    public AttributeMapping attributeMapping() {
        return this.attributeMapping;
    }

    public boolean ignoreNulls() {
        return this.ignoreNulls;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
            .attributeMapping(attributeMapping)
            .ignoreNulls(ignoreNulls);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MappingConfiguration that = (MappingConfiguration) o;

        if (attributeMapping != that.attributeMapping) {
            return false;
        }
        return ignoreNulls == that.ignoreNulls;
        
    }

    @Override
    public int hashCode() {
        int result = attributeMapping != null ? attributeMapping.hashCode() : 0;
        result = 31 * result + Boolean.hashCode(ignoreNulls);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("MappingConfiguration")
                       .add("attributeMapping", attributeMapping)
                       .add("ignoreNulls", ignoreNulls)
                       .build();
    }

    public static final class Builder implements CopyableBuilder<Builder, MappingConfiguration> {

        private AttributeMapping attributeMapping;
        private boolean ignoreNulls;

        private Builder() {

        }

        public Builder attributeMapping(AttributeMapping attributeMapping) {
            this.attributeMapping = attributeMapping;
            return this;
        }

        public Builder ignoreNulls(boolean ignoreNulls) {
            this.ignoreNulls = ignoreNulls;
            return this;
        }

        @Override
        public MappingConfiguration build() {
            return new MappingConfiguration(this);
        }
    }
}
