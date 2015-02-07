#!/usr/bin/env bash

#set -x

# $1 project path
# $2 lib-project path
function update_lib_project() {
   local project=$1
   local lib_project=$2
   
   if [ "x$ANDROID_TARGET" == "x" ] ; then
      echo "no ANDROID_TARGET exported."
      return 
      break
      exit 1
   fi

   android update project -p $lib_project -t $ANDROID_TARGET
   # yes, we do not use "android update lib-project ..." cmd
   android update project -p $project -t $ANDROID_TARGET -l $lib_project
   set_as_lib_project $lib_project

   if [ "x$DEBUG_UPDATE_PROJECT" != "x" ] ; then
       cat $project/project.properties
       cat $project/build.xml       

       cat $lib_project/project.properties
       cat $lib_project/build.xml
   fi
} 

# $1 lib-project path
# $2 true of fasle
function set_as_lib_project() {
   local p_file=$1/project.properties
   local lib=$2
   if [ "x$lib" == "x" ] ; then
      lib=true
   fi
   if file_content_match $p_file "[ \t]*android\.library=.*"; then
      local p_bak=${p_file}.bak
      mv $p_file $p_bak
      sed   -e "/^[ \t]*android\.library[ \t]*=[ \t]*.*/ s/^[ \t]*\(android\.library\)[ \t]*=[ \t]*.*/\1=$lib/ " $p_bak > $p_file
   else 
      echo "android.library=$lib" >> $p_file
   fi

   if [ "x$DEBUG_UPDATE_PROJECT" != "x" ] ; then
       cat $p_file
   fi
}

# $1 file
# $2 regrex
function file_content_match() {
   grep "$2" "$1" 
   return $?
}

function wait_for_emulator() {
  local bootanim=""
  local failcounter=0
  until [[ "$bootanim" =~ "stopped" ]]; do
       bootanim=`adb -e shell getprop init.svc.bootanim 2>&1`
       echo "$bootanim"
       if [[ "$bootanim" =~ "not found" ]]; then
           let "failcounter += 1"
           if [[ $failcounter -gt 3 ]]; then
              echo "Failed to start emulator"
              exit 1
           fi
        fi
  sleep 1
  done
  echo "Done"
}

function dump_sys() {
  echo "cpu info: [lscpu]"
  lscpu
  echo "df info: [df]"
  df
  echo "memory info: [free]"
  free
}
