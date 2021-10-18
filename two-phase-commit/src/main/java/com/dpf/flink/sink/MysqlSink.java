package com.dpf.flink.sink;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.base.VoidSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

public class MysqlSink extends TwoPhaseCommitSinkFunction<Tuple2<String,Integer>, Connection,Void> {

    private static final Logger log = LoggerFactory.getLogger(MysqlSink.class);

    public MysqlSink() {
        super(new KryoSerializer<>(Connection.class,new ExecutionConfig()), VoidSerializer.INSTANCE);
    }

    @Override
    protected void invoke(Connection transaction, Tuple2<String, Integer> value, Context context) throws Exception {

    }

    @Override
    protected Connection beginTransaction() throws Exception {
        return null;
    }

    @Override
    protected void preCommit(Connection transaction) throws Exception {

    }

    @Override
    protected void commit(Connection transaction) {

    }

    @Override
    protected void abort(Connection transaction) {

    }
}
