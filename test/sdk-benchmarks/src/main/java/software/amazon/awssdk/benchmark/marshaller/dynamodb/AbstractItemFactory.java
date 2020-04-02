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

package software.amazon.awssdk.benchmark.marshaller.dynamodb;

import com.amazonaws.util.ImmutableMapParameter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

abstract class AbstractItemFactory<T> {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    private static final Random RNG = new Random();

    final Map<String, T> tiny() {
        return ImmutableMapParameter.<String, T>builder()
            .put("stringAttr", av(randomS()))
            .build();
    }

    final Map<String, T> small() {
        return ImmutableMapParameter.<String, T>builder()
            .put("stringAttr", av(randomS()))
            .put("binaryAttr", av(randomB()))
            .put("listAttr", av(Arrays.asList(
                av(randomS()),
                av(randomB()),
                av(randomS())
            )))
            .build();
    }

    final Map<String, T> huge() {
        return ImmutableMapParameter.<String, T>builder()
            .put("hashKey", av(randomS()))
            .put("stringAttr", av(randomS()))
            .put("binaryAttr", av(randomB()))
            .put("listAttr", av(
                Arrays.asList(
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomS()),
                    av(randomB()),
                    av(Collections.singletonList(av(randomS()))),
                    av(ImmutableMapParameter.of(
                        "attrOne", av(randomS())
                    )),
                    av(Arrays.asList(
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomS()),
                        av(randomB()),
                        (av(randomS())),
                        av(ImmutableMapParameter.of(
                            "attrOne",
                            av(randomS())
                        ))
                    ))
                )
            ))
            .put("mapAttr", av(
                ImmutableMapParameter.<String, T>builder()
                    .put("attrOne", av(randomS()))
                    .put("attrTwo", av(randomB()))
                    .put("attrThree", av(
                        Arrays.asList(
                            av(randomS()),
                            av(randomS()),
                            av(randomS()),
                            av(randomS()),
                            av(ImmutableMapParameter.<String, T>builder()
                                   .put("attrOne", av(randomS()))
                                   .put("attrTwo", av(randomB()))
                                   .put("attrThree",
                                        av(Arrays.asList(
                                            av(randomS()),
                                            av(randomS()),
                                            av(randomS()),
                                            av(randomS())
                                        ))
                                   )
                                   .build())
                        ))
                    )
                    .build()))
            .build();
    }


    protected abstract T av(String val);

    protected abstract T av(ByteBuffer val);

    protected abstract T av(List<T> val);

    protected abstract T av(Map<String, T> val);

    private String randomS(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append(ALPHA.charAt(RNG.nextInt(ALPHA.length())));
        }
        return sb.toString();
    }

    private String randomS() {
        return randomS(16);
    }

    private ByteBuffer randomB(int len) {
        byte[] b = new byte[len];
        RNG.nextBytes(b);
        return ByteBuffer.wrap(b);
    }

    private ByteBuffer randomB() {
        return randomB(16);
    }
}
