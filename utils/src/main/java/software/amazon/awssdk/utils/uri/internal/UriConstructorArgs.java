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

package software.amazon.awssdk.utils.uri.internal;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Represent the different constructor to the URI class used by the SDK. Implementation of this interface are able to create new
 * URIs based on the different arguments passed to classes to them.
 *
 * @see URI#create(String)
 * @see URI#URI(String, String, String, String, String)
 * @see URI#URI(String, String, String, int, String, String, String)
 */
@SdkInternalApi
public interface UriConstructorArgs {

    /**
     * Creates a new instance of the URI. Can return a new instance everytime it is called.
     *
     * @return a new URI instance
     */
    URI newInstance();
}
