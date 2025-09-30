package com.gridhub.gridhub.infra.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploaderService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final String s3Dir = "images"; // S3 버킷 내에 이미지를 저장할 디렉토리 이름

    /**
     * MultipartFile을 S3에 업로드하고, 업로드된 파일의 URL을 반환합니다.
     *
     * @param multipartFile 업로드할 파일
     * @return 업로드된 파일의 S3 URL
     * @throws IOException 파일 처리 중 발생할 수 있는 예외
     */
    public String upload(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null; // 또는 예외 처리
        }

        // 1. 파일 이름 중복을 피하기 위해 UUID를 사용한 새로운 파일 이름 생성
        String originalFilename = multipartFile.getOriginalFilename();
        String uniqueFileName = createUniqueFileName(originalFilename);
        String s3Key = s3Dir + "/" + uniqueFileName;

        // 2. S3에 업로드하기 위한 요청 객체 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(multipartFile.getContentType())
                .contentLength(multipartFile.getSize())
                .build();

        // 3. S3Client를 사용하여 파일 업로드
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        log.info("S3에 파일 업로드 완료: {}", s3Key);

        // 4. 업로드된 파일의 URL 반환
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(s3Key)).toExternalForm();
    }

    /**
     * S3에 업로드된 파일을 삭제합니다.
     *
     * @param fileUrl 삭제할 파일의 전체 S3 URL
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            // 1. URL에서 S3 객체 키(파일 경로) 추출
            String key = getKeyFromUrl(fileUrl);

            // 2. 삭제 요청 객체 생성
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // 3. S3Client를 사용하여 파일 삭제
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3에서 파일 삭제 완료: {}", key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류 발생: {}", fileUrl, e);
            // TODO: 파일 삭제 실패에 대한 예외 처리 (e.g., 커스텀 예외)
        }
    }

    // 파일 이름에서 확장자를 추출하는 헬퍼 메서드
    private String getFileExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            // 확장자가 없는 파일의 경우
            return "";
        }
    }

    // UUID를 사용하여 고유한 파일 이름을 생성하는 헬퍼 메서드
    private String createUniqueFileName(String originalFilename) {
        return UUID.randomUUID().toString() + getFileExtension(originalFilename);
    }

    // 전체 URL에서 객체 키(e.g., images/uuid-filename.jpg)를 추출하는 헬퍼 메서드
    private String getKeyFromUrl(String fileUrl) {
        // S3 URL 형식: https://<bucket-name>.s3.<region>.amazonaws.com/<key>
        // 이 구조를 기반으로 key를 파싱합니다.
        String baseUrl = "https://"+ bucketName + ".s3.";
        if (fileUrl.contains(baseUrl)) {
            return fileUrl.substring(fileUrl.indexOf(s3Dir));
        }
        return fileUrl; // URL 형식이 다를 경우를 대비
    }
}