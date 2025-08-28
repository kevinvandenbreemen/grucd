extension Circle {
    var diameter: Double {
        return radius * 2
    }

    func area() -> Double {
        return .pi * radius * radius
    }

    mutating func scale(by factor: Double) {
        radius *= factor
    }
}