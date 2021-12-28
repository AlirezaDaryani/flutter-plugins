import Flutter
import UIKit
import CoreMotion
import CoreLocation


public class SwiftActivityRecognitionFlutterPlugin: NSObject, FlutterPlugin,CLLocationManagerDelegate {

    public var globalChannel:FlutterMethodChannel
    private let activityManager = CMMotionActivityManager()
    let locationManager = CLLocationManager()


    private var requestLocationAuthorizationCallback: ((CLAuthorizationStatus) -> Void)?

    private var result:FlutterResult?
    
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
        self.requestLocationAuthorization()
        self.result = result
        self.getActivity()

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
                }else if status == .authorizedAlways {
                    if(self.result != nil){
                        self.result!("done")
                    }
                    self.getActivity()
                }
            }
            self.locationManager.requestWhenInUseAuthorization()
        } else {
            self.locationManager.requestAlwaysAuthorization()
        }
    }
    
    public func getActivity(){
        
        activityManager.stopActivityUpdates()
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





