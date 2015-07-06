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

import java.util.UUID
import java.util.concurrent.atomic.{AtomicInteger, AtomicBoolean}

import org.apache.spark.ml.Predictor
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, DataFrame}

import scala.collection.generic.AtomicIndexFlag
import scala.reflect.ClassTag

// Running in the parameter server to scatter-gather sub-gradient
trait AsyncServerModel[T, U] {
  self: AsyncModel[T, U] =>
  // The total number of steps for models gradient update before terminating all sub-models
  var mStep: Int = _
  // Current number of steps done by sub-models
  lazy val curStep = new AtomicInteger(0)
  // Initialize the params.
  def initParams(): Unit
  // Process the aggregation of params from sub-models
  def aggParams: U => Unit
  // Distribute params to create the model or distribute to sub-models
  def distParams(): U
  // Process params from sub-models, as well as determine the termination condition
  def processParams(p: U): Boolean = {
    aggParams(p)
    return curStep.addAndGet(uStep) == mStep
  }
}

// Running in the executor side to operate on sub-gradient
trait AsyncSubModel[T, U] {
  self: AsyncModel[T, U] =>
  // FetchStep size for each model to fetch gradient from server.
  var fStep: Int = _
  // The maximum number of steps each model should perform the training before termination
  var tStep :Int = _
  // Stop the training, updated by server
  lazy val stopped: AtomicBoolean = new AtomicBoolean(false)
  // Retrieve params from sub-model to be updated to the server
  def retrieveParams(): U
  // Initialization of params retrieved from server
  def setParams: U => Unit
  // Update sub-model params with the params from server
  def updateParams: U => Unit
  // Fit a single data point and change sub-model's internal state(params)
  def train: T => Unit
}

trait AsyncModel[T, U] extends AsyncServerModel[T, U] with AsyncSubModel[T, U]{
  // Unique ID for a model instance to support concurrent training for models.
  var name: String = _
  // The number of sub-model
  var partitionNum: Int = _
  // The step size for updating from sub-models
  var uStep:Int = _
  // extract the feature form data frame
  def extract(dataset: DataFrame): RDD[T]
}

class SampleModel extends AsyncModel[LabeledPoint, Array[Double]] {
  var size = 10
  var params = Array[Double](size)
  var delta = Array[Double](size)
  var accumuDelta = Array[Double](size)
  name = s"SampelModel-${UUID.randomUUID().toString()}"
  uStep = 1
  fStep = 1
  tStep = 1000
  mStep = 10000

  override def initParams() = ???
  override def aggParams: Array[Double] => Unit = ???

  override def distParams() = ???

  // get the accumulated delta
  override def retrieveParams: Array[Double] = accumuDelta

  override def setParams: Array[Double] => Unit = {
    p =>
      params = p
      accumuDelta = accumuDelta.map(x => 0.0)
  }

  override def updateParams: Array[Double] => Unit = {
    p =>
      params = p
  }
  override def extract(dataset: DataFrame): RDD[LabeledPoint] = ???
  //extractLabeledPoints
  // Take one training example, calculate and update internal state.
  override def train: LabeledPoint => Unit = ???

}
