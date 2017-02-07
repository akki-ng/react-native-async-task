"use strict";
import { NativeModules, AppRegistry } from "react-native";
const jobModule = NativeModules.AsyncJob;
/*
  taskDetails = {
    taskId: 10222, // Unique and mandatory
    timeout: {max duration to execute,default 9000ms},
    period: {time to retry task <Android may choose on its own>, default 9000000},
    persist: {boolean, persist for device restart, default: true},
    allowInForeground: {boolean, allow to execute in foregroud, default true},
    networkNeeded: {Type of network needed},
    requiresCharging: {boolean, if charging is needed, default false},
    requiresDeviceIdle: {boolean, if device needs to be idle, default false},
    payLoad: {string data to be reused when onExecute is being called},
    callback: {javascript function which can be used as callback, optional},
    isOneTimeJob: {boolean, is it one time job, default true},
  }
 */

import JobScheduler from './jobScheduler';

export default class AbstractAsyncTask {
  constructor(taskDetails) {
    this.taskDetails = taskDetails;
  }


  queueItNow() {
    // this.validateConfigurations();
  }
};

AbstractAsyncTask.NETWORK_TYPE_UNMETERED = jobModule.UNMETERED;
AbstractAsyncTask.NETWORK_TYPE_NONE = jobModule.NONE;
AbstractAsyncTask.NETWORK_TYPE_ANY = jobModule.ANY;


AbstractAsyncTask.prototype = {

  getTaskName: function() {
    return this.constructor.name;
  },

  getTaskId: function() {
    console.log("getting task id")
    console.log(this)
    console.log(this.taskDetails)
    return (this.taskDetails && this.taskDetails.taskId) ? this.taskDetails.taskId : null;
  },

  getTimeout: function() {
     return (this.taskDetails && this.taskDetails.timeout) ? this.taskDetails.timeout : 9000;
  },

  getPeriod: function() {
    return (this.taskDetails && this.taskDetails.period) ? this.taskDetails.period : 90;
  },

  isPersistedTask: function() {
    return (this.taskDetails && this.taskDetails.persist) ? this.taskDetails.persist : true;
  },

  allowInForeground: function() {
    return (this.taskDetails && this.taskDetails.allowInForeground) ? this.taskDetails.allowInForeground : false;
  },

  getNetworkPrefrence: function() {
    return (this.taskDetails && this.taskDetails.networkNeeded) ? this.taskDetails.networkNeeded : AbstractAsyncTask.NETWORK_TYPE_ANY;
  },

  isChargingNeeded: function() {
    return (this.taskDetails && this.taskDetails.requiresCharging) ? this.taskDetails.requiresCharging : false;
  },

  isDeviceIdleNeeded: function() {
    return (this.taskDetails && this.taskDetails.requiresDeviceIdle) ? this.taskDetails.requiresDeviceIdle : false;
  },

  getTaskPayload: function() {
    return (this.taskDetails && this.taskDetails.payLoad) ? this.taskDetails.payLoad : null;
  },

  getTaskCallback: function() {
    return (this.taskDetails && this.taskDetails.callback) ? this.taskDetails.callback : null;
  },

  isOneTimeJob: function() {
    return (this.taskDetails && this.taskDetails.isOneTimeJob) ? this.taskDetails.isOneTimeJob : true;
  },

  startNow: async function() {
    this._preExecute();
    this._onExecute();
  },

  _preExecute: function() {
    console.log("preExecute the task "+this.getTaskName());
    this.preExecute();
  },

  preExecute: function() {

  },

  _onExecute: function() {
    console.log("Executing the task "+this.getTaskName());
    this.onExecute();
  },

  onExecute: function() {

  },

  _postExecute: function(resultData) {
    console.log("Post execute the task "+this.getTaskName());
    this.postExecute(resultData);
  },

  postExecute: function(resultData) {

  },

  postSchedule: function(status) {
    console.log("Task AAAAAAAAAAAAAAAA background" + this.getTaskName() + " schedule : " + status);
  }
}

