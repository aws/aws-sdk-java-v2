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

package software.amazon.awssdk.v2migration;

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v1EnMethodMatcher;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3EventNotificationMethodToV2 extends Recipe {

    private static final MethodMatcher GET_EVENT_TIME = v1EnMethodMatcher("S3EventNotification.S3EventNotificationRecord "
                                                                          + "getEventTime(..)");

    private static final MethodMatcher  GET_EXPIRY_TIME = v1EnMethodMatcher("S3EventNotification.RestoreEventDataEntity "
                                                                          + "getLifecycleRestorationExpiryTime(..)");

    @Override
    public String getDisplayName() {
        return "S3 Event Notification method to v2";
    }

    @Override
    public String getDescription() {
        return "S3 Event Notification method to v2";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static class Visitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (GET_EVENT_TIME.matches(method) || GET_EXPIRY_TIME.matches(method)) {
                JavaTemplate template = JavaTemplate.builder("new DateTime(#{any(java.time.Instant)}.toEpochMilli())")
                                                    .build();
                J m = super.visitMethodInvocation(method, ctx);
                m = template.apply(getCursor(), ((J.MethodInvocation) m).getCoordinates().replace(), m);
                return m;
            }
            return method;
        }

    }

}
