
## 是否能支持并行协商？
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
        localChecksum = checksum(value in the range [0, msgId)); 
        if(localChecksum == msg.checksum) {
            return <granted, local.value>;
        } else {
            return <refuse, null>;
        }
    } else {
        return <refuse, null>;
    }
```
