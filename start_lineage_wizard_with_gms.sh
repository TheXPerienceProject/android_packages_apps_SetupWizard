#!/bin/bash

adb root
wait ${!}
adb shell su -c pm enable com.google.android.setupwizard || true
wait ${!}
if adb shell su -c pm list packages | grep com.android.provision; then
  adb shell pm disable com.android.provision || true
  wait ${!}
fi
adb shell su -c am start org.lineageos.setupwizard/org.lineageos.setupwizard.SetupWizardTestActivity
wait ${!}
sleep 1
adb shell su -c am start com.google.android.setupwizard/com.google.android.setupwizard.SetupWizardTestActivity
