package helpers

import java.util.UUID

class KeyGenerator {
  def generateKey() = UUID.randomUUID().toString
}
