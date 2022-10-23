package fix

import zio.json._

final case class PersonWithDecoder(name: String, age: Int)
object PersonWithDecoder {
implicit val jsonDecoder: JsonDecoder[PersonWithDecoder] = DeriveJsonDecoder.gen[PersonWithDecoder]
}
