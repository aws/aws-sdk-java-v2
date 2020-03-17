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

package software.amazon.awssdk.codegen.docs;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;

/**
 * Provides documentation for an operation method on the client interface. Use
 * {@link #getDocs(IntermediateModel, OperationModel, ClientType)} to retrieve documentation for the typical overload for an
 * operation (i.e. the one that takes in a request object and returns a response object in the simple case) or use
 * {@link #getDocs(IntermediateModel, OperationModel, ClientType, SimpleMethodOverload)} with the specified
 * convenience overload as defined in {@link SimpleMethodOverload}.
 */
public final class OperationDocs {
    private OperationDocs() {
    }

    /**
     * Get documentation for the {@link SimpleMethodOverload#NORMAL} overload. That is, the actual implementation that
     * simple methods delegate to.
     *
     * @param model      {@link IntermediateModel}
     * @param opModel    {@link OperationModel} to generate Javadocs for.
     * @param clientType Which client type the Javadoc is being generated for.
     * @return Formatted Javadocs for operation method.
     */
    public static String getDocs(IntermediateModel model, OperationModel opModel, ClientType clientType) {
        return getDocs(model, opModel, clientType, SimpleMethodOverload.NORMAL);
    }

    /**
     * Equivalent to calling
     * {@link #getDocs(IntermediateModel, OperationModel, ClientType, SimpleMethodOverload, DocConfiguration)} with a default
     * {@link DocConfiguration}
     */
    public static String getDocs(IntermediateModel model, OperationModel opModel, ClientType clientType,
                                 SimpleMethodOverload simpleMethodOverload) {
        return getDocs(model, opModel, clientType, simpleMethodOverload, new DocConfiguration());
    }


    /**
     * Get documentation for a specific {@link SimpleMethodOverload}.
     *
     * @param model                {@link IntermediateModel}
     * @param opModel              {@link OperationModel} to generate Javadocs for.
     * @param clientType           Which client type the Javadoc is being generated for.
     * @param simpleMethodOverload Overload type to generate docs for.
     * @return Formatted Javadocs for operation method.
     */
    public static String getDocs(IntermediateModel model, OperationModel opModel, ClientType clientType,
                                 SimpleMethodOverload simpleMethodOverload, DocConfiguration docConfig) {
        switch (clientType) {
            case SYNC: return simpleMethodOverload.syncDocsProvider(model, opModel, docConfig).getDocs();
            case ASYNC: return simpleMethodOverload.asyncDocsProvider(model, opModel, docConfig).getDocs();
            default: throw new UnsupportedOperationException("Unknown client type: " + clientType);
        }
    }
}
