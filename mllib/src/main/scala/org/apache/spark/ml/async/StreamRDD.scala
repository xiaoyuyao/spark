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

import org.apache.spark.{Partition, TaskContext}
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
 * This is the RDD created from the other to feed into asynchronous training.
 * It behaves like a infinite stream, and it is down stream processing logic
 * to decide when the stream should be terminated. Note that some operation,
 * such as count, collect, etc., cannot be applied ot StreamRDD. Otherwise,
 * the task will never terminate
 * @param prev
 * @param preservesPartitioning
 * @param evidence$1
 * @tparam T
 */
class StreamRDD[T: ClassTag](
    prev: RDD[T],
    preservesPartitioning: Boolean = false)
  extends RDD[T](prev) {

  override val partitioner = if (preservesPartitioning) firstParent[T].partitioner else None

  override def getPartitions: Array[Partition] = firstParent[T].partitions

  override def compute(split: Partition, context: TaskContext): Iterator[T] = {
    val p = firstParent[T].iterator(split, context)
    if (p.isEmpty) {
      Iterator.empty
    } else {
      var current = p
      new Iterator[T] {
        var current = firstParent[T].iterator(split, context)

        override def hasNext: Boolean = true

        // Todo: add sampling method to facilitate the asynchronous training, instead of iterating
        // through each record.
        override def next(): T = {
          if (!current.hasNext) {
            current = firstParent[T].iterator(split, context)
          }
          current.next()
        }
      }
    }
  }
}

object StreamRDD {
  def toStreamRDD[T: ClassTag](rdd: RDD[T]) = {
    rdd.persist()
    new StreamRDD(rdd, true)
  }
}
