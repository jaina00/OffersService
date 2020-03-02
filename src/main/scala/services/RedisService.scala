package services

import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import config.Settings
import io.circe.parser.decode
import io.circe.syntax._
import models.OffersDomain.OfferID
import models._
import redis.RedisClient

import scala.concurrent.Future

class RedisService extends LazyLogging with EncoderDecoders {
  implicit val system = akka.actor.ActorSystem()
  implicit val executor = system.dispatcher

  lazy protected val redisClient: RedisClient = RedisClient(
    host = Settings.redisHost,
    port = Settings.redisPort)

  def setOffer(key: OfferID, offer: Offer): Future[Either[ServiceError, OfferID]] = {
    redisClient.set(
      key = key,
      value = offer.asJson.toString(),
      exSeconds = Option(offer.ttlInSeconds))
      .map(_ => Right(offer.id))
      .recover {
        case ex =>
          logger.error(s"store in redis failed for the key : $key, message: ${ex.getMessage}, ${ex.printStackTrace()}")
          Left(RedisServiceError(s"store in redis failed for the key: $key"))
      }
  }

  def get(key: String): Future[Either[ServiceError, Offer]] = {
    redisClient.get(key)
      .map(toOffer)
      .map {
        case Some(Right(resp)) if resp.status == Status.ACTIVE => Right(resp)
        case Some(Right(resp)) if resp.status != Status.ACTIVE  =>
          logger.info(s"Offer is cancelled")
          Left(OfferCancelledError(s"Offer is cancelled"))
        case None =>
          logger.error(s"Key not found: $key")
          Left(RedisKeyNotFound(s"Offer is not found or is expired"))
      }
      .recover {
        case ex =>
          logger.error(s"redis error : $key, message: ${ex.getMessage}, ${ex.printStackTrace()}")
          Left(RedisServiceError(s"`for the key: $key"))
      }

  }

  def toOffer(value: Option[ByteString]): Option[Either[io.circe.Error, Offer]] =
    value.map(v => decode[Offer](v.decodeString("UTF-8")))

}
