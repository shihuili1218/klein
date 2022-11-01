
## 是否真的能支持并行协商？
并行协商是指多个Instance同时协商
在Basic-Paxos中，并没有对并行协商做过多的限制，事实上也是如此，每个Instance上都运行一轮Paxos，它们并不会互相干扰。

在Multi-Paxos中，我们优化掉了Prepare阶段，而Prepare阶段是保证已达成共识的提案不再改变的关键逻辑，这个优化过后的算法是否还能支持并行协商？我们看下面的案例

```
使用<instanceId, proposalNo>标识一个提案
A: pre<2, 1> → A, C
A: acc<2, 1> → A, C    reach consensus.

B-T1: pre<2, 1>           network failure.
B-T1: pre<2, 2>           network failure.
B-T1: wait
B-T2: pre<3, 3> → A, B, C
B-T1: notify, acc<2, 3>   once again reach consensus.
```
可以看到在上述的案例中，原本达成共识的提案是<2, 1>，而最终却变成了<2, 3>，明显在ProposalNo为2和3上可以提出不同的值。此时的算法是不安全的。

那么在Multi-Paxos如何能并行协商呢？我们需要修改Prepare的逻辑。

```
origin paxos handle prepare request:
    if(msg.proposalNo > local.proposalNo) {
        return <granted, local.value>;
    } else {
        return <refuse, null>;
    }

klein paxos handle prepare request:
    if(msg.proposalNo > local.proposalNo) {
        msgId = msg.instanceId;
        localValues = local(value with PREPARED/ACCEPTED status within [0, msgId)); 
        return <granted, localValues>;
    } else {
        return <refuse, null>;
    }
```
修改过后的Prepare相当于对当前所有的Instance都进行了Prepare阶段，那么在Accept选择值时，就可以直接从Prepare的响应中获取了。

不过在Prepare的响应中携带所有的instance，是不现实的。针对于此，有两个可优化的地方
- 我们需要引入快照，只携带快照以外的instance。
- 只携带非CONFIRMED的instance。

对于第二点，如果某个instance是CONFIRMED，那么其他成员是感知不到，其他成员仍然可以以自己的值发起提案，因此，还需要修改Acceptor处理accept请求的逻辑：
```
origin paxos handle accept request:
    if(msg.proposalNo >= local.proposalNo) {
        return <granted>;
    } else {
        return <refuse>;
    }

klein paxos handle accept request:
    if(local.instance.state == CONFIRMED) {
        // 
        return <refuse, local.instance>;
    }
    if(msg.proposalNo >= local.proposalNo) {
        return <granted, localValues>;
    } else {
        return <refuse, null>;
    }

```

## 到底哪个提案会达成共识？
```
A: pre<2, 1> → A, C
A: acc<2, 1> → A

B: pre<2, 2> → B, C
B: acc<2, 2> → B

C: pre<2, 3> → A, C     -----> 1
C: pre<2, 3> → B, C     -----> 2
```

## Confirm阶段（应用状态转移）是否真的可以异步执行？
- 无返回值的写请求可以
- 有返回值的写请求、读请求不行


## 如何为一个运行的系统创建快照？
先为状态机创建一个镜像，然后为镜像创建快照

## Group的拆分是否有必要完全隔离？
作为一个共识类库，拆分Group是行业内通用的优化手段，这样做几个好处：
- 最大限度的利用计算资源
- 提高协商效率

解释一下这两个好处，以Raft为例，Raft是一个串行协商的共识算法，一个Instance需要等到上一个Instance完成后才能协商。
因此，Raft同一时间只有一个Instance协商，这样的设计，势必有很大的CPU资源空闲着，而拆分Group，可以使得一个Raft集群服务于多个Group，每个Group负责互不干扰的业务，拆分的Group越多，使用CPU资源越多；
第二个好处，是因为Raft是串行协商的，单个协商效率虽然很高，但是面对复杂的生产业务，协商效率不行的，所以也需要拆分Group，使得Group与Group之间能并行协商。

而Klein作为一个内嵌式的库，跟其他的共识库不同。我们没必要执迷于把计算资源耗尽，相反我们希望把更多的计算资源留给业务系统。
因此拆分Group虽然有必要，但是没必要完全隔离出Proposer、Acceptor、Learner角色，只需要隔离Instance即可，原因有以下几个：

- 因为基于Paxos的乱序协商，不会受到串行协商的限制，因此整体的协商效率与完全隔离不会相差很多。
- 另外不隔离Proposer等角色，各个Group之间还可以共享Prepare阶段，进一步减少执行Prepare阶段的次数。
- 还有一个原因是，Klein作为一个内嵌库，所实现的功能往往是单一的，很少遇到负责的业务场景。

这样分析下来，只隔离Instance的协商效率会更高，因此只隔离Instance是一个高效且正确的决定。