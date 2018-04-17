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

package software.amazon.awssdk.codegen.docs;

import software.amazon.awssdk.codegen.docs.AsyncOperationDocProvider.AsyncFile;
import software.amazon.awssdk.codegen.docs.AsyncOperationDocProvider.AsyncNoArg;
import software.amazon.awssdk.codegen.docs.AsyncOperationDocProvider.AsyncPaginated;
import software.amazon.awssdk.codegen.docs.AsyncOperationDocProvider.AsyncPaginatedNoArg;
import software.amazon.awssdk.codegen.docs.SyncOperationDocProvider.SyncBytes;
import software.amazon.awssdk.codegen.docs.SyncOperationDocProvider.SyncFile;
import software.amazon.awssdk.codegen.docs.SyncOperationDocProvider.SyncInputStream;
import software.amazon.awssdk.codegen.docs.SyncOperationDocProvider.SyncNoArg;
import software.amazon.awssdk.codegen.docs.SyncOperationDocProvider.SyncPaginated;
import software.amazon.awssdk.codegen.docs.SyncOperationDocProvider.SyncPaginatedNoArg;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;

/**
 * Enum describing all the convenience overloads we generate for operation methods.
 */
public enum SimpleMethodOverload {
    /**
     * The standard method overload. For non-streaming sync this is the method that take
     * in a request object and returns a response object.
     */
    NORMAL(SyncOperationDocProvider::new, AsyncOperationDocProvider::new),

    /**
     * The standard paginated method overload that takes in a request object and returns a response object.
     */
    PAGINATED(SyncPaginated::new, AsyncPaginated::new),

    /**
     * Simple method that takes no arguments and creates an empty request object that delegates to the {@link #NORMAL} overload.
     */
    NO_ARG(SyncNoArg::new, AsyncNoArg::new),

    /**
     * Paginated simple method that takes no arguments and creates an empty request object.
     */
    NO_ARG_PAGINATED(SyncPaginatedNoArg::new, AsyncPaginatedNoArg::new),

    /**
     * Simple method for streaming operations (input or output) that takes in the request object and a file to
     * upload from or download to.
     */
    FILE(SyncFile::new, AsyncFile::new),

    /**
     * Simple method only for sync operations that have a streaming output. Takes a request object
     * and returns an unmanaged {@link ResponseInputStream} to read the response
     * contents.
     */
    INPUT_STREAM(SyncInputStream::new, null),

    /**
     * Simple method only for sync operations that have a streaming output. Takes a request object and return a
     * {@link ResponseBytes} to give byte-buffer access and convenience methods for type conversion.
     */
    BYTES(SyncBytes::new, null);

    private final Factory syncDocsFactory;
    private final Factory asyncDocsFactory;

    SimpleMethodOverload(Factory syncDocsFactory, Factory asyncDocsFactory) {
        this.syncDocsFactory = syncDocsFactory;
        this.asyncDocsFactory = asyncDocsFactory;
    }

    public OperationDocProvider syncDocsProvider(IntermediateModel model, OperationModel opModel,
                                                 DocConfiguration docConfiguration) {
        if (syncDocsFactory == null) {
            throw new UnsupportedOperationException(this + " is not currently documented for sync clients.");
        }
        return syncDocsFactory.create(model, opModel, docConfiguration);
    }

    public OperationDocProvider asyncDocsProvider(IntermediateModel model, OperationModel opModel,
                                                  DocConfiguration docConfiguration) {
        if (asyncDocsFactory == null) {
            throw new UnsupportedOperationException(this + " is not currently documented for async clients.");
        }
        return asyncDocsFactory.create(model, opModel, docConfiguration);
    }
}
