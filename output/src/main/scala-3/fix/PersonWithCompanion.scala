package fix


import zio.json._

case class PersonWithCompanion(name: String, age: Int)


final object PersonWithCompanion {
def hello = s"Hello!"
def apply() = new PersonWithCompanion("", 0)
implicit val jsonDecoder: JsonDecoder[PersonWithCompanion] = DeriveJsonDecoder.gen[PersonWithCompanion]
implicit val jsonEncoder: JsonEncoder[PersonWithCompanion] = DeriveJsonEncoder.gen[PersonWithCompanion]
}