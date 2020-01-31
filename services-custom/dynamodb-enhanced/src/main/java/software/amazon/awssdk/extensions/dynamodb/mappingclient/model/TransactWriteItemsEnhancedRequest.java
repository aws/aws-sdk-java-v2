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
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public class TransactWriteItemsEnhancedRequest {

    private final List<WriteTransaction> writeTransactions;

    private TransactWriteItemsEnhancedRequest(List<WriteTransaction> writeTransactions) {
        this.writeTransactions = writeTransactions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().writeTransactions(this.writeTransactions);
    }

    public List<WriteTransaction> writeTransactions() {
        return writeTransactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactWriteItemsEnhancedRequest that = (TransactWriteItemsEnhancedRequest) o;

        return writeTransactions != null ? writeTransactions.equals(that.writeTransactions) : that.writeTransactions == null;
    }

    @Override
    public int hashCode() {
        return writeTransactions != null ? writeTransactions.hashCode() : 0;
    }

    public static final class Builder {
        private List<WriteTransaction> writeTransactions;

        private Builder() {
        }

        public Builder writeTransactions(List<WriteTransaction> writeTransactions) {
            this.writeTransactions = writeTransactions;
            return this;
        }

        public Builder addWriteBatch(WriteTransaction writeTransaction) {
            if (writeTransactions == null) {
                writeTransactions = new ArrayList<>();
            }
            writeTransactions.add(writeTransaction);
            return this;
        }

        public Builder writeTransactions(WriteTransaction... writeTransactions) {
            this.writeTransactions = Arrays.asList(writeTransactions);
            return this;
        }

        public TransactWriteItemsEnhancedRequest build() {
            return new TransactWriteItemsEnhancedRequest(this.writeTransactions);
        }
    }
}
