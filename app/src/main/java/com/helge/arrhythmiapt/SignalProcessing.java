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

    static final String TAG = "SignalProcessing";
    static final int NUMBER_OF_FEATURES = 17;
    static final int FS = 360; // Sample rate in Hz
    static final int SEGMENT_LENGTH = (int) Math.floor((200.0 / 1000) * FS);
    static final int REFRACTORY_PERIOD = (int) Math.floor((250.0 / 1000) * FS);
    private static SVMStruct mSVMStruct_VT;
    private static SVMStruct mSVMStruct_AF;
    private static SVMStruct mSVMStruct_N;
    final Context mContext;
    public List<Double> mSignal = new ArrayList<>();
    public ArrayList<ArrayList<Double>> mSegments = new ArrayList<>();
    ECGRecording mECGgRecording;

    public SignalProcessing(Context context) {
        mContext = context;
        mSVMStruct_VT = new SVMStruct(mContext, "vt");
        mSVMStruct_AF = new SVMStruct(mContext, "af");
        mSVMStruct_N = new SVMStruct(mContext, "n");
    }

    // TODO: check sorting is correct
    public static double median(double[] m) {
        int middle = m.length / 2;
        java.util.Arrays.sort(m);
        if (m.length % 2 == 1) {
            return m[middle];
        } else {
            return (m[middle - 1] + m[middle]) / 2.0;
        }
    }

    // http://stackoverflow.com/questions/4191687/how-to-calculate-mean-median-mode-and-range-from-a-set-of-numbers :
    // http://stackoverflow.com/questions/8835464/qrs-detection-in-java-from-ecg-byte-array
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
    // Final function output


    //public int[] circshift(int[] last_qrs) {
    //    for (int i = 0; i < last_qrs.length-1; i++) {
    //        last_qrs[4-i] = last_qrs[3-i];
    //    }
    //    return last_qrs;
    //}

    public static List<Double> demean(List<Double> m) {
        double mMean = mean(m);
        List<Double> dSignal = new ArrayList<>();
        for (int i = 0; i < m.size(); i++) {
            dSignal.add(m.get(i) - mMean);
        }
        return dSignal;
    }

    public void readECG() throws IOException {

        //The file is saved in the internal storage , and is found as such:
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
        double[] features;
        ArrayList<ArrayList<Double>> all_features;
        int detected_qrs;
        List<Integer> qrs_loc;
        List<String> classification;

        qrs_detected = detect_qrs();
        qrs_loc = getQrsLoc(qrs_detected);
        // High-pass filter signal
        filter_signal();

        // Extract ±200 ms mSegments around QRS. This is used for classification.
        segments = segments_around_qrs(qrs_loc);

        // Compute features
        all_features = get_features(segments, qrs_loc);

        // Classify with support vector machine
        classification = classify_segments(all_features);

        // Save classification and mSignal to database
        save_classification(classification, qrs_loc);

        // TODO: variable 'mSignal' must be emptied like below, so a new QRS detection can be performed,
        // but the mSignal must also be saved to a variable with continuous ECG mSignal containing
        // all three QRS complexes can be accessed. This is needed for RR interval computation.

        // Empty mSignal variable
        //this.mSignal = new ArrayList<Double>();


        //}

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
// --- b_avg - averaging filter coefficients
// --- delay - delay caused by filters (check with grpdelay function)

        //public void QRS_detection( double[] ecg, double[] b_low, double[] b_high,int b_avg, int delay) { //outputs: qrs, h_thres_array, ecg

        mSignal = mECGgRecording.getData();
        int org_length = mSignal.size();
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

        double[] rr_tolerance_phys = new double[2];
        rr_tolerance_phys[0] = 60.0 / 220 * FS;
        rr_tolerance_phys[1] = 60.0 / 40 * FS;
        double[] rr_tolerance = rr_tolerance_phys;

        //ArrayList<Double>[] qrs_loc = new ArrayList<Double>[mSignal.length];

        List<Integer> qrs_loc = new ArrayList<Integer>(Collections.nCopies(mSignal.size(), 0));

        double[] h_thres_array = new double[mSignal.size()];
        boolean first_candidate = true;


        //Keeping track of maximum values in last 5 windows
        double[] window_max_buff = new double[5];
        double window_max = 0;
        int[] last_qrs = new int[5];

        int time_since_last_qrs;
        int end_cand_search = -1;

        double rr_cur = 0;
        double rr_last = 0;
        int[] rr = new int[last_qrs.length - 1];

        //// Filter Stage
        List<Double> b_low = new ArrayList<>(Arrays.asList(-0.00300068847555824, -0.0888956549729993, -0.00978073251699008, -0.00913537555132255, -0.00348493467952550, 0.00341086079804900, 0.0102865391156571, 0.0156935263250855, 0.0182699445494703, 0.0171711581672353, 0.0120921576301221, 0.00357779560317851, -0.00713041168615247, -0.0180134156879530, -0.0268696487375390, -0.0313147178636851, -0.0295416693658266, -0.0203155162205885, -0.00356416221151197, 0.0196106546763700, 0.0473078208714944, 0.0765698351683510, 0.104067354017840, 0.126566433194386, 0.141348431488521, 0.146497228887632, 0.141348431488521, 0.126566433194386, 0.104067354017840, 0.0765698351683510, 0.0473078208714944, 0.0196106546763700, -0.00356416221151197, -0.0203155162205885, -0.0295416693658266, -0.0313147178636851, -0.0268696487375390, -0.0180134156879530, -0.00713041168615247, 0.00357779560317851, 0.0120921576301221, 0.0171711581672353, 0.0182699445494703, 0.0156935263250855, 0.0102865391156571, 0.00341086079804900, -0.00348493467952550, -0.00913537555132255, -0.00978073251699008, -0.0888956549729993, -0.00300068847555824));
        List<Double> b_high = new ArrayList<>(Arrays.asList(-0.228978661265879, -0.00171102587088224, -0.00170984736925881, -0.00172585023852312, -0.00171670183861103, -0.00173441466404043, -0.00172074947278717, -0.00174642908958524, -0.00172358128257744, -0.00179523693754943, -0.00152325352849759, -0.00174186673973176, -0.00181454686131743, -0.00178950839600038, -0.00181602269748676, -0.00179779361369206, -0.00181709183410123, -0.00179726831399164, -0.00181784648985584, -0.00177254752223322, -0.00182670159370370, -0.00180614699233377, -0.00176688340230918, -0.00178033152841735, -0.00176699315180865, 0.998222620155216, -0.00176699315180865, -0.00178033152841735, -0.00176688340230918, -0.00180614699233377, -0.00182670159370370, -0.00177254752223322, -0.00181784648985584, -0.00179726831399164, -0.00181709183410123, -0.00179779361369206, -0.00181602269748676, -0.00178950839600038, -0.00181454686131743, -0.00174186673973176, -0.00152325352849759, -0.00179523693754943, -0.00172358128257744, -0.00174642908958524, -0.00172074947278717, -0.00173441466404043, -0.00171670183861103, -0.00172585023852312, -0.00170984736925881, -0.00171102587088224, -0.228978661265879));
        List<Double> b_avg = new ArrayList<>(Arrays.asList(0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625, 0.0625));

        // Lowpass Filter
        mSignal = filter(mSignal, b_low);

        // Highpass Filter
        mSignal = filter(mSignal, b_high);

        // Subtract mean
        mSignal = demean(mSignal);

        // Absolute
        mSignal = abs(mSignal);

        // Average
        mSignal = filter(mSignal, b_avg);

        // Correct for filter delay
        int delay = 59;
        mSignal = circshift(mSignal,delay);

        /* Detection*/
        int i = 0;
        // Loop through entire signal
        while (i < org_length) {

            // Check for new window max
            if (mSignal.get(i) > window_max) {
                window_max = mSignal.get(i);
            }

            // Candidate QRS value is the maximum value and position from initial high threshold crossing to a refractory period after that

            // Check if refractory period is over
            // (If not, don't check for new QRS)
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

                            // Save RR-interval of last 5 qrs and use it to
                            // define search window
                            rr = diff(last_qrs);
                            rr = neglectZeros(rr);
                            rr = abs(rr);
                            rr_last = mean(rr);

                            if (rr_cur < rr_tolerance_phys[0] || rr_cur > rr_tolerance_phys[1]) {
                                rr_tolerance = rr_tolerance_phys;
                            } else {
                                rr_tolerance[0] = rr_last * 0.1;
                                rr_tolerance[1] = rr_last * 2;
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
                    } else if (mSignal.get(i) > candidate) {
                        candidate = mSignal.get(i);
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
                    if (first_candidate) {
                        end_cand_search = i + REFRACTORY_PERIOD*4;
                    }
                }
            }

            h_thres_array[i] = h_thresh;

            i = i + 1;
        }
        // let us print all the elements available in list
        for (Double number : h_thres_array) {
            System.out.println("H_thres_array = " + number);
        }

        return qrs_loc;

        //TODO: if qrs_loc contains no ones - return empty list
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
        for (int i = 0; i < array.length-shift-1;i++){
            array[array.length-1-i] = array[array.length-i-2];
        }
        array[0] = temp;
        return array;
    }

    public double[] circshift(double[] array){
        double temp = array[array.length-1];
        for (int i = 0; i < array.length-2;i++){
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
        for (int i = 0; i < array.size()-shift-1;i++){
            array.set(array.size()-1-i,array.get(array.size()-i-2));
        }
        for (int i = 0; i < temp.length; i++) {
            array.add(temp[i]);
        }

        return array;
    }

    public List<Double> filter(List<Double> signal, List<Double> b) {
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

    public List<Double> filtfilt(List<Double> signal, List<Double> b, List<Double> a) {
        List<Double> _signal = signal;

        double lin_sum;
        int b_order = b.size();
        int a_order = a.size();

        for (int times = 0; times < 2; times++) {
            List<Double> _filtered_signal = new ArrayList<>();

            for (int i = 0; i < b_order; i++) {
                _signal.add(0, 0.0);
            }
            for (int i = 0; i < a_order; i++) {
                _filtered_signal.add(0.0);
            }


            for (int i = b_order; i < _signal.size(); i++) {
                lin_sum = 0;
                for (int j = 0; j < b_order; j++) {
                    lin_sum += b.get(j) * _signal.get(i - j);
                }
                for (int j = 1; j < a_order; j++) {
                    try {
                        lin_sum -= a.get(j) * _filtered_signal.get(i - j);
                    } catch (Exception e) {
                        Log.d("Test", "test");
                    }


                }
                _filtered_signal.add(lin_sum);
            }

            Collections.reverse(_filtered_signal);
            _signal = _filtered_signal;

        }

        return _signal;
    }



    private List<List<Double>> segments_around_qrs(List<Integer> qrsloc) {
        // INPUT:
        //      - mSignal:   raw mSignal (or filtered mSignal from detect_qrs()?)
        // OUTPUT:
        //      - mSegments:  mSegments consisting of ±200 ms around each QRS complex

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


//
private ArrayList<ArrayList<Double>> get_features(List<List<Double>> segments, List<Integer> qrs_loc) {
        // INPUT:
        //      - mSegments:  Segmented mSignal from segments_around_qrs()
        //      - mQrs:  Segmented mSignal from segments_around_qrs()
        //      - mSignal:  Segmented mSignal from segments_around_qrs()
        // OUTPUT:
        //      - features: Computed feature vector

    ArrayList<ArrayList<Double>> all_features = new ArrayList<ArrayList<Double>>();
    //double[] features = new double[NUMBER_OF_FEATURES];

    ArrayList<Double> features = new ArrayList<>();
    List<Integer> rr_intervals = compute_RR(qrs_loc);

        for (int iSegment = 1; iSegment < segments.size() - 1; iSegment++) {
            // Only use middle segment
            List<Double> segment = segments.get(iSegment);
            double[] segmentArray = Doubles.toArray(segments.get(iSegment));

            double K = 300; //Estimate since in Song (2005) they have a fs = 360 and K=300

            // Feature 1
            features.add(K / rr_intervals.get(iSegment - 1)); //TODO: iSegment ??
            // Feature 2
            features.add(K / rr_intervals.get(iSegment)); //TODO: iSegment + 1 ??

            // Feature 3-17
            // Implement wavelet transform from Jwave.
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

    private List<Integer> compute_RR(List<Integer> qrs_loc) {
        // INPUT:
        //      - qrs_loc:  qrs locations in samples
        // OUTPUT:
        //      - rr_intervals: computed RR-intervals in samples
        List<Integer> rr_intervals = new ArrayList<Integer>();

        for (int i = 0; i <= qrs_loc.size() - 2; i++) {
            rr_intervals.add(Math.abs(qrs_loc.get(i + 1) - qrs_loc.get(i)));
        }

        return rr_intervals;
    }

    private List<String> classify_segments(ArrayList<ArrayList<Double>> all_features) {
        // INPUT:
        //      - segments:
        //      - features:
        // OUTPUT:
        //      - segments:  The three segments consisting of ±200 ms around each QRS complex

        ArrayList<String> classification = new ArrayList<String>();
        String group_belonging;


        // classify each segment
        for (int i = 0; i < all_features.size(); i++) {

            double[] cur_features = new double[17];

            cur_features = Doubles.toArray(all_features.get(i));

            // Estimate degree of belonging to VT group
            int c1 = 0;
            double bias1 = mSVMStruct_VT.getBias();
            double[] alpha1 = mSVMStruct_VT.getAlpha();
            double[][] vectors1 = mSVMStruct_VT.getSupportVectors();
            for (int ii = 0; ii < mSVMStruct_VT.getNumberOfVectors(); ii++) {
                c1 += alpha1[ii] * innerProduct(vectors1[ii], cur_features) + bias1;
            }

            // Estimate degree of belonging to AF group
            int c2 = 0;
            double bias2 = mSVMStruct_AF.getBias();
            double[] alpha2 = mSVMStruct_AF.getAlpha();
            double[][] vectors2 = mSVMStruct_AF.getSupportVectors();
            for (int ii = 0; ii < mSVMStruct_AF.getNumberOfVectors(); ii++) {
                c2 += alpha2[ii] * innerProduct(vectors2[ii], cur_features) + bias2;
            }

            // Estimate degree of belonging to VT group
            int c3 = 0;
            double bias3 = mSVMStruct_N.getBias();
            double[] alpha3 = mSVMStruct_N.getAlpha();
            double[][] vectors3 = mSVMStruct_N.getSupportVectors();
            for (int ii = 0; ii < mSVMStruct_N.getNumberOfVectors(); ii++) {
                c3 += alpha3[ii] * innerProduct(vectors3[ii], cur_features) + bias3;
            }

            if (c3 > c1 && c3 > c2) {
                group_belonging = "N";
            } else if (c2 > c1 && c2 > c3) {
                group_belonging = "AF";
            } else {
                group_belonging = "VT";
            }

            classification.add(group_belonging);


        }

        return classification;
    }

    private double innerProduct(double[] a, double[] b) {

        double product = 0;
        for (int i = 0; i < a.length - 1; i++) {
            product += a[i] * b[i];
        }

        return product;
    }


    private void save_classification(List<String> classification, List<Integer> qrs_loc) {
        // INPUT:
        //      - classification:    from classify_segments()
        //      - qrs:               detected qrs locations
        // Saves classification to database

        List<Arrhythmia> arrhythmias = extractArrhythmias(classification, qrs_loc);

        ParseObject.saveAllInBackground(arrhythmias);

        //TODO: Save signal segments to database or maybe just QRS locations?

    }

    private double[] asArray(ArrayList<Double> arrayList) {

        double[] doubleArray = new double[arrayList.size()]; //create an array with the size of the mSegments

        for (int i = 0; i < mSegments.size(); i++) {
            doubleArray[i] = arrayList.get(i);
        }
        return doubleArray;
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

    private Arrhythmia computeArrhythmiaTimes(List<Integer> arrythmia_index, List<Integer> qrs_loc, String type) {
        int start = qrs_loc.get(arrythmia_index.get(0));
        int stop = qrs_loc.get(arrythmia_index.get(arrythmia_index.size() - 1));
        Arrhythmia a = new Arrhythmia(start, stop);
        a.setRecordingId(mECGgRecording.getObjectId());
        a.setType(type);

        return a;
    }

    private List<Arrhythmia> extractArrhythmias(List<String> detected_arrhythmias, List<Integer> qrs_loc) {
        List<Arrhythmia> arrhythmias = new ArrayList<>();
        List<Integer> cur_arrhythmias = new ArrayList<>();
        String arrhythmia;

        boolean arrhythmia_found = true;
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
        if (cur_arrhythmias.size() > 0) {
            arrhythmias.add(computeArrhythmiaTimes(cur_arrhythmias, qrs_loc, arrhythmia));
        }

        return arrhythmias;
    }

    private List<Integer> getQrsLoc(List<Integer> qrs) {
        List<Integer> qrs_loc = new ArrayList<>();
        for (int i = 0; i < qrs.size(); i++) {
            if (qrs.get(i) == 1) {
                qrs_loc.add(i);
            }
        }

        return qrs_loc;
    }



}
