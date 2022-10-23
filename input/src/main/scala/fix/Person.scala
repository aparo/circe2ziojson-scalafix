/*
rule = MigrateCirce2ZioJson
*/
package fix

import io.circe.generic.{JsonCodec, JsonKey}

@JsonCodec case class Person(@JsonKey("name") name: String, age: Int)