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

package software.amazon.awssdk.services.ssm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.services.ssm.model.CreateDocumentRequest;
import software.amazon.awssdk.services.ssm.model.CreateDocumentResponse;
import software.amazon.awssdk.services.ssm.model.DeleteDocumentRequest;
import software.amazon.awssdk.services.ssm.model.DescribeDocumentRequest;
import software.amazon.awssdk.services.ssm.model.DescribeDocumentResponse;
import software.amazon.awssdk.services.ssm.model.DocumentDescription;
import software.amazon.awssdk.services.ssm.model.GetDocumentRequest;
import software.amazon.awssdk.services.ssm.model.GetDocumentResponse;
import software.amazon.awssdk.services.ssm.model.ListDocumentsRequest;
import software.amazon.awssdk.services.ssm.model.ListDocumentsResponse;
import software.amazon.awssdk.utils.IoUtils;

public class SSMServiceIntegrationTest extends IntegrationTestBase {

    private static final Log LOG = LogFactory.getLog(SSMServiceIntegrationTest.class);
    private static final String DOCUMENT_LOCATION = "documentContent.json";
    private static final String DOCUMENT_NAME = "my-document-" + System.currentTimeMillis();

    @AfterClass
    public static void tearDown() {
        try {
            ssm.deleteDocument(DeleteDocumentRequest.builder().name(DOCUMENT_NAME).build());
        } catch (Exception e) {
            LOG.error("Failed to delete config document.", e);
        }
    }

    @Test
    public void testAll() throws Exception {

        String documentContent = IoUtils.toString(getClass().getResourceAsStream(DOCUMENT_LOCATION));
        testCreateDocument(DOCUMENT_NAME, documentContent);
        testDescribeDocument();
    }

    private void testDescribeDocument() {
        DescribeDocumentResponse result = ssm.describeDocument(DescribeDocumentRequest.builder().name(DOCUMENT_NAME).build());
        assertNotNull(result.document());
    }

    private void testCreateDocument(String docName, String docContent) {

        CreateDocumentResponse createResult = ssm
                .createDocument(CreateDocumentRequest.builder().name(docName).content(docContent).build());

        DocumentDescription description = createResult.documentDescription();

        assertEquals(docName, description.name());
        assertNotNull(description.status());
        assertNotNull(description.createdDate());

        GetDocumentResponse getResult = ssm.getDocument(GetDocumentRequest.builder().name(docName).build());

        assertEquals(DOCUMENT_NAME, getResult.name());
        assertEquals(docContent, getResult.content());

        ListDocumentsResponse listResult = ssm.listDocuments(ListDocumentsRequest.builder().build());

        assertFalse("ListDocuments should at least returns one element", listResult.documentIdentifiers().isEmpty());

    }

}
