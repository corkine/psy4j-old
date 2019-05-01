package com.mazhangjing.refactor

import scala.collection.mutable

//v1. v2. v3. v4 Movie
//case class Movie(title:String, var priceCode:Int)

//v5 Movie
case class Movie(title:String, priceIn:Int) {
  private var _price: Price = _

  priceCode_=(priceIn)

  def priceCode_=(in: Int): Unit = {
    _price = in match {
      case Movie.REGULAR =>
        new RegularPrice
      case Movie.NEW_RELEASE =>
        new NewReleasePrice
      case Movie.CHILDREN =>
        new ChildrenPrice
    }
  }
  def priceCode: Int = {
    _price.getPriceCode
  }
  //实现采用多态交给子类完成
  //这种方法虽然在添加 Movie 类型的时候依然需要更改 priceCode_= 的代码
  //但是因为利用了多态，这意味着，如果频繁变动每种不同标签的折扣，换句话说
  //标签的变动相比较标签折扣的变动较小的话，这种拆分 - 多态还是值得的
  val amountAll: Int => Double = _price.amountAll(_)
}

trait Price {
  def getPriceCode: Int
  val amountAll: Int => Double
}

class ChildrenPrice extends Price {
  override def getPriceCode: Int = Movie.CHILDREN
  override val amountAll: Int => Double = delay => {
    var result = 1.5
    if (delay > 3) result += (delay - 3) * 1.5
    result
  }
}

class NewReleasePrice extends Price {
  override def getPriceCode: Int = Movie.NEW_RELEASE
  override val amountAll: Int => Double = _ * 3.0
}

class RegularPrice extends Price {
  override def getPriceCode: Int = Movie.REGULAR
  override val amountAll: Int => Double = delay => {
    var result = 2.0
    if (delay > 2) result += (delay - 2) * 1.5
    result
  }
}

object Movie {
  val CHILDREN = 2
  val REGULAR = 0
  val NEW_RELEASE = 1
}

// v1. v2 Rental
// case class Rental(movie: Movie, daysRented: Int)
// v3. v4 Rental
/*case class Rental(movie: Movie, daysRented: Int) {
  //v4 这里可以看到，这里的 Rental 操纵了属性 Movie 的 PriceCode 属性
  //这是一个选择，我们希望将根据 Movie 类型得到 Amount 的代码放在 Movie 中
  //还是 Rental 中，这取决于我们的程序目的，在这里，因为 Movie 的分类是一个很大的变量
  //而 Rental 则长久不变，因此，考虑到变化分离，我们更希望在之后只用维护 Movie，为
  //其添加新的分类，这样的话，将根据分类计算价格的代码放在 Movie 中更好
  //如果移动的话，很简单，但是，我们还想玩点新的，提高 Movie 的可复用行，将
  //Movie 的变化进一步分离出变化和不变的，很显然，我们不能为 Movie 创建几个根据分类
  //不同的子类，因为 Movie 在运行时不允许转型，但是我们业务或许需要不断调整 Movie 分类
  //因此，考虑为 Movie 提供 Price 类，而为 Price 类提供多态继承能力
  def amountAll: Double = {
    var result = 0.0
    this.movie.priceCode match {
      case Movie.REGULAR =>
        result += 2
        if (this.daysRented > 2)
          result += (this.daysRented - 2) * 1.5
      case Movie.NEW_RELEASE =>
        result += this.daysRented * 3
      case Movie.CHILDREN =>
        result += 1.5
        if (this.daysRented > 3)
          result += (this.daysRented - 3) * 1.5
    }
    result
  }

  val getFrequentRenterPoint: Int = {
    if (this.movie.priceCode == Movie.NEW_RELEASE &&
      this.daysRented > 1) 2 else 1
  }
}*/

// v5 Rental
case class Rental(movie: Movie, daysRented: Int) {
  //老旧的 API 保存，但是使用新的实现
  val amountAll: Double = movie.amountAll(daysRented)
  val getFrequentRenterPoint: Int = {
    if (this.movie.priceCode == Movie.NEW_RELEASE &&
      this.daysRented > 1) 2 else 1
  }
}

case class Customer(name:String) {
  val rentals: mutable.Buffer[Rental] = mutable.Buffer[Rental]()
  def addRental(rental: Rental): Unit = rentals.append(rental)
}

object Store {
  //v1 问题：函数过长，可以抽取功能边界
  def statement(customer: Customer): String = {
    var totalAmount = 0.0
    var frequentRenterPoints = 0
    val elements = customer.rentals
    var result = "Rental Record for " + customer.name + "\n"
    for (i <- elements.indices) {
      var thisAmount = 0.0
      val rental = elements(i)
      rental.movie.priceCode match {
        case Movie.REGULAR =>
          thisAmount += 2
          if (rental.daysRented > 2)
            thisAmount += (rental.daysRented - 2) * 1.5
        case Movie.NEW_RELEASE =>
          thisAmount += rental.daysRented * 3
        case Movie.CHILDREN =>
          thisAmount += 1.5
          if (rental.daysRented > 3)
            thisAmount += (rental.daysRented - 3) * 1.5
      }
      frequentRenterPoints += 1
      if (rental.movie.priceCode == Movie.NEW_RELEASE &&
      rental.daysRented > 1) frequentRenterPoints += 1
      result += "\t" + rental.movie.title + "\t" + thisAmount + "\n"
      totalAmount += thisAmount
    }
    result += "Amount owned is " + totalAmount + "\n"
    result += "You earned " + frequentRenterPoints + " frequent renter points"
    result
  }
}

object Store2 {
  def amountFor(rental: Rental): Double = {
    //对于函数而言，要确定其依赖的不变的量（入参）和改变的值（返回值）
    //对于函数内部的变量重命名是一个好习惯，比如返回值使用 result 表示
    //代码总应该表现自己的目的
    var result = 0.0
    rental.movie.priceCode match {
      case Movie.REGULAR =>
        result += 2
        if (rental.daysRented > 2)
          result += (rental.daysRented - 2) * 1.5
      case Movie.NEW_RELEASE =>
        result += rental.daysRented * 3
      case Movie.CHILDREN =>
        result += 1.5
        if (rental.daysRented > 3)
          result += (rental.daysRented - 3) * 1.5
    }
    result
  }
  //v2 问题：功能总应该和其主体关联
  //虽然我们抽象了函数，但是，函数也会发生爆炸，难以管理
  //面向对象的优势在这里应该被很好利用， amountFor(rental) 应该写成 rental#anmoutAll
  def statement(customer: Customer): String = {
    var totalAmount = 0.0
    var frequentRenterPoints = 0
    val elements = customer.rentals
    var result = "Rental Record for " + customer.name + "\n"
    for (i <- elements.indices) {
      var thisAmount = 0.0
      val rental = elements(i)
      //划分边界，并且抽取功能后的代码
      //优点：功能分割，将变化和不变分离，易于维护和后期扩充
      thisAmount = amountFor(rental)
      frequentRenterPoints += 1
      if (rental.movie.priceCode == Movie.NEW_RELEASE &&
        rental.daysRented > 1) frequentRenterPoints += 1
      result += "\t" + rental.movie.title + "\t" + thisAmount + "\n"
      totalAmount += thisAmount
    }
    result += "Amount owned is " + totalAmount + "\n"
    result += "You earned " + frequentRenterPoints + " frequent renter points"
    result
  }
}

object Store3 {
  def amountFor(rental: Rental): Double = {
    //对于公共函数，如果不想去除 API，那么使用新的实现即可
    rental.amountAll
  }

  //v3 包含了大量的临时变量，即在一个 for 循环中不断变化的值
  //下一个版本和原文有较大出入，其更好的利用了 Scala 的函数式编程，减少了临时变量
  def statement(customer: Customer): String = {
    var totalAmount = 0.0
    var frequentRenterPoints = 0
    val elements = customer.rentals
    var result = "Rental Record for " + customer.name + "\n"
    for (i <- elements.indices) {
      var thisAmount = 0.0
      val rental = elements(i)
      //OOP 风格代码，函数各归其家
      thisAmount = amountFor(rental) //thisAmount = rental.amountAll
      frequentRenterPoints += rental.getFrequentRenterPoint.toInt
      result += "\t" + rental.movie.title + "\t" + thisAmount + "\n"
      totalAmount += thisAmount
    }
    result += "Amount owned is " + totalAmount + "\n"
    result += "You earned " + frequentRenterPoints + " frequent renter points"
    result
  }
}

object Store4 {
  val amountFor: Rental => Double = _.amountAll

  val totalAmount: Customer => Double = customer =>
    customer.rentals.foldLeft(0.0)((sum, r) => sum + r.amountAll)

  val frequentRenterPoints: Customer => Int = customer =>
    customer.rentals.foldLeft(0)((sum, r) => sum + r.getFrequentRenterPoint)

  //这一版已经很好的达成目的了，然而，其实还是有一些可以重构的
  //比如多态和调用，这些重构可能会在程序扩展的时候起到重要作用
  //问题出现在 Rental 的 amountAll 方法上
  def statement(customer: Customer): String = {
    var result = "Rental Record for " + customer.name + "\n"
    customer.rentals.foreach(rental => {
      result += "\t" + rental.movie.title + "\t" + rental.amountAll + "\n"
    })
    result += "Amount owned is " + totalAmount(customer) + "\n"
    result += "You earned " + frequentRenterPoints(customer) + " frequent renter points"
    result
  }

  //作为重构的问题和成果，现在我们可以很轻松的添加打印 HTML 的报表
  def statementForHTML(customer: Customer): String = {
    var result = "<h1>Rental Record for " + customer.name + "</h1>\n<ul>"
    customer.rentals.foreach(rental => {
      result += "<li>" + rental.movie.title + ", " + rental.amountAll + "</li>\n"
    })
    result += "</ul><p>Amount owned is " + totalAmount(customer) + "</p>\n"
    result += "<p>You earned " + frequentRenterPoints(customer) + " frequent renter points</p>"
    result
  }
}

object Store5 {

  val totalAmount: Customer => Double = customer =>
    customer.rentals.foldLeft(0.0)((sum, r) => sum + r.amountAll)

  val frequentRenterPoints: Customer => Int = customer =>
    customer.rentals.foldLeft(0)((sum, r) => sum + r.getFrequentRenterPoint)

  //这一版已经很好的达成目的了，然而，其实还是有一些可以重构的
  //比如多态和调用，这些重构可能会在程序扩展的时候起到重要作用
  //问题出现在 Rental 的 amountAll 方法上
  def statement(customer: Customer): String = {
    var result = "Rental Record for " + customer.name + "\n"
    customer.rentals.foreach(rental => {
      result += "\t" + rental.movie.title + "\t" + rental.amountAll + "\n"
    })
    result += "Amount owned is " + totalAmount(customer) + "\n"
    result += "You earned " + frequentRenterPoints(customer) + " frequent renter points"
    result
  }

  //作为重构的问题和成果，现在我们可以很轻松的添加打印 HTML 的报表
  def statementForHTML(customer: Customer): String = {
    var result = "<h1>Rental Record for " + customer.name + "</h1>\n<ul>"
    customer.rentals.foreach(rental => {
      result += "<li>" + rental.movie.title + ", " + rental.amountAll + "</li>\n"
    })
    result += "</ul><p>Amount owned is " + totalAmount(customer) + "</p>\n"
    result += "<p>You earned " + frequentRenterPoints(customer) + " frequent renter points</p>"
    result
  }
}