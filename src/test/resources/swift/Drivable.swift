import Foundation

protocol Drivable: AnyObject {
    var make: String { get }
    func start()
    func stop()
}

extension Drivable {
    func honk() {
        return ("Honk! Honk!")
    }
}