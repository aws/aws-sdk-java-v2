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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.adapter.TypeAdapter;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkPublicApi
public final class TaggingAdapter implements TypeAdapter<Tagging, String> {

    @Override
    public String adapt(Tagging tagging) {
        StringBuilder tagBuilder = new StringBuilder();

        Tagging taggingClone = tagging.toBuilder().build();

        Tag firstTag = taggingClone.tagSet().get(0);
        tagBuilder.append(SdkHttpUtils.urlEncode(firstTag.key()));
        tagBuilder.append("=");
        tagBuilder.append(SdkHttpUtils.urlEncode(firstTag.value()));

        for (int i = 1; i < taggingClone.tagSet().size(); i++) {
            Tag t = taggingClone.tagSet().get(i);
            tagBuilder.append("&");
            tagBuilder.append(SdkHttpUtils.urlEncode(t.key()));
            tagBuilder.append("=");
            tagBuilder.append(SdkHttpUtils.urlEncode(t.value()));
        }

        return tagBuilder.toString();
    }
}
