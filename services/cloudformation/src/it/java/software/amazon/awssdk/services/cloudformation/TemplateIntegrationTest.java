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

package software.amazon.awssdk.services.cloudformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.SdkAsserts.assertNotEmpty;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.EstimateTemplateCostRequest;
import software.amazon.awssdk.services.cloudformation.model.EstimateTemplateCostResponse;
import software.amazon.awssdk.services.cloudformation.model.TemplateParameter;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateResponse;

/**
 * TODO Remove Ignore
 * Integration tests of the template-related API of CloudFormation.
 */
@Ignore
public class TemplateIntegrationTest extends CloudFormationIntegrationTestBase {

    private static final String TEMPLATE_DESCRIPTION = "Template Description";

    @Test
    public void testValidateTemplateURL() {
        ValidateTemplateResponse response = cf.validateTemplate(ValidateTemplateRequest.builder()
                                                                                     .templateURL(
                                                                                             templateUrlForCloudFormationIntegrationTests)
                                                                                     .build());

        assertTrue(response.parameters().size() > 0);
        for (TemplateParameter tp : response.parameters()) {
            assertNotNull(tp.parameterKey());
        }
    }

    @Test
    public void testValidateTemplateBody() throws Exception {
        String templateText = FileUtils.readFileToString(templateForCloudFormationIntegrationTestsFile, StandardCharsets.UTF_8);
        ValidateTemplateResponse response = cf.validateTemplate(ValidateTemplateRequest.builder()
                                                                                     .templateBody(templateText).build());
        assertEquals(TEMPLATE_DESCRIPTION, response.description());
        assertEquals(3, response.parameters().size());

        Set<String> expectedTemplateKeys = new HashSet<String>();
        expectedTemplateKeys.add("InstanceType");
        expectedTemplateKeys.add("WebServerPort");
        expectedTemplateKeys.add("KeyPair");

        for (TemplateParameter tp : response.parameters()) {
            assertTrue(expectedTemplateKeys.remove(tp.parameterKey()));
            assertNotNull(tp.defaultValue());
            assertNotEmpty(tp.description());
        }

        assertTrue("expected parameter not found", expectedTemplateKeys.isEmpty());
    }

    @Test
    public void testInvalidTemplate() {
        try {
            cf.validateTemplate(ValidateTemplateRequest.builder().templateBody("{\"Foo\" : \"Bar\"}").build());
            fail("Should have thrown an exception");
        } catch (AwsServiceException acfx) {
            assertEquals("ValidationError", acfx.awsErrorDetails().errorCode());
        } catch (Exception e) {
            fail("Should have thrown an AmazonCloudFormation Exception");
        }
    }

    @Test
    public void testEstimateCost() throws Exception {
        String templateText = FileUtils.readFileToString(templateForCloudFormationIntegrationTestsFile, StandardCharsets.UTF_8);
        EstimateTemplateCostResponse estimateTemplateCost = cf.estimateTemplateCost(EstimateTemplateCostRequest.builder()
                                                                                                             .templateBody(
                                                                                                                     templateText)
                                                                                                             .build());
        assertNotNull(estimateTemplateCost.url());

        estimateTemplateCost = cf.estimateTemplateCost(EstimateTemplateCostRequest.builder()
                                                                                  .templateURL(
                                                                                          templateUrlForCloudFormationIntegrationTests)
                                                                                  .build());
        assertNotNull(estimateTemplateCost.url());
    }

}
