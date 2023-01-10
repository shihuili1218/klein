### [English](readme.md)

# 介绍
![](logo.svg)

Klein是一个基于Paxos的分布式收集工具库，包括分布式ArrayList、分布式HashMap、分布式Cache、分布式Lock等。。

此外，基于Paxos，我们可以有无限想象，例如：KV存储、注册表、配置中心等。

我们希望Klein可以独立部署或嵌入到您的项目中。你可以像Redis一样使用它，但是仅仅是这样的话，也太没有新意了，它有趣的地方在于：Klein可以内嵌入你的项目中，你可以不依赖任何中间件，保证各个成员之间的数据一致。
当然 这仍在实施过程中。😆😆😆

**Look forward to your star⭐**

## 缓存/K-V存储
- 最大存储的大小（LRU）
- TTL自动过期
## 锁

# 使用
### 引入klein
```xml
<dependency>
    <groupId>com.ofcoder.klein.core</groupId>
    <artifactId>klein-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
```xml
<repositories>
    <repository>
        <id>ossrh</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>
```
### 启动
```
Klein instance = Klein.startup();
instance.getCache().put("hello", "klein");
```
### 配置
For all configurable parameters, see: `com.ofcoder.klein.core.config.KleinProp`

You can set it through `System#setProperty` or get the `KleinProp` object
```
System.setProperty("klein.id", "2")

// or

KleinProp prop = KleinProp.loadIfPresent();
```

# 里程map

## 进度
### paxos
- [x] 写请求、乱序协商，顺序确认
- [x] 读请求，使用协商log完成
- [x] 批量协商
- [x] 优化prepare阶段
- [x] 快照
- [x] 拆分Group，proposer等角色无须隔离，只需隔离instance
- [x] 增加Master：
  - [x] 成员变更
  - [x] master晋升应拥有最完整的数据(使用提案协商来选举master，如果成功晋升成master，接着推进未达成共识的提案)
  - [x] 数据对齐
    - [x] Master心跳触发对齐
    - [x] 快照同步（心跳携带checkpoint、learn消息返回checkpoint）
    - [x] 新成员加入集群，主动向master学习
  - [ ] 优化读请求(写请求一定要复制到Master)
  - [ ] 优化写请求(写请求只能由Master执行，避免活锁)
- [ ] 成员自动发现(调研)
- [ ] NWR
- [ ] confirm优化读请求
- [ ] 不存在干扰key，无需执行一轮Prepare
- [ ] Verified by jepsen

### 缓存
- [x] 读、写、等基础功能
- [x] 配合持久化实现LRU
- [x] TTL自动过期

### 集合
- [ ] list
- [ ] map


### 待优化
- [ ] LogManager行锁
- [ ] 监控协商效率
- [ ] 监控线程池指标(DefaultTimer, ThreadExecutor)
- [x] ProposalNo全局唯一
- [x] 状态机持久化（master、lock）

# 章解
[Paxos](klein-consensus/klein-consensus-paxos/readme.md)
- ProposalNo怎么生成？
- 是否真的能支持并行协商？
- 到底哪个提案会达成共识？
- Confirm阶段（应用状态转移）是否真的可以异步执行？
- 如何为一个运行的系统创建快照？
- Group的拆分是否有必要完全隔离？
- 优化Prepare阶段
- 批量协商（队列），减少RPC交互

# Star History

[![Star History Chart](https://api.star-history.com/svg?repos=shihuili1218/klein&type=Date)](https://star-history.com/#shihuili1218/klein&Date)
