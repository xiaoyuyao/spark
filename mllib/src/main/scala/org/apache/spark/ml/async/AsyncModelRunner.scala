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

import org.apache.spark.SparkEnv
import org.apache.spark.sql.DataFrame

import scala.reflect.ClassTag

class AsyncModelRunner[T: ClassTag, U: ClassTag](model: AsyncModel[T, U]) {

  def fit(dataset: DataFrame): Seq[(Int, Int)] = {
    val rdd = model.extract(dataset)
    model.partitionNum = rdd.partitions.size
    val streamRDD = StreamRDD.toStreamRDD(rdd)
    val resultRDD = streamRDD.mapPartitionsWithIndex(train)
    resultRDD.reduce{ (r1, r2) =>
      r1 ++ r2
    }
  }

  var pServer: ParamServerProxy[U] = _

  def train: (Int, Iterator[T]) => Iterator[Seq[(Int, Int)]] = {
    (modelId, iter) => {
      pServer = new ParamServerProxy[U](SparkEnv.get, model.name)
      // Start the AsyncModelEndPoint, and register to the parameter server.
      val e = AsyncModelEndPoint.start(model, modelId, pServer.env, pServer)
      var cur = 0
      var propagated = pServer.fetch(modelId)
      model.setParams(propagated.params)
      while (iter.hasNext && cur < model.tStep && !model.stopped.get()) {
        model.train(iter.next())
        cur += 1
        if (cur % model.uStep == 0 && cur % model.fStep == 0) {
          propagated = pServer.updateAndFetch(modelId, model.retrieveParams)
          model.setParams(propagated.params)
        } else if (cur % model.uStep == 0) {
          pServer.update(modelId, model.retrieveParams)
        } else if (cur % model.fStep == 0) {
          propagated = pServer.fetch(modelId)
          model.updateParams(propagated.params)
        }
      }
      // Register myself from parameter server, which will call StopAsyncEndPoint
      // to really steop the AsyncModelEndPoint. We don't stop the endpointhere to avoid
      // some contention.
      pServer.deregister(modelId)
      Iterator(Seq((modelId, cur)))
    }
  }
}
