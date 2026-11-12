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

package software.amazon.awssdk.codegen.smithy.customization.s3control;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.AuthType;

/**
 * Sets the auth type to {@code S3V4}, so the client signs with the S3 Control variant of SigV4.
 */
@SdkInternalApi
final class S3ControlSetAuthType extends AbstractS3ControlCustomization {

    @Override
    public void postprocess(IntermediateModel model) {
        model.getMetadata().setAuthType(AuthType.S3V4);
    }
}
