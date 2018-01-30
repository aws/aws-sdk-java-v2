/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document.spec;

import java.util.Collection;
import java.util.List;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexUpdate;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;

/**
 * Full parameter specification for the UpdateTable API.
 */
public class UpdateTableSpec extends AbstractSpec<UpdateTableRequest> {
    public UpdateTableSpec() {
        super(UpdateTableRequest.builder().build());
    }

    public ProvisionedThroughput getProvisionedThroughput() {
        return getRequest().provisionedThroughput();
    }

    public UpdateTableSpec withProvisionedThroughput(
            ProvisionedThroughput provisionedThroughput) {
        setRequest(getRequest().toBuilder().provisionedThroughput(provisionedThroughput).build());
        return this;
    }

    public List<AttributeDefinition> getAttributeDefinitions() {
        return getRequest().attributeDefinitions();
    }

    public UpdateTableSpec withAttributeDefinitions(
            AttributeDefinition... attributeDefinitions) {
        setRequest(getRequest().toBuilder().attributeDefinitions(attributeDefinitions).build());
        return this;
    }

    public UpdateTableSpec withAttributeDefinitions(
            Collection<AttributeDefinition> attributeDefinitions) {
        setRequest(getRequest().toBuilder().attributeDefinitions(attributeDefinitions).build());
        return this;
    }

    public UpdateTableSpec withGlobalSecondaryIndexUpdates(
            GlobalSecondaryIndexUpdate... globalSecondaryIndexUpdates) {
        setRequest(getRequest().toBuilder().globalSecondaryIndexUpdates(
                globalSecondaryIndexUpdates).build());
        return this;
    }

    public UpdateTableSpec withGlobalSecondaryIndexUpdates(
            Collection<GlobalSecondaryIndexUpdate> globalSecondaryIndexUpdates) {
        setRequest(getRequest().toBuilder().globalSecondaryIndexUpdates(
                globalSecondaryIndexUpdates)
                .build());
        return this;
    }
}
