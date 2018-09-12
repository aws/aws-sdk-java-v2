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

package software.amazon.awssdk.services.sfn.builder.conditions;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Base interface for n-ary conditions like {@link AndCondition}.
 *
 * <p>This interface should not be implemented outside of the SDK.</p>
 */
@SdkInternalApi
public interface NAryCondition extends Condition {

    /**
     * @return List of conditions contained in the n-ary expression.
     */
    List<Condition> getConditions();

}
