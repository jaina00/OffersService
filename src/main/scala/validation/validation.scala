package validation

import akka.http.scaladsl.server.directives.{BasicDirectives, RouteDirectives}
import akka.http.scaladsl.server.{Directive0, ValidationRejection}
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.syntax.apply._
import cats.syntax.validated._
import config.Settings
import models.{Currency, OfferVM, ServiceError, ValidationError}

trait HttpRequestHandler[T] {
  def validateRequest(req: T): Directive0
}

object RequestHandler extends BasicDirectives with RouteDirectives {

  type ValidationResult[A] = ValidatedNec[ServiceError, A]

  private def isValidPrice(price: BigDecimal): ValidationResult[BigDecimal] = {
    if (price >= Settings.minPrice) {
      price.validNec
    } else {
      ValidationError(s"price cannot be less than ${Settings.minPrice}").invalidNec
    }
  }

  private def isValidTTL(ttlInSeconds: Long): ValidationResult[Long] = {
    if (ttlInSeconds >= Settings.minTtl) {
      ttlInSeconds.validNec
    } else {
      ValidationError(s"ttl should  be at least ${Settings.minTtl}").invalidNec
    }
  }

  private def isValidCurrency(currency: String): ValidationResult[String] = {
    if (Currency.valueOf(currency).isDefined) {
      currency.validNec
    } else {
      ValidationError(s"invalid currency $currency support currencies are ${Currency.values.mkString(",")}").invalidNec
    }
  }

  val offersVMRequest = new HttpRequestHandler[OfferVM] {

    override def validateRequest(req: OfferVM) = {
      val result: ValidationResult[String] = (
        isValidPrice(req.price),
        isValidTTL(req.ttlInSeconds),
        isValidCurrency(req.currency)
      ).mapN(_ + _ + _)

      result match {
        case Valid(_) => pass
        case Invalid(failures) => reject(ValidationRejection(failures.iterator.map(_.msg).mkString("\n")))
      }
    }
  }
}
