import Flutter
import UIKit
import CoreMotion
import CoreLocation


public class SwiftActivityRecognitionFlutterPlugin: NSObject, FlutterPlugin,CLLocationManagerDelegate {

    public var globalChannel:FlutterMethodChannel
    private let activityManager = CMMotionActivityManager()
    let locationManager = CLLocationManager()

    private var requestLocationAuthorizationCallback: ((CLAuthorizationStatus) -> Void)?

    var backgroundTask: UIBackgroundTaskIdentifier = .invalid

   func registerBackgroundTask() {
     backgroundTask = UIApplication.shared.beginBackgroundTask { [weak self] in
       self?.endBackgroundTask()
     }
     assert(backgroundTask != .invalid)
   }
     
   func endBackgroundTask() {
     UIApplication.shared.endBackgroundTask(backgroundTask)
     backgroundTask = .invalid
   }
    
    init(channel:FlutterMethodChannel) {
        self.globalChannel = channel
        

    }
    

  public static func register(with registrar: FlutterPluginRegistrar) {
    //let handler = ActivityStreamHandler()
//    let channel = FlutterEventChannel(name: "activity_recognition_flutter", binaryMessenger: registrar.messenger())
    let methodChannel = FlutterMethodChannel(name: "activity_recognition_flutter_ios", binaryMessenger: registrar.messenger())
    let instance = SwiftActivityRecognitionFlutterPlugin(channel: methodChannel)

    registrar.addMethodCallDelegate(instance, channel: methodChannel)

   // channel.setStreamHandler(handler)
  }
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      if (call.method == "getPlatformVersion") {
          result("iOS " + UIDevice.current.systemVersion)
      }else if (call.method == "start"){
        self.locationManager.delegate = self
        self.registerBackgroundTask()
        self.requestLocationAuthorization()
        self.locationManager.allowsBackgroundLocationUpdates = true
        //1
        //self.locationManager.pausesLocationUpdatesAutomatically = true
        //TODO self.locationManager.pausesLocationUpdatesAutomatically = true
        //self.locationManager.startMonitoringVisits()
        //8
        //2
        self.locationManager.activityType = .fitness
        //3
        //self.locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        //6
        if #available(iOS 11.0, *) {
            self.locationManager.showsBackgroundLocationIndicator = false
        }
        
        //self.locationManager.startUpdatingLocation()
        self.locationManager.startMonitoringSignificantLocationChanges()


        //4
//        activityManager.startActivityUpdates(to: OperationQueue.main) { (activity) in
//            if let a = activity {
//
//                let type = self.extractActivityType(a: a)
//                let confidence = self.extractActivityConfidence(a: a)
//                let data = "\(type),\(confidence)"
//
//                /// Send event to flutter
//                self.globalChannel.invokeMethod("g_data", arguments: data)
//
//
//            }
//        }
      }
    }
    
    public func requestLocationAuthorization() {
        let currentStatus = CLLocationManager.authorizationStatus()

        // Only ask authorization if it was never asked before
        guard currentStatus == .notDetermined else { return }

        // Starting on iOS 13.4.0, to get .authorizedAlways permission, you need to
        // first ask for WhenInUse permission, then ask for Always permission to
        // get to a second system alert
        if #available(iOS 13.4, *) {
            self.requestLocationAuthorizationCallback = { status in
                if status == .authorizedWhenInUse {
                    self.locationManager.requestAlwaysAuthorization()
                }
            }
            self.locationManager.requestWhenInUseAuthorization()
        } else {
            self.locationManager.requestAlwaysAuthorization()
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        
        //TODO we can save data in some field and sned it in update location
        activityManager.startActivityUpdates(to: OperationQueue.main) { (activity) in
            if let a = activity {

                let type = self.extractActivityType(a: a)
                let confidence = self.extractActivityConfidence(a: a)
                let data = "\(type),\(confidence)"

                /// Send event to flutter
                self.globalChannel.invokeMethod("g_data", arguments: data)

                
            }
        }
    }
    
    
    // MARK: - CLLocationManagerDelegate
    public func locationManager(_ manager: CLLocationManager,
                                didChangeAuthorization status: CLAuthorizationStatus) {
        self.requestLocationAuthorizationCallback?(status)
    }
    //5
//    public func locationManagerDidPauseLocationUpdates(_ manager: CLLocationManager) {
//        self.locationManager.startMonitoringVisits()
//        self.locationManager.startMonitoringSignificantLocationChanges()
//        self.locationManager.startUpdatingLocation()
//
//        activityManager.startActivityUpdates(to: OperationQueue.main) { (activity) in
//            if let a = activity {
//
//                let type = self.extractActivityType(a: a)
//                let confidence = self.extractActivityConfidence(a: a)
//                let data = "\(type),\(confidence)"
//
//                /// Send event to flutter
//                self.globalChannel.invokeMethod("g_data", arguments: data)
//
//
//            }
//        }
//    }
    //7
//    public func locationManagerDidResumeLocationUpdates(_ manager: CLLocationManager) {
//        self.locationManager.startMonitoringVisits()
//        self.locationManager.startMonitoringSignificantLocationChanges()
//        self.locationManager.startUpdatingLocation()
//
//        activityManager.startActivityUpdates(to: OperationQueue.main) { (activity) in
//            if let a = activity {
//
//                let type = self.extractActivityType(a: a)
//                let confidence = self.extractActivityConfidence(a: a)
//                let data = "\(type),\(confidence)"
//
//                /// Send event to flutter
//                self.globalChannel.invokeMethod("g_data", arguments: data)
//
//
//            }
//        }
//    }

    
    
    
    func extractActivityType(a: CMMotionActivity) -> String {
      var type = "UNKNOWN"
      switch true {
      case a.stationary:
          type = "STILL"
      case a.walking:
          type = "WALKING"
      case a.running:
          type = "RUNNING"
      case a.automotive:
          type = "IN_VEHICLE"
      case a.cycling:
          type = "ON_BICYCLE"
      default:
          type = "UNKNOWN"
      }
      return type
    }

    func extractActivityConfidence(a: CMMotionActivity) -> Int {
      var conf = -1
      
      switch a.confidence {
      case CMMotionActivityConfidence.low:
          conf = 10
      case CMMotionActivityConfidence.medium:
          conf = 50
      case CMMotionActivityConfidence.high:
          conf = 100
      default:
          conf = -1
      }
      return conf
    }

  }





