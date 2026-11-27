#!/usr/bin/env bash
#
# Runs the 6 enhanced-query functional test classes and prints a table: Test (class.method) | Time (ms).
# Then prints a report listing tests that exceeded 1 second.
# Run from repo root: ./services-custom/dynamodb-enhanced/run-enhanced-query-tests-and-print-timing.sh
# Requires: Maven, full SDK build (e.g. mvn install -DskipTests from root first).
#

set -e
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"
REPORT_DIR="services-custom/dynamodb-enhanced/target/surefire-reports"
CLASSES=(
  "EnhancedQueryJoinSyncTest"
  "EnhancedQueryJoinAsyncTest"
  "EnhancedQueryAggregationSyncTest"
  "EnhancedQueryAggregationAsyncTest"
  "NestedAttributeFilteringTest"
  "BuildValidationTest"
)
PACKAGE="software.amazon.awssdk.enhanced.dynamodb.functionaltests"
EXCEEDED_THRESHOLD_MS=5000
EXCEEDED_FILE="${REPORT_DIR}/exceeded_1s.txt"

get_operation() {
  local class_method="$1"
  local method="${class_method##*.}"
  case "$method" in
    aggregation_withFilter|aggregation_treeCondition|aggregation_orderByAggregate|aggregation_orderByKey|aggregation_limit|\
    executionMode_allowScan|aggregation_withJoinedTableCondition_allowScan_returnsFilteredCounts|\
    withCondition_returnsFilteredRows|treeCondition_returnsMatchingRows|limit_enforced|\
    filterByNestedState_scanMode|filterByNestedZip_between|filterByNestedCity_contains|filterByNestedCity_beginsWith|\
    filterByNestedAndFlatAttribute_combined|filterByNestedAttribute_orCombination)
      echo "scan()"
      ;;
    executionMode_strictKeyOnly_withoutKey_returnsEmptyOrNoScan|\
    groupByWithoutAggregate_throwsWithMessage|filterBaseWithoutJoin_throwsWithMessage|filterJoinedWithoutJoin_throwsWithMessage|\
    validSingleTableQuery_doesNotThrow|validJoinQuery_doesNotThrow|validAggregationQuery_doesNotThrow|\
    validJoinWithAggregation_doesNotThrow|aggregateWithoutGroupBy_doesNotThrow)
      echo "none"
      ;;
    *)
      echo "query()"
      ;;
  esac
}

METRICS_FILE="${REPORT_DIR}/query-metrics.txt"

echo "Running enhanced-query functional test classes (EnhancedQuery*, NestedAttributeFiltering, BuildValidation)..."
# Remove stale metrics file so tests write fresh data
rm -f "$METRICS_FILE"
# Run each class in a separate Maven invocation to isolate local DynamoDB lifecycle per test class.
for class in "${CLASSES[@]}"; do
  mvn test -pl services-custom/dynamodb-enhanced \
    -Dspotbugs.skip=true \
    -Dcheckstyle.skip=true \
    -Dtest="${PACKAGE}.${class}"
done

printf "\n"
echo "Per-test execution time (milliseconds):"
echo "----------------------------------------"
printf "%-95s %10s\n" "TEST (class.method)" "TIME (ms)"
echo "----------------------------------------"

rm -f "$EXCEEDED_FILE"
mkdir -p "$REPORT_DIR"
for class in "${CLASSES[@]}"; do
  xml="${REPORT_DIR}/TEST-${PACKAGE}.${class}.xml"
  if [[ -f "$xml" ]]; then
    grep -oE '<testcase [^>]+>' "$xml" | while read -r line; do
      name=$(echo "$line" | sed -n 's/.* name="\([^"]*\)".*/\1/p')
      label="${class}.${name}"
      # Prefer query-execution time from the metrics file written by the test
      metric_ms=""
      if [[ -f "$METRICS_FILE" ]]; then
        metric_line=$(grep "^${label} " "$METRICS_FILE" | tail -n1)
        if [[ -n "$metric_line" ]]; then
          metric_ms=$(echo "$metric_line" | awk '{print $2}')
        fi
      fi
      if [[ -n "$metric_ms" ]]; then
        ms="$metric_ms"
      else
        # Fallback: Surefire total test-method time (includes @Before/@After)
        time=$(echo "$line" | sed -n 's/.* time="\([^"]*\)".*/\1/p')
        ms=$(awk "BEGIN { printf \"%.0f\", ${time:-0} * 1000 }")
      fi
      printf "%-95s %10s\n" "$label" "$ms"
      if [[ "$ms" =~ ^[0-9]+$ ]] && [[ "$ms" -gt "$EXCEEDED_THRESHOLD_MS" ]]; then
        echo "${label} ${ms}" >> "$EXCEEDED_FILE"
      fi
    done
  else
    printf "%-95s %10s\n" "${class}.(no report - run failed?)" "-"
  fi
done
echo "----------------------------------------"
printf "\n"
echo "Report — tests that exceeded ${EXCEEDED_THRESHOLD_MS} ms:"
echo "----------------------------------------"
if [[ -f "$EXCEEDED_FILE" ]] && [[ -s "$EXCEEDED_FILE" ]]; then
  while read -r label ms; do
    op=$(get_operation "$label")
    printf "  EXCEEDED  %-80s %8s ms  (%s)\n" "$label" "$ms" "$op"
  done < "$EXCEEDED_FILE"
else
  echo "  (none)"
fi
echo "----------------------------------------"

printf "\n"
echo "Summary table (all tests):"
echo "----------------------------------------"
printf "%-80s %12s %14s %10s\n" "TEST (class.method)" "TIME (ms)" "OPERATION" "ROWS"
echo "----------------------------------------"
ALL_RESULTS_FILE="${REPORT_DIR}/all_results.txt"
rm -f "$ALL_RESULTS_FILE"
for class in "${CLASSES[@]}"; do
  xml="${REPORT_DIR}/TEST-${PACKAGE}.${class}.xml"
  if [[ -f "$xml" ]]; then
    grep -oE '<testcase [^>]+>' "$xml" | while read -r line; do
      name=$(echo "$line" | sed -n 's/.* name=\"\([^\"]*\)\".*/\1/p')
      label="${class}.${name}"
      op=$(get_operation "$label")
      # Read query execution time and row count from the metrics file (written by tests)
      ms=""
      rows="-"
      if [[ -f "$METRICS_FILE" ]]; then
        metric_line=$(grep "^${label} " "$METRICS_FILE" | tail -n1)
        if [[ -n "$metric_line" ]]; then
          ms=$(echo "$metric_line" | awk '{print $2}')
          rows=$(echo "$metric_line" | awk '{print $3}')
        fi
      fi
      if [[ -z "$ms" ]]; then
        # Fallback: Surefire total test-method time (includes @Before/@After)
        time=$(echo "$line" | sed -n 's/.* time=\"\([^\"]*\)\".*/\1/p')
        ms=$(awk "BEGIN { printf \"%.0f\", ${time:-0} * 1000 }")
      fi
      echo "${label} ${ms} ${op} ${rows}" >> "$ALL_RESULTS_FILE"
    done
  fi
done

if [[ -f "$ALL_RESULTS_FILE" ]] && [[ -s "$ALL_RESULTS_FILE" ]]; then
  GREEN_BG="\033[42;30m"
  RED_BG="\033[41;37m"
  RESET="\033[0m"
  while read -r label ms op rows; do
    if [[ "$ms" =~ ^[0-9]+$ ]] && [[ "$ms" -gt "$EXCEEDED_THRESHOLD_MS" ]]; then
      color="$RED_BG"
    else
      color="$GREEN_BG"
    fi
    printf "${color}%-80s %12s %14s %10s${RESET}\n" "$label" "$ms" "$op" "$rows"
  done < "$ALL_RESULTS_FILE"
else
  echo "  (no test results found)"
fi
echo "----------------------------------------"
