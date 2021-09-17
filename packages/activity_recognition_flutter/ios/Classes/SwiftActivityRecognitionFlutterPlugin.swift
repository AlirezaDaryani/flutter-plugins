import Flutter
import UIKit
import CoreMotion
import CoreLocation


public class SwiftActivityRecognitionFlutterPlugin: NSObject, FlutterPlugin {

    public var globalChannel:FlutterMethodChannel
    private let activityManager = CMMotionActivityManager()
    let locationManager = CLLocationManager()

    
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
        self.registerBackgroundTask()
        self.locationManager.requestAlwaysAuthorization()
        self.locationManager.allowsBackgroundLocationUpdates = true
        self.locationManager.startMonitoringSignificantLocationChanges()
        
        activityManager.startActivityUpdates(to: OperationQueue.init()) { (activity) in
            if let a = activity {

                let type = self.extractActivityType(a: a)
                let confidence = self.extractActivityConfidence(a: a)
                let data = "\(type),\(confidence)"

                /// Send event to flutter
                self.globalChannel.invokeMethod("g_data", arguments: data)

                
            }
        }
      }
    }
    

    
    
    
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





