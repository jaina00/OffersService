package repository

import helpers.KeyGenerator
import models._
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatest.{FlatSpecLike, Matchers}
import services.RedisService
import org.mockito.Mockito.verify
import org.mockito.captor.ArgCaptor
import org.mockito.integrations.scalatest.ResetMocksAfterEachTest
import org.scalatest.concurrent.ScalaFutures
import offers.Fixtures._
import scala.concurrent.Future

class OffersRepositoryTest extends FlatSpecLike with Matchers with IdiomaticMockito with ArgumentMatchersSugar with ScalaFutures
  with ResetMocksAfterEachTest {

  val keyGenerator = mock[KeyGenerator]
  val redisService = mock[RedisService]
  val offersRepo = new OffersRepository(redisService, keyGenerator)
  val userRequest = OfferVM(offerDesc, offerPrice, offerCurrency, offerTTL)
  val offer = Offer(id, offerDesc, Status.ACTIVE, offerPrice, Currency.USD, offerTTL)

  "create offers" should "generate a new offer" in {
    //Given
    keyGenerator.generateKey() returns id
    redisService.setOffer(id, offer) returns Future.successful(Right(id))

    //When
    val offerId = offersRepo.createOffer(userRequest)

    //Then
    verify(redisService).setOffer(id, offer)
    whenReady(offerId.value) { res => res shouldBe Right(id) }

  }

  it should "return error returned by the redis service" in {
    //Given
    keyGenerator.generateKey() returns id
    redisService.setOffer(id, offer) returns Future.successful(Left(RedisServiceError("error")))

    //When
    val offerId = offersRepo.createOffer(userRequest)

    //Then
    verify(redisService).setOffer(id, offer)
    whenReady(offerId.value) { res => res shouldBe Left(RedisServiceError("error")) }

  }


  "retrieve offers" should "retrieve stored offer" in {
    //Given
    redisService.get(id) returns Future.successful(Right(offer))

    //When
    val offerId = offersRepo.retrieveOffer(id)

    //Then
    verify(redisService).get(id)
    whenReady(offerId.value) { res => res shouldBe Right(OfferVM(offerDesc, offerPrice, offerCurrency, offerTTL)) }

  }

  it should "return error returned by the redis service" in {
    //Given
    redisService.get(id) returns Future.successful(Left(RedisServiceError("error")))

    //When
    val result = offersRepo.retrieveOffer(id)

    //Then
    verify(redisService).get(id)
    whenReady(result.value) { error => error shouldBe Left(RedisServiceError("error")) }

  }


  "update offer status" should "set status tp Cancelled" in {
    //Given
    redisService.get(id) returns Future.successful(Right(offer))
    redisService.setOffer(any, any) returns Future.successful(Right(id))
    val captor1 = ArgCaptor[String]
    val captor2 = ArgCaptor[Offer]

    //When
    offersRepo.updateOfferStatus(id, Status.CANCELLED)

    //Then
    verify(redisService).get(id)
    verify(redisService).setOffer(captor1, captor2)
    captor2.value.status shouldBe Status.CANCELLED

  }

  it should "return error when get by key from redis failed" in {
    //Given
    redisService.get(id) returns Future.successful(Left(RedisServiceError("error")))

    //When
    val result = offersRepo.updateOfferStatus(id, Status.CANCELLED)

    //Then
    verify(redisService).get(id)
    whenReady(result.value) { error => error shouldBe Left(RedisServiceError("error")) }

  }

  it should "return error when setting to redis failed" in {
    //Given
    redisService.get(id) returns Future.successful(Right(offer))
    redisService.setOffer(any, any) returns Future.successful(Left(RedisServiceError("error")))

    //When
    val result = offersRepo.updateOfferStatus(id, Status.CANCELLED)

    //Then
    verify(redisService).get(id)
    whenReady(result.value) { error => error shouldBe Left(RedisServiceError("error")) }

  }
}
