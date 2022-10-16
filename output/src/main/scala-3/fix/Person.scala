package fix

import zio.json._

case class Person(name: String, age: Int)
object Person {
implicit val jsonDecoder: JsonDecoder[Person] = DeriveJsonDecoder.gen[Person]
implicit val jsonEncoder: JsonEncoder[Person] = DeriveJsonEncoder.gen[Person]
}
