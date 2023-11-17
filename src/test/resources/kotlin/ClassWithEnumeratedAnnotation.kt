package kotlin

enum class AnnotationType {
    Type1,
    Type2
}

annotation class SpecialTypedAnnotation(val type: AnnotationType)

@SpecialTypedAnnotation(type = AnnotationType.Type2)
class ClassWithEnumeratedAnnotation {
}