# Flink SQL Demo

本项目主要基于flink 1.13.1开发业务demo。


### 问题：

- 采用Protocol buffer序列化kafka消息，用来代替json处理行为数据松散的数据结构。
- Kafka Schema Registry管理kafka元数据信息，在这里定义完之后别的地方直接用，做到一劳永逸。



### 优化：

- 两阶段聚合 / 三阶段聚合 / MiniBatch
1. 两阶段聚合主要解决数据倾斜情况下聚合的问题，由于某些分区的数据量比较大，聚合效率会比较慢，而且会出现热点问题。通过两阶段聚合可打散数据进行初步的聚合，
   二次聚合时数据量会明显减少。解决方法是在第一阶段keyby的时候采用（key + 随机数）进行keyby，第二阶段的时候再用key进行keyby二次聚合。
   注意flink为了保证分区的确定性，KeySelector中是不能使用Random生成随机数的，可以采用取message的hash值的方式获取，或者也可以对要分组的字段乘以一个大数加个随机小数进行打散，
   恢复时除以一个大数进行还原，小数部分自动舍弃。
2. 三阶段聚合主要处理大key的问题，比如进行distinct求值的时候，如果统计当天的数据，那必须在hastset中存放所有的值，如果又由于数据倾斜问题导致某个hashset很多，状态序列化就会很慢，
   这时候我们可以首先采用低纬度进行聚合，最后在高纬度进行汇总的方式。
3. 采用AggregateFunction进行聚合的流程是，首先从状态读取累加器，然后对累加器进行操作，最后写回状态后端，每条数据都要进行一遍这样的操作
   生产中如果数据倾斜回使这个问题恶化，容易导致job发生反压。MiniBatch 聚合的核心思想是将一组输入的数据缓存在聚合算子内部的缓冲区中。当输入的数据被触发处理时，每个 key 只需一个操作即可访问状态。
   这样可以大大减少状态开销并获得更好的吞吐量。需要知道的是Window TVF默认是开启了两阶段聚合和MiniBatch的。
   
- ETL过程中提高缓存命中率
1. 对事实表【fact】采用datastream API进行补维度时，为了提高缓存效率，同时保证事实数据严格有序，可以约定打入Kafka的数据根据特定维度分区
2. 采用flink sql做ETL时，需要关联多个维度表【dim】，为了提到LookupJoin Operator中缓存的利用率，可在StreamPhysicalLookupJoin以后自动添加一个hash-by-key Operator，
   当然该方法也会带来额外的问题，就是如果数据倾斜可能处理效率会锐减，还有如果参与分组的key如果很少，有可能很多算子上都没有数据。
   这种场景下有同事也建议加大task manager的内存，增加slot的数量，采用本地jvm缓存的方式来增加维表数据的缓存利用率。
   
- 修改canal-json format源码
1. 添加元数据字段binlogType，同于筛选数据。
2. Option中添加`'canal-json.decode.stream-as-append-only' = 'true'`参数可以使其丢弃Changelog语义，插入和更新全部视为INSERT，可按需使用。主要解决UpsetStream无法Join维表，如果再做一次转运生产ods层数据会显的冗余。

- 零点追数。可以通过reset-offsets kafka topic 的时候指定to-datetime时间到UTC的零点来解决，该方法主要在实时大屏中使用，解决当天数据计算错误时追数问题。
  
- UTF方法无法重用问题。问题发生在自定义UDF解析行为日志，返回map，外层根据key获取所有字段，但是每次根据key获取的时候都会调用一遍方法，中间结果没有缓存，导致执行效率下降。
  这块目前的解决方法是采用guava cache缓存计算结果，减少调用，后期看社区什么时候会修复这个bug，还有一种方法是采用udtf，把行数据拆分成多列，这块由于我们采用zeppelin作为运行平台，zeppelin不支持udtf，我们项目组长修改了相关源码。

- RocksDB TTL失效问题。偶然发现线上binlog抽取任务的去重状态不会减少，经过一天一夜排查确认是使用RocksDB状态后端时table.exec.state.ttl参数不生效导致的。
  这个bug比较诡异，可能涉及RocksDB的细节，这个咱们不可能了解，先提ticket给社区了。为了暂时解决问题，先把flink-conf.yaml里的默认状态后端换成了filesystem，重启zeppelin note。
  
- 自定义Mysql Catalog。这块由于维表数据存在tidb中，所以通过like语法很容易获取到元数据中的字段信息，方便建表。

- 修改bahir开源项目中redis connector，让其支持flink sql。

- 对象池化和复用
1. 如果是结构化的数据，可以采用CSV格式批量写入。
2. 如果非结构化数据，如：json，可以JSON的格式写入，此时一批数据需要拼成一个大的字符串写入，可以采用StringBuilder池化技术加少GC。
3. 开启对象复用，这样Operator Chain之间数据传递就不用进行深拷贝了。详见源码#CopyingChainingOutput#pushToOperator#serializer.copy#RowDataSerializer#copy


### 分享
1. Flink 网络参数里的buffer，一个buffer实际上还是一个MemorySegment，不管是Spark、Flink还是其他做了主动内存管理的框架，本质上都是在堆外模拟操作系统的分页内存管理，MemorySegment就相当于一个Page。 
2. 对象构成及overhead解释：一个对象有header（对象头）、payment（对象数据）、footer（对象尾）、checkstyle（校验模块）等组成，其实只有payment真正存储数据，其他信息都属于overhead。
3. Flink POJO类在进行传输时，会序列化为MemorySegment进行传输，一个MemorySegment相当于一个分页内存，采用的是堆外内存。
4. 对CPU和内存消耗比较小的程序，比如操作外存多的。可以拆开Operator Chain进行运行，原理就是异步处理，这样能够提高并行度，如果采用async sink进行输出。
5. Flink某些算子需要显示的注册序列化类型，比如Hypeloglog，需要自定义序列化器进行注册。FlatMap算子需要在后面采用return的方式显示的指定序列化类型，flink在进行类型推断的时候会用。其他算子可以自动进行类型推断，FlatMap由于返回的是Collecter<T>，这种范型在虚拟机编译时会进行类型擦除，比如List<String>和List<Integer>对JVM来说是同种类型。
6. Timer使用注意事项。Timer底层存储采用的是最小堆（获取最小值的时间复杂度为O1），如果同一时间注册太多，或者删除太多Timer，就会导致频繁操作最小堆（因为要排序）线程就会夯在onProcessingTime方法中。在实际场景，我们用来统计APP内各个商品的相关数据，压力还好。如果key规模过大引起Timer过期风暴的话，性能确实不行，只能加大并行度，让KeyGroup尽量分散到各个sub-task了。


### 注意事项
1. 创建Kafka流表时，不要在建表语句的WITH区段内指定offset起始位点和消费组ID，而应当在任务中实际消费时使用SQL hints指定。消费组ID一般应与对应的`jobName`相同。TVF中无法使用hints语法，目前只能采用like语法针对每个query创建一个表，然后表中指定group id。
2. 采用CAST做类型转换时，只能用于事实表，对维表进行转换会报错。
3. TVF中不能使用WHERE进行过滤，可以采用WITH子句包裹一层进行过滤。