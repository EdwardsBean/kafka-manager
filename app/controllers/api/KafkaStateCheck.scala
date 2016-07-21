/**
 * Copyright 2015 Yahoo Inc. Licensed under the Apache License, Version 2.0
 * See accompanying LICENSE file.
 */

package controllers.api

import controllers.KafkaManagerContext
import features.ApplicationFeatures
import models.navigation.Menus
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc._

/**
 * @author jisookim0513
 */

class KafkaStateCheck (val messagesApi: MessagesApi, val kafkaManagerContext: KafkaManagerContext)
                      (implicit af: ApplicationFeatures, menus: Menus) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private[this] val kafkaManager = kafkaManagerContext.getKafkaManager

  def brokers(c: String) = Action.async { implicit request =>
    kafkaManager.getBrokerList(c).map { errorOrBrokerList =>
      errorOrBrokerList.fold(
        error => BadRequest(Json.obj("msg" -> error.msg)),
        brokerList => Ok(Json.obj("brokers" -> brokerList.list.map(bi => bi.id).sorted))
      )
    }
  }

  def topics(c: String) = Action.async { implicit request =>
    kafkaManager.getTopicList(c).map { errorOrTopicList =>
      errorOrTopicList.fold(
        error => BadRequest(Json.obj("msg" -> error.msg)),
        topicList => Ok(Json.obj("topics" -> topicList.list.sorted))
      )
    }
  }

  def consumerAndTopic(cluster: String, consumerGroup: String, topic: String, consumerType: String) = Action.async {
    kafkaManager.getConsumedTopicState(cluster,consumerGroup,topic, consumerType).map { errorOrConsumedTopicState =>
      errorOrConsumedTopicState.fold(
        error => BadRequest(Json.obj("msg" -> error.msg)),
        consumedTopicState => Ok(Json.obj(
          "topic" -> topic,
          "partitionNum" -> consumedTopicState.numPartitions,
          "totalLag" -> consumedTopicState.totalLag
        ))
      )
    }
  }

  def consumer(cluster: String, consumerGroup: String, consumerType: String) = Action.async {
    kafkaManager.getConsumerIdentity(cluster,consumerGroup, consumerType).map { errorOrConsumerIdentity =>
      errorOrConsumerIdentity.fold(
        error => BadRequest(Json.obj("msg" -> error.msg)),
        consumerIdentity => Ok(Json.toJson(
          consumerIdentity.topicMap.map({ case (topic, state) => (topic, state.totalLag) })
        ))
      )
    }
  }

  def underReplicatedPartitions(c: String, t: String) = Action.async { implicit request =>
    kafkaManager.getTopicIdentity(c,t).map { errorOrTopicIdentity =>
      errorOrTopicIdentity.fold(
        error => BadRequest(Json.obj("msg" -> error.msg)),
        topicIdentity => Ok(Json.obj("topic" -> t, "underReplicatedPartitions" -> topicIdentity.partitionsIdentity.filter(_._2.isUnderReplicated).map{case (num, pi) => pi.partNum}))
      )
    }
  }

  def unavailablePartitions(c: String, t: String) = Action.async { implicit request =>
    kafkaManager.getTopicIdentity(c,t).map { errorOrTopicIdentity =>
      errorOrTopicIdentity.fold(
        error => BadRequest(Json.obj("msg" -> error.msg)),
        topicIdentity => Ok(Json.obj("topic" -> t, "unavailablePartitions" -> topicIdentity.partitionsIdentity.filter(_._2.isr.isEmpty).map{case (num, pi) => pi.partNum}))
      )
    }
  }
}

