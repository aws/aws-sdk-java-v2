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

package software.amazon.awssdk.enhanced.dynamodb.query.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.ConditionEvaluator;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.AggregationFunction;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.SortDirection;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.AggregateSpec;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.OrderBySpec;

/**
 * Shared helpers for the sync and async query engines. Contains aggregation bucket management, group-key building, index
 * detection, and common constants.
 */
@SdkInternalApi
public final class QueryEngineSupport {

    public static final String BASE_ALIAS = "base";
    public static final String JOINED_ALIAS = "joined";

    public static final int MAX_BASE_PAGE_SIZE = 1000;
    public static final int INLINE_AGG_SCAN_THRESHOLD = 100;
    public static final int PARALLEL_SCAN_SEGMENTS = Math.min(Runtime.getRuntime().availableProcessors() * 2, 16);

    /**
     * Internal bucket key for the first-seen base table row map (join + aggregation). Not an aggregate output.
     */
    static final String REPRESENTATIVE_BASE_KEY = "__ddbEnhancedQueryRepresentativeBase";

    private QueryEngineSupport() {
    }

    // ---- group key ---------------------------------------------------

    public static List<Object> buildGroupKey(List<String> groupByAttrs,
                                             Map<String, Object> primary,
                                             Map<String, Object> secondary) {
        List<Object> key = new ArrayList<>(groupByAttrs.size());
        for (String attr : groupByAttrs) {
            key.add(ConditionEvaluator.lookupCombined(attr, primary, secondary));
        }
        return key;
    }

    // ---- aggregation bucket management -------------------------------

    public static Map<String, Object> createEmptyBucket(List<AggregateSpec> aggregateSpecs) {
        Map<String, Object> bucket = new HashMap<>();
        for (AggregateSpec agg : aggregateSpecs) {
            switch (agg.function()) {
                case COUNT:
                    bucket.put(agg.outputName(), 0L);
                    break;
                case SUM:
                case AVG:
                    bucket.put(agg.outputName() + "_sum", BigDecimal.ZERO);
                    bucket.put(agg.outputName() + "_count", 0L);
                    break;
                case MIN:
                case MAX:
                    bucket.put(agg.outputName(), null);
                    break;
                default:
                    break;
            }
        }
        return bucket;
    }

    public static void updateBucket(Map<String, Object> bucket,
                                    Map<String, Object> row,
                                    List<AggregateSpec> aggregateSpecs) {
        for (AggregateSpec agg : aggregateSpecs) {
            Object val = row.get(agg.attribute());
            applyAggregate(bucket, agg, val);
        }
        putRepresentativeBaseIfAbsent(bucket, row);
    }

    public static void updateBucketTwoMap(Map<String, Object> bucket,
                                          Map<String, Object> primary,
                                          Map<String, Object> secondary,
                                          List<AggregateSpec> aggregateSpecs) {
        for (AggregateSpec agg : aggregateSpecs) {
            Object val = ConditionEvaluator.lookupCombined(agg.attribute(), primary, secondary);
            applyAggregate(bucket, agg, val);
        }
        putRepresentativeBaseIfAbsent(bucket, secondary);
    }

    /**
     * Stores the first base table row for this aggregate bucket (join path: secondary map is base).
     */
    @SuppressWarnings("unchecked")
    public static void putRepresentativeBaseIfAbsent(Map<String, Object> bucket,
                                                     Map<String, Object> baseMap) {
        if (baseMap == null || bucket.containsKey(REPRESENTATIVE_BASE_KEY)) {
            return;
        }
        bucket.put(REPRESENTATIVE_BASE_KEY, new LinkedHashMap<>(baseMap));
    }

    @SuppressWarnings("unchecked")
    public static void mergeBucket(Map<String, Object> target,
                                   Map<String, Object> source,
                                   List<AggregateSpec> aggregateSpecs) {
        for (AggregateSpec agg : aggregateSpecs) {
            String out = agg.outputName();
            switch (agg.function()) {
                case COUNT:
                    target.put(out, ((Number) target.get(out)).longValue()
                                    + ((Number) source.get(out)).longValue());
                    break;
                case SUM:
                case AVG:
                    BigDecimal ts = (BigDecimal) target.get(out + "_sum");
                    BigDecimal ss = (BigDecimal) source.get(out + "_sum");
                    target.put(out + "_sum", ts.add(ss));
                    long tc = ((Number) target.get(out + "_count")).longValue();
                    long sc = ((Number) source.get(out + "_count")).longValue();
                    target.put(out + "_count", tc + sc);
                    break;
                case MIN:
                    Object tMin = target.get(out);
                    Object sMin = source.get(out);
                    if (tMin == null
                        || (sMin != null && compareForAggregate(sMin, tMin) < 0)) {
                        target.put(out, sMin);
                    }
                    break;
                case MAX:
                    Object tMax = target.get(out);
                    Object sMax = source.get(out);
                    if (tMax == null
                        || (sMax != null && compareForAggregate(sMax, tMax) > 0)) {
                        target.put(out, sMax);
                    }
                    break;
                default:
                    break;
            }
        }
        if (!target.containsKey(REPRESENTATIVE_BASE_KEY) && source.containsKey(REPRESENTATIVE_BASE_KEY)) {
            target.put(REPRESENTATIVE_BASE_KEY,
                       new LinkedHashMap<>((Map<String, Object>) source.get(REPRESENTATIVE_BASE_KEY)));
        }
    }

    public static Map<String, Object> aggregatesFromBucket(Map<String, Object> bucket,
                                                           List<AggregateSpec> aggregateSpecs) {
        Map<String, Object> aggregates = new HashMap<>();
        for (AggregateSpec agg : aggregateSpecs) {
            String out = agg.outputName();
            Object val;
            if (agg.function() == AggregationFunction.AVG) {
                BigDecimal sum = (BigDecimal) bucket.get(out + "_sum");
                Long count = bucket.get(out + "_count") != null
                             ? ((Number) bucket.get(out + "_count")).longValue() : null;
                val = (sum != null && count != null && count > 0)
                      ? (sum.doubleValue() / count.doubleValue())
                      : null;
            } else if (agg.function() == AggregationFunction.SUM) {
                val = bucket.get(out + "_sum");
            } else {
                val = bucket.get(out);
            }
            if (val != null) {
                aggregates.put(out, val);
            }
        }
        return aggregates;
    }

    /**
     * Builds an {@link EnhancedQueryRow} from an aggregation bucket, including representative base attributes when present.
     */
    @SuppressWarnings("unchecked")
    public static EnhancedQueryRow aggregationRowFromBucket(Map<String, Object> bucket,
                                                            List<AggregateSpec> aggregateSpecs,
                                                            List<String> projectAttributes) {
        Map<String, Object> aggregates = aggregatesFromBucket(bucket, aggregateSpecs);
        Map<String, Object> rawBase = (Map<String, Object>) bucket.get(REPRESENTATIVE_BASE_KEY);
        Map<String, Map<String, Object>> itemsByAlias = Collections.emptyMap();
        if (rawBase != null) {
            itemsByAlias = Collections.singletonMap(BASE_ALIAS, projectAttributeMap(rawBase, projectAttributes));
        }
        return EnhancedQueryRow.builder()
                               .itemsByAlias(itemsByAlias)
                               .aggregates(aggregates)
                               .build();
    }

    private static Map<String, Object> projectAttributeMap(Map<String, Object> source,
                                                           List<String> projectAttributes) {
        if (projectAttributes == null || projectAttributes.isEmpty()) {
            return new LinkedHashMap<>(source);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (String attr : projectAttributes) {
            if (source.containsKey(attr)) {
                out.put(attr, source.get(attr));
            }
        }
        return out;
    }

    /**
     * Applies {@link OrderBySpec} ordering to result rows (stable multi-key sort).
     */
    public static void sortEnhancedQueryRows(List<EnhancedQueryRow> rows, List<OrderBySpec> orderBy) {
        if (orderBy == null || orderBy.isEmpty()) {
            return;
        }
        rows.sort((a, b) -> compareRowsWithOrderBy(a, b, orderBy));
    }

    private static int compareRowsWithOrderBy(EnhancedQueryRow a,
                                              EnhancedQueryRow b,
                                              List<OrderBySpec> orderBy) {
        for (OrderBySpec spec : orderBy) {
            int c = compareRowBySpec(a, b, spec);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static int compareRowBySpec(EnhancedQueryRow a, EnhancedQueryRow b, OrderBySpec spec) {
        Object va;
        Object vb;
        if (spec.isByAggregate()) {
            va = a.getAggregate(spec.attributeOrAggregateName());
            vb = b.getAggregate(spec.attributeOrAggregateName());
        } else {
            va = a.getItem(BASE_ALIAS).get(spec.attributeOrAggregateName());
            vb = b.getItem(BASE_ALIAS).get(spec.attributeOrAggregateName());
        }
        int cmp = compareNullableSortValues(va, vb);
        if (spec.direction() == SortDirection.DESC) {
            cmp = -cmp;
        }
        return cmp;
    }

    private static int compareNullableSortValues(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        if (a instanceof Number && b instanceof Number) {
            double da = ((Number) a).doubleValue();
            double db = ((Number) b).doubleValue();
            return Double.compare(da, db);
        }
        return compareForAggregate(a, b);
    }

    // ---- index detection ---------------------------------------------

    public static String findIndexForAttribute(TableSchema<?> schema, String attrName) {
        Collection<IndexMetadata> indices = schema.tableMetadata().indices();
        for (IndexMetadata idx : indices) {
            Optional<KeyAttributeMetadata> pk = idx.partitionKey();
            if (pk.isPresent() && pk.get().name().equals(attrName)) {
                String idxName = idx.name();
                if (!"$PRIMARY_INDEX".equals(idxName)) {
                    return idxName;
                }
            }
        }
        return null;
    }

    // ---- internal helpers --------------------------------------------

    @SuppressWarnings("unchecked")
    static void applyAggregate(Map<String, Object> bucket, AggregateSpec agg, Object val) {
        String out = agg.outputName();
        switch (agg.function()) {
            case COUNT:
                bucket.put(out, ((Number) bucket.get(out)).longValue() + 1);
                break;
            case SUM:
            case AVG:
                Number n = val instanceof Number ? (Number) val
                                                 : (val != null ? new BigDecimal(val.toString()) : null);
                if (n != null) {
                    BigDecimal sum = (BigDecimal) bucket.get(out + "_sum");
                    long count = ((Number) bucket.get(out + "_count")).longValue();
                    bucket.put(out + "_sum", sum.add(n instanceof BigDecimal
                                                     ? (BigDecimal) n
                                                     : new BigDecimal(n.toString())));
                    bucket.put(out + "_count", count + 1);
                }
                break;
            case MIN:
                Object curMin = bucket.get(out);
                if (curMin == null
                    || (val != null && compareForAggregate(val, curMin) < 0)) {
                    bucket.put(out, val);
                }
                break;
            case MAX:
                Object curMax = bucket.get(out);
                if (curMax == null
                    || (val != null && compareForAggregate(val, curMax) > 0)) {
                    bucket.put(out, val);
                }
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("unchecked")
    static int compareForAggregate(Object a, Object b) {
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Object>) a).compareTo(b);
            } catch (ClassCastException e) {
                return a.toString().compareTo(b.toString());
            }
        }
        return a.toString().compareTo(b.toString());
    }
}
