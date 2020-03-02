package routes

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import cats.data.EitherT
import models.OffersDomain.OfferID
import models._
import offers.Fixtures._
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.mockito.integrations.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, Matchers}
import repository.OffersRepository

import scala.concurrent.Future

class OffersRouteTest extends FlatSpecLike with Matchers with ScalatestRouteTest with ScalaFutures
  with IdiomaticMockito with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  val offersRepository: OffersRepository = mock[OffersRepository]

  val routes = new OffersRoute(offersRepository).route

  "doing POST on /offers" should "create an offer" in {
    //Given
    val jsonRequest = ByteString(
      """
        |{
        |    "description": "item 1",
        |    "price": 10,
        |    "currency": "USD",
        |    "ttlInSeconds": 100
        |}
        |""".stripMargin)

    offersRepository.createOffer(any) returns EitherT(Future.successful[Either[ServiceError, OfferID]](Right(id)))

    //When
    val request = HttpRequest(HttpMethods.POST, "/offers/", Nil, HttpEntity(MediaTypes.`application/json`, jsonRequest))

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """"key-123""""
      verify(offersRepository).createOffer(any[OfferVM])

    }
  }

  it should "if invalid payload return bad request and validation errors" in {
    //Given
    val jsonRequest = ByteString(
      """
        |{
        |    "description": "item 1",
        |    "price": -10,
        |    "currency": "asdfasdf",
        |    "ttlInSeconds": 0
        |}
        |""".stripMargin)

    offersRepository.createOffer(any) returns EitherT(Future.successful[Either[ServiceError, OfferID]](Right(id)))

    //When
    val request = HttpRequest(HttpMethods.POST, "/offers/", Nil, HttpEntity(MediaTypes.`application/json`, jsonRequest))

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.BadRequest
      responseAs[String] shouldEqual
        """|price cannot be less than 10
          |ttl should  be at least 1
          |invalid currency asdfasdf support currencies are GBP,USD,EUR""".stripMargin
      verifyZeroInteractions(offersRepository)

    }
  }

  it should "handle error sent by downstream" in {
    //Given
    val jsonRequest = ByteString(
      """
        |{
        |    "description": "item 1",
        |    "price": 10,
        |    "currency": "USD",
        |    "ttlInSeconds": 100
        |}
        |""".stripMargin)

    offersRepository.createOffer(any) returns EitherT(Future.successful[Either[ServiceError, OfferID]](Left(RedisServiceError("error"))))

    //When
    val request = HttpRequest(HttpMethods.POST, "/offers/", Nil, HttpEntity(MediaTypes.`application/json`, jsonRequest))

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.InternalServerError
      responseAs[String] shouldEqual """"error""""
    }
  }


  "doing get on /offers" should "return offer" in {
    //Given
    val offer = OfferVM(offerDesc, offerPrice, "USD", offerTTL)

    offersRepository.retrieveOffer(id) returns EitherT(Future.successful[Either[ServiceError, OfferVM]](Right(offer)))

    //When
    val request = HttpRequest(HttpMethods.GET, s"/offers/$id")

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """{"description":"my offer","price":10.0,"currency":"USD","ttlInSeconds":100}"""
      verify(offersRepository).retrieveOffer(id)

    }
  }

  it should "return 404 if offer is expired / does not exists" in {
    //Given
    offersRepository.retrieveOffer(id) returns EitherT(Future.successful[Either[ServiceError, OfferVM]](Left(RedisKeyNotFound("no offer exists"))))

    //When
    val request = HttpRequest(HttpMethods.GET, s"/offers/$id")

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.NotFound
      responseAs[String] shouldEqual """"no offer exists""""
      verify(offersRepository).retrieveOffer(id)

    }
  }

  it should "return 410 Gone if offer is is cancelled" in {
    //Given
    offersRepository.retrieveOffer(id) returns EitherT(Future.successful[Either[ServiceError, OfferVM]](Left(OfferCancelledError("offer is cancelled"))))

    //When
    val request = HttpRequest(HttpMethods.GET, s"/offers/$id")

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.Gone
      responseAs[String] shouldEqual """"offer is cancelled""""
      verify(offersRepository).retrieveOffer(id)

    }
  }

  "doing patch on /offers" should "set offer status to CANCELLED" in {
    //Given
    offersRepository.updateOfferStatus(id, Status.CANCELLED) returns EitherT(Future.successful[Either[ServiceError, OfferID]](Right(id)))

    //When
    val request = HttpRequest(HttpMethods.PATCH, s"/offers/$id/CANCELLED")

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """"key-123""""
      verify(offersRepository).updateOfferStatus(id, Status.CANCELLED)

    }
  }

  it should "return 404 if offer is expired / does not exists" in {
    //Given
    offersRepository.updateOfferStatus(id, Status.CANCELLED) returns EitherT(Future.successful[Either[ServiceError, OfferID]](Left(RedisKeyNotFound("no offer exists"))))

    //When
    val request = HttpRequest(HttpMethods.PATCH, s"/offers/$id/CANCELLED")

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.NotFound
      responseAs[String] shouldEqual """"no offer exists""""
      verify(offersRepository).updateOfferStatus(id, Status.CANCELLED)

    }
  }

  it should "return 410 Gone if offer is is cancelled" in {
    //Given
    offersRepository.updateOfferStatus(id, Status.CANCELLED)  returns EitherT(Future.successful[Either[ServiceError, OfferID]](Left(OfferCancelledError("offer is cancelled"))))

    //When
    val request = HttpRequest(HttpMethods.PATCH, s"/offers/$id/CANCELLED")

    //Then
    request ~> Route.seal(routes) ~> runRoute ~> check {
      status shouldEqual StatusCodes.Gone
      responseAs[String] shouldEqual """"offer is cancelled""""
      verify(offersRepository).updateOfferStatus(id, Status.CANCELLED)

    }
  }

}
