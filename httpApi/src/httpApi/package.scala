package object httpApi {
  type HttpApiService[F[_]] = smithy4s.kinds.FunctorAlgebra[HttpApiServiceGen, F]
  val HttpApiService = HttpApiServiceGen

}
