package com.mazhangjing.lab;

import javafx.scene.Scene;
import javafx.stage.Stage;

public interface ExperimentHelper {
    void initStage(Stage stage);
    Experiment getExperiment();
    Scene getScene();
}
