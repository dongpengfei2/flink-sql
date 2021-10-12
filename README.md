# Flink SQL Demo

本项目主要基于flink 1.13.1开发业务demo。


### 问题：

- 采用Protocol buffer序列化kafka消息，用来代替json处理行为数据松散的数据结构。
- Kafka Schema Registry管理kafka元数据信息，在这里定义完之后别的地方直接用，做到一劳永逸。



### 调优： 
1. 两阶段聚合 / 三阶段聚合 / MiniBatch  
    1.1. 两阶段聚合主要解决数据倾斜情况下聚合的问题，由于某些分区的数据量比较大，聚合效率会比较慢，而且会出现热点问题。通过两阶段聚合可打散数据进行初步的聚合，
       二次聚合时数据量会明显减少。解决方法是在第一阶段keyby的时候采用（key + 随机数）进行keyby，第二阶段的时候再用key进行keyby二次聚合。
       注意flink为了保证分区的确定性，KeySelector中是不能使用Random生成随机数的，可以采用取message的hash值的方式获取，或者也可以对要分组的字段乘以一个大数加个随机小数进行打散，
       恢复时除以一个大数进行还原，小数部分自动舍弃。  
    1.2. 三阶段聚合主要处理大key的问题，比如进行distinct求值的时候，如果统计当天的数据，那必须在hastset中存放所有的值，如果又由于数据倾斜问题导致某个hashset很多，状态序列化就会很慢，
       这时候我们可以首先采用低纬度进行聚合，最后在高纬度进行汇总的方式。  
    1.3. 采用AggregateFunction进行聚合的流程是，首先从状态读取累加器，然后对累加器进行操作，最后写回状态后端，每条数据都要进行一遍这样的操作
       生产中如果数据倾斜回使这个问题恶化，容易导致job发生反压。MiniBatch 聚合的核心思想是将一组输入的数据缓存在聚合算子内部的缓冲区中。当输入的数据被触发处理时，每个 key 只需一个操作即可访问状态。
       这样可以大大减少状态开销并获得更好的吞吐量。需要知道的是Window TVF默认是开启了两阶段聚合和MiniBatch的。
   
2. ETL过程中提高缓存命中率  
    2.1. 对事实表【fact】采用datastream API进行补维度时，为了提高缓存效率，同时保证事实数据严格有序，可以约定打入Kafka的数据根据特定维度分区。  
    2.2. 采用flink sql做ETL时，需要关联多个维度表【dim】，为了提到LookupJoin Operator中缓存的利用率，可在StreamPhysicalLookupJoin以后自动添加一个hash-by-key Operator，
       当然该方法也会带来额外的问题，就是如果数据倾斜可能处理效率会锐减，还有如果参与分组的key如果很少，有可能很多算子上都没有数据。
       这种场景下有同事也建议加大task manager的内存，增加slot的数量，采用本地jvm缓存的方式来增加维表数据的缓存利用率。

3. 零点追数。可以通过reset-offsets kafka topic 的时候指定to-datetime时间到UTC的零点来解决，该方法主要在实时大屏中使用，解决当天数据计算错误时追数问题。
  
4. UTF方法无法重用问题。问题发生在自定义UDF解析行为日志，返回map，外层根据key获取所有字段，但是每次根据key获取的时候都会调用一遍方法，中间结果没有缓存，导致执行效率下降。
  这块目前的解决方法是采用guava cache缓存计算结果，减少调用，后期看社区什么时候会修复这个bug，还有一种方法是采用udtf，把行数据拆分成多列，这块由于我们采用zeppelin作为运行平台，zeppelin不支持udtf，我们项目组长修改了相关源码。

5. RocksDB TTL失效问题。偶然发现线上binlog抽取任务的去重状态不会减少，经过一天一夜排查确认是使用RocksDB状态后端时table.exec.state.ttl参数不生效导致的。
  这个bug比较诡异，可能涉及RocksDB的细节，这个咱们不可能了解，先提ticket给社区了。为了暂时解决问题，先把flink-conf.yaml里的默认状态后端换成了filesystem，重启zeppelin note。
  
6. 对象池化和复用  
    6.1. 如果是结构化的数据，可以采用CSV格式批量写入。  
    6.2. 如果非结构化数据，如：json，可以JSON的格式写入，此时一批数据需要拼成一个大的字符串写入，可以采用StringBuilder池化技术加少GC。  
    6.3. 开启对象复用，这样Operator Chain之间数据传递就不用进行深拷贝了。详见源码#CopyingChainingOutput#pushToOperator#serializer.copy#RowDataSerializer#copy

7. 集群升级时save point数据问题。如果对hadoop进行了升级，集群名字发生了变化，首先需要把save point数据拷贝到新的集群，然后修改save point目录中metadata文件中集群的名称，能使其定位到具体的数据。

8. 使用LocalKeyBy解决keyBy后聚合操作存在的数据倾斜问题。思想是在上游算子数据发送之前，首先在上游算子的本地对数据进行聚合后再发送到下游，使下游接收到的数据量大大减少，从而使得 keyBy 之后 的聚合操作不再是任务的瓶颈。  
   8.1 开一个步长为1秒的全局窗口，窗口函数里面根据关键字手动进行聚合输出，然后再keyBy进行实际指标的运算。这个跟两阶段聚合的区别是第一次聚合是在keyBy之前手动聚合的。
   8.2 在keyby之前，使用flatMap积攒一批数据之后触发手动聚合。这种需要使用状态缓存一批数据。定时或者定量聚合输出。

### 源码加强

1. 修改canal-json format源码  
   1.1 添加元数据字段binlogType，同于筛选数据。  
   1.2  Option中添加`'canal-json.decode.stream-as-append-only' = 'true'`参数可以使其丢弃Changelog语义，插入和更新全部视为INSERT，可按需使用。主要解决UpsetStream无法Join维表，如果再做一次转运生产ods层数据会显的冗余。
2. 自定义Mysql Catalog。这块由于维表数据存在tidb中，所以通过like语法很容易获取到元数据中的字段信息，方便建表  
3. 修改bahir开源项目中redis connector，让其支持flink sql  
4. TVF 窗口添加参数，实现增量输出，减小访问redis的压力

### 注意事项
1. 创建Kafka流表时，不要在建表语句的WITH区段内指定offset起始位点和消费组ID，而应当在任务中实际消费时使用SQL hints指定。消费组ID一般应与对应的`jobName`相同。
2. TVF中无法使用hints语法，目前只能写一段胶水代码，采用like语法针对每个query创建一个表，然后表中指定group id。
3. 采用CAST做类型转换时，只能用于事实表，对维表进行转换会报错。
4. TVF中不能使用WHERE进行过滤，可以采用WITH子句包裹一层进行过滤。

### Flink SQL 常用配置项
作业配置项  
```
//作业名称
yarn.application.name FSQL_RealtimeWarReportDashboard
//jobmanager内存配置
jobmanager.memory.process.size  1536m
//taskmanager内存配置，一个slot建议配置3g
taskmanager.memory.process.size  5120m
//slot数
taskmanager.numberOfTaskSlots  2
//扩展包
flink.execution.packages  org.apache.flink:flink-sql-connector-kafka_2.11:1.13.0,org.apache.flink:flink-json:1.13.0
//udf jar路径
flink.udf.jars  /var/flink-ext-jars/flink-sql-udf-1.1-production.jar
//时间语义
pipeline.time-characteristic  ProcessingTime
//checkout point时常
execution.checkpointing.interval  200s
//checkout point最小时常
execution.checkpointing.min-pause  20s
//checkout point超时时间
execution.checkpointing.timeout  180s
```
SQL优化项
```
//支持SQL标准中hints的语法
SET table.dynamic-table-options.enabled=true;
//TTL时间
SET table.exec.state.ttl=30h;
//MiniBatch配置
SET table.exec.mini-batch.enabled=true;
SET table.exec.mini-batch.allow-latency=1s;
//开启两阶段聚合
SET table.optimizer.agg-phase-strategy=TWO_PHASE;
//Distinct解热点优化
SET table.optimizer.distinct-agg.split.enabled=true;
SET table.optimizer.distinct-agg.split.bucket-num=256;
```
若要观察codegen出来的代码，可在log4j.properties文件中加上：
```
logger.codegen.name = org.apache.flink.table.runtime.generated
logger.codegen.level = DEBUG
```
