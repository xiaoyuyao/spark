package org.apache.spark.examples

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by zzhang on 6/26/15.
 */


object OrcExample {

  case class AllDataTypes(
                           stringField: String,
                           intField: Int,
                           longField: Long,
                           floatField: Float,
                           doubleField: Double,
                           shortField: Short,
                           byteField: Byte,
                           booleanField: Boolean)

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName("SimpleOrcTest").setMaster("local")
    val sc = new SparkContext(conf)

    import org.apache.spark.sql.hive.orc._
    import org.apache.spark.sql._

    val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
    import sqlContext.implicits._

    val range = (0 to 255)
    val data: RDD[AllDataTypes] = sc.parallelize(range).map(x => AllDataTypes(s"$x", x, x.toLong,
      x.toFloat, x.toDouble, x.toShort, x.toByte, x % 2 == 0))
    data.toDF().write.format("orc").save("orcTest")
  }
}