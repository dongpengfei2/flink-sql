# Flink SQL Demo

本项目主要基于flink 1.13.1开发业务demo。

### Features

- Zeppelin内不能使用case class，所以采用元组+Schema的方式定义表结构。
- 程序出现task can not serializable异常，逐一排除各个类后发现是Pattern类split引起的，改用String.split方法后可以了。
- 程序出现Exception in thread "main" java.lang.IllegalArgumentException: Hash collision on user-specified ID "opt". Most likely cause is a non-unique ID. Please check that all IDs specified via uid(String) are unique，该问题在程序中加入keyby之后解决了。后又出现Table sink 'hive.rtdw_dwd.kafka_analytics_access_log_taobao' doesn't support consuming update and delete changes which is produced by node Join，这明显是输出端不支持回撤流引起的，把sink kafka改成upsert kafka就行了。整个问题的原因是程序生成的临时表是changelog stream类型的，所有后续的操作都要满足该语义。
- 采用竖线切分字段时message.split("\\\\|"))，注意不能直接使用｜切分，要采用双斜杠正则匹配的方式，否则会出错，这个在很多切分场景都出现过，需格外注意。