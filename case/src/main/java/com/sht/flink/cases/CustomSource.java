package com.sht.flink.cases;

import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

public class CustomSource extends RichParallelSourceFunction<Integer> {
    @Override
    public void run(SourceContext<Integer> ctx) throws Exception {
        int num = getRuntimeContext().getNumberOfParallelSubtasks();
        int index = getRuntimeContext().getIndexOfThisSubtask();
        while (true){
            index+=num;
            ctx.collect(index);
        }
    }

    @Override
    public void cancel() {

    }
}
