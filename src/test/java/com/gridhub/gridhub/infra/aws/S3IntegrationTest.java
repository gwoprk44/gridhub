package com.gridhub.gridhub.infra.aws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // 실제 Spring 컨텍스트를 로드하여 S3Client Bean을 주입받음
class S3IntegrationTest {

    @Autowired
    private S3Client s3Client; // S3Config에서 생성된 Bean이 주입되는지 확인

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @DisplayName("AWS S3 연동 테스트: 파일을 업로드하고 삭제한다")
    @Test
    void s3Connection_UploadAndDelete_Success() throws IOException {
        // given: 테스트용 임시 파일 생성
        String fileName = "integration-test-" + System.currentTimeMillis() + ".txt";
        File testFile = createTestFile("Hello, S3!");

        // when: S3에 파일 업로드
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName) // S3에 저장될 파일 이름
                .build();

        // s3Client.putObject()가 예외 없이 실행되는지 확인
        assertDoesNotThrow(() -> {
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(testFile));
        });

        // then: 파일이 S3에 실제로 존재하는지 확인
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        // s3Client.headObject()는 파일이 존재하면 정상 실행, 없으면 예외 발생
        assertDoesNotThrow(() -> {
            s3Client.headObject(headObjectRequest);
        });

        // cleanup: 테스트 후 생성된 파일과 S3 객체 삭제
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        assertDoesNotThrow(() -> {
            s3Client.deleteObject(deleteObjectRequest);
        });

        testFile.delete(); // 로컬 임시 파일 삭제
    }

    // 테스트용 임시 파일을 생성하는 헬퍼 메서드
    private File createTestFile(String content) throws IOException {
        File file = File.createTempFile("test", ".txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}