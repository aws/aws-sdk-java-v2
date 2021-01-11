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

package software.amazon.awssdk.services.datapipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.datapipeline.model.ActivatePipelineRequest;
import software.amazon.awssdk.services.datapipeline.model.ActivatePipelineResponse;
import software.amazon.awssdk.services.datapipeline.model.CreatePipelineRequest;
import software.amazon.awssdk.services.datapipeline.model.CreatePipelineResponse;
import software.amazon.awssdk.services.datapipeline.model.DeletePipelineRequest;
import software.amazon.awssdk.services.datapipeline.model.DescribeObjectsRequest;
import software.amazon.awssdk.services.datapipeline.model.DescribeObjectsResponse;
import software.amazon.awssdk.services.datapipeline.model.DescribePipelinesRequest;
import software.amazon.awssdk.services.datapipeline.model.DescribePipelinesResponse;
import software.amazon.awssdk.services.datapipeline.model.Field;
import software.amazon.awssdk.services.datapipeline.model.GetPipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.GetPipelineDefinitionResponse;
import software.amazon.awssdk.services.datapipeline.model.ListPipelinesRequest;
import software.amazon.awssdk.services.datapipeline.model.ListPipelinesResponse;
import software.amazon.awssdk.services.datapipeline.model.PipelineObject;
import software.amazon.awssdk.services.datapipeline.model.PutPipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.PutPipelineDefinitionResponse;
import software.amazon.awssdk.services.datapipeline.model.ValidatePipelineDefinitionRequest;
import software.amazon.awssdk.services.datapipeline.model.ValidatePipelineDefinitionResponse;

public class DataPipelineIntegrationTest extends IntegrationTestBase {

    private static final String PIPELINE_NAME = "my-pipeline";
    private static final String PIPELINE_ID = "my-pipeline" + System.currentTimeMillis();
    private static final String PIPELINE_DESCRIPTION = "my pipeline";
    private static final String OBJECT_ID = "123";
    private static final String OBJECT_NAME = "object";
    private static final String VALID_KEY = "startDateTime";
    private static final String INVALID_KEY = "radom_key";
    private static final String FIELD_VALUE = "2012-09-25T17:00:00";
    private static String pipelineId;

    @AfterClass
    public static void tearDown() {
        try {
            dataPipeline.deletePipeline(DeletePipelineRequest.builder().pipelineId(pipelineId).build());
        } catch (Exception e) {
            // Do nothing.
        }
    }

    @Test
    public void testPipelineOperations() throws InterruptedException {
        // Create a pipeline.
        CreatePipelineResponse createPipelineResult = dataPipeline.createPipeline(
                CreatePipelineRequest.builder()
                        .name(PIPELINE_NAME)
                        .uniqueId(PIPELINE_ID)
                        .description(PIPELINE_DESCRIPTION)
                        .build());
        pipelineId = createPipelineResult.pipelineId();
        assertNotNull(pipelineId);


        // Invalid field
        PipelineObject pipelineObject = PipelineObject.builder()
                .id(OBJECT_ID + "1")
                .name(OBJECT_NAME)
                .fields(Field.builder()
                        .key(INVALID_KEY)
                        .stringValue(FIELD_VALUE)
                        .build())
                .build();

        ValidatePipelineDefinitionResponse validatePipelineDefinitionResult =
                dataPipeline.validatePipelineDefinition(ValidatePipelineDefinitionRequest.builder()
                        .pipelineId(pipelineId)
                        .pipelineObjects(pipelineObject)
                        .build());
        assertTrue(validatePipelineDefinitionResult.errored());
        assertNotNull(validatePipelineDefinitionResult.validationErrors());
        assertTrue(validatePipelineDefinitionResult.validationErrors().size() > 0);
        assertNotNull(validatePipelineDefinitionResult.validationErrors().get(0));
        assertNotNull(validatePipelineDefinitionResult.validationWarnings());
        assertEquals(0, validatePipelineDefinitionResult.validationWarnings().size());

        // Valid field
        pipelineObject = PipelineObject.builder()
                .id(OBJECT_ID)
                .name(OBJECT_NAME)
                .fields(Field.builder()
                        .key(VALID_KEY)
                        .stringValue(FIELD_VALUE)
                        .build())
                .build();

        // Validate pipeline definition.
        validatePipelineDefinitionResult =
                dataPipeline.validatePipelineDefinition(ValidatePipelineDefinitionRequest.builder()
                        .pipelineId(pipelineId)
                        .pipelineObjects(pipelineObject)
                        .build());
        assertFalse(validatePipelineDefinitionResult.errored());
        assertNotNull(validatePipelineDefinitionResult.validationErrors());
        assertEquals(0, validatePipelineDefinitionResult.validationErrors().size());
        assertNotNull(validatePipelineDefinitionResult.validationWarnings());
        assertEquals(0, validatePipelineDefinitionResult.validationWarnings().size());

        // Put pipeline definition.
        PutPipelineDefinitionResponse putPipelineDefinitionResult =
                dataPipeline.putPipelineDefinition(PutPipelineDefinitionRequest.builder()
                        .pipelineId(pipelineId)
                        .pipelineObjects(pipelineObject)
                        .build());
        assertFalse(putPipelineDefinitionResult.errored());
        assertNotNull(putPipelineDefinitionResult.validationErrors());
        assertEquals(0, putPipelineDefinitionResult.validationErrors().size());
        assertNotNull(putPipelineDefinitionResult.validationWarnings());
        assertEquals(0, putPipelineDefinitionResult.validationWarnings().size());

        // Get pipeline definition.
        GetPipelineDefinitionResponse pipelineDefinitionResult =
                dataPipeline.getPipelineDefinition(GetPipelineDefinitionRequest.builder().pipelineId(pipelineId).build());
        assertEquals(1, pipelineDefinitionResult.pipelineObjects().size());
        assertEquals(OBJECT_ID, pipelineDefinitionResult.pipelineObjects().get(0).id());
        assertEquals(OBJECT_NAME, pipelineDefinitionResult.pipelineObjects().get(0).name());
        assertEquals(1, pipelineDefinitionResult.pipelineObjects().get(0).fields().size());
        assertTrue(pipelineDefinitionResult.pipelineObjects().get(0).fields()
                                              .contains(Field.builder().key(VALID_KEY).stringValue(FIELD_VALUE).build()));

        // Activate a pipeline.
        ActivatePipelineResponse activatePipelineResult =
                dataPipeline.activatePipeline(ActivatePipelineRequest.builder().pipelineId(pipelineId).build());
        assertNotNull(activatePipelineResult);

        // List pipeline.
        ListPipelinesResponse listPipelinesResult = dataPipeline.listPipelines(ListPipelinesRequest.builder().build());
        assertTrue(listPipelinesResult.pipelineIdList().size() > 0);
        assertNotNull(pipelineId, listPipelinesResult.pipelineIdList().get(0).id());
        assertNotNull(PIPELINE_NAME, listPipelinesResult.pipelineIdList().get(0).name());

        Thread.sleep(1000 * 5);

        // Describe objects.
        DescribeObjectsResponse describeObjectsResult =
                dataPipeline.describeObjects(DescribeObjectsRequest.builder().pipelineId(pipelineId).objectIds(OBJECT_ID).build());
        assertEquals(1, describeObjectsResult.pipelineObjects().size());
        assertEquals(OBJECT_ID, describeObjectsResult.pipelineObjects().get(0).id());
        assertEquals(OBJECT_NAME, describeObjectsResult.pipelineObjects().get(0).name());
        assertTrue(describeObjectsResult.pipelineObjects().get(0).fields()
                                        .contains(Field.builder().key(VALID_KEY).stringValue(FIELD_VALUE).build()));
        assertTrue(describeObjectsResult.pipelineObjects().get(0).fields()
                                        .contains(Field.builder().key("@pipelineId").stringValue(pipelineId).build()));

        // Describe a pipeline.
        DescribePipelinesResponse describepipelinesResult =
                dataPipeline.describePipelines(DescribePipelinesRequest.builder().pipelineIds(pipelineId).build());
        assertEquals(1, describepipelinesResult.pipelineDescriptionList().size());
        assertEquals(PIPELINE_NAME, describepipelinesResult.pipelineDescriptionList().get(0).name());
        assertEquals(pipelineId, describepipelinesResult.pipelineDescriptionList().get(0).pipelineId());
        assertEquals(PIPELINE_DESCRIPTION, describepipelinesResult.pipelineDescriptionList().get(0).description());
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields().size() > 0);
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields()
                                          .contains(Field.builder().key("name").stringValue(PIPELINE_NAME).build()));
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields()
                                          .contains(Field.builder().key("@id").stringValue(pipelineId).build()));
        assertTrue(describepipelinesResult.pipelineDescriptionList().get(0).fields()
                                          .contains(Field.builder().key("uniqueId").stringValue(PIPELINE_ID).build()));

        // Delete a pipeline.
        dataPipeline.deletePipeline(DeletePipelineRequest.builder().pipelineId(pipelineId).build());
        Thread.sleep(1000 * 5);
        try {
            describepipelinesResult = dataPipeline.describePipelines(DescribePipelinesRequest.builder().pipelineIds(pipelineId).build());
            if (describepipelinesResult.pipelineDescriptionList().size() > 0) {
                fail();
            }
        } catch (SdkServiceException e) {
            // Ignored or expected.
        }
    }
}
