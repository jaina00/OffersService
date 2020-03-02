import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import models._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.time.{Millis, Seconds, Span}


class OffersTest extends WordSpec with ScalaFutures
  with BeforeAndAfterAll
  with MockitoSugar
  with Matchers
  with ScalatestRouteTest
  with Eventually {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(100, Millis))

  private val restApiUrl = s"http://localhost:8080"

  "Offers" should {
    "user posts an offer and get the offer" in {

      val jsonRequest = ByteString(
        """
          |{
          |    "description": "item 1",
          |    "price": 10,
          |    "currency": "USD",
          |    "ttlInSeconds": 100
          |}
          |""".stripMargin)

      val postOfferRequest = HttpRequest(HttpMethods.POST, s"$restApiUrl/offers", Nil, HttpEntity(MediaTypes.`application/json`, jsonRequest))

      def getOfferRequest(uuid: String) = HttpRequest(HttpMethods.GET, s"$restApiUrl/offers/$uuid")

      val result = for {
        res <- Http().singleRequest(postOfferRequest)
        offerId <- Unmarshal(res.entity).to[String]
        offerRes <- Http().singleRequest(getOfferRequest(offerId))
        offer <- Unmarshal(offerRes.entity).to[OfferVM]
      } yield {
        (res, offerRes, offer)
      }

      whenReady(result){ res =>
        res._1.status shouldBe OK
        res._2.status shouldBe OK
        res._3.currency shouldBe "USD"
        res._3.description shouldBe "item 1"
        res._3.price shouldBe 10.0
        res._3.ttlInSeconds shouldBe 100
      }
    }

    "user posts an offer and cancel the offer" in {
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

      //When
      val postOfferRequest = HttpRequest(HttpMethods.POST, s"$restApiUrl/offers", Nil, HttpEntity(MediaTypes.`application/json`, jsonRequest))
      def getOfferRequest(uuid: String) = HttpRequest(HttpMethods.GET, s"$restApiUrl/offers/$uuid")
      def cancelOfferRequest(uuid: String) = HttpRequest(HttpMethods.PATCH, s"$restApiUrl/offers/$uuid/CANCELLED")

      val result = for {
        res <- Http().singleRequest(postOfferRequest)
        offerId <- Unmarshal(res.entity).to[String]
        _ <- Http().singleRequest(cancelOfferRequest(offerId))
        offerRes <- Http().singleRequest(getOfferRequest(offerId))
      } yield {
        offerRes
      }

      //Then
      whenReady(result){ res =>
        res.status shouldBe StatusCodes.Gone
        whenReady(Unmarshal(res.entity).to[String]){ errorMsg =>
          errorMsg shouldBe "Offer is cancelled"
        }
      }
    }

    "user posts an offer and offer is expired" in {
      //Given
      val jsonRequest = ByteString(
        """
          |{
          |    "description": "item 1",
          |    "price": 10,
          |    "currency": "USD",
          |    "ttlInSeconds": 1
          |}
          |""".stripMargin)

      //When
      val postOfferRequest = HttpRequest(HttpMethods.POST, s"$restApiUrl/offers", Nil, HttpEntity(MediaTypes.`application/json`, jsonRequest))
      def getOfferRequest(uuid: String) = HttpRequest(HttpMethods.GET, s"$restApiUrl/offers/$uuid")

      val offerIdFuture = for {
        res <- Http().singleRequest(postOfferRequest)
        offerId <- Unmarshal(res.entity).to[String]
      } yield {
        offerId
      }

      //Then
      Thread.sleep(2000) //let offer expire

      val expiredOffer = for {
        offerId <- offerIdFuture
        res <- Http().singleRequest(getOfferRequest(offerId))
      } yield {
        res
      }


      //Then
      whenReady(expiredOffer){ res =>
        res.status shouldBe StatusCodes.NotFound
//        whenReady(Unmarshal(res.entity).to[String]){ errorMsg =>
//          errorMsg shouldBe "Offer is cancelled"
//        }
      }
    }

  }

}
