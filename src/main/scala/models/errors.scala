package models

sealed trait ServiceError{
  val msg:String
}

case class RedisServiceError(msg: String) extends ServiceError
case class RedisKeyNotFound(msg: String) extends ServiceError
case class OfferCancelledError(msg: String) extends ServiceError
case class ValidationError(msg: String) extends ServiceError