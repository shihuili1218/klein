<center>

### [中文](readme_zh_cn.md)
<strong>Open source：</strong> <a target="_blank" href='https://gitee.com/bleemliu/klein'>Gitee</a> | <a target="_blank" href='https://github.com/shihuili1218/klein'>Github</a> | <a target="_blank" href='https://gitcode.net/gege87417376/klein'>CodeChina</a>

[![Join the chat at https://gitter.im/klein-gitter/community](https://badges.gitter.im/klein-gitter/community.svg)](https://gitter.im/klein-gitter/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/764fda630fd845949ae492a1f6469173)](https://www.codacy.com/gh/shihuili1218/klein/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=shihuili1218/klein&amp;utm_campaign=Badge_Grade)
</center>

# Introduce
![logo](logo.svg)

Klein is a Paxos based distributed collection tool library, including distributed ArrayList, distributed HashMap, distributed Cache, distributed Lock, etc..

In addition, based on Paxos, we can imagine endlessly, such as: KV-storage, Registry, Configuration center, etc.

We hope that Klein can be deployed independently or embedded in your project. You can use it just like Redis,
or embed Klein into your project without relying on any middleware to ensure data consistency among members. Of course,
this is still in the process of implementation. 😆😆😆

**Look forward to your star⭐**

# Quick Start
### dependence klein
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
### startup
```
Klein instance = Klein.startup();
instance.getCache().put("hello", "klein");
```
### set property
For all configurable parameters, see: `com.ofcoder.klein.core.config.KleinProp`

You can set it through `System#setProperty` or get the `KleinProp` object
```
System.setProperty("klein.id", "2")

// or

KleinProp prop = KleinProp.loadIfPresent();
```

# Milepost

## Evolve
### Paxos
- [x] Write request, disordered negotiation, sequential confirmation
- [x] Read request, using negotiation log
- [x] Batch negotiation
- [x] Optimize the prepare phase
- [x] Snapshot
- [x] To split roles such as Group and proposer, you only need to isolate instance instead of isolating them
- [x] Master role：
  - [x] Change of members
  - [x] The master promotion should have the most complete data (the master should be elected through negotiation with the proposal. If the promotion is successful, then boost did not reach a consensus)
  - [x] Keep data consistent
    - [x] Master heartbeat triggers data synchronization
    - [x] Snapshot synchronization (the heartbeat carries the checkpoint and the learn message returns the checkpoint)
    - [x] New members join the cluster and actively learn from the master
  - [ ] ~~Optimize read requests (write requests must be copied to the master)~~
  - [x] Optimize write requests (write requests can only be executed by the master to avoid livelocks)
- [ ] Automatic member discovery (research)
- [ ] NWR
- [ ] Verified by jepsen

### Cache
- [x] Basic functions such as reading, writing, etc
- [x] Implement LRU with persistence
- [x] Cache Automatic Expiration (TTL)
- [x] Clock skew

### Collection
- [ ] list
- [ ] map

### To be optimized
- [ ] LogManager row lock
- [ ] Monitor negotiation efficiency
- [ ] Monitoring thread pool indicators (DefaultTimer, ThreadExecutor)
- [x] ProposalNo全局唯一
- [x] 状态机持久化（master、lock）

# Design ideas
[Paxos](klein-consensus/klein-consensus-paxos/readme.md)
- How to generate ProposalNo?
- Can parallel negotiation really be supported?
- Which proposal will reach consensus?
- Can the Confirm phase (application state transition) really be executed asynchronously?
- How do I create a snapshot of a running system?
- Is it necessary to completely isolate the splitting of a group?
- Optimize the Prepare phase
- Batch negotiation (queue) to reduce RPC interaction

# Star History

[![Star History Chart](https://api.star-history.com/svg?repos=shihuili1218/klein&type=Date)](https://star-history.com/#shihuili1218/klein&Date) 
