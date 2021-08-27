import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, ExplainDetail}
import org.apache.flink.table.catalog.hive.HiveCatalog

object DiveIntoBlinkExp {
  // logger.codegen.name = org.apache.flink.table.runtime.generated
  // logger.codegen.level = DEBUG
  def main(args: Array[String]): Unit = {
    val streamEnv = StreamExecutionEnvironment.getExecutionEnvironment
    streamEnv.getConfig.setAutoWatermarkInterval(0)
    streamEnv.setParallelism(4)

    val tableEnv = StreamTableEnvironment.create(streamEnv, EnvironmentSettings.newInstance().build())
    val tableEnvConfig = tableEnv.getConfig.getConfiguration
    tableEnvConfig.setBoolean("table.dynamic-table-options.enabled", true)
    tableEnvConfig.setBoolean("table.exec.lookup.distribute-by-key", true)

    val catalogName = "hive"
    val catalog = new HiveCatalog(
      catalogName,
      "default",
      "/Users/david.dong/Documents/flink-hive-conf/TEST",
      "2.1.1")
    tableEnv.registerCatalog(catalogName, catalog)
    tableEnv.useCatalog(catalogName)

    tableEnv.executeSql(/* language=SQL */
      s"""
         |CREATE TABLE IF NOT EXISTS expdb.kafka_analytics_access_log_app (
         |  ts BIGINT,
         |  userId BIGINT,
         |  eventType STRING,
         |  columnType STRING,
         |  fromType STRING,
         |  grouponId BIGINT,
         |  siteId BIGINT,
         |  merchandiseId BIGINT,
         |  procTime AS PROCTIME()
         |) WITH (
         |  'connector' = 'kafka',
         |  'topic' = 'rtdw_dwd_analytics_access_log_app',
         |  'properties.bootstrap.servers' = '10.2.8.162:9092,10.2.8.163:9092',
         |  'format' = 'json',
         |  'json.fail-on-missing-field' = 'false',
         |  'json.ignore-parse-errors' = 'true')
         |""".stripMargin)

    tableEnv.executeSql(/* language=SQL */
      s"""
         |CREATE TABLE IF NOT EXISTS expdb.print_joined_result (
         |  tss STRING,
         |  userId BIGINT,
         |  eventType STRING,
         |  siteId BIGINT,
         |  siteName STRING
         |) WITH ('connector' = 'print')
         |""".stripMargin)

    val dimJoinSql = /* language=SQL */
      s"""
         |INSERT INTO expdb.print_joined_result
         |SELECT FROM_UNIXTIME(a.ts / 1000, 'yyyy-MM-dd HH:mm:ss') AS tss, a.userId, a.eventType, a.siteId, b.site_name AS siteName
         |FROM expdb.kafka_analytics_access_log_app /*+ OPTIONS('scan.startup.mode'='latest-offset','properties.group.id'='DiveIntoBlinkExp') */ a
         |LEFT JOIN rtdw_dim.mysql_site_war_zone_mapping_relation FOR SYSTEM_TIME AS OF a.procTime AS b ON CAST(a.siteId AS INT) = b.site_id
         |WHERE a.userId > 3 + 4
         |""".stripMargin

    val extendedPlan = tableEnv.explainSql(dimJoinSql, ExplainDetail.JSON_EXECUTION_PLAN)
    println(extendedPlan)

    tableEnv.executeSql(dimJoinSql)

    // val jsonPlan = tableEnv.asInstanceOf[TableEnvironmentInternal].getJsonPlan(dimJoinSql)
    // println(jsonPlan)

    // tableEnv.asInstanceOf[TableEnvironmentInternal].executeJsonPlan(jsonPlan)
  }
}
