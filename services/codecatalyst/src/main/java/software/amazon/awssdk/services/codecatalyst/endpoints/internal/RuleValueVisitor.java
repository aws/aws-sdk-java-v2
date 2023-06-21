/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Visitor for the right-hand side of rules (tree, error, endpoint)
 * 
 * @param <R>
 *        The return type of the visitor
 */
@SdkInternalApi
public interface RuleValueVisitor<R> {
    R visitTreeRule(List<Rule> rules);

    R visitErrorRule(Expr error);

    R visitEndpointRule(EndpointResult endpoint);
}
