package com.sht.flink.cases;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkProcessTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.addSource(new CustomSource())
                .map(s->{TestUtil.fib1(35);return 1;})
                .map(s->{TestUtil.fib1(35);return 1;})
                .map(s->{TestUtil.fib1(35);return 1;})
                .map(s->{TestUtil.fib1(35);return 1;})
                .map(s->{TestUtil.fib1(35);return 1;})
                .map(s->{TestUtil.fib1(35);return 1;})
                .print();

        env.addSource(new CustomSource())
                .map(s->{TestUtil.fib2(35);return 2;})
                .map(s->{TestUtil.fib2(35);return 2;})
                .map(s->{TestUtil.fib2(35);return 2;})
                .map(s->{TestUtil.fib2(35);return 2;})
                .map(s->{TestUtil.fib2(35);return 2;})
                .map(s->{TestUtil.fib2(35);return 2;})
                .print();

        env.execute();
    }
}
