package models

import models.Currency.Currency
import models.OffersDomain.OfferID
import models.Status.Status

case class OfferVM(description: String, price: BigDecimal, currency: String, ttlInSeconds: Long)

case class OfferStatus(status: Status)

object Status extends Enumeration {
  type Status = Value
  val ACTIVE, CANCELLED = Value
}

object Currency extends Enumeration {
  type Currency = Value
  val GBP, USD, EUR = Value

  def valueOf(currency: String): Option[models.Currency.Value] = this.values.find(_.toString.equalsIgnoreCase(currency))
}

case class Offer(id: OfferID, description: String, status: Status, price: BigDecimal, currency: Currency, ttlInSeconds: Long)

object OffersDomain {
  type OfferID = String
}