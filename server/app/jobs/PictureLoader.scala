package jobs

import javax.inject.Inject

import akka.actor.Actor
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import com.cloudinary.Cloudinary
import jobs.PictureLoader.{LoadAll, LoadByName, LoadUpdated}
import models.{DrugsAdminRq, DrugsProduct}
import models.daos.ProductDAO
import play.api.Configuration
import utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Class used to add pictures for all drugs
  * The pictures loaded from other sites using special patterns
  * This actor used configuration parameter to locale working actors - 'picture-load-worker'
  */
class PictureLoader @Inject() (configuration: Configuration, productDAO: ProductDAO, cloudinary: Cloudinary) extends Actor with Logger {
  val workerActorPath = configuration.get[String]("picloader.workActor.location")
  val worketActorCount = configuration.get[Int]("picloader.workActor.count")

  def makeLocalRouter = {
    val routees = Vector.fill(worketActorCount) {
      val worker = context.actorOf(PictureLoadWorker.props(cloudinary, productDAO))
      context watch worker
      ActorRefRoutee (worker)
    }

    Router(RoundRobinRoutingLogic(), routees)
  }

  /**
    * Create pool of actors with routing
    */
  val workerRoute = makeLocalRouter

  override def receive: Receive = {
    case LoadAll => // for all drugs it need to load pictures from other site. Site should be defined within child actor (workRoute)
      productDAO.getAll(DrugsAdminRq("Антигриппин")).onComplete(_ match {
        case Success(drugList) => drugList.foreach(drug => workerRoute.route(drug, sender()))
        case Failure(e) => logger.error(s"Fail to get product list: ${e.getMessage}")
      })

    case ln:LoadByName => // for all drugs it need to load pictures from other site. Site should be defined within child actor (workRoute)
      productDAO.getAll(DrugsAdminRq(ln.name)).onComplete(_ match {
        case Success(drugList) => drugList.foreach(drug => workerRoute.route(drug, sender()))
        case Failure(e) => logger.error(s"Fail to get product list: ${e.getMessage}")
      })

    case LoadUpdated(ids) =>
      ids.foreach (id => workerRoute.route(id, self))
  }
}

object PictureLoader {
  case object LoadAll
  case class LoadByName (name: String)
  case class LoadUpdated (ids: List[DrugsProduct])
}
