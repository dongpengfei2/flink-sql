package com.sht.flink

import com.sht.flink.udf.SplitQueryParamsAsMap
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment

object CsvFormatTest {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val tableEnvironment = StreamTableEnvironment.create(env)

    // 注册函数
    tableEnvironment.createTemporarySystemFunction("SplitQueryParamsAsMap", classOf[SplitQueryParamsAsMap])

    // access flink configuration// access flink configuration
    val configuration = tableEnvironment.getConfig.getConfiguration
    // set low-level key-value options
    configuration.setString("table.dynamic-table-options.enabled", "true")

    tableEnvironment.executeSql("" +
      "CREATE TABLE kafka_analytics_access_log ( " +
      "  remote_addr STRING, " +
      "  host STRING, " +
      "  request_time STRING, " +
      "  time_local STRING, " +
      "  msec STRING, " +
      "  request STRING, " +
      "  status STRING, " +
      "  body_bytes_sent STRING, " +
      "  http_referer STRING, " +
      "  http_cookie STRING, " +
      "  http_user_agent STRING, " +
      "  http_x_forwarded_for STRING, " +
      "  upstream_addr STRING, " +
      "  request_length STRING, " +
      "  query_string STRING, " +
      "  procTime AS PROCTIME() " +
      ") WITH ( " +
      "  'connector' = 'kafka', " +
      "  'topic' = 'rtdw_ods_analytics_access_log_app', " +
      "  'properties.bootstrap.servers' = '127.0.0.1:9092', " +
      "  'properties.enable.auto.commit' = 'false', " +
      "  'properties.session.timeout.ms' = '90000', " +
      "  'properties.request.timeout.ms' = '325000', " +
      "  'format' = 'csv', " +
      "  'csv.field-delimiter' = '|', " +
      "  'csv.ignore-parse-errors' = 'true' " +
      ") " +
      "")

    tableEnvironment.executeSql("" +
      "SELECT " +
      "    ts, " +
      "    FROM_UNIXTIME(ts / 1000) AS tss, " +
      "    SUBSTR(FROM_UNIXTIME(ts / 1000), 0, 10) AS tssDay, " +
      "    CAST(COALESCE(mp['userid'], '-1') AS BIGINT) AS userId, " +
      "    COALESCE(mp['eventType'], '') AS eventType, " +
      "    COALESCE(mp['fromType'], '') AS fromType, " +
      "    COALESCE(mp['columnType'], '') AS columnType, " +
      "    CAST(COALESCE(mp['site_id'], '-1') AS BIGINT) AS siteId, " +
      "    CAST(COALESCE(mp['grouponid'], '-1') AS BIGINT) AS grouponId, " +
      "    CAST(COALESCE(mp['partner_id'], '-1') AS BIGINT) AS partnerId, " +
      "    CAST(COALESCE(mp['categoryid'], '-1') AS BIGINT) AS categoryId, " +
      "    CAST(COALESCE(mp['categorySec_id'], '-1') AS BIGINT) AS secCategoryId, " +
      "    CAST(COALESCE(mp['merchandiseid'], '-1') AS BIGINT) AS merchandiseId, " +
      "    CAST(COALESCE(mp['share_userid'], '-1') AS BIGINT) AS shareUserId, " +
      "    CAST(COALESCE(mp['activeid'], '-1') AS BIGINT) AS activeId, " +
      "    CAST(COALESCE(mp['point_index'], '-1') AS BIGINT) AS pointIndex, " +
      "    CAST(COALESCE(mp['kingkong_id'], '-1') AS BIGINT) AS kingkongId, " +
      "    CAST(COALESCE(mp['flashkilltabid'], '-1') AS BIGINT) AS flashKillTabId, " +
      "    CAST(COALESCE(mp['live_id'], '-1') AS BIGINT) AS liveId, " +
      "    CAST(COALESCE(mp['orderid'], '-1') AS BIGINT) AS orderId, " +
      "    CAST(COALESCE(mp['lat'], '-1.0') AS DOUBLE) AS latitude, " +
      "    CAST(COALESCE(mp['lon'], '-1.0') AS DOUBLE) AS longitude, " +
      "    COALESCE(mp['couponid'], '') AS couponId, " +
      "    COALESCE(mp['searchTag'], '') AS searchTag, " +
      "    COALESCE(mp['to_areaname'], '') AS areaName, " +
      "    procTime " +
      "    FROM ( " +
      "    SELECT  " +
      "        CAST(REPLACE(msec, '.', '') AS BIGINT) AS ts,  " +
      "        SplitQueryParamsAsMap(REPLACE(query_string, '%', '')) AS mp, " +
      "        procTime " +
      "    FROM kafka_analytics_access_log /*+ OPTIONS('scan.startup.mode'='earliest-offset') */ " +
      "    WHERE CHAR_LENGTH(query_string) > 1 " +
      ") t " +
      "").print()
  }
}
