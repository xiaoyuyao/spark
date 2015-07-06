/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.ml.async

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.async._

class Example {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("StreamRDD")
    val sc = new SparkContext(sparkConf)
    val s = sc.parallelize(List(1,2))
    val v = StreamRDD.toStreamRDD(s)
    val c = v.mapPartitions{
      iter => {
        val count = 100
        var cur = 0
        new Iterator[Int] {
          override def hasNext = {
            cur += 1
            if (iter.hasNext && cur <= 5) true
            else false
          }
          override def next(): Int = { iter.next}
        }
      } }
    c.count
  }
}


