### [中文](readme_zh_cn.md)

<p align="center">
    <strong>Thanks to JetBrains for the <a target="_blank" href="https://www.jetbrains.com/community/opensource">free license</a>.</strong>
    <br/>
    <strong>Open source：</strong> <a target="_blank" href='https://gitee.com/bleemliu/klein'>Gitee</a> | <a target="_blank" href='https://github.com/shihuili1218/klein'>Github</a> | <a target="_blank" href='https://gitcode.net/gege87417376/klein'>CodeChina</a>
    <br/>
    <strong>Document：</strong> <a target="_blank" href='https://klein-doc.gitbook.io/zh_cn'>Gitbook</a>
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

# Introduce
![logo](logo.svg)

Klein is a distributed collection tool library based on Paxos, including distributed Cache, distributed message queue, distributed List, distributed Map, distributed Lock and so on. What's interesting about this is that you can keep data consistent across members without relying on any middleware.

You can understand that Klein is a distributed tool that depends on your project through Maven. We hope it will replace your existing Redis, messaging middleware, registry, configuration center, etc.

If you are interested in distributed message queue, distributed List, distributed Map, distributed Lock, we can share the existing design, you can participate in the coding work. 😆 😆 😆

**Look forward to your star⭐**

# Quick Start
### dependence klein
```xml
<dependency>
    <groupId>com.ofcoder.klein.core</groupId>
    <artifactId>klein-core</artifactId>
    <version>{last-version}</version>
</dependency>
```

### startup
```
Klein instance = Klein.startup();
instance.awaitInit();

KleinCache klein = KleinFactory.getInstance().createCache("klein");
klein.put("hello", "klein");

KleinCache lock = KleinFactory.getInstance().createLock("klein");
cache.acquire(1, TimeUnit.SECONDS);
```
### set property
For all configurable parameters, see: `com.ofcoder.klein.KleinProp`

You can set it through `System#setProperty` or get the `KleinProp` object
```
System.setProperty("klein.id", "2")

// or

KleinProp prop = KleinProp.loadIfPresent();
```

# Jepsen Test

[Run on Gitpod](https://gitpod.io/#/github.com/shihuili1218/klein)

