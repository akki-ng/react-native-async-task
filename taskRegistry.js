"use strict";
import { NativeModules, AppRegistry } from "react-native";

import AbstractAsyncTask from './abstractAsyncTask'

const TaskRegistry = {
  register: function(taskClassRef) {
    if(taskClassRef.prototype instanceof AbstractAsyncTask) {
      var taskUniqueName = taskClassRef.name;
      if(TaskRegistry.registeredTasks[taskUniqueName]) {
        console.log("task is already registered, will update")
      }
      var fn = async (data) => {
        console.log(data);
        var taskClass = TaskRegistry.registeredTasks[data.taskName];
       if(taskClass) {
          var newTaskObj = new taskClass(data);
          newTaskObj.startNow();
        }else {
          console.error(`Task not found with name ${data.taskName}`);
        }
      };

      AppRegistry.registerHeadlessTask(taskClassRef.name, () => fn);
      TaskRegistry.registeredTasks[taskUniqueName] = taskClassRef;
    }else {
      console.error("[" + taskClassRef + "] is not an extension of " + AbstractAsyncTask.name);
    }
  },

  isRegisteredTask: function(taskName){
    var taskClass = TaskRegistry.registeredTasks[taskName];
    if(taskClass) {
      return true;
    }
    return false;
  },

  getRegisteredTask: function(taskName){
    var taskClass = TaskRegistry.registeredTasks[taskName];
    if(taskClass) {
      return taskClass;
    }
    return null;
  }
};

TaskRegistry.registeredTasks = {};

module.exports = TaskRegistry;
