package com.sht.flink.cases;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.util.Collector;

public class FlinkTimerTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.addSource(new CustomSource())
                .keyBy(s->s)
                .process(new KeyedProcessFunction<Integer, Integer, Integer>() {
                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Integer> out) throws Exception {
                        out.collect(123);
                    }

                    @Override
                    public void processElement(Integer value, Context ctx, Collector<Integer> out) throws Exception {
                        long time = System.currentTimeMillis();
                        ctx.timerService().registerProcessingTimeTimer(time - time %10000+10000);
                    }
                }).addSink(new SinkFunction<Integer>() {
            @Override
            public void invoke(Integer value, Context context) throws Exception {
                TestUtil.onlySleep();
            }
        });

        env.execute();
    }
}
