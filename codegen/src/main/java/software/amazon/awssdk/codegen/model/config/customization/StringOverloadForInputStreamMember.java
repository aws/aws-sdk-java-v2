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

package software.amazon.awssdk.codegen.model.config.customization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.core.adapter.StringToInputStreamAdapter;

/**
 * Basically a facade POJO over {@link ConvenienceTypeOverload} that hides (i.e. hard codes) some
 * configuration options
 */
public class StringOverloadForInputStreamMember {

    private static final String STRING_TO_INPUT_STREAM_ADAPTER = StringToInputStreamAdapter.class.getName();

    private final ConvenienceTypeOverload convenienceTypeOverload;

    @JsonCreator
    public StringOverloadForInputStreamMember(@JsonProperty("shapeName") String shapeName,
                                              @JsonProperty("memberName") String memberName) {
        this.convenienceTypeOverload = new ConvenienceTypeOverload()
                .withShapeName(shapeName)
                .withMemberName(memberName)
                .withConvenienceType("String")
                .withTypeAdapterFqcn(STRING_TO_INPUT_STREAM_ADAPTER);
    }

    public ConvenienceTypeOverload getConvenienceTypeOverload() {
        return convenienceTypeOverload;
    }

}
