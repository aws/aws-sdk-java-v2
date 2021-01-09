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

package software.amazon.awssdk.services.s3.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.adapter.TypeAdapter;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * {@link TypeAdapter} that converts the {@link Tagging} modeled object into a
 * URL encoded map of key to values. Used for Put and Copy object operations
 * which models the Tagging as a string.
 */
@SdkInternalApi
public final class TaggingAdapter implements TypeAdapter<Tagging, String> {

    private static final TaggingAdapter INSTANCE = new TaggingAdapter();


    private TaggingAdapter() {
    }

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

    /**
     * @return Singleton instance of {@link TaggingAdapter}.
     */
    public static TaggingAdapter instance() {
        return INSTANCE;
    }
}
