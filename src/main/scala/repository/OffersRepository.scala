package repository

import cats.implicits._
import cats.data.EitherT
import helpers.KeyGenerator
import models.OffersDomain.OfferID
import models.Status.Status
import models._
import services.RedisService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OffersRepository(redisService: RedisService, keyGenerator: KeyGenerator) {

  def createOffer(offerData: OfferVM): EitherT[Future, ServiceError, OfferID] = EitherT {
    val key: OfferID = keyGenerator.generateKey()
    val offer = Offer(key,
      offerData.description,
      Status.ACTIVE,
      offerData.price,
      Currency.valueOf(offerData.currency).get,
      offerData.ttlInSeconds)

    redisService.setOffer(key, offer)
  }


  def retrieveOffer(id: OfferID): EitherT[Future, ServiceError, OfferVM] = EitherT {
    redisService.get(id)
      .map(x => x.map(offer => OfferVM(offer.description, offer.price, offer.currency.toString, offer.ttlInSeconds)))
  }

  def updateOfferStatus(id: OfferID, newStatus: Status): EitherT[Future, ServiceError, OfferID] = {
    for {
      offer <- EitherT(redisService.get(id))
      offerId <- EitherT(redisService.setOffer(id, offer.copy(status = newStatus)))
    } yield {
      offerId
    }
  }
}
