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

package software.amazon.awssdk.runtime.adapters.types;

import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Adapter interface to convert one type to another
 *
 * @param <SourceT>
 *            Source type
 * @param <DestinationT>
 *            Destination type
 */
@SdkInternalApi
public interface TypeAdapter<SourceT, DestinationT> {

    @ReviewBeforeRelease("This isn't used.")
    DestinationT adapt(SourceT source);

}
