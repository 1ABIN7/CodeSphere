package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.QuestionImportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    private final QuestionBankRepository repository;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public List<Question> importBulkQuestions(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 1. Archive the raw import file into MinIO object storage vault
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload import log to MinIO vault", e);
        }

        // 2. Identify type and process
        List<Question> parsedQuestions;
        String extension = getFileExtension(file.getOriginalFilename());

        try {
            if ("json".equalsIgnoreCase(extension)) {
                parsedQuestions = objectMapper.readValue(file.getBytes(), new TypeReference<List<Question>>() {});
            } else if ("csv".equalsIgnoreCase(extension)) {
                parsedQuestions = parseCsv(file);
            } else {
                throw new IllegalArgumentException("Unsupported file type. Only JSON and CSV files are accepted.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing file structure: " + e.getMessage(), e);
        }

        // 3. Persist imported list to DB
        return repository.saveAll(parsedQuestions);
    }

    private List<Question> parseCsv(MultipartFile file) throws Exception {
        List<Question> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) { // Skip columns row
                    isHeader = false;
                    continue;
                }
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Regex helps ignore inner quotes
                if (columns.length < 5) continue;

                Question q = new Question();
                q.setTitle(cleanCsvField(columns[0]));
                q.setContent(cleanCsvField(columns[1]));
                q.setCategory(cleanCsvField(columns[2]));
                q.setType(cleanCsvField(columns[3]));
                q.setDifficulty(cleanCsvField(columns[4]));

                if (columns.length > 5 && !columns[5].isBlank()) {
                    q.setTags(Arrays.asList(cleanCsvField(columns[5]).split(";")));
                }
                list.add(q);
            }
        }
        return list;
    }

    private String cleanCsvField(String val) {
        return val.trim().replaceAll("^\"|\"$", "");
    }

    private String getFileExtension(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf(".") + 1);
    }
}