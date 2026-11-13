package com.uvindu.crane_management_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.uvindu.crane_management_system.config.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CraneManagementSystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
