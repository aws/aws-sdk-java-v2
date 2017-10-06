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

package software.amazon.awssdk.services.machinelearning.internal;

import java.util.UUID;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.machinelearning.model.CreateBatchPredictionRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromRDSRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromRedshiftRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromS3Request;
import software.amazon.awssdk.services.machinelearning.model.CreateEvaluationRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateMLModelRequest;

/**
 * CreateXxx API calls require a unique (for all time!) ID parameter for
 * idempotency. If the user doesn't specify one, fill in a GUID.
 */
@ReviewBeforeRelease("They should be using the idempotency trait")
public class RandomIdInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest request = context.request();

        if (request instanceof CreateBatchPredictionRequest) {
            CreateBatchPredictionRequest copy = (CreateBatchPredictionRequest) request;

            if (copy.batchPredictionDataSourceId() == null) {
                return copy.toBuilder().batchPredictionDataSourceId(UUID.randomUUID().toString()).build();
            }

            return copy;
        } else if (request instanceof CreateDataSourceFromRDSRequest) {
            CreateDataSourceFromRDSRequest copy =
                    (CreateDataSourceFromRDSRequest) request;

            if (copy.dataSourceId() == null) {
                copy = copy.toBuilder().dataSourceId(UUID.randomUUID().toString()).build();
            }

            return copy;
        } else if (request instanceof CreateDataSourceFromRedshiftRequest) {
            CreateDataSourceFromRedshiftRequest copy =
                    (CreateDataSourceFromRedshiftRequest) request;

            if (copy.dataSourceId() == null) {
                copy = copy.toBuilder().dataSourceId(UUID.randomUUID().toString()).build();
            }

            return copy;
        } else if (request instanceof CreateDataSourceFromS3Request) {
            CreateDataSourceFromS3Request copy =
                    (CreateDataSourceFromS3Request) request;

            if (copy.dataSourceId() == null) {
                copy = copy.toBuilder().dataSourceId(UUID.randomUUID().toString()).build();
            }

            return copy;
        } else if (request instanceof CreateEvaluationRequest) {
            CreateEvaluationRequest copy =
                    (CreateEvaluationRequest) request;

            if (copy.evaluationId() == null) {
                copy = copy.toBuilder().evaluationId(UUID.randomUUID().toString()).build();
            }

            return copy;
        } else if (request instanceof CreateMLModelRequest) {
            CreateMLModelRequest copy = (CreateMLModelRequest) request;

            if (copy.mlModelId() == null) {
                copy = copy.toBuilder().mlModelId(UUID.randomUUID().toString()).build();
            }

            return copy;
        }

        return request;
    }

}
