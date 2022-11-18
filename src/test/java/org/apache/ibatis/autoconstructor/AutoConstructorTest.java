/*
 *    Copyright 2009-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.autoconstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Reader;
import java.util.List;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tianjing.Read;

@Read(s = Read.Status.READING, postil = {
  "框架会使用 @AutomapConstructor 注解标识过的实体的构造方法来实例化对象",
  "此注解只可以标识在类中的某一个构造方法上，如果标识多个，框架解析会产生歧义，无法得知使用哪一个构造方法来实例化对象，从而抛出异常。"
})
class AutoConstructorTest {
  private static SqlSessionFactory sqlSessionFactory;

  @Read(s = Read.Status.HAVE_READ, postil = {
    "由于 @BeforeAll 注解的特性，在当前类中加载测试方法前会优先运行此方法仅一次。"
  })
  @BeforeAll
  static void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("org/apache/ibatis/autoconstructor/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
        "org/apache/ibatis/autoconstructor/CreateDB.sql");
  }

  @Read(s = Read.Status.HAVE_READ, postil = {
    "id 为 1 的数据中每一个字段都是有值的，所以在通过类 PrimitiveSubject 的构造方法可以实例化对象。"
  })
  @Test
  void fullyPopulatedSubject() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
      final Object subject = mapper.getSubject(1);
      assertNotNull(subject);
    }
  }

  @Read(s = Read.Status.HAVE_READ, postil = {
    "CreateDB.sql 中 subject 表中添加的数据一共有三条，其中第二条和第三条存在值为 NULL 的字段，导致通过 PrimitiveSubject 类的构造方法反射构造对象的时候出现异常。"
  })
  @Test
  void primitiveSubjects() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
      assertThrows(PersistenceException.class, mapper::getSubjects);
    }
  }

  @Read(s = Read.Status.HAVE_READ, postil = {
    "AnnotatedSubject 类中有两个构造方法，框架会使用其中标注了 @AutomapConstructor 的构造方法来实例化对象。"
  })
  @Test
  void annotatedSubject() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
      verifySubjects(mapper.getAnnotatedSubjects());
    }
  }

  @Read(s = Read.Status.HAVE_READ, postil = {
    "BadAnnotatedSubject 类中的两个构造方法都标注了 @AutomapConstructor 注解造成歧义，框架不知道使用哪一个构造方法来实例化对象，所以抛出了异常。"
  })
  @Test
  void badMultipleAnnotatedSubject() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
      final PersistenceException ex = assertThrows(PersistenceException.class, mapper::getBadAnnotatedSubjects);
      final ExecutorException cause = (ExecutorException)  ex.getCause();
      assertEquals("@AutomapConstructor should be used in only one constructor.", cause.getMessage());
    }
  }

  @Read(s = Read.Status.HAVE_READ, postil = {
    "BadSubject 中构造方法的入参字段类型与数据库同名字段并不匹配，无法实例化对象，导致抛出异常。"
  })
  @Test
  void badSubject() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
      assertThrows(PersistenceException.class, mapper::getBadSubjects);
    }
  }

  @Read(s = Read.Status.HAVE_READ, postil = {
    "extensive_subject 表中的字段都是有值的，所以使用基础类型来接收数据不会存在无法映射的问题。"
  })
  @Test
  void extensiveSubject() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
      verifySubjects(mapper.getExtensiveSubjects());
    }
  }

  private void verifySubjects(final List<?> subjects) {
    assertNotNull(subjects);
    Assertions.assertThat(subjects.size()).isEqualTo(3);
  }
}
