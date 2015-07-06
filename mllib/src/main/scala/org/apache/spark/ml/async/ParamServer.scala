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

import java.util.concurrent.atomic.AtomicInteger

import org.apache.spark.rdd.RDD
import org.apache.spark.rpc._
import org.apache.spark.sql.DataFrame
import org.apache.spark.{SparkException, SparkContext, Logging}

import scala.collection.mutable.{SynchronizedMap, HashMap}
import scala.reflect.ClassTag

/**
 * Messages used by the NetworkReceiver and the ReceiverTracker to communicate
 * with each other.
 */
private[async] sealed trait ParamServerMessage
private[async] case class RegisterModel(
                                         modelId: Int,
                                         typ: String,
                                         host: String,
                                         receiverEndpoint: RpcEndpointRef
                                         ) extends ParamServerMessage

private[async] case class DeregisterModel(modelId: Int, msg: String, error: String)
  extends ParamServerMessage

// Report error from sub-model
private[async] case class ReportError(modelId: Int, message: String, error: String)
// model updates params to server
private[async] case class Update[U: ClassTag](modelId: Int, params: U)
// model requests updated params
private[async] case class Fetch(modelId: Int)
// model updates the params, and request updated params
private[async] case class UpdateAndFetch[U: ClassTag](modelId: Int, params: U)
// server propagate params to model upon receiving request
private[async] case class Propagate[U: ClassTag](modelId: Int, params: U)


// In the fit(dataset: DataFrame) method, we call
// new AsyncTracker(sc, model, dataset).start
class ParamServer[T: ClassTag, U: ClassTag](
     val model: AsyncModel[T, U],
     dataset: DataFrame
   ) extends Logging{

  // endpoint is created when generator starts.
  // This not being null means the tracker has been started and not stopped
  private var endpoint: RpcEndpointRef = null
  private val modelInfo = new HashMap[Int, ModelInfo] with SynchronizedMap[Int, ModelInfo]
  private var runner: AsyncModelRunner[T, U] = _
  private var sc = dataset.rdd.sparkContext
  private val env = sc.env
  private var modelNum = new AtomicInteger(0)

  /** Start the endpoint and receiver execution thread. */
  def start(): Unit = synchronized {
    if (endpoint != null) {
      throw new SparkException("ParamServer already started")
    }

    endpoint = sc.env.rpcEnv.setupEndpoint(
      s"ParamServer-${model.name}", new AyncServerEndpoint(sc.env.rpcEnv))
    logInfo("ParamServer started")
    runner = new AsyncModelRunner[T, U](model)
    runner.fit(dataset)
  }

  /** Register a model */
  private def registerModel(
      modelId: Int,
      typ: String,
      host: String,
      modelEndpoint: RpcEndpointRef,
      senderAddress: RpcAddress) = {
    modelNum.addAndGet(1)
    modelInfo(modelId) = ModelInfo(
      modelId, s"${typ}-${modelId}", modelEndpoint, true, host)
    //listenerBus.post(StreamingListenerReceiverStarted(receiverInfo(streamId)))
    logInfo("Registered for model " + modelId + " from " + senderAddress)
  }

  /** Stop the AsyncModelEndPoint. */
  def stop(): Unit = synchronized {
    modelInfo.values.foreach(_.stop)
    modelInfo.clear()
    // take case the case that overall training is done, but some model are not even started yet
    if (modelNum.get() == model.partitionNum) {
      env.rpcEnv.stop(endpoint)
    }
    logInfo("Parameter server stopped")
  }

  // Deregister a model
  // If all existing models are terminating, try to stop the training.
  private def deregisterModel(modelId: Int, message: String, error: String) = synchronized {
    modelInfo(modelId).stop
    modelInfo.remove(modelId)
    if (modelInfo.size == 0) {
      stop()
    }
    logInfo(s"Deregistered for model $modelId")
  }

  /** Report error sent by a receiver */
  private def reportError(modelId: Int, message: String, error: String) = {
    deregisterModel(modelId, message, error)
  }


  private def processParams(p: U) = {
    if (model.processParams(p)) {
      // we are done on the param server side, terminate all models
      stop()
    }
  }
  /** RpcEndpoint to receive messages from the receivers. */
  private class AyncServerEndpoint(override val rpcEnv: RpcEnv) extends ThreadSafeRpcEndpoint {

    override def receive: PartialFunction[Any, Unit] = {
      case ReportError(modelId, message, error) =>
        reportError(modelId, message, error)
      case DeregisterModel(modelId, message, error) =>
        deregisterModel(modelId, message, error)
      case Update(modelId, params) =>
        processParams(params.asInstanceOf[U])
    }

    override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
      case RegisterModel(modelId, typ, host, receiverEndpoint) =>
        registerModel(modelId, typ, host, receiverEndpoint, context.sender.address)
        context.reply(true)
      case Fetch(modelId) =>
        context.reply(Propagate(modelId, model.distParams()))
      case UpdateAndFetch(modelId, params) =>
        processParams(params.asInstanceOf[U])
        context.reply(Propagate(modelId, model.distParams()))
    }
  }
}
