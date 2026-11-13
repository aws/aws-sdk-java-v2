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

package software.amazon.awssdk.codegen.smithy.customization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.smithy.customization.apigateway.ApiGatewayCustomizations;
import software.amazon.awssdk.codegen.smithy.customization.cloudwatch.CloudWatchCustomizations;
import software.amazon.awssdk.codegen.smithy.customization.s3.S3Customizations;
import software.amazon.awssdk.codegen.smithy.customization.shapename.ShapeNameCustomizations;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * The service-specific customizations, looked up by the service's sdkId.
 */
@SdkInternalApi
public final class SmithyServiceCustomizations {
    private static final List<SmithyServiceCustomization> ALL = all();

    private SmithyServiceCustomizations() {
    }

    private static List<SmithyServiceCustomization> all() {
        List<SmithyServiceCustomization> all = new ArrayList<>(S3Customizations.all());
        all.addAll(CloudWatchCustomizations.all());
        all.addAll(ShapeNameCustomizations.all());
        all.addAll(ApiGatewayCustomizations.all());
        return Collections.unmodifiableList(all);
    }

    public static List<SmithyServiceCustomization> forService(ServiceShape service) {
        String sdkId = service.getTrait(ServiceTrait.class).map(ServiceTrait::getSdkId).orElse(null);
        return ALL.stream().filter(c -> c.sdkIds().contains(sdkId)).collect(Collectors.toList());
    }

    public static boolean appliesConfig(ServiceShape service, String key) {
        return forService(service).stream().anyMatch(c -> c.appliesConfig(key));
    }
}
