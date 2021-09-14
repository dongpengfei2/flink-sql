package com.sht.flink.cases;

import com.alibaba.fastjson.JSONObject;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkJSONTest {
    public static void main(String[] args) throws Exception {
        //使用本地模式并开启WebUI
        Configuration conf = new Configuration();
        conf.setString(RestOptions.BIND_PORT,"8081");
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);

        env.addSource(new JSONSource())
                .map(JSONObject::parseObject)
                .map(s->s)
                .map(s->s)
                .map(s->s)
                .map(s->s)
                .map(s->s)
                .print();

        env.addSource(new JSONSource())
                .map(s->{JSONObject.parseObject(s);return s;})
                .map(s->s)
                .map(s->s)
                .map(s->s)
                .map(s->s)
                .map(s->s)
                .print();

        env.execute();
    }
}
