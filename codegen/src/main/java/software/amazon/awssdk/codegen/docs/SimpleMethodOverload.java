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

import software.amazon.awssdk.core.sync.ResponseBytes;

/**
 * Enum describing all the convenience overloads we generate for operation methods.
 */
public enum SimpleMethodOverload {

    /**
     * The standard method overload. For non-streaming sync this is the method that take
     * in a request object and returns a response object.
     */
    NORMAL,

    /**
     * The standard paginated method overload that takes in a request object and returns a response object.
     */
    PAGINATED,

    /**
     * Simple method that takes no arguments and creates an empty request object that delegates to the {@link #NORMAL} overload.
     */
    NO_ARG,

    /**
     * Paginated simple method that takes no arguments and creates an empty request object.
     */
    NO_ARG_PAGINATED,

    /**
     * Simple method for streaming operations (input or output) that takes in the request object and a file to
     * upload from or download to.
     */
    FILE,

    /**
     * Simple method for allowing a Consumer of Builder to be passed to save having to create the builder manually.
     */
    CONSUMER_BUILDER,

    /**
     * Simple method only for sync operations that have a streaming output. Takes a request object
     * and returns an unmanaged {@link software.amazon.awssdk.core.sync.ResponseInputStream} to read the response
     * contents.
     */
    INPUT_STREAM,

    /**
     * Simple method only for sync operations that have a streaming output. Takes a request object and return a
     * {@link ResponseBytes} to give byte-buffer access and convenience methods for type conversion.
     */
    BYTES
}
