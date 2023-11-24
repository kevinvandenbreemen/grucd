import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

enum MyTypes {
    Type1,
    Type2
}

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface MyJavaAnnotation {

    public String getName();
    public MyTypes type();

}

@MyJavaAnnotation(getName = "TestAnnotation", type = MyTypes.Type2)
public class AnnotatedClass {



}