package com.helge.arrhythmiapt;

import android.content.Context;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Doubles;
import com.helge.arrhythmiapt.Models.Arrhythmia;
import com.helge.arrhythmiapt.Models.ECGRecording;
import com.helge.arrhythmiapt.Models.SVMStruct;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jwave.Transform;
import jwave.transforms.AncientEgyptianDecomposition;
import jwave.transforms.FastWaveletTransform;
import jwave.transforms.wavelets.daubechies.Daubechies4;

public class SignalProcessing {

    static final int FS                 = 360; // Sample rate in Hz
    static final int SEGMENT_LENGTH     = (int) Math.floor((200.0 / 1000) * FS);
    static final int REFRACTORY_PERIOD  = (int) Math.floor((250.0 / 1000) * FS);
    private static SVMStruct mSVMStruct_AF;
    final Context mContext;
    public List<Double> mSignal         = new ArrayList<>();
    ECGRecording mECGgRecording;

    public SignalProcessing(Context context) {
        mContext = context;
        mSVMStruct_AF = new SVMStruct(mContext, "af");
    }

    public void readECG() throws IOException {

        //The CSV file is read and converted to a byte array, which can readily be stored in the
        // database as a ParseFile
        InputStream is = mContext.getResources().openRawResource(R.raw.samples);
        byte[] data = ByteStreams.toByteArray(is);
        mECGgRecording = new ECGRecording();
        mECGgRecording.setData(new ParseFile("data.csv", data));

        mECGgRecording.setFs(360);
        mECGgRecording.setDownSamplingRate(5);

        try {
            mECGgRecording.save();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void detect_and_classify() {
        List<Integer> qrs_detected;
        List<List<Double>> segments;
        ArrayList<ArrayList<Double>> all_features;
        List<Integer> qrs_loc;
        List<String> classification;

        qrs_detected = detect_qrs();
        qrs_loc = getQRSLoc(qrs_detected);

        // High-pass filter signal
        filter_signal();

        // Extract +/- 200 ms mSegments around QRS. This is used for classification.
        segments = segments_around_qrs(qrs_loc);

        // Compute features
        all_features = get_features(segments, qrs_loc);

        // Classify with support vector machine
        classification = classify_segments(all_features);


        // Save classification and mSignal to database
        save_classification(classification, qrs_loc);
    }

    private List<Integer> detect_qrs() {
    /* QRS detection function
    This method detects the location of the QRS complexes and returns an array with
    1's at all locations with QRS, and 0's at all other locations.
    */
        mSignal = mECGgRecording.getData();
        List<Double> _signal = new ArrayList<>(mSignal);
        int org_length = _signal.size();

        // Important Values
        int window                  = 2 * FS;   // 2 second window
        double h_thresh             = 0;        // initial value of h_thresh
        double h_thresh_correct     = 0.7;      // correction value for h_thresh

        // Detecting candidate
        boolean candidate_detected  = false;
        int candidate_pos           = 0;
        double candidate            = 0;        // Candidate value

        // Setting the physical tolerance
        double[] rr_tolerance_phys  = new double[2];
        rr_tolerance_phys[0]        = 60.0 / 220 * FS;
        rr_tolerance_phys[1]        = 60.0 / 40 * FS;
        double[] rr_tolerance       = rr_tolerance_phys;

        List<Integer> qrs_loc = new ArrayList<Integer>(Collections.nCopies(_signal.size(), 0));

        double[] h_thres_array      = new double[_signal.size()];
        boolean first_candidate     = true;


        // Keeping track of maximum values in last 5 windows
        double[] window_max_buff    = new double[5];
        double window_max           = 0;
        int[] last_qrs              = new int[5];

        int time_since_last_qrs;
        int end_cand_search         = -1;

        double rr_cur               = 0;
        double rr_last              = 0;
        int[] rr                    = new int[last_qrs.length - 1];

        /* Filter Stage */
        List<Double> b_low = new ArrayList<>(Arrays.asList(-0.00300068847555824, -0.0888956549729993, -0.00978073251699008, -0.00913537555132255, -0.00348493467952550, 0.00341086079804900, 0.0102865391156571, 0.0156935263250855, 0.0182699445494703, 0.0171711581672353, 0.0120921576301221, 0.00357779560317851, -0.00713041168615247, -0.0180134156879530, -0.0268696487375390, -0.0313147178636851, -0.0295416693658266, -0.0203155162205885, -0.00356416221151197, 0.0196106546763700, 0.0473078208714944, 0.0765698351683510, 0.104067354017840, 0.126566433194386, 0.141348431488521, 0.146497228887632, 0.141348431488521, 0.126566433194386, 0.104067354017840, 0.0765698351683510, 0.0473078208714944, 0.0196106546763700, -0.00356416221151197, -0.0203155162205885, -0.0295416693658266, -0.0313147178636851, -0.0268696487375390, -0.0180134156879530, -0.00713041168615247, 0.00357779560317851, 0.0120921576301221, 0.0171711581672353, 0.0182699445494703, 0.0156935263250855, 0.0102865391156571, 0.00341086079804900, -0.00348493467952550, -0.00913537555132255, -0.00978073251699008, -0.0888956549729993, -0.00300068847555824));
        List<Double> b_high = new ArrayList<>(Arrays.asList(-0.228978661265879, -0.00171102587088224, -0.00170984736925881, -0.00172585023852312, -0.00171670183861103, -0.00173441466404043, -0.00172074947278717, -0.00174642908958524, -0.00172358128257744, -0.00179523693754943, -0.00152325352849759, -0.00174186673973176, -0.00181454686131743, -0.00178950839600038, -0.00181602269748676, -0.00179779361369206, -0.00181709183410123, -0.00179726831399164, -0.00181784648985584, -0.00177254752223322, -0.00182670159370370, -0.00180614699233377, -0.00176688340230918, -0.00178033152841735, -0.00176699315180865, 0.998222620155216, -0.00176699315180865, -0.00178033152841735, -0.00176688340230918, -0.00180614699233377, -0.00182670159370370, -0.00177254752223322, -0.00181784648985584, -0.00179726831399164, -0.00181709183410123, -0.00179779361369206, -0.00181602269748676, -0.00178950839600038, -0.00181454686131743, -0.00174186673973176, -0.00152325352849759, -0.00179523693754943, -0.00172358128257744, -0.00174642908958524, -0.00172074947278717, -0.00173441466404043, -0.00171670183861103, -0.00172585023852312, -0.00170984736925881, -0.00171102587088224, -0.228978661265879));
        List<Double> b_avg = new ArrayList<>(Arrays.asList(0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625));

        // Lowpass Filter
        _signal = filter(_signal, b_low);

        // Highpass Filter
        _signal = filter(_signal, b_high);

        // Subtract mean
        _signal = demean(_signal);

        // Absolute
        _signal = abs(_signal);

        // Average
        _signal = filter(_signal, b_avg);

        // Correct for filter delay
        int delay = 59;
        _signal = circshift(_signal,delay);

        /* QRS detection*/
        int i = 0;

        // Loop through entire signal
        while (i < org_length) {

            // Check for new window max
            if (_signal.get(i) > window_max) {
                window_max = _signal.get(i);
            }

            // Candidate QRS value is the maximum value and position from initial high threshold crossing to a refractory period after that
            // Check if refractory period is over (If not, don't check for new QRS)
            time_since_last_qrs = i - last_qrs[0];

            if ((time_since_last_qrs > REFRACTORY_PERIOD) || first_candidate) {
                // Check if a candidate QRS was detected
                if (candidate_detected) {
                    // if end of candidate search was reached
                    if (i == end_cand_search) {

                        if (time_since_last_qrs < rr_tolerance[0] && !first_candidate) {
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
                            qrs_loc.set(candidate_pos, 1);

                            // Save last 5 detected qrs
                            last_qrs = circshift(last_qrs, 1);
                            last_qrs[0] = candidate_pos;

                            // Save RR-interval of last 5 qrs and use it to define search window
                            rr = diff(last_qrs);
                            rr = neglectZeros(rr);
                            rr = abs(rr);
                            rr_last = mean(rr);

                            if (rr_cur < rr_tolerance_phys[0] || rr_cur > rr_tolerance_phys[1]) {
                                rr_tolerance = rr_tolerance_phys;
                            } else {
                                rr_tolerance[0] = rr_last * 0.5;
                                rr_tolerance[1] = rr_last * 1.6;
                            }

                            // Set new max in buffer
                            window_max_buff = circshift(window_max_buff);
                            window_max_buff[0] = window_max;
                            // Update threshold as median of last 5 window_max
                            //(window_max_buff) weighted by threshold correction
                            // factor (thresh_correct)
                            h_thresh = h_thresh_correct * median(neglectZeros(window_max_buff));
                            // Reset window_max
                            window_max = 0;


                            // Reset candidate variables
                            candidate_detected = false;
                            candidate_pos = 0;
                            candidate = 0;

                            if (first_candidate) {
                                first_candidate = false;
                            }
                        }
                    } else if (_signal.get(i) > candidate) {
                        candidate = _signal.get(i);
                        candidate_pos = i;
                    }

                } else if (time_since_last_qrs > rr_tolerance[1] && !first_candidate) {
                    // Adjust threshold and search again.
                    h_thresh = 0.9 * h_thresh;
                    i = last_qrs[0] + REFRACTORY_PERIOD;

                    candidate_detected = false;
                    candidate_pos = 0;
                    candidate = 0;

                    window_max = 0;
                } else {
                    // Check if high threshold is surpassed
                    if (_signal.get(i) > h_thresh) {
                        // Make this position the first candidate value
                        candidate = _signal.get(i);
                        candidate_pos = i;
                        candidate_detected = true;
                        // Set candidate search to refractory period from current candidate.
                        end_cand_search = i + REFRACTORY_PERIOD;
                    }
                    if (first_candidate) {
                        end_cand_search = i + REFRACTORY_PERIOD*4;
                    }
                }
            }

            h_thres_array[i] = h_thresh;

            i = i + 1;
        }
        _signal = _signal;
        return qrs_loc;
    }

    private List<Integer> getQRSLoc(List<Integer> qrs) {
        List<Integer> qrs_loc = new ArrayList<>();
        for (int i = 0; i < qrs.size(); i++) {
            if (qrs.get(i) == 1) {
                qrs_loc.add(i);
            }
        }
        return qrs_loc;
    }
    private void filter_signal() {
        List<Double> a = new ArrayList<>();
        a.add(1.0);
        a.add(-0.97);

        List<Double> b = new ArrayList<>();
        b.add(1.0);
        b.add(-1.0);

        mSignal = filtfilt(mSignal, b, a);
    }

    private List<List<Double>> segments_around_qrs(List<Integer> qrsloc) {
        /*
        INPUT
        qrsloc:   array of QRS complex locations (1=QRS , 0=no QRS)
        OUTPUT
        segments:  consists of +/- 200 ms around each QRS complex
        */
        List<List<Double>> segments = new ArrayList<>();

        List segment;
        int pre_qrs, post_qrs, cur_qrs;


        for (int j = 0; j < qrsloc.size(); j++) {
            // Find sample index for segment
            cur_qrs = qrsloc.get(j);
            if (cur_qrs >= SEGMENT_LENGTH && cur_qrs < cur_qrs + SEGMENT_LENGTH) {
                pre_qrs = cur_qrs - SEGMENT_LENGTH;
                post_qrs = cur_qrs + SEGMENT_LENGTH;

                segment = mSignal.subList(pre_qrs, post_qrs);

                segments.add(segment);
            }
        }
        return segments;
    }


    private ArrayList<ArrayList<Double>> get_features(List<List<Double>> segments, List<Integer> qrs_loc) {
        /* This method
        INPUT
        segments:  segmented mSignal from segments_around_qrs()
        qrs_loc:  segmented mSignal from segments_around_qrs()

        OUTPUT
        all_features: 2D array with computed features
        */
        ArrayList<ArrayList<Double>> all_features = new ArrayList<ArrayList<Double>>();

        ArrayList<Double> features;
        List<Integer> rr_intervals = compute_RR(qrs_loc);

        for (int iSegment = 0; iSegment < segments.size() - 1; iSegment++) {
            features = new ArrayList<>();
            // Only use middle segment
            double[] segmentArray = Doubles.toArray(segments.get(iSegment));

            double K = 300; //Estimate, since in Song (2005) they have a fs = 360 and K=300

            // Feature 1: RR feature
            features.add((double) rr_intervals.get(iSegment + 1));
            // Feature 2: RR feature
            features.add((double) rr_intervals.get(iSegment));

            // Feature 3-17: JWave feature
            // Implement wavelet transform from JWave.
            double[] wavelet_coefficients;
            Transform t = new Transform(new AncientEgyptianDecomposition(new FastWaveletTransform(new Daubechies4())));
            wavelet_coefficients = t.forward(segmentArray);

            // Set features 3-17 to wavelet coefficients
            for (int i = 2; i < 17; i++) {
                features.add(wavelet_coefficients[i-2]);
            }
            all_features.add(features);
        }
        return all_features;
    }

    private List<String> classify_segments(ArrayList<ArrayList<Double>> all_features) {
        /*
        INPUT
        segments:
        features:

        OUTPUT
        segments:  The three segments consisting of +/- 200 ms around each QRS complex
        */
        ArrayList<String> classification = new ArrayList<String>();
        String group_belonging;

        double[] c = new double[all_features.size()];

        // classify each segment
        for (int i = 0; i < all_features.size(); i++) {

            double[] cur_features = new double[17];

            cur_features = Doubles.toArray(all_features.get(i));

            // Estimate degree of belonging to AF group
            c[i] = 0;
            double bias = mSVMStruct_AF.getBias();
            double[] alpha = mSVMStruct_AF.getAlpha();
            double[][] vectors = mSVMStruct_AF.getSupportVectors();
            double[] shift = mSVMStruct_AF.getShift();
            double[] scaleFactor = mSVMStruct_AF.getScaleFactor();



            // Scaling
            for (int ii = 0; ii < all_features.get(0).size(); ii++) {
                cur_features[ii] = scaleFactor[ii] * (cur_features[ii] + shift[ii]);
            }

            // Classification
            for (int ii = 0; ii < mSVMStruct_AF.getNumberOfVectors(); ii++) {
                c[i] += alpha[ii] * innerProduct(vectors[ii], cur_features) + bias;
            }

            // Threshold is signal specific and can only be obtained from using svmclassify
            // in matlab. See article for details.
            double threshold = 0;
            if (c[i] < threshold) {
                group_belonging = "AF";
            } else {
                group_belonging = "N";
            }
            classification.add(group_belonging);
        }

        return classification;
    }

    private void save_classification(List<String> classification, List<Integer> qrs_loc) {
        /* Saves classification to database

        INPUT
        classification:    from classify_segments()
        qrs:               detected qrs locations
        */
        List<Arrhythmia> arrhythmias = extractArrhythmias(classification, qrs_loc);

        ParseObject.saveAllInBackground(arrhythmias);
    }

    private List<Double> filter(List<Double> signal, List<Double> b) {
        List<Double> _signal = signal;
        List<Double> _filtered_signal = new ArrayList<>();
        double lin_sum;
        int filter_order = b.size();
        for (int i = 0; i < filter_order; i++) {
            _signal.add(0,0.0);
        }

        for (int i = filter_order;i<_signal.size();i++){
            lin_sum = 0;
            for (int j = 0; j < filter_order; j++) {
                lin_sum += b.get(j) * _signal.get(i - j);
            }
            _filtered_signal.add(lin_sum);
        }

        return _filtered_signal;
    }

    private List<Double> filtfilt(List<Double> signal, List<Double> b, List<Double> a) {
        List<Double> _signal = new ArrayList<>(signal);

        double lin_sum;
        int b_order = b.size();
        int a_order = a.size();

        for (int times = 0; times < 2; times++) {
            List<Double> _filtered_signal = new ArrayList<>();


            for (int i = 0; i < b_order - 1; i++) {
                _signal.add(0, 0.0);
            }
            for (int i = 0; i < a_order; i++) {
                _filtered_signal.add(0.0);
            }


            for (int i = b_order - 1; i < _signal.size(); i++) {
                lin_sum = 0;
                for (int j = 0; j < b_order; j++) {
                    lin_sum += b.get(j) * _signal.get(i - j);
                }
                for (int j = 1; j < a_order; j++) {
                    lin_sum -= a.get(j) * _filtered_signal.get(i - j);
                }
                _filtered_signal.add(lin_sum);
            }

            Collections.reverse(_filtered_signal);
            _signal = new ArrayList<>(_filtered_signal);
            _signal.remove(_signal.size() - 1);
            _signal.remove(_signal.size() - 1);

        }

        return _signal;
    }
//

    private List<Integer> compute_RR(List<Integer> qrs_loc) {
        /*
        INPUT
        qrs_loc:  qrs locations in samples

        OUTPUT
        rr_intervals: computed RR-intervals in samples
        */
        List<Integer> rr_intervals = new ArrayList<Integer>();

        for (int i = 0; i <= qrs_loc.size() - 2; i++) {
            rr_intervals.add(Math.abs(qrs_loc.get(i + 1) - qrs_loc.get(i)));
        }

        return rr_intervals;
    }



    private double innerProduct(double[] a, double[] b) {

        double product = 0;
        for (int i = 0; i < a.length; i++) {
            product += a[i] * b[i];
        }

        return product;
    }

    private List<Arrhythmia> extractArrhythmias(List<String> detected_arrhythmias, List<Integer> qrs_loc) {

        List<Arrhythmia> arrhythmias = new ArrayList<>();
        List<Integer> cur_arrhythmias = new ArrayList<>();
        String arrhythmia;
        boolean arrhythmia_found = false;
        int i = 0;
        arrhythmia = "";

        while (i < detected_arrhythmias.size()) {
            arrhythmia = detected_arrhythmias.get(i);
            if (!arrhythmia.equals("N")) {
                arrhythmia_found = true;
                cur_arrhythmias.add(i);
            } else if (arrhythmia_found) {
                arrhythmias.add(computeArrhythmiaTimes(cur_arrhythmias, qrs_loc, arrhythmia));
                arrhythmia_found = false;
                cur_arrhythmias.clear();
            }
            i++;
        }
        return arrhythmias;
    }

    private Arrhythmia computeArrhythmiaTimes(List<Integer> arrythmia_index, List<Integer> qrs_loc, String type) {
        int start = qrs_loc.get(arrythmia_index.get(0));
        int stop = qrs_loc.get(arrythmia_index.get(arrythmia_index.size() - 1));
        Arrhythmia a = new Arrhythmia(start, stop);
        a.setRecordingId(mECGgRecording.getObjectId());
        a.setType(type);

        return a;
    }

    public int[] diff(int[] last_qrs) {
        int[] rr = new int[4];
        for (int i = 0; i < last_qrs.length-1; i++) {
            rr[i] = Math.abs(last_qrs[i+1]-last_qrs[i]);
        }
        return rr;
    }

    public int[] neglectZeros(int[] rr) {
        int j = 0;
        for (int k = 0; k < rr.length; k++) {
            if (rr[k] != 0)
                rr[j++] = rr[k];
        }
        int[] newArray = new int[j];
        System.arraycopy(rr, 0, newArray, 0, j);
        return newArray;
    }

    public double[] neglectZeros(double[] array) {
        int j = 0;
        for (int k = 0; k < array.length; k++) {
            if (array[k] != 0)
                array[j++] = array[k];
        }
        double[] newArray = new double[j];
        System.arraycopy(array, 0, newArray, 0, j);
        return newArray;
    }

    public int[] abs(int[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = Math.abs(array[i]);
        }
        return array;
    }

    public List<Double> abs(List<Double> array) {
        List<Double> absArray = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            absArray.add(Math.abs(array.get(i)));
        }
        return absArray;
    }

    public int[] circshift(int[] array, int shift){
        int temp = array[array.length-1];
        for (int i = 0; i <= array.length-shift-1;i++){
            array[array.length-1-i] = array[array.length-i-2];
        }
        array[0] = temp;
        return array;
    }

    public double[] circshift(double[] array){
        double temp = array[array.length-1];
        for (int i = 0; i < array.length-1;i++){
            array[array.length-1-i] = array[array.length-i-2];
        }
        array[0] = temp;
        return array;
    }

    public List<Double> circshift(List<Double> array, int shift){
        double[] temp = new double[shift];
        for (int i = 0; i < shift; i ++) {
            double value = array.get(i);
            temp[i] = value;
        }
        for (int i = shift; i < array.size()-1;i++){
            array.set(i-shift, array.get(i));
        }
        for (int i = 0; i < temp.length - 1; i++) {
            array.set(array.size() - shift + i, temp[i]);
        }

        return array;
    }
    // TODO: check sorting is correct...
    public static double median(double[] m) {
        int middle = m.length / 2;
        java.util.Arrays.sort(m);
        if (m.length % 2 == 1) {
            return m[middle];
        } else {
            return (m[middle - 1] + m[middle]) / 2.0;
        }
    }

    public static double mean(int[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    public static double mean(List<Double> m) {
        double sum = 0;
        for (int i = 0; i < m.size(); i++) {
            sum += m.get(i);
        }
        return sum / m.size();
    }

    public static List<Double> demean(List<Double> m) {
        double mMean = mean(m);
        List<Double> dSignal = new ArrayList<>();
        for (int i = 0; i < m.size(); i++) {
            dSignal.add(m.get(i) - mMean);
        }
        return dSignal;
    }
}

