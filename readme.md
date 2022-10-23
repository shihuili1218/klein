
# 介绍
klein是一个分布式基于本地的k-v存储项目，它可以不依赖任何中间件，保证各个成员之间的缓存一致

## 缓存/K-V存储
- 最大存储的大小（LRU）
- TTL自动过期
## 锁

# 技术点
paxos, grpc, h2

# 优化
## paxos写请求
- 支持乱序协商，顺序确认
- 支持批量协商

## Paxos读请求优化
- 增加Master节点
- NWR
- confirm
- 不存在干扰key，无需执行一轮Prepare

# 章解
- 批量协商（队列），减少Prepare阶段，减少RPC交互
- 拆分group