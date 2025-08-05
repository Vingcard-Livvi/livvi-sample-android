# Livvi Integration Sample Project

This project is a sample of how to integrate the Livvi BLE communication technology with your Android project.

## Vingcard Livvi

Vingcard is the leading provider for access control systems for the hospitality industry. The Livvi system
is an access control system that specializes in the multifamily industry, providing hardware and software
for remotely-managed properties.

If you're interested in learning more, reach out to support.livvi@vingcard.com.

## Getting Started

Since the Livvi platform supports both Livvi and TTLock locks, this sample project offers an implementation
that will work for both types of locks.

This repository doesn't include the Livvi SDK, as it is a proprietary library. To use this sample project, you will need to
obtain the Livvi SDK from Vingcard. The SDK is available as an AAR file **lklib.aar** which must be placed in the
app/libs directory of your Android project.

To be able to scan to locks and unlock them, some files are necessary.

1. Place the provided file **lklib.aar** in app > libs and copy the entire folder **app > libs to your Project > App destination**;
2. Copy the entire folder **app > src > main > java > com > vingcard > livvi > sample > lk, to your Project > App > src folder**;
3. Open the classes copied to your project and fix the Package Structure;
4. On your build.gradle placed at the root of the project(It's NOT the one placed under app folder),
paste the content below or adapt the existing one to avoid problems with version mismatch of Kotlin plugin:

```groovy
buildscript {
    ext {
        kotlin_version = '2.1.0'
    }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '8.12.0' apply false
    id 'com.android.library' version '8.12.0' apply false
    id 'org.jetbrains.kotlin.android' version "$kotlin_version" apply false
    id 'org.jetbrains.kotlin.plugin.compose' version "$kotlin_version" apply false
}

tasks.register('clean', Delete) {
    delete layout.buildDirectory
}
```

5. At build.gradle placed under the app directory add the following:
```groovy
android {
    // ...
    compileOptions {
        // ...
        // Desugaring is needed for Java 8+ APIs used by Livvi
        coreLibraryDesugaringEnabled true
    }
}

dependencies {
    // Livvi Mandatory Dependencies
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.5'

    implementation 'commons-codec:commons-codec:1.19.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2'
    implementation "com.squareup.moshi:moshi-kotlin:1.15.2"
    implementation 'com.squareup.retrofit2:retrofit:3.0.0'
    implementation "com.squareup.retrofit2:converter-moshi:3.0.0"
    implementation fileTree(include: ['*.jar', '*.aar'], dir: 'libs')
    // End of Livvi Mandatory Dependencies
}
```

6. At your AndroidManifest.xml file, make sure you have the permissions:

``` xml
    <!-- Location Setup -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

    <!-- BLE Setup -->
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
```

## Controlling the Locks

1. In order to scan for devices, your class will need to implement the LKScannerProtocol, for example:

```kotlin
class Example: LKScannerProtocol
{
    var _reachableDevices: ReachableDevices = mutableListOf() // Reachable devices obtained via Livvi SDK Scanner
    private lateinit var _livviBLEScanner: LKReachableDevice
}
```

2. You MUST request the instance of the scanner after your applications is fully initialized to avoid any kind
of mistake that may be caused by unavailable sources during the startup time;

```kotlin
class Example: LKScannerProtocol
{
    var _reachableDevices: ReachableDevices = mutableListOf() // Reachable devices obtained via Livvi SDK Scanner
    private lateinit var _livviBLEScanner: LKReachableDevice

    override fun onViewCreated()
    {
        _livviBLEScanner = LKReachableDeviceImpl.INSTANCE
        _livviBLEScanner.subscribe(this)
    }
}
```

3. Before requesting scanner to notify you about new locks, be sure you have requested required permissions:

```
ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH_SCAN, BLUETOOTH_CONNECT
```

4. Finally, you will need to implement the `didUpdateVisible(visibleDevices: List<LKScanResult>)` method,
which will run every time a new device is detected or gets out of reach. It receives an array of `LKScanResult`
containing the visible devices as the scan time.

5. After you make sure your device is reachable, you may use commands. You may check if a door is in the
reachable devices by requesting to the scanner a visible device using the serial or the lockMac:

```kotlin
    scanner.get(serialBase32, lockMac)
```

6. To send the Unlock Command to the locker you must instantiate the `LKUnlockDeviceInteractor` class.

```kotlin
class Example: LKScannerProtocol
{
    var _reachableDevices: ReachableDevices = mutableListOf() // Reachable devices obtained via Livvi SDK Scanner
    private lateinit var _livviBLEScanner: LKReachableDevice
    private lateinit var _unlockInteractor: LKUnlockDeviceInteractor

    override fun onViewCreated()
    {
        _requireBluetoothAndLocationPermissionsIfNecessary()
    }

    fun onPermissionsGranted()
    {
        this._unlockInteractor = LKUnlockDeviceInteractor(context = applicationContext)
        this._scanner = LKScanner(context = applicationContext)
        _scanner.subscribe(this)
    }
}
```

7. To send a Unlock command to any locker you must have the fields: `lockData`, `lockMac`, `serial`, `userKey`.
Some of the fields could be null accordingly to the lock type. The field Serial may come as base64 and/or
base32 encoding depending on how you fetch data from the API.
**By default, the Scanner and Unlock classes uses the serial as Base32.**

8. To send an Unlock command to a locker you simple need to call the function on the UnlockInteractor Class.

```kotlin
_unlockInteractor.unlock(serialBase32 = serialBase32,
            lockMac = lockMac,
            userKey = userKey,
            lockData = lockData) { result ->
        }
``` 

If the door is not in range, you will receive and error informing about it on the result callback.
All possible values of results can be found at LKDeviceUnlockInteractor.kt file.

### Retrieving data from server

Documentation on Livvi server APIs can be found at [https://docs.livvi.vingcard.com](https://docs.livvi.vingcard.com).

Interacting with the Livvi server API should be done as a cloud-to-cloud integration, and never directly from the
end-user device.

To retrieve door keys in order to issue commands to the locks, you should call the
[GET /corp/site/doors](https://docs.livvi.vingcard.com/api-documentation#tag/corporation-controller/get/corp/site/doors)
endpoint and use the `getDoorKeys` parameter as `true`.


## License

**This license is valid for the source-code in this sample repository only.
It does not apply to the Livvi BLE communication frameworks, which are proprietary.**

MIT License

Copyright (c) 2025 Vingcard

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions
of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
