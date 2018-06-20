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

package software.amazon.awssdk.services.dynamodb;

import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class ImmutableObjectUtils {

    private ImmutableObjectUtils() {
    }

    public static <T> void setObjectMember(Object o, String memberName, T value) {
        Arrays.stream(o.getClass().getDeclaredFields())
              .filter(f -> f.getName().equals(memberName))
              .findFirst()
              .ifPresent(f -> {
                  f.setAccessible(true);
                  try {
                      f.set(o, value);
                  } catch (IllegalAccessException e) {
                      throw new RuntimeException("Unable to reflectively set member " + memberName);
                  }
              });
    }
}