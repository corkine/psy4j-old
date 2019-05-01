package com.mazhangjing.lab.sound

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import javax.sound.sampled.{AudioFileFormat, AudioFormat, AudioInputStream, AudioSystem}

trait AudioFunction {
  var stopPlayMarker: Boolean
  var stopRecordMarker: Boolean
  def playSound(audioInputStream: AudioInputStream): Unit
  def playSound(file: File): Unit = {
    val audioInputStream = AudioSystem.getAudioInputStream(file)
    playSound(audioInputStream)
  }
  def playSound(implicit audioMaker: AudioMaker, audioFormat: AudioFormat, millSeconds: Int, freq: Double, volume: Double): Unit = {
    val bytes = audioMaker.makeTone(audioFormat, millSeconds, freq, volume)
    val stream = new AudioInputStream(new ByteArrayInputStream(bytes), audioFormat, bytes.length / audioFormat.getFrameSize)
    playSound(stream)
  }
  def recordToFile(audioFormat: AudioFormat, audioFileFormatType: AudioFileFormat.Type, file: File): Unit = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    recordToStream(audioFormat, byteArrayOutputStream)
    val data = byteArrayOutputStream.toByteArray
    val inpStream = new ByteArrayInputStream(data)
    AudioSystem.write(
      new AudioInputStream(inpStream, audioFormat, data.length/audioFormat.getFrameSize),
      audioFileFormatType , file)
    inpStream.close()
    byteArrayOutputStream.close()
  }
  def recordToStream(audioFormat: AudioFormat, byteArrayOutputStream: ByteArrayOutputStream): Unit
}
