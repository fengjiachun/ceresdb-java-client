# CeresDB Java Client
[中文](./README_CN.md)

## Introduction
CeresDBxClient is a high-performance Java client for CeresDB.
CeresDB is a high-performance, distributed, schema-less, cloud native time-series database that can handle both time-series and analytics workloads.

## Features
- With the well designed SPI, the network transport layer is extensible. And we provide the default implementation which uses the gRPC framework.
- The client provides high-performance async streaming write API.
- The client also collects lots of performance metrics by default. These metrics can be configured to write to local file.
- We can take memory snapshots that contains the status of critical objects. The snapshots can also be configured to write to local file, which helps a lot when we diagnose complex problems.

## Data ingestion process

```
                   ┌─────────────────────┐  
                   │   CeresDBxClient    │  
                   └─────────────────────┘  
                              │  
                              ▼  
                   ┌─────────────────────┐  
                   │     WriteClient     │───┐  
                   └─────────────────────┘   │  
                              │     Async to retry and merge responses  
                              │              │  
                 ┌────Split requests         │  
                 │                           │  
                 │  ┌─────────────────────┐  │   ┌─────────────────────┐       ┌─────────────────────┐
                 └─▶│    RouterClient     │◀─┴──▶│     RouterCache     │◀─────▶│      RouterFor      │
                    └─────────────────────┘      └─────────────────────┘       └─────────────────────┘
                               ▲                                                          │  
                               │                                                          │  
                               ▼                                                          │  
                    ┌─────────────────────┐                                               │  
                    │      RpcClient      │◀──────────────────────────────────────────────┘  
                    └─────────────────────┘  
                               ▲  
                               │  
                               ▼  
                    ┌─────────────────────┐  
                    │  Default gRPC impl  │  
                    └─────────────────────┘  
                               ▲  
                               │  
           ┌───────────────────┴ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐  
           │                            │  
           ▼                            ▼                            ▼  
┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐  
│   CeresDBx Node1    │      │   CeresDBx Node2    │      │         ...         │  
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘  
```

## Data query process
```
                   ┌─────────────────────┐  
                   │   CeresDBxClient    │  
                   └─────────────────────┘  
                              │  
                              ▼  
                   ┌─────────────────────┐  
                   │     QueryClient     │───┐  
                   └─────────────────────┘   │  
                              │              │Async to retry  
                              │              │  
                 ┌────────────┘              │  
                 │                           │  
                 │  ┌─────────────────────┐  │   ┌─────────────────────┐       ┌─────────────────────┐
                 └─▶│    RouterClient     │◀─┴──▶│     RouterCache     │◀─────▶│      RouterFor      │
                    └─────────────────────┘      └─────────────────────┘       └─────────────────────┘
                               ▲                                                          │  
                               │                                                          │  
                               ▼                                                          │  
                    ┌─────────────────────┐                                               │  
                    │      RpcClient      │◀──────────────────────────────────────────────┘  
                    └─────────────────────┘  
                               ▲  
                               │  
                               ▼  
                    ┌─────────────────────┐  
                    │  Default gRPC impl  │  
                    └─────────────────────┘  
                               ▲  
                               │  
           ┌───────────────────┴ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐  
           │                            │  
           ▼                            ▼                            ▼  
┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐  
│   CeresDBx Node1    │      │   CeresDBx Node2    │      │         ...         │  
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘  
```

## Requirements
- Java 8 or later is required for compilation 


## Create table
CeresDB is a Schema-less time-series database, so creating table schema ahead of data ingestion is not required (CeresDB will create a default schema according to the very first data you write into it). Of course, you can also manually create a schema for fine grained management purposes (eg. managing index).

The following table creation statement（using the SQL API included in SDK ）shows all field types supported by CeresDB：

```java
SqlResult result = client.management().executeSql("CREATE TABLE MY_FIRST_TABL(" +
    "ts TIMESTAMP NOT NULL," +
    "c1 STRING TAG NOT NULL," +
    "c2 STRING TAG NOT NULL," +
    "c3 DOUBLE NULL," +
    "c4 STRING NULL," +
    "c5 INT64 NULL," +
    "c6 FLOAT NULL," +
    "c7 INT32 NULL," +
    "c8 INT16 NULL," +
    "c9 INT8 NULL," +
    "c10 BOOLEAN NULL,"
    "c11 UINT64 NULL,"
    "c12 UINT32 NULL,"
    "c13 UINT16 NULL,"
    "c14 UINT8 NULL,"
    "c15 TIMESTAMP NULL,"
    "c16 VARBINARY NULL,"
    "TIMESTAMP KEY(ts)) ENGINE=Analytic"
);
```

## Data ingestion example
```java
// CeresDBx options
final CeresDBxOptions opts = CeresDBxOptions.newBuilder("127.0.0.1", 8081) //
        .tenant("test", "sub_test", "test_token") // tenant info
        // maximum retry times when write fails
        // (only some error codes will be retried, such as the routing table failure)
        .writeMaxRetries(1)
        // maximum retry times when read fails
        // (only some error codes will be retried, such as the routing table failure)
        .readMaxRetries(1)
        .build();

final CeresDBxClient client = new CeresDBxClient();
if (!client.init(this.opts)) {
    throw new IllegalStateException("Fail to start CeresDBxClient");
}

final long t0 = System.currentTimeMillis();
final long t1 = t0 + 1000;
final long t2 = t1 + 1000;
final Rows data = Series.newBuilder("machine_metric")
    .tag("city", "Singapore")
    .tag("ip", "127.0.01")
    .toRowsBuilder()
    // codes below organizes 3 lines data (3 timestamps) for the `cpu` and `mem` column, this will just transport once through network. CeresDB encourage practices like this, because the SDK could use efficient compression algorithm to reduce network overhead and also be friedly to the sever side.
    .field(t0, "cpu", FieldValue.withDouble(0.23)) // first row, first column
    .field(t0, "mem", FieldValue.withDouble(0.55)) // first row, second column
    .field(t1, "cpu", FieldValue.withDouble(0.25)) // second row, first column
    .field(t1, "mem", FieldValue.withDouble(0.56)) // second row, second column
    .field(t2, "cpu", FieldValue.withDouble(0.21)) // third row, first column
    .field(t2, "mem", FieldValue.withDouble(0.52)) // third row, second column
    .build();

final CompletableFuture<Result<WriteOk, Err>> wf = client.write(data);
// here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
final Result<WriteOk, Err> wr = wf.get();

Assert.assertTrue(wr.isOk());
Assert.assertEquals(3, wr.getOk().getSuccess());
// `Result` class referenced the Rust language practice, provides rich functions (such as mapXXX, andThen) transforming the result value to improve programming efficiency. You can refer to the API docs for detail usage.
Assert.assertEquals(3, wr.mapOr(0, WriteOk::getSuccess()));
Assert.assertEquals(0, wr.getOk().getFailed());
Assert.assertEquals(0, wr.mapOr(-1, WriteOk::getFailed));

final QueryRequest queryRequest = QueryRequest.newBuilder()
        .forMetrics("machine_metric") // table name is optional. If not provided, SQL parser will parse the `ql` to get the table name and do the routing automaticly
        .ql("select timestamp, cpu, mem from machine_metric") //
        .build();
final CompletableFuture<Result<QueryOk, Err>> qf = client.query(queryRequest);
// here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
final Result<QueryOk, Err> qr = qf.get();

Assert.assertTrue(qr.isOk());

final QueryOk queryOk = qr.getOk();

final List<Record> records = queryOk.mapToRecord().collect(Collectors.toList())
```
## Licensing
Under [Apache License 2.0](./LICENSE).

## Community and support
- Join the user group on [Slack](https://join.slack.com/t/ceresdbcommunity/shared_invite/zt-1au1ihbdy-5huC9J9s2462yBMIWmerTw)
- Join the use group on WeChat [WeChat QR code](https://github.com/CeresDB/assets/blob/main/WeChatQRCode.jpg)
- Join the user group on DingTalk: 44602802
