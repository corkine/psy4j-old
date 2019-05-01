package com.mazhangjing.lab.sound;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Date;

public class AudioSynth01 extends JFrame{

    private AudioFormat audioFormat;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;

    private static float sampleRate = 16000.0F;
    private static int channels = 1;
    //Allowable true,false

    //A buffer to hold two seconds monaural and one
    // second stereo data at 16000 samp/sec for
    // 16-bit samples
    private byte[] audioData = new byte[16000 * 4];

    private final JButton generateBtn = new JButton("Generate");
    private final JButton playOrFileBtn = new JButton("Play/File");
    private final JLabel elapsedTimeMeter = new JLabel("0000");

    private final JRadioButton tones = new JRadioButton("Tones",true);
    private final JRadioButton stereoPanning = new JRadioButton("Stereo Panning");
    private final JRadioButton stereoPingpong = new JRadioButton("Stereo Pingpong");
    private final JRadioButton fmSweep = new JRadioButton("FM Sweep");
    private final JRadioButton decayPulse = new JRadioButton("Decay Pulse");
    private final JRadioButton echoPulse = new JRadioButton("Echo Pulse");
    private final JRadioButton waWaPulse = new JRadioButton("WaWa Pulse");

    private final JRadioButton listen = new JRadioButton("Listen",true);
    private final JTextField fileName = new JTextField("junk",10);

    public static void main(String[] args){
        new AudioSynth01();
    }

    public AudioSynth01(){
        final JPanel controlButtonPanel = new JPanel();
        controlButtonPanel.setBorder(BorderFactory.createEtchedBorder());

        final JPanel synButtonPanel = new JPanel();
        final ButtonGroup synButtonGroup = new ButtonGroup();
        final JPanel centerPanel = new JPanel();

        final JPanel outputButtonPanel = new JPanel();
        outputButtonPanel.setBorder(BorderFactory.createEtchedBorder());
        final ButtonGroup outputButtonGroup = new ButtonGroup();

        playOrFileBtn.setEnabled(false);

        generateBtn.addActionListener(e -> {
            playOrFileBtn.setEnabled(false);
            new SynGen().getSyntheticData(audioData);
            playOrFileBtn.setEnabled(true);
        });

        playOrFileBtn.addActionListener(e -> playOrFileData());

        controlButtonPanel.add(generateBtn);
        controlButtonPanel.add(playOrFileBtn);
        controlButtonPanel.add(elapsedTimeMeter);

        synButtonGroup.add(tones);
        synButtonGroup.add(stereoPanning);
        synButtonGroup.add(stereoPingpong);
        synButtonGroup.add(fmSweep);
        synButtonGroup.add(decayPulse);
        synButtonGroup.add(echoPulse);
        synButtonGroup.add(waWaPulse);

        synButtonPanel.setLayout(new GridLayout(0,1));
        synButtonPanel.add(tones);
        synButtonPanel.add(stereoPanning);
        synButtonPanel.add(stereoPingpong);
        synButtonPanel.add(fmSweep);
        synButtonPanel.add(decayPulse);
        synButtonPanel.add(echoPulse);
        synButtonPanel.add(waWaPulse);

        centerPanel.add(synButtonPanel);

        outputButtonGroup.add(listen);
        JRadioButton file = new JRadioButton("File");
        outputButtonGroup.add(file);

        outputButtonPanel.add(listen);
        outputButtonPanel.add(file);
        outputButtonPanel.add(fileName);

        getContentPane().add(controlButtonPanel,BorderLayout.NORTH);
        getContentPane().add(centerPanel, BorderLayout.CENTER);
        getContentPane().add(outputButtonPanel, BorderLayout.SOUTH);

        setTitle("Copyright 2003, R.G.Baldwin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(250,275);
        setVisible(true);
    }

    private void playOrFileData() {
        try{
            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);

            boolean bigEndian = true;
            boolean signed = true;
            //Allowable 8000,11025,16000,22050,44100
            int sampleSizeInBits = 16;
            audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
            audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length/audioFormat.getFrameSize());
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

            if(listen.isSelected()){
                new ListenThread().start();
            } else{
                generateBtn.setEnabled(false);
                playOrFileBtn.setEnabled(false);
                try{
                    AudioSystem.write(audioInputStream, AudioFileFormat.Type.AU, new File(fileName.getText() + ".au"));
                }catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
                generateBtn.setEnabled(true);
                playOrFileBtn.setEnabled(true);
            }
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    class ListenThread extends Thread{
        byte[] playBuffer = new byte[16384];

        public void run(){
            try{
                generateBtn.setEnabled(false);
                playOrFileBtn.setEnabled(false);

                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

                int cnt;
                long startTime = new Date().getTime();

                while((cnt = audioInputStream.read(playBuffer, 0, playBuffer.length)) != -1){
                    if(cnt > 0){
                        sourceDataLine.write(playBuffer, 0, cnt);
                    }
                }

                sourceDataLine.drain();
                int elapsedTime = (int)(new Date().getTime() - startTime);
                elapsedTimeMeter.setText("" + elapsedTime);

                sourceDataLine.stop();
                sourceDataLine.close();

                generateBtn.setEnabled(true);
                playOrFileBtn.setEnabled(true);
            }catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    class SynGen{
        ByteBuffer byteBuffer;
        ShortBuffer shortBuffer;
        int byteLength;

        void getSyntheticData(byte[] synDataBuffer){
            byteBuffer = ByteBuffer.wrap(synDataBuffer);
            shortBuffer = byteBuffer.asShortBuffer();

            byteLength = synDataBuffer.length;

            if(tones.isSelected()) tones();
            if(stereoPanning.isSelected())
                stereoPanning();
            if(stereoPingpong.isSelected())
                stereoPingpong();
            if(fmSweep.isSelected()) fmSweep();
            if(decayPulse.isSelected()) decayPulse();
            if(echoPulse.isSelected()) echoPulse();
            if(waWaPulse.isSelected()) waWaPulse();

        }

        void tones(){
            channels = 1;//Java allows 1 or 2
            //Each channel requires two 8-bit bytes per
            // 16-bit sample.
            int bytesPerSamp = 2;
            sampleRate = 16000.0F;
            // Allowable 8000,11025,16000,22050,44100
            int sampLength = byteLength/bytesPerSamp;
            for(int cnt = 0; cnt < sampLength; cnt++){
                double time = cnt/sampleRate;
                double freq = 950.0;//arbitrary frequency
                double sinValue =
                        (Math.sin(2*Math.PI*freq*time));
                System.out.println("Time" + time + ", Value" + sinValue);
                shortBuffer.put((short)(16000*sinValue));
            }//end for loop
        }//end method tones
        //-------------------------------------------//

        //This method generates a stereo speaker sweep,
        // starting with a relatively high frequency
        // tone on the left speaker and moving across
        // to a lower frequency tone on the right
        // speaker.
        void stereoPanning(){
            channels = 2;//Java allows 1 or 2
            int bytesPerSamp = 4;//Based on channels
            sampleRate = 16000.0F;
            // Allowable 8000,11025,16000,22050,44100
            int sampLength = byteLength/bytesPerSamp;
            for(int cnt = 0; cnt < sampLength; cnt++){
                //Calculate time-varying gain for each
                // speaker
                double rightGain = 16000.0*cnt/sampLength;
                double leftGain = 16000.0 - rightGain;

                double time = cnt/sampleRate;
                double freq = 600;//An arbitrary frequency
                //Generate data for left speaker
                double sinValue =
                        Math.sin(2*Math.PI*(freq)*time);
                shortBuffer.put(
                        (short)(leftGain*sinValue));
                //Generate data for right speaker
                sinValue =
                        Math.sin(2*Math.PI*(freq*0.8)*time);
                shortBuffer.put(
                        (short)(rightGain*sinValue));
            }//end for loop
        }//end method stereoPanning
        //-------------------------------------------//

        //This method uses stereo to switch a sound
        // back and forth between the left and right
        // speakers at a rate of about eight switches
        // per second.  On my system, this is a much
        // better demonstration of the sound separation
        // between the two speakers than is the
        // demonstration produced by the stereoPanning
        // method.  Note also that because the sounds
        // are at different frequencies, the sound
        // produced is similar to that of U.S.
        // emergency vehicles.

        void stereoPingpong(){
            channels = 2;//Java allows 1 or 2
            int bytesPerSamp = 4;//Based on channels
            sampleRate = 16000.0F;
            // Allowable 8000,11025,16000,22050,44100
            int sampLength = byteLength/bytesPerSamp;
            double leftGain = 0.0;
            double rightGain = 16000.0;
            for(int cnt = 0; cnt < sampLength; cnt++){
                //Calculate time-varying gain for each
                // speaker
                if(cnt % (sampLength/8) == 0){
                    //swap gain values
                    double temp = leftGain;
                    leftGain = rightGain;
                    rightGain = temp;
                }//end if

                double time = cnt/sampleRate;
                double freq = 600;//An arbitrary frequency
                //Generate data for left speaker
                double sinValue =
                        Math.sin(2*Math.PI*(freq)*time);
                shortBuffer.put(
                        (short)(leftGain*sinValue));
                //Generate data for right speaker
                sinValue =
                        Math.sin(2*Math.PI*(freq*0.8)*time);
                shortBuffer.put(
                        (short)(rightGain*sinValue));
            }//end for loop
        }//end stereoPingpong method
        //-------------------------------------------//

        //This method generates a monaural linear
        // frequency sweep from 100 Hz to 1000Hz.
        void fmSweep(){
            channels = 1;//Java allows 1 or 2
            int bytesPerSamp = 2;//Based on channels
            sampleRate = 16000.0F;
            // Allowable 8000,11025,16000,22050,44100
            int sampLength = byteLength/bytesPerSamp;
            double lowFreq = 100.0;
            double highFreq = 1000.0;

            for(int cnt = 0; cnt < sampLength; cnt++){
                double time = cnt/sampleRate;

                double freq = lowFreq +
                        cnt*(highFreq-lowFreq)/sampLength;
                double sinValue =
                        Math.sin(2*Math.PI*freq*time);
                shortBuffer.put((short)(16000*sinValue));
            }//end for loop
        }//end method fmSweep
        //-------------------------------------------//

        //This method generates a monaural triple-
        // frequency pulse that decays in a linear
        // fashion with time.
        void decayPulse(){
            channels = 1;//Java allows 1 or 2
            int bytesPerSamp = 2;//Based on channels
            sampleRate = 16000.0F;
            // Allowable 8000,11025,16000,22050,44100
            int sampLength = byteLength/bytesPerSamp;
            for(int cnt = 0; cnt < sampLength; cnt++){
                //The value of scale controls the rate of
                // decay - large scale, fast decay.
                double scale = 2*cnt;
                if(scale > sampLength) scale = sampLength;
                double gain =
                        16000*(sampLength-scale)/sampLength;
                double time = cnt/sampleRate;
                double freq = 499.0;//an arbitrary freq
                double sinValue =
                        (Math.sin(2*Math.PI*freq*time) +
                                Math.sin(2*Math.PI*(freq/1.8)*time) +
                                Math.sin(2*Math.PI*(freq/1.5)*time))/3.0;
                shortBuffer.put((short)(gain*sinValue));
            }//end for loop
        }//end method decayPulse
        //-------------------------------------------//

        //This method generates a monaural triple-
        // frequency pulse that decays in a linear
        // fashion with time.  However, three echoes
        // can be heard over time with the amplitude
        // of the echoes also decreasing with time.
        void echoPulse(){
            channels = 1;//Java allows 1 or 2
            int bytesPerSamp = 2;//Based on channels
            sampleRate = 16000.0F;
            // Allowable 8000,11025,16000,22050,44100
            int sampLength = byteLength/bytesPerSamp;
            int cnt2 = -8000;
            int cnt3 = -16000;
            int cnt4 = -24000;
            for(int cnt1 = 0; cnt1 < sampLength;
                cnt1++,cnt2++,cnt3++,cnt4++){
                double val = echoPulseHelper(
                        cnt1,sampLength);
                if(cnt2 > 0){
                    val += 0.7 * echoPulseHelper(
                            cnt2,sampLength);
                }//end if
                if(cnt3 > 0){
                    val += 0.49 * echoPulseHelper(
                            cnt3,sampLength);
                }//end if
                if(cnt4 > 0){
                    val += 0.34 * echoPulseHelper(
                            cnt4,sampLength);
                }//end if

                shortBuffer.put((short)val);
            }//end for loop
        }//end method echoPulse
        //-------------------------------------------//

        double echoPulseHelper(int cnt,int sampLength){
            //The value of scale controls the rate of
            // decay - large scale, fast decay.
            double scale = 2*cnt;
            if(scale > sampLength) scale = sampLength;
            double gain =
                    16000*(sampLength-scale)/sampLength;
            double time = cnt/sampleRate;
            double freq = 499.0;//an arbitrary freq
            double sinValue =
                    (Math.sin(2*Math.PI*freq*time) +
                            Math.sin(2*Math.PI*(freq/1.8)*time) +
                            Math.sin(2*Math.PI*(freq/1.5)*time))/3.0;
            return(short)(gain*sinValue);
        }//end echoPulseHelper

        //-------------------------------------------//

        //This method generates a monaural triple-
        // frequency pulse that decays in a linear
        // fashion with time.  However, three echoes
        // can be heard over time with the amplitude
        // of the echoes also decreasing with time.
        //Note that this method is identical to the
        // method named echoPulse, except that the
        // algebraic sign was switched on the amplitude
        // of two of the echoes before adding them to
        // the composite synthetic signal.  This
        // resulted in a difference in the
        // sound.
        void waWaPulse(){
            channels = 1;//Java allows 1 or 2
            int bytesPerSamp = 2;//Based on channels
            sampleRate = 16000.0F;
            // Allowable 8000,11025,16000,22050,44100
            int sampLength = byteLength/bytesPerSamp;
            int cnt2 = -8000;
            int cnt3 = -16000;
            int cnt4 = -24000;
            for(int cnt1 = 0; cnt1 < sampLength;
                cnt1++,cnt2++,cnt3++,cnt4++){
                double val = waWaPulseHelper(
                        cnt1,sampLength);
                if(cnt2 > 0){
                    val += -0.7 * waWaPulseHelper(
                            cnt2,sampLength);
                }//end if
                if(cnt3 > 0){
                    val += 0.49 * waWaPulseHelper(
                            cnt3,sampLength);
                }//end if
                if(cnt4 > 0){
                    val += -0.34 * waWaPulseHelper(
                            cnt4,sampLength);
                }//end if

                shortBuffer.put((short)val);
            }//end for loop
        }//end method waWaPulse
        //-------------------------------------------//

        double waWaPulseHelper(int cnt,int sampLength){
            //The value of scale controls the rate of
            // decay - large scale, fast decay.
            double scale = 2*cnt;
            if(scale > sampLength) scale = sampLength;
            double gain =
                    16000*(sampLength-scale)/sampLength;
            double time = cnt/sampleRate;
            double freq = 499.0;//an arbitrary freq
            double sinValue =
                    (Math.sin(2*Math.PI*freq*time) +
                            Math.sin(2*Math.PI*(freq/1.8)*time) +
                            Math.sin(2*Math.PI*(freq/1.5)*time))/3.0;
            return(short)(gain*sinValue);
        }
    }
}