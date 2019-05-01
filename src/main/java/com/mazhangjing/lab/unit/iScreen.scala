package com.mazhangjing.lab.unit

/**
  * 2019年04月05日 修正了度数计算为视角一半的问题
  * @param screenPX 屏幕长边尺寸
  * @param screenCM 屏幕长边像素
  * @param personToScreenCM 人距离屏幕距离
  */
case class iScreen(var screenPX: Double, var  screenCM: Double, var personToScreenCM: Double) {
  def init(screenPX: Double, screenCM: Double, personToScreenCM: Double): iScreen = {
    this.screenPX = screenPX
    this.screenCM = screenCM
    this.personToScreenCM = personToScreenCM
    this
  }
  def pxInScreenToDeg(objectPX:Double):Double = {
    (math.atan((screenCM * (objectPX / 2)) /(screenPX * personToScreenCM)) * (180/math.Pi)) * 2
  }

  def degToScreenPx(objectDeg:Double):Double = {
    math.tan(math.Pi * (objectDeg / 2) / 180) * (screenPX * personToScreenCM / screenCM) * 2
  }
}