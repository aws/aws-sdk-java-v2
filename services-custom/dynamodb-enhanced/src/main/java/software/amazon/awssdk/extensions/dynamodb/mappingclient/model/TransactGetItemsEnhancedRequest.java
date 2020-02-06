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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public class TransactGetItemsEnhancedRequest {

    private final List<ReadTransaction> readTransactions;

    private TransactGetItemsEnhancedRequest(Builder builder) {
        this.readTransactions = Collections.unmodifiableList(builder.readTransactions);
    }

    public static TransactGetItemsEnhancedRequest create(Collection<ReadTransaction> transactGetRequests) {
        return builder().readTransactions(transactGetRequests).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().readTransactions(readTransactions);
    }

    public List<ReadTransaction> readTransactions() {
        return readTransactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactGetItemsEnhancedRequest that = (TransactGetItemsEnhancedRequest) o;

        return readTransactions != null ? readTransactions.equals(that.readTransactions) : that.readTransactions == null;
    }

    @Override
    public int hashCode() {
        return readTransactions != null ? readTransactions.hashCode() : 0;
    }

    public static final class Builder {
        private List<ReadTransaction> readTransactions;

        private Builder() {
        }

        public Builder readTransactions(Collection<ReadTransaction> readTransactions) {
            this.readTransactions = new ArrayList<>(readTransactions);
            return this;
        }

        public Builder readTransactions(ReadTransaction... readTransactions) {
            this.readTransactions = Arrays.asList(readTransactions);
            return this;
        }

        public Builder addWriteBatch(ReadTransaction readTransaction) {
            if (readTransactions == null) {
                readTransactions = new ArrayList<>();
            }
            readTransactions.add(readTransaction);
            return this;
        }

        public TransactGetItemsEnhancedRequest build() {
            return new TransactGetItemsEnhancedRequest(this);
        }
    }
}
