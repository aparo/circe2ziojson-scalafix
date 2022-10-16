package io.circe.generic

import scala.annotation.StaticAnnotation

class JsonCodec(
encodeOnly: Boolean = false,
  decodeOnly: Boolean = false
  ) extends StaticAnnotation
