package httpApi

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

trait HttpApiServiceGen[F[_, _, _, _, _]] {
  self =>

  def hello(name: String, town: Option[String] = None): F[Person, Nothing, Greeting, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[HttpApiServiceGen[F]] = Transformation.of[HttpApiServiceGen[F]](this)
}

object HttpApiServiceGen extends Service.Mixin[HttpApiServiceGen, HttpApiServiceOperation] {

  val id: ShapeId     = ShapeId("httpApi", "HttpApiService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson()
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[HttpApiServiceOperation, _, _, _, _, _]] = Vector(
    HttpApiServiceOperation.Hello
  )

  def input[I, E, O, SI, SO](op: HttpApiServiceOperation[I, E, O, SI, SO]): I          = op.input
  def ordinal[I, E, O, SI, SO](op: HttpApiServiceOperation[I, E, O, SI, SO]): Int      = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: HttpApiServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing])
      extends HttpApiServiceOperation.Transformed[HttpApiServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: HttpApiServiceGen[HttpApiServiceOperation] = HttpApiServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: HttpApiServiceGen[P], f: PolyFunction5[P, P1]): HttpApiServiceGen[P1] =
    new HttpApiServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[HttpApiServiceOperation, P]): HttpApiServiceGen[P] =
    new HttpApiServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: HttpApiServiceGen[P]): PolyFunction5[HttpApiServiceOperation, P] =
    HttpApiServiceOperation.toPolyFunction(impl)

}

sealed trait HttpApiServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: HttpApiServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[HttpApiServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object HttpApiServiceOperation {

  object reified extends HttpApiServiceGen[HttpApiServiceOperation] {
    def hello(name: String, town: Option[String] = None): Hello = Hello(Person(name, town))
  }
  class Transformed[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: HttpApiServiceGen[P], f: PolyFunction5[P, P1]) extends HttpApiServiceGen[P1] {
    def hello(name: String, town: Option[String] = None): P1[Person, Nothing, Greeting, Nothing, Nothing] =
      f[Person, Nothing, Greeting, Nothing, Nothing](alg.hello(name, town))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: HttpApiServiceGen[P]): PolyFunction5[HttpApiServiceOperation, P] =
    new PolyFunction5[HttpApiServiceOperation, P] {
      def apply[I, E, O, SI, SO](op: HttpApiServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl)
    }
  final case class Hello(input: Person) extends HttpApiServiceOperation[Person, Nothing, Greeting, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HttpApiServiceGen[F]): F[Person, Nothing, Greeting, Nothing, Nothing] =
      impl.hello(input.name, input.town)
    def ordinal: Int                                                                                      = 0
    def endpoint: smithy4s.Endpoint[HttpApiServiceOperation, Person, Nothing, Greeting, Nothing, Nothing] = Hello
  }
  object Hello extends smithy4s.Endpoint[HttpApiServiceOperation, Person, Nothing, Greeting, Nothing, Nothing] {
    val schema: OperationSchema[Person, Nothing, Greeting, Nothing, Nothing] = Schema
      .operation(ShapeId("httpApi", "Hello"))
      .withInput(Person.schema)
      .withOutput(Greeting.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/{name}"), code = 200))
    def wrap(input: Person): Hello = Hello(input)
  }
}
