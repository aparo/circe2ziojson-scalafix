package fix

import zio.json._

case class PersonWithEncoder(name: String, age: Int)
object PersonWithEncoder {
implicit val jsonEncoder: JsonEncoder[PersonWithEncoder] = DeriveJsonEncoder.gen[PersonWithEncoder]
}
