"use strict";
import { NativeModules, AppRegistry } from "react-native";
const AppState = NativeModules.AppState;
const tag = "AsyncJob:";
const jobModule = NativeModules.AsyncJob;

import AbstractAsyncTask from './abstractAsyncTask'
import TaskRegistry from './taskRegistry'


const JobScheduler = {
  NETWORK_TYPE_UNMETERED: jobModule.UNMETERED,
  NETWORK_TYPE_NONE: jobModule.NONE,
  NETWORK_TYPE_ANY: jobModule.ANY,

  scheduleJob: function(taskObj) {
    if(taskObj instanceof AbstractAsyncTask) {
      console.log(taskObj);
      if(TaskRegistry.isRegisteredTask(taskObj.getTaskName())) {
        if(taskObj.getTaskId()) {
          AppState.getCurrentAppState(
          ({ app_state }) => {
            const appActive = app_state == "active";
              jobModule.scheduleAsyncJob(
                taskObj.getTaskId(),
                taskObj.getTaskName(),
                taskObj.getTimeout(),
                taskObj.getPeriod(),
                taskObj.isPersistedTask(),
                appActive,
                taskObj.allowInForeground(),
                taskObj.getNetworkPrefrence(),
                taskObj.isChargingNeeded(),
                taskObj.isDeviceIdleNeeded(),
                taskObj.isOneTimeJob(),
                taskObj.getTaskPayload(),
                taskObj.postSchedule
              );
          },
          () => console.err(`${tag} Can't get Current App State`)
        );
      }

        console.log("Done scheduling")
      }else {
        console.error("Task is not registred ", taskObj);
      }
    }else {
      console.error("Not a valid Task Object")
    }
  },
};

module.exports = JobScheduler;
