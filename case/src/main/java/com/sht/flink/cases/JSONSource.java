package com.sht.flink.cases;

import org.apache.flink.streaming.api.functions.source.SourceFunction;

public class JSONSource implements SourceFunction<String> {
    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        while (true){
            ctx.collect("{\n" +
                    "    \"groupon_id\": 199495,\n" +
                    "    \"main_site_id\": 10246,\n" +
                    "    \"merchandise_id\": 2486504,\n" +
                    "    \"category_id\": 1515,\n" +
                    "    \"category_title\": \"家庭清洁\",\n" +
                    "    \"merchandise_activity_type\": 0,\n" +
                    "    \"max_quantity\": 100,\n" +
                    "    \"limit_quantity\": 20,\n" +
                    "    \"groupon_sale_price_avg\": 2628,\n" +
                    "    \"merchtype_supplyprice_avg\": 2600,\n" +
                    "    \"deleted\": 0,\n" +
                    "    \"soldout\": 0,\n" +
                    "    \"is_operation\": 0,\n" +
                    "    \"is_supcon\": 0,\n" +
                    "    \"is_special\": 0,\n" +
                    "    \"is_direct_partner\": 0,\n" +
                    "    \"binlog_ts\": 1630059470350\n" +
                    "  }");
        }
    }

    @Override
    public void cancel() {

    }
}
