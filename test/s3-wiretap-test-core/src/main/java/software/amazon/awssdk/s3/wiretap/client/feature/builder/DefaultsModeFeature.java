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

package software.amazon.awssdk.s3.wiretap.client.feature.builder;

import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;

public final class DefaultsModeFeature extends AbstractFeature<DefaultsMode> {

    public DefaultsModeFeature() {
        super("defaultsMode", DefaultsMode.class);
        onSync((b, v) -> b.client().defaultsMode(v));
        onAsync((b, v) -> b.asyncClient().defaultsMode(v));
    }

    @Override
    public Category<DefaultsMode> defaultCategory(Class<?> paramType) {
        return new Unset();
    }

    public static final class Unset implements Category<DefaultsMode> {
        @Override
        public DefaultsMode value() {
            return null;
        }
    }

    public static final class Legacy implements Category<DefaultsMode> {
        @Override
        public DefaultsMode value() {
            return DefaultsMode.LEGACY;
        }
    }

    public static final class Standard implements Category<DefaultsMode> {
        @Override
        public DefaultsMode value() {
            return DefaultsMode.STANDARD;
        }
    }

    public static final class Mobile implements Category<DefaultsMode> {
        @Override
        public DefaultsMode value() {
            return DefaultsMode.MOBILE;
        }
    }

    public static final class CrossRegion implements Category<DefaultsMode> {
        @Override
        public DefaultsMode value() {
            return DefaultsMode.CROSS_REGION;
        }
    }

    public static final class InRegion implements Category<DefaultsMode> {
        @Override
        public DefaultsMode value() {
            return DefaultsMode.IN_REGION;
        }
    }

    public static final class Auto implements Category<DefaultsMode> {
        @Override
        public DefaultsMode value() {
            return DefaultsMode.AUTO;
        }
    }
}
