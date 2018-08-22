package arrow.instances

import arrow.Kind
import arrow.core.Eval
import arrow.data.*
import arrow.data.eq.eq
import arrow.extension
import arrow.typeclasses.*

@extension
interface MapKFunctorInstance<K> : Functor<MapKPartialOf<K>> {
  override fun <A, B> Kind<MapKPartialOf<K>, A>.map(f: (A) -> B): MapK<K, B> = fix().map(f)
}

@extension
interface MapKFoldableInstance<K> : Foldable<MapKPartialOf<K>> {

  override fun <A, B> Kind<MapKPartialOf<K>, A>.foldLeft(b: B, f: (B, A) -> B): B = fix().foldLeft(b, f)

  override fun <A, B> Kind<MapKPartialOf<K>, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)
}

@extension
interface MapKTraverseInstance<K> : Traverse<MapKPartialOf<K>>, MapKFoldableInstance<K> {

  override fun <G, A, B> MapKOf<K, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, MapKOf<K, B>> =
    fix().traverse(AP, f)
}

@extension
interface MapKSemigroupInstance<K, A> : Semigroup<MapK<K, A>> {

  fun SG(): Semigroup<A>

  override fun MapK<K, A>.combine(b: MapK<K, A>): MapK<K, A> = with(SG()) {
    if (fix().size < b.fix().size) fix().foldLeft<A>(b.fix()) { my, (k, b) -> my.updated(k, b.maybeCombine(my[k])) }
    else b.fix().foldLeft<A>(fix()) { my, (k, a) -> my.updated(k, a.maybeCombine(my[k])) }
  }

}

@extension
interface MapKMonoidInstance<K, A> : Monoid<MapK<K, A>>, MapKSemigroupInstance<K, A> {

  override fun empty(): MapK<K, A> = emptyMap<K, A>().k()
}

@extension
interface MapKEqInstance<K, A> : Eq<MapK<K, A>> {

  fun EQK(): Eq<K>

  fun EQA(): Eq<A>

  override fun MapK<K, A>.eqv(b: MapK<K, A>): Boolean =
    if (SetK.eq(EQK()).run { keys.k().eqv(b.keys.k()) }) {
      keys.map { key ->
        b[key]?.let {
          EQA().run { getValue(key).eqv(it) }
        } ?: false
      }.fold(true) { b1, b2 -> b1 && b2 }
    } else false

}

@extension
interface MapKShowInstance<K, A> : Show<MapK<K, A>> {
  override fun MapK<K, A>.show(): String =
    toString()
}

class MapKContext<L> : MapKTraverseInstance<L>

class MapKContextPartiallyApplied<L> {
  infix fun <A> extensions(f: MapKContext<L>.() -> A): A =
    f(MapKContext())
}

fun <L> ForMapK(): MapKContextPartiallyApplied<L> =
  MapKContextPartiallyApplied()