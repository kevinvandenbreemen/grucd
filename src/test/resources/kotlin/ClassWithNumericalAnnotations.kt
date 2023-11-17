package kotlin


annotation class SpecialMultiArgAnnotation(val type: AnnotationType, val someNum: Int, val name: String)

@SpecialMultiArgAnnotation(type = AnnotationType.Type2, someNum = 42, name = "Testing Annotation")
class ClassWithNumericalAnnotations {
}