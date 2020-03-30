package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

@RunWith(MockitoJUnitRunner.class)
public class ChainConverterProviderTest {

    @Mock
    private AttributeConverterProvider mockConverterProvider1;

    @Mock
    private AttributeConverterProvider mockConverterProvider2;

    @Mock
    private AttributeConverter mockAttributeConverter1;

    @Mock
    private AttributeConverter mockAttributeConverter2;

    @Test
    public void checkSingleProviderChain() {
        ChainConverterProvider chain = ChainConverterProvider.create(mockConverterProvider1);
        List providerQueue = chain.chainedProviders();
        assertThat(providerQueue.size()).isEqualTo(1);
        assertThat(providerQueue.get(0)).isEqualTo(mockConverterProvider1);
    }

    @Test
    public void checkMultipleProviderChain() {
        ChainConverterProvider chain = ChainConverterProvider.create(mockConverterProvider1, mockConverterProvider2);
        List providerQueue = chain.chainedProviders();
        assertThat(providerQueue.size()).isEqualTo(2);
        assertThat(providerQueue.get(0)).isEqualTo(mockConverterProvider1);
        assertThat(providerQueue.get(1)).isEqualTo(mockConverterProvider2);
    }

    @Test
    public void resolveSingleProviderChain() {
        when(mockConverterProvider1.converterFor(any())).thenReturn(mockAttributeConverter1);
        ChainConverterProvider chain = ChainConverterProvider.create(mockConverterProvider1);
        assertThat(chain.converterFor(EnhancedType.of(String.class))).isSameAs(mockAttributeConverter1);
    }

    @Test
    public void resolveMultipleProviderChain_noMatch() {
        ChainConverterProvider chain = ChainConverterProvider.create(mockConverterProvider1, mockConverterProvider2);
        assertThat(chain.converterFor(EnhancedType.of(String.class))).isNull();
    }

    @Test
    public void resolveMultipleProviderChain_matchSecond() {
        when(mockConverterProvider2.converterFor(any())).thenReturn(mockAttributeConverter2);
        ChainConverterProvider chain = ChainConverterProvider.create(mockConverterProvider1, mockConverterProvider2);
        assertThat(chain.converterFor(EnhancedType.of(String.class))).isSameAs(mockAttributeConverter2);
    }

    @Test
    public void resolveMultipleProviderChain_matchFirst() {
        when(mockConverterProvider1.converterFor(any())).thenReturn(mockAttributeConverter1);
        ChainConverterProvider chain = ChainConverterProvider.create(mockConverterProvider1, mockConverterProvider2);
        assertThat(chain.converterFor(EnhancedType.of(String.class))).isSameAs(mockAttributeConverter1);
    }

}