package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;

@RunWith(MockitoJUnitRunner.class)
public class ConverterProviderResolverTest {

    @Mock
    private AttributeConverterProvider mockConverterProvider1;

    @Mock
    private AttributeConverterProvider mockConverterProvider2;

    @Test
    public void resolveProviders_null() {
        assertThat(ConverterProviderResolver.resolveProviders(null)).isNull();
    }

    @Test
    public void resolveProviders_empty() {
        assertThat(ConverterProviderResolver.resolveProviders(emptyList())).isNull();
    }

    @Test
    public void resolveProviders_singleton() {
        assertThat(ConverterProviderResolver.resolveProviders(singletonList(mockConverterProvider1)))
            .isSameAs(mockConverterProvider1);
    }

    @Test
    public void resolveProviders_multiple() {
        AttributeConverterProvider result = ConverterProviderResolver.resolveProviders(
            Arrays.asList(mockConverterProvider1, mockConverterProvider2));
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ChainConverterProvider.class);
    }

    @Test
    public void defaultProvider_returnsInstance() {
        AttributeConverterProvider defaultProvider = ConverterProviderResolver.defaultConverterProvider();
        assertThat(defaultProvider).isNotNull();
        assertThat(defaultProvider).isInstanceOf(DefaultAttributeConverterProvider.class);
    }

}