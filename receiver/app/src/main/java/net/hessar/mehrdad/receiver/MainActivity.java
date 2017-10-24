package net.hessar.mehrdad.receiver;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static final int samplingfrequency = 44100;
    static final int sl = 100;
    static final int minBuffSize = AudioRecord.getMinBufferSize(samplingfrequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private AudioRecord audioRecord;
    private boolean isRecording;
    private Integer duration = 4;
    private Double[] samples;
    short [] buffTemp;
    private String outputMessage;
    private int fileNameCounter = 0;
    private String dir;
    private int preambleDuration = 13;


    Handler mHandler;
    private Button recordB;
    private Button stopB;
    private TextView messageT;
    private EditText timeT;

    //These states are for communicating between threads
    static final int RECORDING_STARTED  = 0;
    static final int RECORDING_FINISHED = 1;
    static final int MESSAGE_DECODING   = 2;
    static final int MESSAGE_SHOWING    = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {

                switch (inputMessage.what) {
                    // The decoding is done
                    case RECORDING_STARTED:
                        updateMesasge("Recording Started!!!");
                        break;
                    case RECORDING_FINISHED:
                        updateMesasge("Recording Finished!!!");
                        break;
                    case MESSAGE_DECODING:
                        updateMesasge("Message Decoding!!!");
                        break;
                    case MESSAGE_SHOWING:
                        updateMesasge(outputMessage);
                        break;

                }
            }

        };

        dir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "AcousticTransmitter";

        messageT = (TextView) findViewById(R.id.messageT);
        timeT = (EditText) findViewById(R.id.durationT);
        timeT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                duration = Integer.parseInt(timeT.getText().toString());
                System.out.println("Duration Updated!");
            }
        });



        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingfrequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffSize);
        buffTemp = new short[minBuffSize];
        samples = new Double[duration*samplingfrequency];

        recordB = (Button) findViewById(R.id.recordB);
        recordB.setEnabled(true);
        recordB.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                samples = new Double[duration*samplingfrequency];
                recordB.setEnabled(false);
                stopB.setEnabled(true);


                new Thread(new Runnable() {
                    public void run() {

                        //Record audio to a file
                        audioRecord.startRecording();
                        isRecording = true;
                        int numOfReads;
                        int counter = 0;

                        Message recordingStarted = mHandler.obtainMessage(RECORDING_STARTED);
                        recordingStarted.sendToTarget();

                        while (isRecording)
                        {
                            // TODO clear buffTemp here
                            numOfReads = audioRecord.read(buffTemp, 0, minBuffSize);
                            for (int i=0; i<numOfReads; i++)
                            {
                                if (counter >= samples.length)
                                {
                                    audioRecord.stop();
                                    audioRecord.release();
                                    isRecording = false;
                                    Message recordingFinished = mHandler.obtainMessage(RECORDING_FINISHED);
                                    recordingFinished.sendToTarget();

                                    writeWaveToFile("NewRecorded.wav", samples);

                                    samples = filter(samples);
                                    writeWaveToFile("NewRecorded_Filtered.wav", samples);

                                    Message decodingStarted = mHandler.obtainMessage(MESSAGE_DECODING);
                                    decodingStarted.sendToTarget();

                                    outputMessage = ASKDemodulator(samples, syncFinder(samples), 3, 200);
                                    System.out.println("Binary: " + outputMessage);
                                    outputMessage = messageDecoder(outputMessage);
                                    System.out.println("Message: " + outputMessage);
                                    Message showMessage = mHandler.obtainMessage(MESSAGE_SHOWING);
                                    showMessage.sendToTarget();
                                    break;
                                }
                                else
                                {
                                    samples[counter] = Double.valueOf(buffTemp[i]);
                                    counter++;
                                }
                            }
                        }
                    }
                }).start();
            }
        });

        stopB = (Button) findViewById(R.id.stopB);
//        stopB.setEnabled(false);
        stopB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Reading File!!!!!!!!!!");
                        samples = readWavFile("NewRecorded_LP.wav");
//                        syncFinder(samples);

                        String messaage = ASKDemodulator(samples, 31301, 3, 2200);
                        System.out.println(messaage);
                        System.out.println(messageDecoder(messaage));

                        System.out.println("Filter FINISHED!!!!!!!!!!");



                    }
                }).start();
            }
        });



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Double [] readDataFile (String fileName)
    {
//        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +  File.separator + fileName);
        File file = new File(dir + File.separator + fileName);
        ArrayList<Double> dataArray = new ArrayList<>();
        String line = new String("");
        try {
            BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((line = bf.readLine()) != null)
            {
                dataArray.add(Double.parseDouble(line.replace(" ", "")));
            }

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Double [] result = new Double[dataArray.size()];
        result = dataArray.toArray(result);
//        for (Double test: dataArray)
//        {
//            System.out.println(test);
//        }
//        System.out.println("SIZE:" + dataArray.size());
        return result;
    }

    /**
     * readWavFile is a function that read wave file that was recorded and written by writeWaveToFile function
     * to file.
     * IMP: This function doesn't read a .WAV file!!!
     */
    private Double [] readWavFile (String fileName)
    {
//        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName);
        File file = new File(dir + File.separator + fileName);
        ArrayList<Double> temp = new ArrayList<>();
        String line;
        int i = 0;

        try {
            BufferedReader readBuf = new BufferedReader(new FileReader(file));

            line = readBuf.readLine();
            while (line != null)
            {

                temp.add(Double.parseDouble(line));
                i++;
                line = readBuf.readLine();
            }
//            for (int j=9000; j<10000; j++)
//                System.out.println(result[j]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Double [] result = new Double[temp.size()];
        result = temp.toArray(result);
        return result;
    }

    private void writeWaveToFile (String fileName, Double [] inputData)
    {
//        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
//        File file = new File(dir + File.separator + fileName);
        File file = new File(dir + File.separator + fileName);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, false));

            for (int i=0; i<inputData.length; i++){
                buf.append("" + inputData[i]);
                buf.newLine();
            }

            buf.flush();
            buf.close();

        }
        catch (IOException e) {
                e.printStackTrace();
            }

    }
    private void updateMesasge (String message)
    {
        messageT.setText(message);
    }

    private Double [] bandpassFilter (Double [] data)
    {
        Double [] bpFilter = readDataFile("BP_8500_11500_fs_44100.dat");
        Double [] result = new Double[data.length];

        int j;
        for (int i=0; i<data.length; i++)
        {
            result[i] = 0.0;
            j = 0;
            while((i-j >= 0) && (j < bpFilter.length))
            {
                result[i] = result[i] + data[i-j] * bpFilter[j];
                j++;
            }
        }
        return result;
    }

    private Double [] lowpassFilter (Double [] inputData)
    {
        Double [] lpFilter = readDataFile("LP_2k_4k_fs_44100.dat");
        Double [] result = new Double[inputData.length];

        int j;
        for (int i=0; i<inputData.length; i++)
        {
            result[i] = 0.0;
            j = 0;
            while((i-j >= 0) && (j < lpFilter.length))
            {
                result[i] = result[i] + inputData[i-j] * lpFilter[j];
                j++;
            }
        }
        return result;
    }

    private int syncFinder (Double [] inputData)
    {
        Double [] syncFilter = readDataFile("syncFilter2.dat");
        Double [] corelatedData = new Double[inputData.length];

        System.out.println("FILTER SIZE: " + syncFilter.length);

        int j;
        for (int i=0; i<inputData.length; i++)
        {
            corelatedData[i] = 0.0;
            j = 0;
            while((i-j >= 0) && (j < syncFilter.length))
            {
                corelatedData[i] = corelatedData[i] + inputData[i-j] * syncFilter[syncFilter.length-1 - j];
                j++;
            }
            System.out.println(i);
        }

//        writeWaveToFile("NewRecorded_correlated.wav", corelatedData);
        Double maxData = corelatedData[0];
        int maxIndex = 0;
        for (int k=0; k<corelatedData.length; k++)
        {
            if (corelatedData[k] > maxData)
            {
                maxData = corelatedData[k];
                maxIndex = k;
            }
        }
        System.out.println("SYNC INDEX: " + maxIndex);
        return maxIndex;
    }

    private String ASKDemodulator (Double [] inputData, int startIndex, int delay, int numOfBits)
    {
        String result = new String("");
        Double threshold = 0.0;
        Double temp;
        int peambleDelay = preambleDuration*sl;

        for (int k=0; k<preambleDuration*sl; k++)
        {
            threshold = threshold + inputData[startIndex + delay + k];
        }

        System.out.println("Threshold1: " + threshold.toString());
        threshold = threshold / (preambleDuration + 0.0);
        System.out.println("Threshold2: " + threshold.toString());

        threshold = threshold*0.75;
        System.out.println("Threshold3: " + threshold.toString());
        for (int i=0; i<numOfBits; i++)
        {
            temp = 0.0;
            for (int j=0; j<sl; j++)
            {
                temp = temp + inputData[i*sl + startIndex + delay + j + peambleDelay];
            }
//            System.out.println(temp);
            if (temp > threshold)
                result = result + '1';
            else
                result = result + '0';
        }



        return result;
    }

    private String messageDecoder (String bitString)
    {
        String result = new String("");
        String temp;
        if ((bitString.length()%8) != 0)
            bitString = bitString.substring(0, (bitString.length() / 8) * 8);

        for (int i=0; i<bitString.length()/8; i++)
        {
            temp = bitString.substring(i*8+1, (i+1)*8);
            result = result + (char) Integer.parseInt(temp, 2);
        }

        return result;
    }

    private Double [] filter (Double [] inputData)
    {
        Double [] result;

        result = bandpassFilter(inputData);
        result = arrayAbsolute(result);
        result = lowpassFilter(result);

        return result;
    }

    private Double [] arrayAbsolute (Double [] inputData)
    {
        Double [] result = new Double[inputData.length];

        for (int i=0; i<inputData.length; i++)
            result[i] = Math.abs(inputData[i]);
        return result;
    }

}
