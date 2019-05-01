package com.mazhangjing.lab.sound

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import javax.sound.sampled.{AudioFormat, AudioInputStream, AudioSystem}
import scala.collection.mutable
import scala.util.Random

//函数接受的值都是值引用，如果需要在一个线程中的函数根据某个 Marker 进行变化，那么需要使用一个类
//并且将其 Marker 设置为一个类变量，然后这个函数在类空间中访问此类变量以进行实时处理。
class SimpleAudioFunctionMakerToneUtilsImpl extends AudioFunction with AudioMaker with ToneUtils {

  override var stopPlayMarker: Boolean = false

  override var stopRecordMarker: Boolean = false

  override def playSound(audioInputStream: AudioInputStream): Unit = {
    val line = AudioSystem.getSourceDataLine(audioInputStream.getFormat)
    line.open()
    line.start()
    val buffer = new Array[Byte](10000)
    var cnt = 0
    while (cnt != -1 && !stopPlayMarker) {
      cnt = audioInputStream.read(buffer)
      if (cnt > 0) line.write(buffer, 0, cnt)
    }
    line.drain() //当 Line 被覆盖前，其会先试图播放，因此，只用在最后 drain 即可
    line.stop()
    line.close() //Line close 会自动处理 AudioInputStream 的关闭吗？
    audioInputStream.close()
    stopPlayMarker = false
  }

  override def recordToStream(audioFormat: AudioFormat, byteArrayOutputStream: ByteArrayOutputStream): Unit = {
    val line = AudioSystem.getTargetDataLine(audioFormat)
    line.open()
    line.start()
    val buffer = new Array[Byte](10000)
    var cnt = 0
    while (!stopRecordMarker) {
      cnt = line.read(buffer, 0, buffer.length)
      if (cnt > 0) byteArrayOutputStream.write(buffer, 0, cnt)
    }
    val array = byteArrayOutputStream.toByteArray
    val byteArrayInputStream = new ByteArrayInputStream(array)
    byteArrayInputStream.close()
    //byteArrayOutputStream.close()
    line.close()
    stopRecordMarker = false
  }

  override def makeTone(audioFormat: AudioFormat, millSeconds: Int, freq: Double, volume: Double = 16000.0): Array[Byte] = {
    val channels = audioFormat.getChannels
    val rate = audioFormat.getSampleRate.toInt
    val bytesPerSamp = audioFormat.getSampleSizeInBits match {
      case 16 => 2
      case 8 => 1
      case _ => throw new RuntimeException("AudioFormat SampleSizeInBits 参数不对")
    }
    val arraySize = channels match {
      case i => (i * rate * (millSeconds * 1.0 / 1000) * bytesPerSamp).toInt
    }
    val array = new Array[Byte](arraySize)
    val byteBuffer = ByteBuffer.wrap(array)
    val shortBuffer = byteBuffer.asShortBuffer()
    for (i <- 0 until arraySize/bytesPerSamp) {
      val time = i * 1.0/rate
      val sinValue = Math.sin(2 * Math.PI * freq * time)
      shortBuffer.put((volume * sinValue).asInstanceOf[Short])
    }
    array
  }

  var toneSampleRate: Float = 16 * 1000

  override def playForDuration(soundFrequency: Double, soundDurationMs: Int, allDurationMS: Int, spaceMS: Int): Unit = {
    val format = new AudioFormat(toneSampleRate,16,1,true,true)
    val toneData = makeTone(format, soundDurationMs, soundFrequency)
    val emptyData = makeTone(format, spaceMS, soundFrequency, 0)
    val periodMs = soundDurationMs + spaceMS
    val periodData = toneData ++ emptyData
    val period = allDurationMS * 1.0 / periodMs
    val totalLength = (periodData.length * period).toInt
    val array = new Array[Byte](totalLength)
    for (index <- 0 until totalLength) {
      array(index) = periodData(index % periodData.length)
    }
    val byteInStream = new ByteArrayInputStream(array)
    val inpStream = new AudioInputStream(byteInStream, format, array.length/format.getFrameSize)
    playSound(inpStream)
    inpStream.close()
    byteInStream.close()
  }

  override def playForDuration(soundFrequency: Double, soundDurationMs: Int, allDurationMS: Int, randomSpaceFrom: Int, randomSpaceTo: Int): Unit = {
    val format = new AudioFormat(toneSampleRate,16,1,true,true)
    val times = getTimeArray(soundDurationMs, randomSpaceFrom, randomSpaceTo, allDurationMS)
    val allData = mutable.Buffer[Byte]()
    val isSound: Int => Boolean = _ == soundDurationMs
    times.foreach(i => {
      if (isSound(i)) allData ++= makeTone(format, i, soundFrequency)
      else allData ++= makeTone(format, i, soundFrequency, 0)
    })
    val byteInStream = new ByteArrayInputStream(allData.toArray)
    val inpStream = new AudioInputStream(byteInStream, format, allData.length/format.getFrameSize)
    playSound(inpStream)
    inpStream.close()
    byteInStream.close()
  }

  def getTimeArray(sound: Int, randomFrom: Int, randomTo: Int, total: Int): Array[Int] = {
    def getIt: mutable.Buffer[Int] = {
      val random = new Random()
      var currentLength = 0
      val array = mutable.Buffer[Int]()
      var lastRand = -1
      while (currentLength < total) {
        val rand = random.nextInt(randomTo - randomFrom) + randomFrom
        array.append(sound)
        array.append(rand)
        currentLength += sound
        currentLength += rand
        lastRand = rand
      }
      val diff = lastRand - (currentLength - total)
      if (diff > randomFrom) {//如果一个可以处理
      val rev = array.reverse
        rev(0) = diff
        if (rev.sum != total) throw new RuntimeException("总数不对")
        rev.reverse
      } else {//否则使用最后两个
        throw new RuntimeException("总数不对2")
      }
    }
    var res = mutable.Buffer[Int]()
    var notOk = true
    while (notOk) {
      try {
        res = getIt
        notOk = false
      } catch {
        case _: Throwable =>
      }
    }
    res.toArray
  }
}
