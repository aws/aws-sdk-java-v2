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

package software.amazon.awssdk.services.codepipeline;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.codepipeline.model.ActionCategory;
import software.amazon.awssdk.services.codepipeline.model.ActionOwner;
import software.amazon.awssdk.services.codepipeline.model.ActionType;
import software.amazon.awssdk.services.codepipeline.model.ActionTypeId;
import software.amazon.awssdk.services.codepipeline.model.ArtifactDetails;
import software.amazon.awssdk.services.codepipeline.model.CreateCustomActionTypeRequest;
import software.amazon.awssdk.services.codepipeline.model.DeleteCustomActionTypeRequest;
import software.amazon.awssdk.services.codepipeline.model.InvalidNextTokenException;
import software.amazon.awssdk.services.codepipeline.model.ListActionTypesRequest;
import software.amazon.awssdk.services.codepipeline.model.ListActionTypesResponse;
import software.amazon.awssdk.services.codepipeline.model.ListPipelinesRequest;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Smoke tests for the {@link CodePipelineClient}.
 */
public class AwsCodePipelineClientIntegrationTest extends AwsTestBase {

    private static CodePipelineClient client;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        client = CodePipelineClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    /**
     * Determine whether the requested action type is in the provided list.
     *
     * @param actionTypes
     *            List of {@link ActionType}s to search
     * @param actionTypeId
     *            {@link ActionType} to search for
     * @return True if actionTypeId is in actionTypes, false otherwise
     */
    private static boolean containsActionTypeId(List<ActionType> actionTypes, ActionTypeId actionTypeId) {
        for (ActionType actionType : actionTypes) {
            if (actionType.id().equals(actionTypeId)) {
                return true;
            }
        }
        return false;
    }

    @Test(expected = InvalidNextTokenException.class)
    public void listPipelines_WithInvalidNextToken_ThrowsInvalidNextTokenException() {
        client.listPipelines(ListPipelinesRequest.builder().nextToken("invalid_next_token").build());
    }

    @Test
    public void listActionTypes_WithNoFilter_ReturnsNonEmptyList() {
        assertThat(client.listActionTypes(ListActionTypesRequest.builder().build()).actionTypes().size(), greaterThan(0));
    }

    /**
     * Simple smoke test to create a custom action, make sure it was persisted, and then
     * subsequently delete it.
     */
    @Test
    public void createFindDelete_ActionType() {
        ActionTypeId actionTypeId = ActionTypeId.builder()
                .category(ActionCategory.Build)
                .provider("test-provider")
                .version("1")
                .owner(ActionOwner.Custom)
                .build();
        ArtifactDetails artifactDetails = ArtifactDetails.builder()
                .maximumCount(1)
                .minimumCount(1)
                .build();
        client.createCustomActionType(CreateCustomActionTypeRequest.builder()
                .category(actionTypeId.category())
                .provider(actionTypeId.provider())
                .version(actionTypeId.version())
                .inputArtifactDetails(artifactDetails)
                .outputArtifactDetails(artifactDetails)
                .build());
        final ListActionTypesResponse actionTypes = client.listActionTypes(ListActionTypesRequest.builder().build());
        assertTrue(containsActionTypeId(actionTypes.actionTypes(), actionTypeId));
        client.deleteCustomActionType(DeleteCustomActionTypeRequest.builder()
                .category(actionTypeId.category())
                .provider(actionTypeId.provider())
                .version(actionTypeId.version()).build());
    }

}
