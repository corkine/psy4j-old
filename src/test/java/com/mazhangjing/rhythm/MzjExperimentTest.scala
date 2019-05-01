package com.mazhangjing.rhythm

import com.mazhangjing.lab.sound.{SimpleAudioFunctionMakerToneUtilsImpl, SimpleLongToneUtilsImpl, SimpleShortToneUtilsImpl, ToneUtils}
import org.scalatest.FunSuite

class MzjExperimentTest extends FunSuite {

  object TestIt {
    var SOUND_IMPL_CLASS: Class[_] = _
    def getSoundImpl: ToneUtils = {
      if (SOUND_IMPL_CLASS != null) {
        SOUND_IMPL_CLASS.newInstance().asInstanceOf[ToneUtils]
      } else throw new RuntimeException("没有可选的 SoundImpl 实现")
    }
  }

  test("testSOUND_IMPL_CLASS") {
    TestIt.SOUND_IMPL_CLASS = classOf[SimpleLongToneUtilsImpl]
    val impl = TestIt.getSoundImpl
    impl.playForDuration(300,30,2000,100)
  }

  test("testSOUND_IMPL_CLASS2") {
    TestIt.SOUND_IMPL_CLASS = classOf[SimpleShortToneUtilsImpl]
    val impl = TestIt.getSoundImpl
    impl.playForDuration(300,30,2000,100)
  }

  test("testSOUND_IMPL_CLASS3") {
    TestIt.SOUND_IMPL_CLASS = classOf[SimpleAudioFunctionMakerToneUtilsImpl]
    val impl = TestIt.getSoundImpl
    impl.playForDuration(300,30,2000,100)
  }

}
