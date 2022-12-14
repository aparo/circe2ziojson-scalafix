package fix

import scalafix.v1._

import scala.collection.immutable
import scala.meta._

class MigrateCirce2ZioJson(config: Configuration) extends SemanticRule("MigrateCirce2ZioJson") {
  val scalaBinaryVersion = ScalaBinaryVersion.fromString(config.scalaVersion)
  def this() = this(Configuration.apply())

  override def withConfiguration(config: Configuration): metaconfig.Configured[Rule] =
    metaconfig.Configured.ok(new MigrateCirce2ZioJson(config))
  private def wildcardImport(ref: Term.Ref): Importer =
    Importer(ref, List(Importee.Wildcard()))

  object JsonCodecAnnotation {
    def unapply(cls: Defn.Class): Option[(Defn.Class)] = cls.mods.collectFirst {
      case Mod.Annot(init) =>
        init.tpe match {
          case Type.Name("JsonCodec") => Some(cls)
          case _ => None
        }
    }.flatten
  }

  def generateClass(cls: Defn.Class) = {
    val scala212 = scalaBinaryVersion == ScalaBinaryVersion.Scala212

    val fields = cls.ctor.paramss.head.map(p => Term.Name(p.name.value))
    val fieldsWithType =
      cls.ctor.paramss.head.flatMap(p => p.decltpe.map(Term.Name(p.name.value) -> _))
    val equal = fields.map(n => q"this.${n} == c.${n}").reduce((a, b) => q"$a && $b")
    val ctor = cls.ctor.copy(
      paramss = cls.ctor.paramss.map(paramList =>
        paramList.map(param =>
          param.copy(
            mods = param.mods.map{
              case m@Mod.Annot(init) if init.tpe.toString()=="JsonKey" =>
                m.copy(init = m.init.copy(tpe = Type.Name("jsonField")))
              case default => default
            }
          )
        )
      )//,
//      mods = cls.mods.filterNot(_.toString().contains("JsonCodec")),
    )

    def matchesSignature(def1: Defn.Def, def2: Defn.Def) =
      def1.mods.map(_.toString()).toSet == def2.mods.map(_.toString()).toSet &&
        def1.tparams.size == def2.tparams.size &&
        def1.paramss.flatMap(_.map(_.toString())) == def2.paramss.flatMap(_.map(_.toString()))

    val methods = {
      val existingDefs = cls.templ.stats.collect { case d: Defn.Def =>
        d.name.value -> d
      }.toMap

      val generatedMethods = Nil
//        equals :: hashCode :: copy :: productDefs ::: withFields ::: List(toString)
      generatedMethods.collect {
        case m @ Defn.Def(_, n, _, _, _, _)
            if existingDefs.get(n.value).forall(!matchesSignature(m, _)) =>
          m
        case v @ Defn.Val(_, List(Pat.Var(n)), _, _) if !existingDefs.contains(n.value) => v
      }
    }

    val template = cls.templ.copy(
      stats = Nil,
      inits = cls.templ.inits//.filterNot(t => inits.exists(_.name == t)) ::: inits,
    )

//    val mods= cls.mods.filter {
//      case Mod.Annot(init) if init.tpe.toString() == "JsonCodec" => false
//      case _ => true
//    }

    var hasFinal=false

    val mods= cls.mods
      .flatMap {
      case Mod.Annot(init) if init.tpe.toString() == "JsonCodec" => None
      case m:Mod.Final =>
        hasFinal=true
        Some(m)
      case m:Mod.Case =>
        if (!hasFinal) List(Mod.Final(), m) else Some(m)
      case default => Some(default)
    }

//    val modified = cls.copy(mods = mods, ctor = ctor/*, templ = template*/)

    val modified = cls.copy(mods = mods, ctor = ctor /*::: List(Mod.Final()), templ = template*/)

    val code = s"""|$modified
          |""".stripMargin
    Patch.replaceTree(cls, code)
  }

  def generateCompanion(cls: Defn.Class, maybeCompanion: Option[Defn.Object]) = {
    val params = cls.ctor.paramss.head
    val typeparams = cls.tparams
    val fieldsWithDefault = params.map(p => Term.Name(p.name.value) -> p.default)
    val fields = params.map(p => Term.Name(p.name.value))
    var generateEncoder=false
    var generateDecoder=false
    cls.mods.filter {
      case Mod.Annot(init) if init.tpe.toString() == "JsonCodec" =>
        if(init.argss.nonEmpty) {
          init.argss.flatten.foreach{
            case Term.Assign(left, Lit.Boolean(right)) =>
              left.toString() match {
                case "encodeOnly" =>
                  if (right) {
                    generateEncoder=right
                    generateDecoder=false
                  }
                case "decodeOnly" =>
                  if (right) {
                    generateDecoder=right
                    generateEncoder=false
                  }

              }
          }
        } else {
          generateEncoder = true
          generateDecoder = true

        }
        false
      case _ =>
        true
    }

    //    val allSinceValues = (for {
//      params <- cls.ctor.paramss
//      param <- params
//    } yield param.mods.find(m => m.toString.startsWith(sinceStr)) match {
//      case Some(mod) => mod.toString
//      case _ => ""
//    }).distinct.sorted

//    def isParamIn(param: Term.Param, sinces: Set[String]): Boolean =
//      param.mods.find(m => m.toString.startsWith(sinceStr)) match {
//        case Some(mod) => sinces(mod.toString)
//        case None => true
//      }

    def cleanParam(param: Term.Param): Term.Param =
      param.copy(
        default = None,
        mods = Nil,
      )

//    val first = Defn.Def(
//      Nil,
//      Term.Name("apply"),
//      typeparams,
//      cls.ctor.paramss.map(_.map(cleanParam)),
//      Some(cls.name),
//      q"new ${cls.name}(..$fields)",
//    )

//    val sinceApplies =
//      if (allSinceValues.size > 1) {
//        (for {
//          i <- 0 to (allSinceValues.size - 1)
//        } yield {
//          val sub = allSinceValues.slice(0, i + 1).toSet
//          cls.ctor.paramss.map(_.collect { case p if isParamIn(p, sub) => p })
//        }).toList
//      } else Nil

    val nonDefaults =
      if (cls.ctor.paramss.exists(_.exists(_.default.isDefined))) {
        List(cls.ctor.paramss.map(_.collect { case p if p.default.isEmpty => p }))
      } else Nil

//    val restParamsss = (sinceApplies ::: nonDefaults).distinct.filter { paramss =>
//      paramss.head.size != cls.ctor.paramss.head.size
//    }
//    val rest = restParamsss.map { paramss =>
//      val fieldNames = paramss.head.map(_.name.value).toSet
//      val applyFields = fieldsWithDefault.map { case (name, default) =>
//        if (fieldNames(name.value)) name
//        else default.getOrElse(sys.error(s"default value missing for $name"))
//      }
//      Defn.Def(
//        Nil,
//        Term.Name("apply"),
//        typeparams,
//        paramss.map(_.map(cleanParam)),
//        Some(cls.name),
//        q"new ${cls.name}(..$applyFields)",
//      )
//    }

    var stats: List[scala.meta.Stat] = Nil
    if (generateEncoder) {
      stats +:= q"""implicit val jsonEncoder: JsonEncoder[${cls.name}] = DeriveJsonEncoder.gen[${cls.name}]"""
    }
    if (generateDecoder){
      stats +:= q"""implicit val jsonDecoder: JsonDecoder[${cls.name}] = DeriveJsonDecoder.gen[${cls.name}]"""
    }

    def getNonApplyStats(companion: Defn.Object) = {
      companion.templ.stats.collect {
        case Defn.Def(_, name, _, _, _, _) if name.value == "apply" => None
        case s => Some(s)
      }.flatten
    }

    maybeCompanion match {
      case Some(companion) => {
        val block = (companion.templ.stats ::: stats).mkString("\n")
        val code =
          s"""|object ${cls.name.value} {
              |$block
              |}""".stripMargin
        Patch.replaceTree(companion, code)
      }
      case None => {
        val block = stats.mkString("\n")
        val code =
          s"""|object ${cls.name.value} {
              |$block
              |}""".stripMargin
        Patch.addRight(cls, code)
      }
    }
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    def getExistingCompanion(cls: Defn.Class) = {
      val name = Term.Name(cls.name.value).structure
      doc.tree.collect {
        case o: Defn.Object => o.name.structure match {
          case `name` => Some(o)
          case _ => None
        }
      }.flatten.headOption
    }




    val allAnnotationedClasses = doc.tree.collect {
      case JsonCodecAnnotation(cls) =>
        generateClass(cls) + generateCompanion(cls, getExistingCompanion(cls))
      case t@Importer(ref, _) if ref.toString()=="io.circe.generic" =>
        Patch.replaceTree(t, "zio.json._")

    }
    Patch.fromIterable(allAnnotationedClasses)
  }

}

sealed trait ScalaBinaryVersion
object ScalaBinaryVersion {
  case object Scala3 extends ScalaBinaryVersion
  case object Scala213 extends ScalaBinaryVersion
  case object Scala212 extends ScalaBinaryVersion

  def fromString(value: String): ScalaBinaryVersion = {
    val toBinaryVersion = if (value.startsWith("3")) "3" else value.split('.').take(2).mkString(".")
    toBinaryVersion match {
      case "2.12" => Scala212
      case "2.13" => Scala213
      case "3" => Scala3
      case _ => Scala213
    }
  }
}
