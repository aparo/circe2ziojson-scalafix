/*
rule = MigrateCirce2ZioJson
*/
package fix

import io.circe.generic.JsonCodec

@JsonCodec(encodeOnly=true) case class PersonWithEncoder(name: String, age: Int)