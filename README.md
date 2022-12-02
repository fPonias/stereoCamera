# stereoCamera
3d camera application

This project has been an ongoing experiment with taking stereoscopic images with various devices.  The most current version of this app is the iOS 3D42.  I have been keeping it updated (last app store submit on 12/22 as new versions of the iPhone force me to keep the device list updated.  

-- iOS --

The iphone version will take realtime pictures or video with the camera pair for the iphone 10 and up.  Anything older than the iphone 10 doesn't allow similtaneous images to be taken thus requiring sequential images to be taken with each camera.  While the iphone 8 and 9 are technically supported I don't have an iphone 8 to test with and the sequential images aren't the best quaility as the time between shots can be up to 300 ms in delay.

This project doesn't require any special setup.  The project should open in XCode as is.  I have last tested this project in XCode 14.  Simply open stereoCamera.xcworkspace in the ios directory directly.  There is a Podfile present but the project does not have any cocoapods dependencies.

-- android --

The android version as I left it a few years ago uses two devices to network over bluetooth or ethernet.  The images are taken at the same time but due to networking latency, hardware timing, and other issues the images are not perfectly in sync.  This was a problem I planned to tackle when suddenly multi-camera iphones started getting released and I abandoned this line of exploration.  

This app requires two android devices with the same app installed to both of them and networked to each other with bluetooth or accessible over the same network.  Out in the wild I would share my network connection with my good phone to an older android.

I actually had a working iOS version as well and you can see remnants of that project in the ios folder.  Notably the shared c++ code is still intact and worked on both platforms for image processing.  My git management wasn't the best and I overwrote that working project though.  

The android part of the project hasn't been built in years.  I suspect it will need some major updates to work with the newest version of Android Studio.

I did some R&D into making a similar simitaneous camera controller for android and was unable to take previews of the multiple back facing cameras.  I attempted this on a Samsung Galaxy S7, Samsung Galaxy S10, and a OnePlus 9.  While it was possible to do this with a front facing and rear facing camera the designers of both devices made it impossible to access the rear facing cameras similtaneously.  I figured adding this functionality would require hacking the phone at the OS level and I didn't have the patience to experiment with that.

On a related note, I was able to plug in a webcam to the USB port on my OnePlus device and get similtaneous cameras that way.  Unfortunatly the demo program that allowed me to access the USB camera was ancient and wouldn't compile without significant updates to the source.
