package com.mazhangjing.lab.data

/**
  * Psy4J 数据收集类型转换类
  * @since 1.2.5
  * @note 2019-03-06 撰写本工具类
  */
object DataConvert {
  /**
    * 在作用域范围内自动将各种类型的值转换为 String 值
    * @param in 输入的值
    * @return
    */
  implicit class BoolToStr(in: Boolean) {
    def str: String = if (in) "1" else "0"
    def inSb(implicit sb:StringBuilder): Unit = {
      sb.append(in).append(", ")
    }
    def endLineInSb(implicit sb:StringBuilder): Unit = {
      sb.append(in).append("\n")
    }
  }
  implicit class StrSuper(in: String) {
    def inSb(implicit sb:StringBuilder): Unit = {
      sb.append(in).append(", ")
    }
    def endLineInSb(implicit sb:StringBuilder): Unit = {
      sb.append(in).append("\n")
    }
  }
  implicit class LongToStr(in: Long) {
    def inSb(implicit sb:StringBuilder): Unit = {
      sb.append(in.toString).append(", ")
    }
    def endLineInSb(implicit sb:StringBuilder): Unit = {
      sb.append(in.toString).append("\n")
    }
  }
  implicit class IntToStr(in: Int) {
    def inSb(implicit sb:StringBuilder): Unit = {
      sb.append(in.toString).append(", ")
    }
    def endLineInSb(implicit sb:StringBuilder): Unit = {
      sb.append(in.toString).append("\n")
    }
  }
}
