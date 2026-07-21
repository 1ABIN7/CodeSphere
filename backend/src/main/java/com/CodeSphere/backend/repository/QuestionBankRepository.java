package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuestionBankRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    // A cross-compatible approach that works in H2 memory tests and PostgreSQL basic runs
    @Query(value = "SELECT * FROM questions WHERE LOWER(title) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR LOWER(content) LIKE LOWER(CONCAT('%', :searchQuery, '%'))", nativeQuery = true)
    List<Question> searchByTsVector(@Param("searchQuery") String searchQuery);
}

/*
package com.codesphere.backend.repository;

import com.codesphere.backend.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    // Full-Text Search via PostgreSQL tsvector on searchable text fields (e.g., title/content/answers)
    @Query(value = "SELECT * FROM questions WHERE searchable_text_tsvector @@ to_tsquery(:searchQuery)", nativeQuery = true)
    List<Question> searchByTsVector(@Param("searchQuery") String searchQuery);
}
*/
