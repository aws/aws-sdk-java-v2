/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <p>
 * Information about the commands that will be run on a Dev Environment when an SSH session begins.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class ExecuteCommandSessionConfiguration implements SdkPojo, Serializable,
        ToCopyableBuilder<ExecuteCommandSessionConfiguration.Builder, ExecuteCommandSessionConfiguration> {
    private static final SdkField<String> COMMAND_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("command")
            .getter(getter(ExecuteCommandSessionConfiguration::command)).setter(setter(Builder::command))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("command").build()).build();

    private static final SdkField<List<String>> ARGUMENTS_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("arguments")
            .getter(getter(ExecuteCommandSessionConfiguration::arguments))
            .setter(setter(Builder::arguments))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("arguments").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays
            .asList(COMMAND_FIELD, ARGUMENTS_FIELD));

    private static final long serialVersionUID = 1L;

    private final String command;

    private final List<String> arguments;

    private ExecuteCommandSessionConfiguration(BuilderImpl builder) {
        this.command = builder.command;
        this.arguments = builder.arguments;
    }

    /**
     * <p>
     * The command used at the beginning of the SSH session to a Dev Environment.
     * </p>
     * 
     * @return The command used at the beginning of the SSH session to a Dev Environment.
     */
    public final String command() {
        return command;
    }

    /**
     * For responses, this returns true if the service returned a value for the Arguments property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasArguments() {
        return arguments != null && !(arguments instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * An array of arguments containing arguments and members.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasArguments} method.
     * </p>
     * 
     * @return An array of arguments containing arguments and members.
     */
    public final List<String> arguments() {
        return arguments;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(command());
        hashCode = 31 * hashCode + Objects.hashCode(hasArguments() ? arguments() : null);
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExecuteCommandSessionConfiguration)) {
            return false;
        }
        ExecuteCommandSessionConfiguration other = (ExecuteCommandSessionConfiguration) obj;
        return Objects.equals(command(), other.command()) && hasArguments() == other.hasArguments()
                && Objects.equals(arguments(), other.arguments());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ExecuteCommandSessionConfiguration").add("Command", command())
                .add("Arguments", hasArguments() ? arguments() : null).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "command":
            return Optional.ofNullable(clazz.cast(command()));
        case "arguments":
            return Optional.ofNullable(clazz.cast(arguments()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ExecuteCommandSessionConfiguration, T> g) {
        return obj -> g.apply((ExecuteCommandSessionConfiguration) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ExecuteCommandSessionConfiguration> {
        /**
         * <p>
         * The command used at the beginning of the SSH session to a Dev Environment.
         * </p>
         * 
         * @param command
         *        The command used at the beginning of the SSH session to a Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder command(String command);

        /**
         * <p>
         * An array of arguments containing arguments and members.
         * </p>
         * 
         * @param arguments
         *        An array of arguments containing arguments and members.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder arguments(Collection<String> arguments);

        /**
         * <p>
         * An array of arguments containing arguments and members.
         * </p>
         * 
         * @param arguments
         *        An array of arguments containing arguments and members.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder arguments(String... arguments);
    }

    static final class BuilderImpl implements Builder {
        private String command;

        private List<String> arguments = DefaultSdkAutoConstructList.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(ExecuteCommandSessionConfiguration model) {
            command(model.command);
            arguments(model.arguments);
        }

        public final String getCommand() {
            return command;
        }

        public final void setCommand(String command) {
            this.command = command;
        }

        @Override
        public final Builder command(String command) {
            this.command = command;
            return this;
        }

        public final Collection<String> getArguments() {
            if (arguments instanceof SdkAutoConstructList) {
                return null;
            }
            return arguments;
        }

        public final void setArguments(Collection<String> arguments) {
            this.arguments = ExecuteCommandSessionConfigurationArgumentsCopier.copy(arguments);
        }

        @Override
        public final Builder arguments(Collection<String> arguments) {
            this.arguments = ExecuteCommandSessionConfigurationArgumentsCopier.copy(arguments);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder arguments(String... arguments) {
            arguments(Arrays.asList(arguments));
            return this;
        }

        @Override
        public ExecuteCommandSessionConfiguration build() {
            return new ExecuteCommandSessionConfiguration(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
