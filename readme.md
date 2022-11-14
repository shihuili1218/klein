
# 介绍
Klein是一个基于Paxos分布式共识类库，基于它实现了KV存储、缓存。

你可以独立部署Klein，像使用Redis一样使用它；但是仅仅是这样的话，也太没有新意了，它有趣的地方在于：Klein可以内嵌入你的项目中，你可以不依赖任何中间件，保证各个成员之间的数据一致。

基于此，你可以有无限多的想法，例如用Klein来实现KV存储，或者用它来实现分布式缓存，甚至用它来实现分布式锁，etc anything.

## 缓存/K-V存储
- 最大存储的大小（LRU）
- TTL自动过期
## 锁

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
  - [ ] 数据对齐
  - [ ] 优化读请求
- [ ] 成员自动发现(调研)
- [ ] 数据对齐：成员上线、落后成员对齐
- [ ] NWR
- [ ] confirm优化读请求
- [ ] 不存在干扰key，无需执行一轮Prepare

### 缓存
- [x] 读、写、等基础功能
- [ ] 配合持久化实现LRU
- [ ] TTL自动过期

### 待优化
- LogManager行锁
- 监控协商效率
- 监控线程池指标(DefaultTimer, ThreadExecutor)
- ProposalNo全局唯一

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