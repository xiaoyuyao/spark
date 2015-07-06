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

import java.util.concurrent.atomic.AtomicBoolean

import com.google.common.base.Throwables
import org.apache.spark.{Logging, SparkEnv}
import org.apache.spark.rpc.{RpcEndpointRef, RpcEnv, ThreadSafeRpcEndpoint}
import org.apache.spark.util.Utils


/** Messages sent to the endpoint. */
private[async] sealed trait ReceiverMessage extends Serializable
private[async] case class StopAsyncEndPoint(modelId: Int) extends ReceiverMessage


class AsyncModelEndPoint[T, U](model: AsyncModel[T, U], modelId: Int, env: SparkEnv, proxy: ParamServerProxy[U])
  extends Logging {
  private var endpoint: RpcEndpointRef = _

  def start = {
    endpoint = {
      env.rpcEnv.setupEndpoint(
        s"AsyncModel-${model.name}-$modelId-${System.currentTimeMillis()}", new ThreadSafeRpcEndpoint {
          override val rpcEnv: RpcEnv = env.rpcEnv

          override def receive: PartialFunction[Any, Unit] = {
            case StopAsyncEndPoint(mid) =>
              if (modelId == mid) {
                if (model.stopped.getAndSet(true)) {
                  env.rpcEnv.stop(endpoint)
                }
              } else {
                logError(s"received modelId $mid does not match the local $modelId")
              }
            case _ =>
          }
        })
    }
    proxy.register(
      RegisterModel(modelId, model.getClass.getSimpleName, Utils.localHostName(), endpoint))
  }
}

object AsyncModelEndPoint {
  def start[T, U](
                   model: AsyncModel[T, U],
                   modelId: Int,
                   env: SparkEnv,
                   proxy: ParamServerProxy[U]
                   ): AsyncModelEndPoint[T, U] = {
    val e = new AsyncModelEndPoint(model, modelId, env, proxy: ParamServerProxy[U])
    e.start
    e
  }
}