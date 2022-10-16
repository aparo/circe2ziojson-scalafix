/*
rule = MigrateCirce2ZioJson
*/
package fix
import io.circe.generic.JsonCodec

@JsonCodec case class Person(name: String, age: Int)