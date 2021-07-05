package com.sht.flink

import java.util.Properties
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, createTypeInformation}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.joda.time.LocalDateTime
import java.net.URLDecoder

object AccessLog {

  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.put("bootstrap.servers", "127.0.0.1:9092")
    properties.put("auto.offset.reset", "latest")
    properties.put("enable.auto.commit", "false")
    properties.put("session.timeout.ms", "120000")
    properties.put("request.timeout.ms", "180000")
    properties.put("group.id", "AccessLog")
    val senv = StreamExecutionEnvironment.createLocalEnvironment()
    val stenv = StreamTableEnvironment.create(senv)
    val kafkaConsumer = new FlinkKafkaConsumer[String]("rtdw_ods_analytics_access_log_app", new SimpleStringSchema(), properties)
    kafkaConsumer.setStartFromGroupOffsets()

    val accessLogSourceStream = senv.addSource(kafkaConsumer).setParallelism(12)
      .name("source_kafka_rtdw_ods_analytics_access_log_app").uid("source_kafka_rtdw_ods_analytics_access_log_app")

    val accessLogRecordStream = accessLogSourceStream
      .map((message: String) => message.replace("%", "").split("\\|"))
      .filter(fields => {fields.length >= 15 && !fields(0).startsWith("127.0.0.1") && fields(1) == "analytics.youhaodongxi.com" && !(fields(5) == "HEAD / HTTP/1.0") && !(fields(5) == "GET / HTTP/1.0") && !fields(5).startsWith("OPTIONS") && StringUtils.isNotEmpty(fields(14))})
      .map(fields => {
        val timestamp = fields(4).replace(".", "")
        val ts = NumberUtils.createLong(timestamp)
        val tss = new LocalDateTime(ts).toString("yyyy-MM-dd HH:mm:ss")
        val params = fields(14).split("&")
        val map = params.map(_.split("=")).filter(_.length==2).map(arr => (arr(0), URLDecoder.decode(arr(1).replaceAll("\\\\x", "%"), "UTF-8"))).toMap
        (ts, tss
          , if (map.contains("userid")) map("userid").toLong else 0l
          , if (map.contains("eventType")) map("eventType") else ""
          , if (map.contains("fromType")) map("fromType") else ""
          , if (map.contains("columnType")) map("columnType") else ""
          , if (map.contains("grouponid")) map("grouponid").toLong else 0l
          , if (map.contains("site_id")) map("site_id").toLong else 0l
          , if (map.contains("partner_id")) map("partner_id").toLong else 0l
          , if (map.contains("categorySec_id")) map("categorySec_id").toLong else 0l
          , if (map.contains("merchandiseId")) map("merchandiseId").toLong else 0l
          , if (map.contains("share_userid")) map("share_userid").toLong else 0l
          , if (map.contains("orderid")) map("orderid").toLong else 0l
          , if (map.contains("activeid")) map("activeid").toLong else 0l
          , if (map.contains("point_index")) map("point_index").toLong else 0l
          , if (map.contains("flashkilltabid")) map("flashkilltabid").toLong else 0l
          , if (map.contains("live_id")) map("live_id").toLong else 0l
          , if (map.contains("kingkong_id")) map("kingkong_id").toLong else 0l
          , if (map.contains("lat")) map("lat").toDouble else 0
          , if (map.contains("lon")) map("lon").toDouble else 0)
      })
      .name("filter_access_log_reqs").uid("filter_access_log_reqs")

    stenv.createTemporaryView("kafka_analytics_access_log_taobao", stenv.fromDataStream(accessLogRecordStream)
      .as("ts", "tss", "userId", "eventType", "fromType", "columnType", "grouponId", "siteId", "partnerId", "categoryId"
        , "merchandiseId", "shareUserId", "orderId", "activeId", "pointIndex", "flashKillTabId", "liveId", "kingkongId", "latitude", "longitude"))
    stenv.executeSql("" +
      "CREATE TABLE kafka_analytics_access_log_taobao ( " +
      "  ts BIGINT, " +
      "  tss STRING, " +
      "  userId BIGINT, " +
      "  eventType STRING, " +
      "  columnType STRING, " +
      "  fromType STRING, " +
      "  grouponId BIGINT, " +
      "  siteId BIGINT, " +
      "  siteName STRING, " +
      "  mainSiteId BIGINT, " +
      "  mainSiteName STRING, " +
      "  businessAreaId BIGINT, " +
      "  businessAreaName STRING, " +
      "  cityCircleId BIGINT, " +
      "  cityCircleName STRING, " +
      "  warCityCircleId BIGINT, " +
      "  warCityCircleName STRING, " +
      "  warDepId BIGINT, " +
      "  warDepName STRING, " +
      "  warZoneId BIGINT, " +
      "  warZoneName STRING, " +
      "  partnerId BIGINT, " +
      "  categoryId BIGINT, " +
      "  categoryName STRING, " +
      "  secCategoryName STRING, " +
      "  merchandiseId BIGINT, " +
      "  merchandiseName STRING, " +
      "  merchandiseAbbr STRING, " +
      "  shareUserId BIGINT, " +
      "  orderId BIGINT, " +
      "  activeId BIGINT, " +
      "  activeName STRING, " +
      "  pointIndex BIGINT, " +
      "  flashKillTabId BIGINT, " +
      "  liveId BIGINT, " +
      "  kingkongId BIGINT, " +
      "  kingkongName STRING, " +
      "  latitude DOUBLE, " +
      "  longitude DOUBLE, " +
      "  procTime AS PROCTIME(), " +
      "  PRIMARY KEY (siteId) NOT ENFORCED " +
      ") WITH ( " +
      "  'connector' = 'upsert-kafka', " +
      "  'topic' = 'rtdw_dwd_analytics_access_log_taobao', " +
      "  'properties.bootstrap.servers' = '127.0.0.1:9092', " +
      "  'properties.enable.auto.commit' = 'false', " +
      "  'properties.session.timeout.ms' = '90000', " +
      "  'properties.request.timeout.ms' = '325000', " +
      "  'key.format' = 'json', " +
      "  'value.format' = 'json', " +
      "  'value.json.fail-on-missing-field' = 'false', " +
      "  'value.json.ignore-parse-errors' = 'true', " +
      "  'value.json.encode.decimal-as-plain-number' = 'true', " +
      "  'sink.parallelism' = '4' " +
      ") " +
      "")
    stenv.executeSql("select * from kafka_analytics_access_log_taobao").print()
  }
}
