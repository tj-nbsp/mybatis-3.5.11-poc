package tianjing;

import java.lang.annotation.*;

/**
 * identify whether the class or method has been read.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Read {

  /**
   * 阅读状态
   */
  Status s() default Status.UN_READ;

  /**
   * 批注
   */
  String[] postil() default {};

  enum Status {
    UN_READ,
    READING,
    HAVE_READ,
    NOTICE,
    ;
  }

}
