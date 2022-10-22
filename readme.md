klein

paxos, grpc, h2

paxos写请求
- 支持乱序协商，顺序确认

Paxos读请求优化
- 增加Master节点
- NWR
- confirm
- 不存在干扰key的优化

klein是一个分布式基于本地的k-v存储项目，它可以不依赖任何中间件，保证各个成员之间的缓存一致

你可以用来实现缓存，分布式k-v数据库


最大存储的大小（LRU）
TTL自动过期