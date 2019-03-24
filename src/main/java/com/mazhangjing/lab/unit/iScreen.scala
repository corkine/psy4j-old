package com.mazhangjing.lab.unit

case class iScreen(var screenPX: Double, var  screenCM: Double, var personToScreenCM: Double) {
  def init(screenPX: Double, screenCM: Double, personToScreenCM: Double): iScreen = {
    this.screenPX = screenPX
    this.screenCM = screenCM
    this.personToScreenCM = personToScreenCM
    this
  }
  def pxInScreenToDeg(objectPX:Double):Double = {
    math.atan((screenCM * objectPX) /(screenPX * personToScreenCM)) * (180/math.Pi)
  }

  def degToScreenPx(objectDeg:Double):Double = {
    math.tan(math.Pi * objectDeg / 180) * (screenPX * personToScreenCM / screenCM)
  }
}