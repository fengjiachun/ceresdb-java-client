# CeresDB Java Client

## 介绍
CeresDBxClient 是 CeresDB 的高性能 Java 版客户端。CeresDB 是定位为高性能的、分布式的、Schema-less 的云原生时序数据库。它可以同时支持时间序列和数据分析型的工作负载。

## 功能特性
- 通信层基于 SPI 可扩展，默认提供 gRPC 的实现
- 提供纯异步的流式高性能写入 API
- 默认提供丰富的性能指标采集，可输指标统计到本地文件
- 支持关键对象内存状态快照或配置输出到本地文件以协助排查问题

## 写入流程图

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

## 查询流程
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

## 需要
编译需要 Java 8 及以上

## 建表
CeresDB 是一个 Schema-less 的时序数据引擎，你可以不必创建 schema 就立刻写入数据（CeresDB 会根据你的第一次写入帮你创建一个默认的 schema）。
当然你也可以自行创建一个 schema 来更精细化的管理的表（比如索引等）

下面的建表语句（使用 SDK 的 SQL API）包含了 CeresDB 支持的所有字段类型：

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

## 写入 Example
```java
// CeresDBx options
final CeresDBxOptions opts = CeresDBxOptions.newBuilder("127.0.0.1", 8081) //
        .tenant("test", "sub_test", "test_token") // 租户信息
        .writeMaxRetries(1) // 写入失败重试次数上限（只有部分错误 code 才会重试，比如路由表失效）
        .readMaxRetries(1) // 查询失败重试次数上限（只有部分错误 code 才会重试，比如路由表失效）
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
    // 下面针对 cpu、mem 两列，一次写入了三行数据（3 个时间戳），CeresDB 鼓励这种实践，SDK 可以通过高效的压缩来减少网络传输，并且对 server 端写入非常友好
    .field(t0, "cpu", FieldValue.withDouble(0.23)) // 第 1 行第 1 列
    .field(t0, "mem", FieldValue.withDouble(0.55)) // 第 1 行第 2 列
    .field(t1, "cpu", FieldValue.withDouble(0.25)) // 第 2 行第 1 列
    .field(t1, "mem", FieldValue.withDouble(0.56)) // 第 2 行第 2 列
    .field(t2, "cpu", FieldValue.withDouble(0.21)) // 第 3 行第 1 列
    .field(t2, "mem", FieldValue.withDouble(0.52)) // 第 3 行第 2 列
    .build();

final CompletableFuture<Result<WriteOk, Err>> wf = client.write(data);
// 这里用 `future.get` 只是方便演示，推荐借助 CompletableFuture 强大的 API 实现异步编程
final Result<WriteOk, Err> wr = wf.get();

Assert.assertTrue(wr.isOk());
Assert.assertEquals(3, wr.getOk().getSuccess());
// `Result` 类参考了 Rust 语言，提供了丰富的 mapXXX、andThen 类 function 方便对结果值进行转换，提高编程效率，欢迎参考 API 文档使用
Assert.assertEquals(3, wr.mapOr(0, WriteOk::getSuccess()));
Assert.assertEquals(0, wr.getOk().getFailed());
Assert.assertEquals(0, wr.mapOr(-1, WriteOk::getFailed));

final QueryRequest queryRequest = QueryRequest.newBuilder()
        .forMetrics("machine_metric") // 表名可选填，不填的话 SQL Parser 会自动解析 ql 涉及到的表名并完成自动路由
        .ql("select timestamp, cpu, mem from machine_metric") //
        .build();
final CompletableFuture<Result<QueryOk, Err>> qf = client.query(queryRequest);
// 这里用 `future.get` 只是方便演示，推荐借助 CompletableFuture 强大的 API 实现异步编程
final Result<QueryOk, Err> qr = qf.get();

Assert.assertTrue(qr.isOk());

final QueryOk queryOk = qr.getOk();

final List<Record> records = queryOk.mapToRecord().collect(Collectors.toList())
```

## Licensing
遵守 [Apache License 2.0](./LICENSE).

## 社区与技术支持
- 加入 Slack 社区与用户群 [Slack 入口](https://join.slack.com/t/ceresdbcommunity/shared_invite/zt-1au1ihbdy-5huC9J9s2462yBMIWmerTw)
- 加入微信社区与用户群 [微信二维码](https://github.com/CeresDB/assets/blob/main/WeChatQRCode.jpg)
- 搜索并加入钉钉社区与用户群 44602802
