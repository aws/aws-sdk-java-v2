package software.amazon.awssdk.services.query;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SomeBuildableClass;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface CustomBuilder extends SdkBuilder<CustomBuilder, QueryAsyncClient> {
    /**
     * A buildable property
     */
    CustomBuilder buildableProperty(SomeBuildableClass buildableProperty);

    default CustomBuilder buildableProperty(Consumer<SomeBuildableClass.Builder> buildablePropertyBuilder) {
        Validate.paramNotNull(buildablePropertyBuilder, "buildablePropertyBuilder");
        return buildableProperty(SomeBuildableClass.builder().applyMutation(buildablePropertyBuilder).build());
    }

    /**
     * A boolean property
     */
    CustomBuilder booleanProperty(Boolean booleanProperty);

    /**
     * A string property
     */
    CustomBuilder stringProperty(String stringProperty);

    /**
     * A boolean client context parameter
     */
    CustomBuilder booleanContextParam(Boolean booleanContextParam);

    /**
     * a string client context parameter
     */
    CustomBuilder stringContextParam(String stringContextParam);

    @Override
    QueryAsyncClient build();
}
