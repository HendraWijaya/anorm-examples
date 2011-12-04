package anorm.examples

import anorm._
import anorm.SqlParser._
import anorm.defaults._
import java.util.Date

trait Dao[T] extends Magic[T] {
  /*
   * Support for Id[Any] and Pk[Any] to fix matching with AnyVal
   */
  override def extendExtractor[C](f: (Manifest[C] => Option[ColumnTo[C]])): PartialFunction[Manifest[C], Option[ColumnTo[C]]] = {
    case m if 
       // Fixed for Id
       m <:< manifest[Id[Any]] ||
       // Added support for checking Pk
       m <:< manifest[Pk[Any]] => {
      val typeParam = m.typeArguments
        .headOption
        .collect { case m: ClassManifest[_] => m }
        .getOrElse(implicitly[Manifest[Any]])
      f(typeParam.asInstanceOf[Manifest[C]]).map(mapper => ColumnTo.rowToPk(mapper)).asInstanceOf[Option[ColumnTo[C]]]
      // OR: getExtractor(typeParam).map(mapper => ColumnTo.rowToPk(mapper)).asInstanceOf[Option[ColumnTo[C]]]
    }
    case _ if false => None
  }
}
