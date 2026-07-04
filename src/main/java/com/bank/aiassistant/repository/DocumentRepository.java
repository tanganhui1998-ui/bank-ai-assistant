package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.enums.DocumentBusinessType;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档主表 MyBatis Mapper。
 *
 * 保留原 Repository 方法名，减少服务层迁移成本；新增/更新时由默认方法补齐时间字段。
 */
@Mapper
public interface DocumentRepository {

    String BASE_COLUMNS = """
            document_id, file_name, file_size, file_type, file_hash,
            oss_bucket, oss_object_key, oss_etag, access_url,
            display_name, document_type, version_no, department, effective_time,
            confidentiality_level, applicable_scope, publishing_unit,
            process_status, parse_error_message, retry_count,
            uploader_id, uploader_name, created_time, updated_time, published_time
            """;

    @Select("select " + BASE_COLUMNS + " from ai_document where document_id = #{documentId}")
    Optional<Document> findById(String documentId);

    @Select("select " + BASE_COLUMNS + " from ai_document where document_id = #{documentId} for update")
    Optional<Document> findByIdForUpdate(String documentId);

    @Select("select " + BASE_COLUMNS + " from ai_document where file_hash = #{fileHash}")
    Optional<Document> findByFileHash(String fileHash);

    @Select("select count(1) > 0 from ai_document where file_hash = #{fileHash}")
    boolean existsByFileHash(String fileHash);

    @Select("select " + BASE_COLUMNS + " from ai_document where display_name = #{displayName} order by created_time desc")
    List<Document> findByDisplayNameOrderByCreatedTimeDesc(String displayName);

    @Select("select " + BASE_COLUMNS + " from ai_document where process_status = #{processStatus} order by created_time desc")
    List<Document> findByProcessStatusOrderByCreatedTimeDesc(DocumentProcessStatus processStatus);

    @Select("""
            select """ + BASE_COLUMNS + """
            from ai_document
            where display_name = #{displayName}
              and process_status = #{processStatus}
            order by published_time desc
            """)
    List<Document> findByDisplayNameAndProcessStatusOrderByPublishedTimeDesc(
            @Param("displayName") String displayName,
            @Param("processStatus") DocumentProcessStatus processStatus
    );

    @Select("select " + BASE_COLUMNS + " from ai_document where process_status = #{processStatus}")
    List<Document> findByProcessStatus(DocumentProcessStatus processStatus);

    @Select("""
            select """ + BASE_COLUMNS + """
            from ai_document
            where document_type = #{documentType}
              and process_status = #{processStatus}
            """)
    List<Document> findByDocumentTypeAndProcessStatus(
            @Param("documentType") DocumentBusinessType documentType,
            @Param("processStatus") DocumentProcessStatus processStatus
    );

    @Select("""
            select """ + BASE_COLUMNS + """
            from ai_document
            where department = #{department}
              and process_status = #{processStatus}
            """)
    List<Document> findByDepartmentAndProcessStatus(
            @Param("department") String department,
            @Param("processStatus") DocumentProcessStatus processStatus
    );

    @Select("""
            <script>
            select count(1)
            from ai_document
            <where>
              <if test="status != null">and process_status = #{status}</if>
              <if test="documentType != null">and document_type = #{documentType}</if>
              <if test="department != null and department != ''">and department = #{department}</if>
            </where>
            </script>
            """)
    long countByFilters(
            @Param("status") DocumentProcessStatus status,
            @Param("documentType") DocumentBusinessType documentType,
            @Param("department") String department
    );

    @Select("""
            <script>
            select """ + BASE_COLUMNS + """
            from ai_document
            <where>
              <if test="status != null">and process_status = #{status}</if>
              <if test="documentType != null">and document_type = #{documentType}</if>
              <if test="department != null and department != ''">and department = #{department}</if>
            </where>
            order by created_time desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Document> findByFilters(
            @Param("status") DocumentProcessStatus status,
            @Param("documentType") DocumentBusinessType documentType,
            @Param("department") String department,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    @Insert("""
            insert into ai_document (
              document_id, file_name, file_size, file_type, file_hash,
              oss_bucket, oss_object_key, oss_etag, access_url,
              display_name, document_type, version_no, department, effective_time,
              confidentiality_level, applicable_scope, publishing_unit,
              process_status, parse_error_message, retry_count,
              uploader_id, uploader_name, created_time, updated_time, published_time
            ) values (
              #{documentId}, #{fileName}, #{fileSize}, #{fileType}, #{fileHash},
              #{ossBucket}, #{ossObjectKey}, #{ossEtag}, #{accessUrl},
              #{displayName}, #{documentType}, #{versionNo}, #{department}, #{effectiveTime},
              #{confidentialityLevel}, #{applicableScope}, #{publishingUnit},
              #{processStatus}, #{parseErrorMessage}, #{retryCount},
              #{uploaderId}, #{uploaderName}, #{createdTime}, #{updatedTime}, #{publishedTime}
            )
            """)
    int insert(Document document);

    @Update("""
            update ai_document
            set file_name = #{fileName},
                file_size = #{fileSize},
                file_type = #{fileType},
                file_hash = #{fileHash},
                oss_bucket = #{ossBucket},
                oss_object_key = #{ossObjectKey},
                oss_etag = #{ossEtag},
                access_url = #{accessUrl},
                display_name = #{displayName},
                document_type = #{documentType},
                version_no = #{versionNo},
                department = #{department},
                effective_time = #{effectiveTime},
                confidentiality_level = #{confidentialityLevel},
                applicable_scope = #{applicableScope},
                publishing_unit = #{publishingUnit},
                process_status = #{processStatus},
                parse_error_message = #{parseErrorMessage},
                retry_count = #{retryCount},
                uploader_id = #{uploaderId},
                uploader_name = #{uploaderName},
                updated_time = #{updatedTime},
                published_time = #{publishedTime}
            where document_id = #{documentId}
            """)
    int update(Document document);

    @Delete("delete from ai_document where document_id = #{documentId}")
    int deleteById(String documentId);

    default Document save(Document document) {
        LocalDateTime now = LocalDateTime.now();
        if (document.getCreatedTime() == null) {
            document.setCreatedTime(now);
        }
        document.setUpdatedTime(now);
        if (document.getRetryCount() == null) {
            document.setRetryCount(0);
        }
        if (document.getProcessStatus() == null) {
            document.setProcessStatus(DocumentProcessStatus.UPLOAD_COMPLETED);
        }
        if (findById(document.getDocumentId()).isPresent()) {
            update(document);
        } else {
            insert(document);
        }
        return document;
    }

}
