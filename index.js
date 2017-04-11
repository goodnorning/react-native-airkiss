'use strict';

import { DeviceEventEmitter, NativeModules, Platform } from 'react-native';
var airkiss = NativeModules.Airkiss;

class Airkiss {

  start(ssid,password,callback) {
    airkiss.start(ssid,password,(ret)=>{
      callback(ret)
    })
  }
  startGetDeviceInfo(callback) {
    airkiss.startGetDeviceInfo(ret=>{
      callback(ret)
    })
  }
  stop(){
    airkiss.stop()
  }

  
}

module.exports = Airkiss;
