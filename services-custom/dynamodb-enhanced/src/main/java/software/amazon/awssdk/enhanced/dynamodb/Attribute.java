package software.amazon.awssdk.enhanced.dynamodb;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents an attribute of a table model.
 *
 * @param <T> type of the model.
 * @param <R> type type of the attribute.
 */
@SdkPublicApi
public interface Attribute<T, R> {

    /**
     * Returns the name of the attribute.
     *
     * @return the name of the attribute.
     */
    String attributeName();

    /**
     * Returns the {@link AttributeType} of the attribute which helps with converting the values.
     *
     * @return the {@link AttributeType} of the attribute which helps with converting the values.
     */
    AttributeType<R> attributeType();

    /**
     * Returns the function which can be used to obtain the attribute value as {@link AttributeValue} from given object.
     *
     * @return the function which can be used to obtain the attribute value as {@link AttributeValue} from given object.
     */
    Function<T, AttributeValue> attributeGetterMethod();

    /**
     * Returns the consumer which can be used to set the attribute value as {@link AttributeValue} from given object.
     *
     * @return the consumer which can be used to set the attribute value as {@link AttributeValue} from given object.
     */
    BiConsumer<T, AttributeValue> updateItemMethod();

    /**
     * Returns the {@link TableMetadata} of the parent model.
     *
     * @return the {@link TableMetadata} of the parent model.
     */
    TableMetadata tableMetadata();
}
