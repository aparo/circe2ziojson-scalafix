/*
rule = MigrateCirce2ZioJson
*/
package fix

import io.circe.generic.JsonCodec

@JsonCodec case class PersonWithCompanion(name: String, age: Int)

object PersonWithCompanion {
def hello = s"Hello!"
def apply() = new PersonWithCompanion("", 0) 
}
