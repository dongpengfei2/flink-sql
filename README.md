# Flink SQL Demo

本项目主要基于flink 1.13.1开发业务demo。


### 问题：

- 采用Protocol buffer序列化kafka消息，用来代替json处理行为数据松散的数据结构。
- flink sql处理kafka数据时能否不定义字段，不用针对ddl对字段类型进行检查，直接通过推断获取表的schema。
- Kafka Schema Registry管理kafka元数据信息，在这里定义完之后别的地方直接用，做到一劳永逸。