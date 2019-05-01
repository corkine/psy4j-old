package com.mazhangjing.lab.sound

import javax.sound.sampled.AudioFormat

trait AudioMaker {
  def makeTone(audioFormat: AudioFormat, millSeconds: Int, freq: Double, volume: Double): Array[Byte]
}