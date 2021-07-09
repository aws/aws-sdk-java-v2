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

package software.amazon.awssdk.core.document;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkNumber;

/**
 * Document visitor interface.
 *
 * @param <R> Return type of the visitor.
 */
@SdkPublicApi
public interface DocumentVisitor<R> {

    /**
     * Visits a Document Null.
     *
     * @return value of the visitor
     */
    R visitNull();

    /**
     * Visits a Boolean Document.
     *
     * @param document Document to visit,
     * @return Return value of the visitor.
     */
    R visitBoolean(Boolean document);

    /**
     * Visits a String Document.
     *
     * @param document Document to visit,
     * @return Return value of the visitor.
     */
    R visitString(String document);

    /**
     * Visits a Number Document.
     *
     * @param document Document to visit,
     * @return Return value of the visitor.
     */
    R visitNumber(SdkNumber document);

    /**
     * Visits a Map Document.
     *
     * @param documentMap Document to visit,
     * @return Return value of the visitor.
     */
    R visitMap(Map<String, Document> documentMap);

    /**
     * Visits a List Document.
     *
     * @param documentList Document to visit,
     * @return Return value of the visitor.
     */
    R visitList(List<Document> documentList);
}
