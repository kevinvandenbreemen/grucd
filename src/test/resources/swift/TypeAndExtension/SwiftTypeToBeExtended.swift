protocol Drawable {
    var color: String { get set }
    func draw()
}

struct Circle: Drawable {
    var color: String
    var radius: Double

    func draw() {
        print("Drawing a \(color) circle with radius \(radius)")
    }
}