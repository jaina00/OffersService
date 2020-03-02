package services

import config.Settings
import io.circe.syntax._
import models.{Currency, Status}
import models.{RedisServiceError, OfferCancelledError, Offer, RedisKeyNotFound}
import offers.Fixtures._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FlatSpecLike, Matchers, _}
import redis.RedisClient
import redis.embedded.RedisServer

import scala.concurrent.Await
import scala.concurrent.duration._

class RedisServiceTest extends FlatSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfter with EncoderDecoders {

  override implicit def patienceConfig = PatienceConfig(timeout = Span(6, Seconds))

  implicit val system = akka.actor.ActorSystem()
  implicit val executor = system.dispatcher

  val redisPort = Settings.redisPort
  val redisServer = new RedisServer()
  val redisClient = RedisClient(Settings.redisHost, redisPort)
  val redisService = new RedisService

  var redisServiceForFailures = new RedisService {
    override lazy val redisClient: RedisClient = RedisClient(
      host = Settings.redisHost,
      port = Settings.redisPort,
      connectTimeout = Option(0 seconds))
  }


  override protected def beforeAll(): Unit = {
    redisServer.start()
    //Waiting for RedisServer to start so that tests do not run before Redis is up
    Await.result(redisClient.ping(), 3 seconds)
  }

  override protected def afterAll(): Unit = {
    redisServer.stop()
  }

  val offer = Offer(id, offerDesc, Status.ACTIVE, offerPrice, Currency.USD, offerTTL)

  "Redis service store offer " should "store the offer in redis" in {
    //When/Then
    whenReady(redisService.setOffer(id, offer)) { res => res shouldBe Right(id) }

    //Then
    whenReady(redisClient.get(id).map(redisService.toOffer)) {
      res => res shouldBe Some(Right(offer))
    }
  }

  it should "return error when storing to redis failed (as the connection timeout is 0)" in {
    //When/Then
    whenReady(redisServiceForFailures.setOffer(id, offer)) { res =>
      res shouldBe Left(RedisServiceError("store in redis failed for the key: key-123"))
    }
  }

  "Redis service retrieve offer" should "retrieve active offer" in {

    //Given
    redisClient.set(
      key = id,
      value = offer.asJson.toString(),
      exSeconds = Option(offer.ttlInSeconds))

    //When/Then
    whenReady(redisService.get(id)) {
      res => res shouldBe Right(offer)
    }
  }

  it should "return error if offer is cancelled" in {
    redisClient.set(
      key = id,
      value = offer.copy(status = Status.CANCELLED).asJson.toString(),
      exSeconds = Option(offer.ttlInSeconds))

    //When/Then
    whenReady(redisService.get(id)) {
      res => res shouldBe Left(OfferCancelledError("Offer is cancelled"))
    }
  }

  it should "return error if offer ttl is expired" in {
    //Given
    redisClient.set(
      key = id,
      value = offer.copy(status = Status.CANCELLED).asJson.toString(),
      exSeconds = Option(1))

    //When
    Thread.sleep(3000)

    // Then
    whenReady(redisService.get(id)) {
      res => res shouldBe Left(RedisKeyNotFound("Offer is not found or is expired"))
    }
  }

}
