package com.mazhangjing.refactor

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable

class StoreTest extends FlatSpec with Matchers {

  val demoCustomer: Customer = {
    val c = Customer("Corkine")
    c.rentals.appendAll(mutable.Buffer(
      Rental(Movie("复仇者联盟 4", Movie.NEW_RELEASE), 5),
      Rental(Movie("复仇者联盟 3", Movie.REGULAR), 3),
      Rental(Movie("复仇者联盟 2", Movie.REGULAR), 4),
      Rental(Movie("复仇者联盟 1", Movie.CHILDREN), 2),
      Rental(Movie("复仇者联盟 0", Movie.CHILDREN), 1)
    ))
    c
  }

  val result: String = """Rental Record for Corkine
                 |	复仇者联盟 4	15.0
                 |	复仇者联盟 3	3.5
                 |	复仇者联盟 2	5.0
                 |	复仇者联盟 1	1.5
                 |	复仇者联盟 0	1.5
                 |Amount owned is 26.5
                 |You earned 6 frequent renter points""".stripMargin

  "v1" should "have a fixed Statement" in {
    val str = Store.statement(demoCustomer)
    assertResult(str, "重构前后返回值应该等同") {
      result
    }
  }

  "v2" should "have same Statement with v1" in {
    val str = Store2.statement(demoCustomer)
    assert(str == result)
  }

  "v3" should "have same Statement with v1" in {
    assert(Store3.statement(demoCustomer) == result)
  }

  "v4" should "have same Statement with v1" in {
    val res = Store4.statement(demoCustomer)
    println(res)
    assert(res == result)
  }

  it should "work well on HTML Statement" in {
    println(Store4.statementForHTML(demoCustomer))
  }

  "v5" should "have same Stagement with v1" in {
    assert(Store5.statement(demoCustomer) == result)
  }

  "A try" can "not live without catch" in {
    val a = List()
    try a(1) catch {
      case _:Throwable =>
    }
  }

}
