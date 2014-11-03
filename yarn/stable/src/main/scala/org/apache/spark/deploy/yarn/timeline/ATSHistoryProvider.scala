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

package org.apache.spark.deploy.yarn.timeline

import org.apache.spark.{Logging, SparkConf}
import org.apache.spark.deploy.history.{HistoryServer, ApplicationHistoryProvider, ApplicationHistoryInfo}
import java.io.FileNotFoundException
import java.net.URI
import java.util.{Collection => JCollection, Map => JMap}
import javax.ws.rs.core.MediaType

import scala.collection.JavaConversions._
import scala.collection.mutable

import com.sun.jersey.api.client.{Client, ClientResponse, WebResource}
import com.sun.jersey.api.client.config.{ClientConfig, DefaultClientConfig}
import org.apache.hadoop.yarn.api.records.timeline.{TimelineEntities, TimelineEvents}
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.webapp.YarnJacksonJaxbJsonProvider
import org.json4s.JsonAST._

import org.apache.spark.{Logging, SecurityManager, SparkConf}
import org.apache.spark.scheduler.{ApplicationEventListener, SparkListenerBus}
import org.apache.spark.ui.SparkUI
import org.apache.spark.util.JsonProtocol
import org.apache.spark.scheduler._

class ATSHistoryProvider(conf: SparkConf) extends ApplicationHistoryProvider
with Logging {

  private val yarnConf = new YarnConfiguration()
  private val NOT_STARTED = "<Not Started>"

  // Copied from Yarn's TimelineClientImpl.java.
  private val RESOURCE_URI_STR = s"/ws/v1/timeline/SparkApplication"

  override def getListing(): Seq[ApplicationHistoryInfo] = {
    null
  }
  private val timelineUri = {
    val isHttps = YarnConfiguration.useHttps(yarnConf)
    val addressKey =
      if (isHttps) {
        YarnConfiguration.TIMELINE_SERVICE_WEBAPP_HTTPS_ADDRESS
      } else {
        YarnConfiguration.TIMELINE_SERVICE_WEBAPP_ADDRESS
      }
    val protocol = if (isHttps) "https://" else "http://"
    URI.create(s"$protocol${yarnConf.get(addressKey)}$RESOURCE_URI_STR")
  }


  private val client = {
    val cc = new DefaultClientConfig()
    // Note: this class is "interface audience private" but since there is no public API
    // for the timeline server, this makes it much easier to talk to it.
    cc.getClasses().add(classOf[YarnJacksonJaxbJsonProvider])
    Client.create(cc)
  }

  override def getAppUI(appId: String): Option[SparkUI] = {

    val eventsUri = timelineUri.resolve(s"${timelineUri.getPath()}/events")
    val resource = client.resource(eventsUri)
      .queryParam("entityId", appId)
      .accept(MediaType.APPLICATION_JSON)

    val events = resource.get(classOf[ClientResponse]).getEntity(classOf[TimelineEvents])
    val bus = new SparkListenerBus() { }
    val appListener = new ApplicationEventListener()
    bus.addListener(appListener)

    val ui = {
      val conf = this.conf.clone()
      val appSecManager = new SecurityManager(conf)
      SparkUI.createHistoryUI(conf, bus, appSecManager, appId,"/history/" + appId)
    }

    events.getAllEvents().foreach { entityEvents =>
      entityEvents.getEvents().reverse.foreach { e =>
        bus.postToAll(e.getEventInfo.asInstanceOf[SparkListenerEvent])
      }
    }
    ui.setAppName(s"${appListener.appName.getOrElse(NOT_STARTED)} ($appId)")

    val uiAclsEnabled = conf.getBoolean("spark.history.ui.acls.enable", false)
    ui.getSecurityManager.setAcls(uiAclsEnabled)
    // make sure to set admin acls before view acls so they are properly picked up
    ui.getSecurityManager.setAdminAcls(appListener.adminAcls.getOrElse(""))
    ui.getSecurityManager.setViewAcls(appListener.sparkUser.getOrElse(NOT_STARTED),
      appListener.viewAcls.getOrElse(""))
    Some(ui)
  }
  override def getConfig(): Map[String, String] =
    Map(("Yarn Application Timeline Server" -> timelineUri.resolve("/").toString()))

  override def stop(): Unit = client.destroy()

}