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

package software.amazon.awssdk.s3.wiretap.client.feature;

import java.util.List;
import software.amazon.awssdk.s3.wiretap.client.ClientBuilder;

public abstract class BooleanFeature extends AbstractFeature<BooleanFeature.State> {

    public enum State {
        TRUE(true),
        FALSE(false),
        UNSET(null);

        private final Boolean value;

        State(Boolean value) {
            this.value = value;
        }

        public Boolean asBoolean() {
            return value;
        }
    }

    protected BooleanFeature(String name) {
        super(name, State.class);
    }

    @FunctionalInterface
    protected interface BoolWriter {
        void write(ClientBuilder builder, boolean value);
    }

    protected final void onSyncBool(BoolWriter writer) {
        onSync((builder, state) -> writer.write(builder, state.asBoolean()));
    }

    protected final void onAsyncBool(BoolWriter writer) {
        onAsync((builder, state) -> writer.write(builder, state.asBoolean()));
    }

    protected final void onCrtBool(BoolWriter writer) {
        onCrt((builder, state) -> writer.write(builder, state.asBoolean()));
    }

    @Override
    protected boolean isNoOp(State value) {
        return value == null || value.asBoolean() == null;
    }

    @Override
    public final Category<State> defaultCategory(Class<?> paramType) {
        return new Unset();
    }

    public static final class True implements Category<State> {
        @Override
        public State value() {
            return State.TRUE;
        }
    }

    public static final class False implements Category<State> {
        @Override
        public State value() {
            return State.FALSE;
        }
    }

    public static final class Unset implements Category<State> {
        @Override
        public State value() {
            return State.UNSET;
        }
    }

    public static final class All implements CategoryGroup<State> {
        @Override
        public List<Category<State>> categories() {
            return List.of(new True(), new False(), new Unset());
        }
    }

    public static final class TrueFalse implements CategoryGroup<State> {
        @Override
        public List<Category<State>> categories() {
            return List.of(new True(), new False());
        }
    }
}
