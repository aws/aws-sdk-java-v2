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

package software.amazon.awssdk.core.util;

import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A list that was auto constructed by the SDK.
 * <p>
 * The main purpose of this class is to help distinguish explicitly empty lists
 * set on requests by the user, as some services may treat {@code null} or
 * missing lists and empty list members differently. As such, this class should
 * not be used directly by the user.
 *
 * @param <T> The element type.
 */
@SdkProtectedApi
public interface SdkAutoConstructList<T> extends List<T> {
}
