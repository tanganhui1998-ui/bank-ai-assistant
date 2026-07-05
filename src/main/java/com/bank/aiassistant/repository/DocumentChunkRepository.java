package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.DocumentChunk;
import com.bank.aiassistant.domain.enums.ChunkStatus;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文档切片 MyBatis Mapper。
 */
@Mapper
public interface DocumentChunkRepository {

    String BASE_COLUMNS = """
            chunk_id, document_id, content, chapter_path, chapter_no,
            chunk_seq, start_page, end_page, token_count, quality_score, status
            """;

    @Select("select " + BASE_COLUMNS + " from knowledge_chunk where chunk_id = #{chunkId}")
    DocumentChunk findOne(String chunkId);

    @Select("select " + BASE_COLUMNS + " from knowledge_chunk where document_id = #{documentId} order by chunk_seq asc")
    List<DocumentChunk> findByDocumentDocumentIdOrderByChunkSeqAsc(String documentId);

    @Select("""
            select """ + BASE_COLUMNS + """
            from knowledge_chunk
            where document_id = #{documentId}
              and status = #{status}
            order by chunk_seq asc
            """)
    List<DocumentChunk> findByDocumentDocumentIdAndStatusOrderByChunkSeqAsc(
            @Param("documentId") String documentId,
            @Param("status") ChunkStatus status
    );

    @Delete("delete from knowledge_chunk where document_id = #{documentId}")
    int deleteByDocumentDocumentId(String documentId);

    @Insert("""
            insert into knowledge_chunk (
              chunk_id, document_id, content, chapter_path, chapter_no,
              chunk_seq, start_page, end_page, token_count, quality_score, status
            ) values (
              #{chunkId}, #{documentId}, #{content}, #{chapterPath}, #{chapterNo},
              #{chunkSeq}, #{startPage}, #{endPage}, #{tokenCount}, #{qualityScore}, #{status}
            )
            """)
    int insert(DocumentChunk chunk);

    @Update("""
            update knowledge_chunk
            set document_id = #{documentId},
                content = #{content},
                chapter_path = #{chapterPath},
                chapter_no = #{chapterNo},
                chunk_seq = #{chunkSeq},
                start_page = #{startPage},
                end_page = #{endPage},
                token_count = #{tokenCount},
                quality_score = #{qualityScore},
                status = #{status}
            where chunk_id = #{chunkId}
            """)
    int update(DocumentChunk chunk);

    default DocumentChunk save(DocumentChunk chunk) {
        if (findOne(chunk.getChunkId()) == null) {
            insert(chunk);
        } else {
            update(chunk);
        }
        return chunk;
    }

    default List<DocumentChunk> saveAll(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            save(chunk);
        }
        return chunks;
    }

}
