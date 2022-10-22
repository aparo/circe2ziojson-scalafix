/*
rule = MigrateCirce2ZioJson
*/
package fix

import io.circe.generic.JsonCodec

@JsonCodec(decodeOnly = true) case class PersonWithDecoder(name: String, age: Int)