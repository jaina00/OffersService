package services

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import models.{Currency, Offer, Status}

trait EncoderDecoders {
  implicit val currencyDecoder: Decoder[Currency.Value] = Decoder.decodeEnumeration(Currency)
  implicit val currencyEncoder: Encoder[Currency.Value] = Encoder.encodeEnumeration(Currency)

  implicit val statusDecoder: Decoder[Status.Value] = Decoder.decodeEnumeration(Status)
  implicit val statusEncoder: Encoder[Status.Value] = Encoder.encodeEnumeration(Status)

  implicit val encoder: Encoder[Offer] = deriveEncoder
  implicit val decoder: Decoder[Offer] = deriveDecoder

}
