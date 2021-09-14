package com.sht.flink.cases;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkOperatorTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.addSource(new CustomSource())
                .map(s->{TestUtil.onlySleep();return 3;})
                .map(s->{TestUtil.onlySleep();return 3;})
                .map(s->{TestUtil.onlySleep();return 3;})
                .map(s->{TestUtil.onlySleep();return 3;})
                .map(s->{TestUtil.onlySleep();return 3;})
                .map(s->{TestUtil.onlySleep();return 3;})
                .print();

        env.addSource(new CustomSource())
                .map(s->{TestUtil.onlySleep();return 4;}).disableChaining()
                .map(s->{TestUtil.onlySleep();return 4;})
                .map(s->{TestUtil.onlySleep();return 4;}).disableChaining()
                .map(s->{TestUtil.onlySleep();return 4;})
                .map(s->{TestUtil.onlySleep();return 4;}).disableChaining()
                .map(s->{TestUtil.onlySleep();return 4;})
                .print();

        env.execute();
    }
}
