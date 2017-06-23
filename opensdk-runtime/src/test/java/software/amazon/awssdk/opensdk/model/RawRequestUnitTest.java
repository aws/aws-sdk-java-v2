package software.amazon.awssdk.opensdk.model;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import software.amazon.awssdk.opensdk.SdkRequestConfig;

/**
 * Unit tests for {@link RawRequest}.
 */
public class RawRequestUnitTest {
    @Test
    public void testHeadersMergedWithSdkRequestConfig() {
        RawRequest req = new RawRequest()
                .header("foo", "1");

        SdkRequestConfig config = SdkRequestConfig.builder().customHeader("bar", "2").build();

        req.sdkRequestConfig(config);

        Map<String,String> mergedHeaders = new HashMap<String,String>() {{
            put("foo", "1");
            put("bar", "2");
        }};

        assertEquals(mergedHeaders, req.sdkRequestConfig().getCustomHeaders());
    }

    @Test
    public void testQueryParametersMergedWithSdkRequestConfig() {
        RawRequest req = new RawRequest()
                .queryParameter("foo", "1")
                .queryParameter("bar", "1")
                .queryParameter("bar", "2");

        SdkRequestConfig config = SdkRequestConfig.builder()
                .customQueryParam("baz", "1")
                .customQueryParam("bar", "3")
                .build();

        req.sdkRequestConfig(config);

        Map<String,List<String>> mergedQueryParams = new HashMap<>();
        mergedQueryParams.put("foo", Stream.of("1").collect(Collectors.toList()));
        mergedQueryParams.put("bar", Stream.of("1", "2", "3").collect(Collectors.toList()));
        mergedQueryParams.put("baz", Stream.of("1").collect(Collectors.toList()));

        Map<String,List<String>> actualQueryParams = req.sdkRequestConfig().getCustomQueryParams();

        actualQueryParams.values().forEach(Collections::sort);

        assertEquals(mergedQueryParams, actualQueryParams);
    }

    @Test
    public void testHeaderSetInRawRequestTakesPrecedence() {
        RawRequest req = new RawRequest().header("foo", "bar");
        SdkRequestConfig config = SdkRequestConfig.builder().customHeader("foo", "baz").build();
        req = req.sdkRequestConfig(config);
        assertEquals("bar", req.sdkRequestConfig().getCustomHeaders().get("foo"));
    }

    @Test
    public void testHeadersSetInRequestOnly() {
        Map<String,String> headers = new HashMap<String,String>() {{
            put("foo", "1");
            put("bar", "2");
            put("baz", "3");
        }};

        RawRequest req = new RawRequest();
        headers.forEach(req::header);

        assertEquals(headers, req.sdkRequestConfig().getCustomHeaders());
    }

    @Test
    public void testQueryParamsSetInRequestOnly() {
        Map<String,List<String>> queryParams = new HashMap<>();
        queryParams.put("foo", Stream.of("1", "2", "3").collect(Collectors.toList()));
        queryParams.put("bar", Stream.of("4", "5").collect(Collectors.toList()));

        RawRequest req = new RawRequest();
        queryParams.forEach((p,values) -> values
                .forEach(v -> req.queryParameter(p,v)));

        assertEquals(queryParams, req.sdkRequestConfig().getCustomQueryParams());
    }

    @Test
    public void testSetSdkRequestConfigMultipleTimesQueryParamListsEqual() {
        SdkRequestConfig config = SdkRequestConfig.builder()
                .customQueryParam("foo", "bar")
                .build();

        RawRequest req = new RawRequest();
        for (int i = 0; i < 10; ++i) {
            req.sdkRequestConfig(config);
        }
        assertEquals(config.getCustomQueryParams(), req.sdkRequestConfig().getCustomQueryParams());
    }
}
