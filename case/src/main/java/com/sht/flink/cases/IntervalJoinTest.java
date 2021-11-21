package com.sht.flink.cases;

import lombok.Data;
import lombok.AllArgsConstructor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class IntervalJoinTest {

    @Data
    @AllArgsConstructor
    public static class Click {
        long tid;
        long offerId;
        long advertiserId;
        int payout;
        int gid;
        int acid;
        long timestamp;
    }

    @Data
    @AllArgsConstructor
    public static class Conversion {
        long tid;
        long conversionId;
        long timestamp;
    }

    public static void main(String[] args) throws Exception {
        //使用本地模式并开启WebUI
        Configuration conf = new Configuration();
        conf.setString(RestOptions.BIND_PORT,"8081");
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);
        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        final KeyedStream<Click, Long> clickSteam = env.fromElements(new Click(
            1, 1, 1, 1, 1, 1, 1637424000
        ), new Click(
            2, 2, 2, 2, 2, 2, 1637424000 + 10
        ), new Click(
            3, 3, 3, 3, 3, 3, 1637424000 + 20
        ), new Click(
            4, 4, 4, 4, 4, 4, 1637424000 + 30
        )).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<Click>(Time.minutes(3)) {
            @Override
            public long extractTimestamp(Click element) {
                return element.getTimestamp() * 1000;
            }
        }).keyBy(new KeySelector<Click, Long>() {
            @Override
            public Long getKey(Click click) throws Exception {
                return click.getTid();
            }
        });

        final KeyedStream<Conversion, Long> conversionStream = env.fromElements(new Conversion(
            1, 100, 1637424000 + 5
        ), new Conversion(
            3, 300, 1637424000 + 25
        )).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<Conversion>(Time.minutes(3)) {
            @Override
            public long extractTimestamp(Conversion element) {
                return element.getTimestamp() * 1000;
            }
        }).keyBy(new KeySelector<Conversion, Long>() {
            @Override
            public Long getKey(Conversion conversion) throws Exception {
                return conversion.getTid();
            }
        });

        //join
        conversionStream.intervalJoin(clickSteam)
            .between(Time.minutes(-10), Time.seconds(0)) //定义上下界为(-10,0)
            .process(new ProcessJoinFunction<Conversion, Click, String>() {
                @Override
                public void processElement(Conversion left, Click right, Context ctx, Collector<String> out) throws Exception {
                    out.collect(left.getConversionId() + " : " + right.getTid());
                }
            }).print();

        env.execute("IntervalJoinTest");
    }
}
