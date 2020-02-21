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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public final class BatchWriteItemEnhancedRequest {

    private final List<WriteBatch> writeBatches;

    private BatchWriteItemEnhancedRequest(Builder builder) {
        this.writeBatches = Collections.unmodifiableList(builder.writeBatches);
    }

    public static BatchWriteItemEnhancedRequest create(Collection<WriteBatch> writeBatches) {
        return builder().writeBatches(writeBatches).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().writeBatches(writeBatches);
    }

    public Collection<WriteBatch> writeBatches() {
        return writeBatches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchWriteItemEnhancedRequest that = (BatchWriteItemEnhancedRequest) o;

        return writeBatches != null ? writeBatches.equals(that.writeBatches) : that.writeBatches == null;
    }

    @Override
    public int hashCode() {
        return writeBatches != null ? writeBatches.hashCode() : 0;
    }

    public static final class Builder {
        private List<WriteBatch> writeBatches;

        private Builder() {
        }

        public Builder writeBatches(Collection<WriteBatch> writeBatches) {
            this.writeBatches = new ArrayList<>(writeBatches);
            return this;
        }

        public Builder writeBatches(WriteBatch... writeBatches) {
            this.writeBatches = Arrays.asList(writeBatches);
            return this;
        }

        public Builder addWriteBatch(WriteBatch writeBatch) {
            if (writeBatches == null) {
                writeBatches = new ArrayList<>();
            }
            writeBatches.add(writeBatch);
            return this;
        }

        public BatchWriteItemEnhancedRequest build() {
            return new BatchWriteItemEnhancedRequest(this);
        }
    }

}
