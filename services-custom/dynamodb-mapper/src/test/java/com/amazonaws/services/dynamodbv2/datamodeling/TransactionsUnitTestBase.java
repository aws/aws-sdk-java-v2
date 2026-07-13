/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
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
package com.amazonaws.services.dynamodbv2.datamodeling;

import com.amazonaws.dynamodbv2.test.util.DynamoDBUnitTestBase;
import com.amazonaws.services.dynamodbv2.model.CancellationReason;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;

public class TransactionsUnitTestBase extends DynamoDBUnitTestBase {

    protected static DynamoDBMapper dynamoMapper;
    protected static final DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    protected static final DynamoDBMapperModelFactory.TableFactory models = factory.getTableFactory(DynamoDBMapperConfig.DEFAULT);

    @SuppressWarnings("unchecked")
    protected static <T> DynamoDBMapperTableModel<T> getTable(T object) {
        return models.getTable((Class<T>)object.getClass());
    }

    /**
     * Use this method to transactionLoad objects for verification after executing transactionWrite.
     */
    protected List<Object> transactionLoadObjects(TransactionLoadRequest transactionLoadRequest) {
        return transactionLoadObjects(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);
    }

    /**
     * Use this method to transactionLoad objects for verification after executing transactionWrite.
     */
    protected List<Object> transactionLoadObjects(TransactionLoadRequest transactionLoadRequest, DynamoDBMapperConfig config) {
        // transactionLoad can sometimes fail due to conflict with a recently submitted transactionWrite which is not yet complete, so we re-try for
        // some time when that type of failure is encountered
        long endTime = System.currentTimeMillis() + 30 * 1000;
        List<Object> actualResponseObjects = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                actualResponseObjects = dynamoMapper.transactionLoad(transactionLoadRequest, config);
                break;
            } catch (TransactionCanceledException tce) {
                List<CancellationReason> cancellationReasons = tce.getCancellationReasons();
                Set<String> uniqueCancellationReasonCodes = new HashSet<String>();
                for (CancellationReason cancellationReason: cancellationReasons) {
                    uniqueCancellationReasonCodes.add(cancellationReason.getCode());
                }
                if (uniqueCancellationReasonCodes.size() != 1 || !"TransactionConflict".equals(cancellationReasons.get(0).getCode())) {
                    fail("transactionLoad failed with TransactionCanceledException having non-TransactionConflict cancellation reason(s): " + tce);
                }
                // Sleep for some time before re-trying transactionLoad
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    fail("interrupted while waiting to re-try transactionLoad: " + ie);
                }
            }
        }
        if (actualResponseObjects == null) {
            fail("Timed out while executing transactionLoad due to conflict with ongoing transactionWrite");
        }
        return actualResponseObjects;
    }
}
