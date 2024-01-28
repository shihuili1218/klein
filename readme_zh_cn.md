### [English](readme.md)
<p align="center">
    <strong>感谢JetBrains提供的<a target="_blank" href="https://www.jetbrains.com/community/opensource">免费授权</a>.</strong>
    <br/>
    <strong>Open source：</strong> <a target="_blank" href='https://gitee.com/bleemliu/klein'>Gitee</a> | <a target="_blank" href='https://github.com/shihuili1218/klein'>Github</a> | <a target="_blank" href='https://gitcode.net/gege87417376/klein'>CodeChina</a>
    <br/>
    <strong>文档：</strong> <a target="_blank" href='https://klein-doc.gitbook.io/en'>Gitbook</a>
</p>

<p align="center">
   <a href="https://gitter.im/klein-gitter/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge"><img src="https://badges.gitter.im/klein-gitter/community.svg"  alt=""/></a>
    &nbsp;
    <a href="https://www.codacy.com/gh/shihuili1218/klein/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=shihuili1218/klein&amp;utm_campaign=Badge_Grade"><img src="https://app.codacy.com/project/badge/Grade/764fda630fd845949ae492a1f6469173" alt="" /></a>
    &nbsp;
    <a href="https://github.com/shihuili1218/klein/actions/workflows/mvn_test.yml"><img src="https://github.com/shihuili1218/klein/actions/workflows/mvn_test.yml/badge.svg" alt="" /></a>
</p>

<p align="center">
    <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0-blue" alt=""/></a>
    &nbsp;
    <a href="https://search.maven.org/search?q=g:com.ofcoder.klein%20AND%20klein"><img src="https://img.shields.io/maven-central/v/com.ofcoder.klein/klein.svg?label=maven%20central" alt="" /></a>
</p>


# 介绍
![](logo.svg)

Klein是一个基于Paxos的分布式集合工具库，包括分布式Cache、分布式消息队列、分布式List、分布式Map、分布式Lock等。。它有趣的地方在于：您可以不依赖任何中间件，保证各个成员之间的数据一致。

你可以理解Klein是一个通过Maven依赖到您项目中的分布式工具。我们希望它可以替换掉您现有的Redis、消息中间件、注册表、配置中心等。

当然，这个工程是庞大的，我们目前已经实现了分布式缓存。

如果您对分布式消息队列、分布式List、分布式Map、分布式Lock兴趣的话，我们可以分享现有相应的设计，您可以参与到编码工作当中来。😆😆😆

**Look forward to your star⭐**

# 使用
### 引入klein
```xml
<dependency>
    <groupId>com.ofcoder.klein.core</groupId>
    <artifactId>klein-core</artifactId>
    <version>{last-version}</version>
</dependency>
```

### 启动
```
Klein instance = Klein.startup();
instance.awaitInit();

KleinCache klein = KleinFactory.getInstance().createCache("klein");
klein.put("hello", "klein");
```
### 配置
所有可配置的参数，请查看：`com.ofcoder.klein.KleinProp`

你可以通过System#setProperty设置，也可以获取到KleinProp对象
```
System.setProperty("klein.id", "2")

// 或者 

KleinProp prop = KleinProp.loadIfPresent();
```

# Jepsen 测试

[Run on Gitpod](https://gitpod.io/#/github.com/shihuili1218/klein)

