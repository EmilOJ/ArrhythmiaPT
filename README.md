# ArrhythmiaPT

## Description

This repo contains a complete app for processing and classifying (with support vector machine - SVM) ECG signals recorded with an external device.
Currently only works on offline test signals.

The most interesting signal processing implementations can be found here:

https://github.com/EmilOJ/ArrhythmiaPT/blob/master/app/src/main/java/com/helge/arrhythmiapt/SignalProcessing.java

and includes:
- a complete QRS detection algorithm implementation (Pan-Tompkins)
- various utility functions known from MATLAB such as
  - `svm_classify()` given weights and biases
  - `filtfilt()`
  - `circshift()`
  - `demean()`
  - etc...
  
  
