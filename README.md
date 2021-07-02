# Flink SQL Demo

本项目主要基于flink 1.13.1开发业务demo。

### Features

* 由于Zeppelin内不能使用case class，所以采用元组+Schema的方式定义表结构，后来又出现采用Pattern类split的时候出现can not serializable，该用string.split方法后可以了