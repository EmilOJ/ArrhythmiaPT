# ArrhythmiaPT

## Description

This repo contains a complete app for processing and classifying ECG signals recorded with an external device.
Currently only works on test offline test signals.

The most interesting signal processing implementations can be found here:

https://github.com/EmilOJ/ArrhythmiaPT/blob/master/app/src/main/java/com/helge/arrhythmiapt/SignalProcessing.java

and includes:
- a complete QRS detection algorithm implementation (Pan-Tompkins)
- various utility functions known from matlab such as
  - `filtfilt()`
  - `circshift()`
  - `demean()`
  - etc...
  

  
