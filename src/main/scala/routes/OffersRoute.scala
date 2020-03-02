package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import models._
import validation.RequestHandler._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import repository.OffersRepository
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import services.EncoderDecoders

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class OffersRoute(offersRepository: OffersRepository) extends Directives with LazyLogging with EncoderDecoders {

  def route: Route = pathPrefix("offers") {
    createOffer ~ getOffer ~ patchOfferStatus
  }

  def createOffer: Route = {
    post {
      entity(as[OfferVM]) { offer: OfferVM =>
        offersVMRequest.validateRequest(offer) {
          onComplete(
            offersRepository.createOffer(offer)
              .fold(handleServiceError, res => complete(res))
          ) {
            case Success(route) => route
            case Failure(ex) =>
              logger.error(s"An error occurred: ${ex.getMessage}")
              complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }

  def getOffer: Route = {
    get {
      path(Segment) {
        offerId: String =>
          onComplete(
            offersRepository.retrieveOffer(offerId)
              .fold(handleServiceError, res => complete(res))
          ) {
            case Success(route) => route
            case Failure(ex) =>
              logger.error(s"An error occurred: ${ex.getMessage}")
              complete(StatusCodes.InternalServerError)
          }
      }
    }
  }

  def patchOfferStatus: Route = {
    patch {
      path(Segment / "CANCELLED") {
        offerID: String =>
          onComplete(
            offersRepository.updateOfferStatus(offerID, Status.CANCELLED)
              .fold(handleServiceError, res => complete(res))
          ) {
            case Success(route) => route
            case Failure(ex) =>
              logger.error(s"An error occurred: ${ex.getMessage}")
              complete(StatusCodes.InternalServerError)
          }
      }
    }
  }

  def handleServiceError(error: ServiceError): Route = error match {
    case RedisServiceError(msg) => complete(StatusCodes.InternalServerError, msg)
    case RedisKeyNotFound(msg) => complete(StatusCodes.NotFound, msg)
    case OfferCancelledError(msg) => complete(StatusCodes.Gone, msg)
  }


}
