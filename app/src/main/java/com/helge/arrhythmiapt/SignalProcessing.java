package com.helge.arrhythmiapt;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;


import jwave.Transform;
import jwave.transforms.FastWaveletTransform;
import jwave.transforms.wavelets.biorthogonal.BiOrthogonal35;

public class SignalProcessing extends AppCompatActivity {

    static final String TAG = "SignalProcessing";
    static final int NUMBER_OF_FEATURES = 17;
    static final int FS = 360; // Sample rate in Hz
    static final int SEGMENT_LENGTH = (int) Math.floor((200 / 1000) * FS); // ms (on each side of QRS complex) //burg math.floor
    static final int REFRACTORY_PERIOD = (int) Math.floor((250 / 1000) * FS); // ms
    // Instance variable containing ECG mSignal
    public List<Double> mSignal = new ArrayList<Double>();
    public ArrayList<ArrayList<Double>> mSegments = new ArrayList<ArrayList<Double>>();
    public List<Integer> mQrs;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    //@Override
    public void ReadECG(Context context) throws IOException {

        //The file is saved in the internal storage , and is found as such:
        InputStream is = getResources().openRawResource(R.raw.samples);
        //The file is read:
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] RowData = line.split(",");
                    this.mSignal.add(Double.parseDouble(RowData[2])); //adding mSignal to array
                    //mSignal.add(Double.parseDouble(RowData[1])); //adding time to array??
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        } catch (IOException ex) {
            Toast toast = Toast.makeText(getApplicationContext(), "ERROR: File not read", Toast.LENGTH_SHORT);
            toast.show();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Toast toast = Toast.makeText(getApplicationContext(), "ERROR: File not closed", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }


    private void detect_and_classify() {
        List<Integer> qrsArray = new ArrayList<Integer>();
        ArrayList<ArrayList<Double>> segments; // = new ArrayList<ArrayList<Double>>();
        List<Double> features = new ArrayList<Double>();
        int detected_qrs;
        int classification;


        // only run QRS detection algorithm if mSignal is longer than refractory period
        if (mSignal.size() > REFRACTORY_PERIOD) {

            // Location (array index) of detected QRS. 0 if no QRS detected.
            List<Integer> qrs_detected = detect_qrs();

            if (qrs_detected.size() != 0) {
                // Add detected QRS location to previous ones (need 3 for classification)

                //mQrs.add(qrs_detected);
                //qrsArray.add(qrs_detected);

                qrsArray = qrs_detected;



                // After detecting 3 consequetive QRS complexes
                // if (qrsArray.size() == 3) {
                // Extract ±200 ms mSegments around QRS. This is used for classification.
                segments = segments_around_qrs(qrsArray);

                // Compute features
                features = get_features(segments, qrsArray);

                // Classify with support vector machine
                /*classification = classify_segments(segments, features);*/

                // Save classification and mSignal to database
                /*save_classification(classification);*/

                // TODO: variable 'mSignal' must be emptied like below, so a new QRS detection can be performed,
                // but the mSignal must also be saved to a variable with continous ECG mSignal containing
                // all three QRS complexes can be accessed. This is needed for RR interval computation.

                // Empty mSignal variable
                //this.mSignal = new ArrayList<Double>();


                //}
            }
        }
    }

    private List<Integer> detect_qrs() {



/**
 * Created by Tine on 02-05-2016.
 */
//------------------QRS Detection Function---------------------------------%
//- This is a starting point including the filtering stages for a QRS
//algorithm. The b and a parameters are meant for 4 filtering stages, and
//can be found using the fdatool in Matlab.

//INPUT
// --- ecg - ECG signal for QRS detection
// --- b_low - FIR low pass b filtering coefficients
// --- b_high - FIR high pass b filtering coefficients
// --- b_avg - averageing filter coefficients
// --- delay - delay caused by filters (check with grpdelay function)

        //public void QRS_detection( double[] ecg, double[] b_low, double[] b_high,int b_avg, int delay) { //outputs: qrs, h_thres_array, ecg

        // Important Values
        int window = 2 * FS; // 2 second window
        double h_thresh = 0; // initial value of h_thresh
        double h_thresh_correct = 0.7; // correction value for h_thresh

        // Detecting candidate
        boolean candidate_detected = false;
        int candidate_pos = 0;
        double candidate = 0;     // Candidate value

        //float[] lowPass;
        //float[] highPass;

        double[] rr_tolerance_phys = {60 / 220 * FS, 60 / 40 * FS};
        double[] rr_tolerance = rr_tolerance_phys;

        //ArrayList<Double>[] qrs_loc = new ArrayList<Double>[mSignal.length];

        List<Integer> qrs_loc = new ArrayList<Integer>(Collections.nCopies(mSignal.size(), 0));

        double[] h_thres_array = new double[mSignal.size()]; // TODO
        boolean first_candidate = true;


        //Keeping track of maximum values in last 5 windows
        double[] window_max_buff = new double[5];
        double window_max =0;
        int[] last_qrs = new int[5];

        //int candidate_detected;
        //int candidate_pos;
        //int candidate;
        int time_since_last_qrs;
        int end_cand_search =-1; // TODO: tjek om det passer!

        double rr_cur = 0;
        double rr_last = 0;
        int[] rr = new int[last_qrs.length-1];

        //// Filter Stage
        List<Double> b_low = new ArrayList<>(Arrays.asList(-0.00300068847555824, -0.0888956549729993, -0.00978073251699008, -0.00913537555132255, -0.00348493467952550, 0.00341086079804900, 0.0102865391156571, 0.0156935263250855, 0.0182699445494703, 0.0171711581672353, 0.0120921576301221, 0.00357779560317851, -0.00713041168615247, -0.0180134156879530, -0.0268696487375390, -0.0313147178636851, -0.0295416693658266, -0.0203155162205885, -0.00356416221151197, 0.0196106546763700, 0.0473078208714944, 0.0765698351683510, 0.104067354017840, 0.126566433194386, 0.141348431488521, 0.146497228887632, 0.141348431488521, 0.126566433194386, 0.104067354017840, 0.0765698351683510, 0.0473078208714944, 0.0196106546763700, -0.00356416221151197, -0.0203155162205885, -0.0295416693658266, -0.0313147178636851, -0.0268696487375390, -0.0180134156879530, -0.00713041168615247, 0.00357779560317851, 0.0120921576301221, 0.0171711581672353, 0.0182699445494703, 0.0156935263250855, 0.0102865391156571, 0.00341086079804900, -0.00348493467952550, -0.00913537555132255, -0.00978073251699008, -0.0888956549729993, -0.00300068847555824));
        List<Double> b_high = new ArrayList<>(Arrays.asList(-0.228978661265879, -0.00171102587088224, -0.00170984736925881, -0.00172585023852312, -0.00171670183861103, -0.00173441466404043, -0.00172074947278717, -0.00174642908958524, -0.00172358128257744, -0.00179523693754943, -0.00152325352849759, -0.00174186673973176, -0.00181454686131743, -0.00178950839600038, -0.00181602269748676, -0.00179779361369206, -0.00181709183410123, -0.00179726831399164, -0.00181784648985584, -0.00177254752223322, -0.00182670159370370, -0.00180614699233377, -0.00176688340230918, -0.00178033152841735, -0.00176699315180865, 0.998222620155216, -0.00176699315180865, -0.00178033152841735, -0.00176688340230918, -0.00180614699233377, -0.00182670159370370, -0.00177254752223322, -0.00181784648985584, -0.00179726831399164, -0.00181709183410123, -0.00179779361369206, -0.00181602269748676, -0.00178950839600038, -0.00181454686131743, -0.00174186673973176, -0.00152325352849759, -0.00179523693754943, -0.00172358128257744, -0.00174642908958524, -0.00172074947278717, -0.00173441466404043, -0.00171670183861103, -0.00172585023852312, -0.00170984736925881, -0.00171102587088224, -0.228978661265879));
        List<Double> b_avg = new ArrayList<>(Arrays.asList(0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625));

        // Lowpass Filter
        mSignal = filter(mSignal,b_low);

        // Highpass Filter
        mSignal = filter(mSignal,b_high);

        // Subtract mean
        mSignal = mSignal - mean(mSignal);

        // Absolute
        mSignal = Math.abs(mSignal);

        // Average
        mSignal = filter(mSignal,b_avg);


        // Detection

        // Shift entire signal to compensate for filter delay (specified by 'delay' variable)
        //ecg=circshift(ecg,[0 -round(delay)]);
        // TODO -- LAV OM TIL JAVA




        int i = 1;
        // Loop through entire signal
        while (i < mSignal.size()); i++; {

            // Check for new window max
            if (mSignal.get(i) > window_max) {
                window_max = mSignal.get(i);
            }

            // Candidate QRS value is the maximum value and position from initial high threshold crossing to a refractory period after that

            // Check if refractory period is over
            // (If not, don't check for new QRS)
            time_since_last_qrs =  i - last_qrs[1];

            if (time_since_last_qrs > REFRACTORY_PERIOD || first_candidate) {
                // Check if a candidate QRS was detected
                if (candidate_detected == true) {
                    // if end of candidate search was reached
                    if (i == end_cand_search) {

                        if (time_since_last_qrs < rr_tolerance[1] && !first_candidate) {
                            // Adjust threshold and search again.
                            h_thresh = 1.1 * h_thresh;
                            i = last_qrs[0] + REFRACTORY_PERIOD;

                            candidate_detected = false;
                            candidate_pos = 0;
                            candidate = 0;

                            window_max = 0;
                        } else {
                            rr_cur = Math.abs((last_qrs[0]) - candidate_pos);

                            // Save candidate as new detected QRS peak.
                            //qrs_loc[candidate_pos] = 1;
                            qrs_loc.set(candidate_pos,1);

                            // Save last 5 detected qrs
                            last_qrs = circshift(last_qrs,1);
                            last_qrs[0] = candidate_pos;

                            // Save RR-interval of last 5 qrs and use it to
                            // define search window
                            rr = diff(last_qrs);
                            rr_last = Math.abs(mean(rr(rr != 0)));
                            if (rr_cur < rr_tolerance_phys[1] || rr_cur > rr_tolerance_phys[2]) {
                                rr_tolerance = rr_tolerance_phys;
                            } else {
                                rr_tolerance = {rr_last * 0.5, rr_last * 1.6};
                            }
                        }

                        //disp(rr_tolerance);

                        // Set new max in buffer
                        window_max_buff = circshift(window_max_buff);
                        window_max_buff[1] = window_max;
                        // Update threshold as median of last 5 window_max
                        //(window_max_buff) weighted by threshold correction
                        // factor (thresh_correct)
                        h_thresh = h_thresh_correct * median(window_max_buff(window_max_buff != 0))
                        ;
                        // Reset window_max
                        window_max = 0;


                        // Reset candidate variables
                        candidate_detected = false;
                        candidate_pos = 0;
                        candidate = 0;

                        if (first_candidate == true) {
                            first_candidate = false;
                        }
                    }
                } else if (mSignal.get(i) > candidate) {
                    candidate = mSignal.get(i);
                    candidate_pos = i;
                }

            } else if (time_since_last_qrs > rr_tolerance[2] && !first_candidate) {
                // Adjust threshold and search again.
                h_thresh = 0.9 * h_thresh;
                i = last_qrs[1] + REFRACTORY_PERIOD;

                candidate_detected = false;
                candidate_pos = 0;
                candidate = 0;

                window_max = 0;
            } else {
                // Check if high threshold is surpassed
                if (mSignal.get(i) > h_thresh) {
                    // Make this position the first candidate value
                    //candidate = mSignal[i];
                    candidate = mSignal.get(i);
                    candidate_pos = i;
                    candidate_detected = true;
                    // Set candidate search to refractory period from
                    // current candidate.
                    end_cand_search = i + REFRACTORY_PERIOD;
                }
            }
            h_thres_array[i] = h_thresh;
            i = i + 1;
            return qrs_loc;
        }


        //TODO: if qrs_loc contains no ones - return empty list
    }
    // Final function output


    //public int[] circshift(int[] last_qrs) {
    //    for (int i = 0; i < last_qrs.length-1; i++) {
    //        last_qrs[4-i] = last_qrs[3-i];
    //    }
    //    return last_qrs;
    //}

    public int[] diff(int[] last_qrs) {
        int[] rr = new int[4];
        for (int i = 0; i < last_qrs.length-1; i++) {
            rr[i] = Math.abs(last_qrs[i+1]-last_qrs[i]);
        }
        return rr;
    }
    // has to be sorted before - see MD app Fragment
    public static double median(double[] m) {
        int middle = m.length/2;
        if (m.length%2 == 1) {
            return m[middle];
        } else {
            return (m[middle-1] + m[middle]) / 2.0;
        }
    }

    // http://stackoverflow.com/questions/4191687/how-to-calculate-mean-median-mode-and-range-from-a-set-of-numbers :
    // http://stackoverflow.com/questions/8835464/qrs-detection-in-java-from-ecg-byte-array
    public static double mean(double[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    public int circshift(int[] array, int shift){
        int temp = array[array.length]);
        for (int i = 0; i < array.length-shift;i++){
            array[array.length-i] = array[array.length-i-1];
        }
        array[0] = temp;
        return array;
    }

    public List<Double> filter(List<Double> signal, List<Double> coefficients) {
        List<Double> _signal = signal;
        List<Double> _filtered_signal = new ArrayList<>();
        double lin_sum;
        int filter_order = coefficients.size();
        for (int i = 0; i < filter_order; i++) {
            _signal.add(0.0);
        }

        for (int i = filter_order;i<_signal.size();i++){
            lin_sum = 0;
            for (int j = 0; j < filter_order; j++) {
                lin_sum += coefficients.get(j)* _signal.get(i - j);
            }
            _filtered_signal.add(lin_sum);
        }

        return _filtered_signal;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Sending data", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.helge.arrhythmiapt/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "SignalProcessing Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.helge.arrhythmiapt/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }




    private ArrayList<ArrayList<Double>> segments_around_qrs(List<Integer> qrsArray) {
        // INPUT:
        //      - mQrs:      location in sample number of the detected mQrs complex
        //      - mSignal:   raw mSignal (or filtered mSignal from detect_qrs()?)
        // OUTPUT:
        //      - mSegments:  The three mSegments consisting of ±200 ms around each QRS complex

        ArrayList<ArrayList<Double>> segments = new ArrayList<ArrayList<Double>>();

      //  int total_segments_length = SEGMENT_LENGTH * 2 + 1;
        List<Double> segment;
        double pre_qrs, post_qrs, cur_qrs;



        // TODO: Convert to parallel computing? Iterations do not depend on each other.. except adding them to mSegments list.
        // Loop through all mQrs (except first and last)

        for (int j = 0; j < 3; j++) {
            // Find sample index for segment
            cur_qrs = qrsArray.get(j);
            pre_qrs = cur_qrs - SEGMENT_LENGTH;
            post_qrs = cur_qrs + SEGMENT_LENGTH;

            segment = mSignal.subList(pre_qrs, post_qrs);

            segments.add((ArrayList<Double>) segment);
        }

        return segments;
    }



    private ArrayList<Double> get_features(ArrayList<ArrayList<Double>> segments, List<Integer> qrs) {
        // INPUT:
        //      - mSegments:  Segmented mSignal from segments_around_qrs()
        //      - mQrs:  Segmented mSignal from segments_around_qrs()
        //      - mSignal:  Segmented mSignal from segments_around_qrs()
        // OUPUT:
        //      - features: Computed feature vector


        List<Double> features = new ArrayList<Double>();
        List<Integer> rr_intervals = compute_RR(qrs);

        for (int iSegment = 0; iSegment < segments.size(); iSegment++) {
            // Only use middle segment
            List<Double> segment = segments.get(1);
            double[] segmentArray = asArray(segments.get(iSegment));

            double K = 267; //Estimate since in Song (2005) they have a fs = 360 and K=300
            //We have a fs = 320, 40/360 = 0.111, 0.111*300 is 33.33, 300-33.33 = 266.66

            // Feature 1
            features.add(K / rr_intervals.get(0));
            // Feature 2
            features.add(K / rr_intervals.get(1));

            // Feauture 3-17
            // TODO: Implement wavelet transform from Jwave.
            double[] wavelet_coefficients = new double[segment.size()];
            Transform t = new Transform(new FastWaveletTransform(new BiOrthogonal35()));
            wavelet_coefficients = t.forward(segmentArray);


            // TODO: Set features 3-17 to wavelet coefficients
            //wavelet_coefficients[1];
        }


        return features;
    }

    private List<Integer> compute_RR(List<Integer> qrs) {
        // INPUT:
        //      - qrs:  Segmentsed signal from segments_around_qrs()
        // OUPUT:
        //      - rr_intervals: Computed feature vector
        List<Integer> rr_intervals = new ArrayList<Integer>();

        // TODO: Computer RR-intervals


        return rr_intervals;
    }

    private int classify_segments(ArrayList<ArrayList<Double>> segments, ArrayList<Double> features) {
        // INPUT:
        //      - segments:
        //      - features:
        // OUTPUT:
        //      - segments:  The three segments consisting of ±200 ms around each QRS complex
        int classification;

        // Only use middle segment
        ArrayList<Double> segment = segments.get(1);



        // TODO: Implement SVM classification



        classification  = 0;
        return classification;
    }

    private HashMap<String, double[]> loadSVMStruct() {
        // TODO: Get trained SVM classifier weights (trained in MATLAB)
        ArrayList<Double> svm = new ArrayList<Double>();
        //The file is saved in the internal storage , and is found as such:
        InputStream android = getResources().openRawResource(R.raw.svmAndroid);
        //svmAndroid.model file exported from MATLAB when the right parameters have been saved.
        //The file is read:
        BufferedReader reader = new BufferedReader(new InputStreamReader(android));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] RowData = line.split(" ");
                svm.add(Double.parseDouble(RowData[2])); //insert the right rowData according to the svm from MATLAB
            }
        } catch (IOException ex) {
        } finally {
            try {
                android.close();
            } catch (IOException e) {
            }
        }

        return parameters;
    }

    private void save_classification(int classification) {
        // INPUT:
        //      - classification:    from classify_segments()
        // Saves classification to database

        // TODO: Save classification to database
        // TODO: Save signal segments to database or maybe just QRS locations?

    }

    private double[] asArray(ArrayList<Double> arrayList) {

        double[] doubleArray = new double[arrayList.size()]; //create an array with the size of the mSegments

        for (int i = 0; i < mSegments.size(); i++) {
            doubleArray[i] = arrayList.get(i);
        }
        return doubleArray;
    }

}
