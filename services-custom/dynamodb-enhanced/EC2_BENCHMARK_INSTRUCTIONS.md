# Enhanced Query Benchmark — EC2 Instructions

Run the Enhanced Query (join + aggregation) benchmark on an EC2 instance against real DynamoDB for production-like latency numbers.

---

## Prerequisites

- AWS account with DynamoDB + EC2 permissions.
- EC2 instance (Amazon Linux 2 or Ubuntu) in the **same region** as the DynamoDB tables.
- IAM role on the EC2 instance with: `dynamodb:Query`, `dynamodb:Scan`, `dynamodb:GetItem`, `dynamodb:PutItem`, `dynamodb:BatchWriteItem`, `dynamodb:DescribeTable`, `dynamodb:CreateTable`.
- Java 11+ and Maven 3.6+ installed on the EC2 instance.

---

## Step 1: Create DynamoDB tables

```bash
export AWS_REGION=us-east-1
export CUSTOMERS_TABLE=customers_large
export ORDERS_TABLE=orders_large

aws dynamodb create-table \
  --table-name $CUSTOMERS_TABLE \
  --attribute-definitions AttributeName=customerId,AttributeType=S \
  --key-schema AttributeName=customerId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region $AWS_REGION

aws dynamodb create-table \
  --table-name $ORDERS_TABLE \
  --attribute-definitions \
    AttributeName=customerId,AttributeType=S \
    AttributeName=orderId,AttributeType=S \
  --key-schema \
    AttributeName=customerId,KeyType=HASH \
    AttributeName=orderId,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region $AWS_REGION
```

Wait until both are ACTIVE:

```bash
aws dynamodb wait table-exists --table-name $CUSTOMERS_TABLE --region $AWS_REGION
aws dynamodb wait table-exists --table-name $ORDERS_TABLE --region $AWS_REGION
```

---

## Step 2: Copy repo to EC2

From your local machine:

```bash
rsync -avz --exclude='.git' --exclude='**/target' \
  /path/to/aws-sdk-java-v2 ec2-user@<EC2_IP>:~/aws-sdk-java-v2
```

---

## Step 3: Build on EC2

SSH into the instance:

```bash
ssh ec2-user@<EC2_IP>
```

Install Java + Maven (Amazon Linux 2):

```bash
sudo yum install -y java-11-amazon-corretto maven
```

Build the test classes:

```bash
cd ~/aws-sdk-java-v2
mvn test-compile -pl services-custom/dynamodb-enhanced -am -DskipTests -q
```

---

## Step 4: Seed data (first run only)

Seeds 1000 customers x 1000 orders (1M items). Takes a few minutes with PAY_PER_REQUEST.

```bash
cd ~/aws-sdk-java-v2

export AWS_REGION=us-east-1
export CUSTOMERS_TABLE=customers_large
export ORDERS_TABLE=orders_large
export CREATE_AND_SEED=true

mvn exec:java -pl services-custom/dynamodb-enhanced \
  -Dexec.mainClass="software.amazon.awssdk.enhanced.dynamodb.functionaltests.query.EnhancedQueryBenchmarkRunner" \
  -Dexec.classpathScope=test -q
```

Once seeded, you don't need `CREATE_AND_SEED` again.

---

## Step 5: Run the benchmark

```bash
cd ~/aws-sdk-java-v2

export AWS_REGION=us-east-1
export CUSTOMERS_TABLE=customers_large
export ORDERS_TABLE=orders_large
export BENCHMARK_ITERATIONS=5
export BENCHMARK_WARMUP=2
export BENCHMARK_OUTPUT_FILE=benchmark_ec2.csv

mvn exec:java -pl services-custom/dynamodb-enhanced \
  -Dexec.mainClass="software.amazon.awssdk.enhanced.dynamodb.functionaltests.query.EnhancedQueryBenchmarkRunner" \
  -Dexec.classpathScope=test -q
```

Results print to stdout (table format) and append to `benchmark_ec2.csv` if set.

---

## Step 6: Collect results

Copy CSV back to your machine:

```bash
scp ec2-user@<EC2_IP>:~/aws-sdk-java-v2/benchmark_ec2.csv .
```

---

## Step 7: Cleanup

```bash
aws dynamodb delete-table --table-name customers_large --region us-east-1
aws dynamodb delete-table --table-name orders_large --region us-east-1
```

Terminate the EC2 instance when done.

---

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `AWS_REGION` | SDK default | DynamoDB region |
| `CUSTOMERS_TABLE` | `customers_large` | Customers table name |
| `ORDERS_TABLE` | `orders_large` | Orders table name |
| `CREATE_AND_SEED` | unset | Set `true` to create tables + seed 1000x1000 data |
| `BENCHMARK_ITERATIONS` | `5` | Measured runs per scenario |
| `BENCHMARK_WARMUP` | `2` | Warm-up runs per scenario (not measured) |
| `BENCHMARK_OUTPUT_FILE` | unset | Path for CSV output |
| `CUSTOMER_COUNT` | `1000` | Number of customers to seed |
| `ORDERS_PER_CUSTOMER` | `1000` | Orders per customer to seed |

---

## Notes

- The benchmark runner class is `software.amazon.awssdk.enhanced.dynamodb.functionaltests.query.EnhancedQueryBenchmarkRunner`.
- For local-only runs (no AWS needed), set `USE_LOCAL_DYNAMODB=true` — this starts an in-process DynamoDB Local, seeds data, and runs the benchmark without any AWS credentials.
- Document in results: **region**, **EC2 instance type**, **dataset size**, **billing mode** (PAY_PER_REQUEST recommended).
