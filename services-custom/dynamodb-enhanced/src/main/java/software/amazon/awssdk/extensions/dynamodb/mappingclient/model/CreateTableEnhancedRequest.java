/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import java.util.Arrays;
import java.util.Collection;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

@SdkPublicApi
public final class CreateTableEnhancedRequest {
    private final ProvisionedThroughput provisionedThroughput;
    private final Collection<LocalSecondaryIndex> localSecondaryIndices;
    private final Collection<GlobalSecondaryIndex> globalSecondaryIndices;

    private CreateTableEnhancedRequest(Builder builder) {
        this.provisionedThroughput = builder.provisionedThroughput;
        this.localSecondaryIndices = builder.localSecondaryIndices;
        this.globalSecondaryIndices = builder.globalSecondaryIndices;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().provisionedThroughput(provisionedThroughput)
                        .localSecondaryIndices(localSecondaryIndices)
                        .globalSecondaryIndices(globalSecondaryIndices);
    }

    public ProvisionedThroughput provisionedThroughput() {
        return provisionedThroughput;
    }

    public Collection<LocalSecondaryIndex> localSecondaryIndices() {
        return localSecondaryIndices;
    }

    public Collection<GlobalSecondaryIndex> globalSecondaryIndices() {
        return globalSecondaryIndices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CreateTableEnhancedRequest that = (CreateTableEnhancedRequest) o;

        if (provisionedThroughput != null ? ! provisionedThroughput.equals(that.provisionedThroughput) :
            that.provisionedThroughput != null) {
            return false;
        }
        if (localSecondaryIndices != null ? ! localSecondaryIndices.equals(that.localSecondaryIndices) :
            that.localSecondaryIndices != null) {
            return false;
        }
        return globalSecondaryIndices != null ? globalSecondaryIndices.equals(that.globalSecondaryIndices) :
            that.globalSecondaryIndices == null;
    }

    @Override
    public int hashCode() {
        int result = provisionedThroughput != null ? provisionedThroughput.hashCode() : 0;
        result = 31 * result + (localSecondaryIndices != null ? localSecondaryIndices.hashCode() : 0);
        result = 31 * result + (globalSecondaryIndices != null ? globalSecondaryIndices.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private ProvisionedThroughput provisionedThroughput;
        private Collection<LocalSecondaryIndex> localSecondaryIndices;
        private Collection<GlobalSecondaryIndex> globalSecondaryIndices;

        private Builder() {
        }

        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        public Builder localSecondaryIndices(Collection<LocalSecondaryIndex> localSecondaryIndices) {
            this.localSecondaryIndices = localSecondaryIndices;
            return this;
        }

        public Builder localSecondaryIndices(LocalSecondaryIndex... localSecondaryIndices) {
            this.localSecondaryIndices = Arrays.asList(localSecondaryIndices);
            return this;
        }

        public Builder globalSecondaryIndices(Collection<GlobalSecondaryIndex> globalSecondaryIndices) {
            this.globalSecondaryIndices = globalSecondaryIndices;
            return this;
        }

        public Builder globalSecondaryIndices(GlobalSecondaryIndex... globalSecondaryIndices) {
            this.globalSecondaryIndices = Arrays.asList(globalSecondaryIndices);
            return this;
        }

        public CreateTableEnhancedRequest build() {
            return new CreateTableEnhancedRequest(this);
        }
    }

}
