// File: src/test/java/com/example/sales/BaseSpringTest.java
package com.example.sales;

import com.example.sales.config.TestMailConfig;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfig.class)
@Disabled
public abstract class BaseSpringTest {
}
