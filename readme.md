
# 介绍
klein是一个分布式基于本地的k-v存储项目，它可以不依赖任何中间件，保证各个成员之间的缓存一致

## 缓存/K-V存储
- 最大存储的大小（LRU）
- TTL自动过期
## 锁

# 技术点
paxos, grpc, h2

## 进度
### paxos
- [x] 写请求、乱序协商，顺序确认
- [x] 读请求，使用协商log完成
- [x] 批量协商
- [x] 优化prepare阶段
- [x] 快照
- [x] 拆分Group，proposer等角色无须隔离，只需隔离instance
- [ ] 增加Master：成员变更、优化读请求
- [ ] 成员自动发现(调研)
- [ ] 数据对齐：成员上线、落后成员对齐
- [ ] NWR
- [ ] confirm优化读请求
- [ ] 不存在干扰key，无需执行一轮Prepare

### 缓存
- [x] 读、写、等基础功能
- [ ] 配合持久化实现LRU
- [ ] TTL自动过期

# 章解
[Paxos](klein-consensus/klein-consensus-paxos/readme.md)
- 是否真的能支持并行协商？
- 到底哪个提案会达成共识？
- Confirm阶段（应用状态转移）是否真的可以异步执行？
- 如何为一个运行的系统创建快照？
- Group的拆分是否有必要完全隔离？
- 优化Prepare阶段
- 批量协商（队列），减少RPC交互