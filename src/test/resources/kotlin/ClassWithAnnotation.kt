package kotlin

annotation class MyAnnotation (val name: String)

@MyAnnotation(name = "Test Annotation")
class ClassWithAnnotation {
}