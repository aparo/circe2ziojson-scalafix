package io.circe.generic

import scala.annotation.StaticAnnotation
final case class JsonKey(value: String) extends StaticAnnotation

final case class JsonNoDefault() extends StaticAnnotation

class JsonCodec(
encodeOnly: Boolean = false,
  decodeOnly: Boolean = false
  ) extends StaticAnnotation
