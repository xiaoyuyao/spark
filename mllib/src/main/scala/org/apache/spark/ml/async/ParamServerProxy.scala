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
import org.apache.spark.util.RpcUtils

import scala.reflect.ClassTag

/**
 * Created by zzhang on 6/29/15.
 */
class ParamServerProxy[U: ClassTag](
     val env: SparkEnv, val name: String
   ) {
  /** Remote RpcEndpointRef for the ReceiverTracker */
  lazy val pServer = RpcUtils.makeDriverRef(name, env.conf, env.rpcEnv)

  def updateAndFetch(modelId: Int, params: U): Propagate[U] = {
    pServer.askWithRetry[Propagate[U]](UpdateAndFetch(modelId, params))
  }

  def update(modelId: Int, params: U) = {
    pServer.ask(Update(modelId, params))
  }

  def fetch(modelId: Int): Propagate[U] = {
    pServer.askWithRetry[Propagate[U]](Fetch(modelId))
  }

  def deregister(modelId: Int) = {
    pServer.ask(DeregisterModel(modelId, "terminating", "NA"))
  }

  def register(reg: RegisterModel) = {
    pServer.ask(reg)
  }
}
