package com.CodeSphere.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = BackendApplication.class)
@TestPropertySource(properties = {
		"minio.bucket.name=questions-import-vault",
		"minio.endpoint=http://localhost:9000",
		"minio.access-key=minioadmin",
		"minio.secret-key=minioadmin"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}