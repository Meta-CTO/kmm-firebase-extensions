import Foundation
import FirebaseRemoteConfig

@objc public class FirebaseRemoteConfigsProvider: NSObject {

    private let remoteConfig: RemoteConfig

    @objc public override init() {
        self.remoteConfig = RemoteConfig.remoteConfig()
    }

    @objc public func setSettings(minFetchIntervalSeconds: Int32, completion: @escaping (NSError?) -> Void) {
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = TimeInterval(minFetchIntervalSeconds)
        remoteConfig.configSettings = settings
        completion(nil)
    }

    @objc public func loadDefaults(from json: [String: Any], completion: @escaping (NSError?) -> Void) {
        var nsDefaults: [String: NSObject] = [:]

        for (key, value) in json {
            switch value {
            case let v as String:
                nsDefaults[key] = v as NSString
            case let v as Bool:
                nsDefaults[key] = NSNumber(value: v)
            case let v as Int:
                nsDefaults[key] = NSNumber(value: v)
            case let v as Double:
                nsDefaults[key] = NSNumber(value: v)
            case let v as NSNumber:
                nsDefaults[key] = v
            default:
                nsDefaults[key] = "\(value)" as NSString
            }
        }

        remoteConfig.setDefaults(nsDefaults)
        completion(nil)
    }

    @objc public func fetchConfigsFromRemote(completion: @escaping ([String: Any]?, NSError?) -> Void) {
        remoteConfig.fetchAndActivate { status, error in
            if let error = error {
                completion(nil, error as NSError)
                return
            }

            var configs: [String: Any] = [:]
            let allKeys = self.remoteConfig.allKeys(from: .remote) // Get all keys from remote source

            for key in allKeys {
                if let value = self.remoteConfig.configValue(forKey: key).jsonValue() {
                    configs[key] = value
                } else {
                    configs[key] = NSNull() // Use NSNull for missing or invalid values
                }
            }

            completion(configs, nil)
        }
    }

    // Accessors

    @objc public func getString(forKey key: String) -> String? {
        remoteConfig.configValue(forKey: key).stringValue
    }

    @objc public func getBool(forKey key: String) -> Bool {
        remoteConfig.configValue(forKey: key).boolValue
    }

    @objc public func getDouble(forKey key: String) -> Double {
        remoteConfig.configValue(forKey: key).numberValue.doubleValue
    }

    @objc public func getLong(forKey key: String) -> Int64 {
        remoteConfig.configValue(forKey: key).numberValue.int64Value
    }

    @objc public func getInt(forKey key: String) -> Int {
        remoteConfig.configValue(forKey: key).numberValue.intValue
    }
}

// MARK: - RemoteConfigValue extension to extract JSON-friendly values
private extension RemoteConfigValue {
    func jsonValue() -> Any? {
        // Try to infer boolean if possible
        let lower = stringValue.lowercased()
        if lower == "true" || lower == "false" {
            return (lower == "true")
        }

        // Try to return numeric if applicable
        if let number = lower as? NSNumber {
            return number
        }

        // Default to string
        return stringValue
    }
}
