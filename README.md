ChromecastDemoStreaming
================================
Example app for [Google Cast](https://developers.google.com/cast/) on Android and a associated [Chromecast](http://chromecast.com) receiver device. The Android app is located in the _ChromecastDemoStreaming_ folder and the receiver (HTML/JavaScript sources) in the _receiver_ folder. The app was developed as an example for my employer [inovex](http://www.inovex.de/) to show basic features of the release version of the Google Cast SDK.

Using the App
-------------------------
The App needs a valid APP ID from Google. See [Registration](https://developers.google.com/cast/docs/registration) for more information. Replace the APP_ID placeholder in the [MainActivity class](https://github.com/dbaelz/ChromecastDemoStreaming/blob/master/ChromecastDemoStreaming/src/main/java/de/inovex/chromecast/demostreaming/MainActivity.java#L30) with your APP ID.

Build the Example
------------------
ChromecastDemoPresentation uses Gradle as build tool with the integrated Gradle Wrapper. Use the _build_ task to build the Android app. For more information on Gradle and Android see the [Gradle Plugin User Guide](http://tools.android.com/tech-docs/new-build-system/user-guide).

