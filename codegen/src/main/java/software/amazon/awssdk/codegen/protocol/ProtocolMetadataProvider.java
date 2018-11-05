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

package software.amazon.awssdk.codegen.protocol;

import java.io.Serializable;

/**
 * Provides various pieces of information that are specific to certain protocols.
 */
// TODO this isn't really pulling it's weight anymore, consider removing
public interface ProtocolMetadataProvider extends Serializable {

    /**
     * @return True if protocol uses some form of JSON wire format. False otherwise.
     */
    boolean isJsonProtocol();

    /**
     * @return True if protocol uses XML as the wire format. False otherwise.
     */
    boolean isXmlProtocol();

    /**
     * @return True if protocol uses CBOR as the wire format. False otherwise.
     */
    boolean isCborProtocol();

    /**
     * @return True if protocol uses Ion as the wire format. False otherwise.
     */
    boolean isIonProtocol();

    /**
     * @return The content type to use when sending requests. Currently only respected by JSON
     *     protocols.
     */
    String getContentType();

}
