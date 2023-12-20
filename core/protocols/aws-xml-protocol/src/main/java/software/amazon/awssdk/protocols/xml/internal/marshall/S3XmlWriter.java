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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import java.io.Writer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Utility for creating easily creating XML documents, one element at a time, for S3 operations.
 */
@SdkInternalApi
public class S3XmlWriter extends XmlWriter {

    /**
     * Creates a new S3XmlWriter, ready to write an XML document to the specified writer. The root element in the XML
     * document will specify an xmlns attribute with the specified namespace parameter.
     *
     * @param w     The writer this S3XmlWriter will write to.
     * @param xmlns The XML namespace to include in the xmlns attribute of the root element.
     */
    S3XmlWriter(Writer w, String xmlns) {
        super(w, xmlns);
    }

    @Override
    protected String escapeXmlEntities(String s) {
        return StringUtils.replaceEach(s, ESCAPE_SEARCHES, ESCAPE_REPLACEMENTS);
    }
}
