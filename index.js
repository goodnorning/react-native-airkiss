'use strict';

import { DeviceEventEmitter, NativeModules, Platform } from 'react-native';
let Airkiss = NativeModules.Airkiss;

class Airkiss {

  start(ssid,password,callback1,callback2) {
    Airkiss.start(ssid,password,(ret)=>{
      callback1(ret)
    },ret=>{
      callback2(ret)
    })
  }
  stop(){
    Airkiss.stop()
  }

  
}

module.exports = Airkiss;
