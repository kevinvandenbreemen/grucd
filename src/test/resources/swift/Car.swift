class Car {
    // Properties
    var make: String
    var model: String
    var year: Int
    var isRunning: Bool

    // Initializer
    init(make: String, model: String, year: Int) {
        self.make = make
        self.model = model
        self.year = year
        self.isRunning = false // Default value
    }

    // Methods
    func startEngine() {
        if !isRunning {
            isRunning = true
            print("\(make) \(model)'s engine started.")
        } else {
            print("\(make) \(model)'s engine is already running.")
        }
    }

    func stopEngine() {
        if isRunning {
            isRunning = false
            print("\(make) \(model)'s engine stopped.")
        } else {
            print("\(make) \(model)'s engine is already off.")
        }
    }

    func displayInfo() {
        print("Car: \(year) \(make) \(model)")
        print("Engine Status: \(isRunning ? "Running" : "Off")")
    }
}

// Creating an instance (object) of the Car class
let myCar = Car(make: "Toyota", model: "Camry", year: 2023)

// Accessing properties and calling methods
myCar.displayInfo()
myCar.startEngine()
myCar.displayInfo()
myCar.stopEngine()
myCar.displayInfo()

let anotherCar = Car(make: "Honda", model: "Civic", year: 2020)
anotherCar.startEngine()