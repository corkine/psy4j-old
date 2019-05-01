package com.mazhangjing.lab.sound

import com.mazhangjing.rhythm.help.Utils
import javafx.application.Application
import javafx.scene.input.TransferMode
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage

class AudioDemo extends Application {

  def getRoot: Parent = {
    Utils.getDragPane((board, event) => {
      val files = board.getFiles
      if (files.size() == 1) event.acceptTransferModes(TransferMode.LINK)
    }, (board, event) => {
      if (event.isAccepted) println(board.getFiles)
    })
  }

  val scene = new Scene(getRoot, 600, 400)

  override def start(stage: Stage): Unit = {
    stage.setTitle("Audio Demo")
    stage.setScene(scene)
    stage.show()
  }
}