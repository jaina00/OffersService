import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import helpers.KeyGenerator
import repository.OffersRepository
import routes.OffersRoute
import services.RedisService

object Application extends App with LazyLogging{
  implicit val system = ActorSystem("offers-system")
  implicit val mat = ActorMaterializer()
  val redisService = new RedisService
  val offersRepository = new OffersRepository(redisService, new KeyGenerator)

  // routes
  val offerRoute = new OffersRoute(offersRepository).route

  val routes: Route = offerRoute
  Http().bindAndHandle(routes, "localhost", 8080)

  logger.info(s"HTTP server started")
}
