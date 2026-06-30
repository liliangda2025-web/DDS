package com.example.dqcadirsystem.knowledge.validation;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 文件校验器单元测试，覆盖每种受支持格式的真实内容识别和常见伪造场景。
 */
class KnowledgeFileValidatorTest {

    private final KnowledgeFileValidator validator = new KnowledgeFileValidator();

    @Test
    void shouldAcceptPdf() {
        assertType(file("manual.pdf", "application/pdf", "%PDF-1.7\nbody"), KnowledgeFileType.PDF);
    }

    @Test
    void shouldAcceptLegacyDoc() {
        byte[] bytes = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0x00};
        assertType(new MockMultipartFile("file", "manual.doc", "application/msword", bytes),
                KnowledgeFileType.DOC);
    }

    @Test
    void shouldAcceptDocxWithRequiredEntries() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertType(new MockMultipartFile("file", "manual.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        output.toByteArray()),
                KnowledgeFileType.DOCX);
    }

    @Test
    void shouldAcceptDwg() {
        assertType(file("drawing.dwg", "application/dwg", "AC1027drawing"), KnowledgeFileType.DWG);
    }

    @Test
    void shouldAcceptAsciiAndBinaryDxf() {
        assertType(file("drawing.dxf", "text/plain", "0\r\nSECTION\r\n2\r\nHEADER"),
                KnowledgeFileType.DXF);
        assertType(file("drawing.dxf", "application/octet-stream", "AutoCAD Binary DXF\r\n\u001a\u0000"),
                KnowledgeFileType.DXF);
    }

    /** 仅修改后缀不能绕过文件特征校验。 */
    @Test
    void shouldRejectSpoofedExtension() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(file("image.pdf", "application/pdf", "not a pdf")));
        assertEquals("文件内容与扩展名不一致或文件已损坏", exception.getMessage());
    }

    /** ZIP 文件没有 Word 目录时不能被当作 DOCX。 */
    @Test
    void shouldRejectMalformedDocx() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("random.txt"));
            zip.write("text".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertThrows(BusinessException.class,
                () -> validator.validate(new MockMultipartFile("file", "bad.docx",
                        "application/octet-stream", output.toByteArray())));
    }

    /** DOCX 条目数设置硬上限，避免恶意 ZIP 用海量条目拖慢校验。 */
    @Test
    void shouldRejectDocxWithTooManyEntries() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int i = 0; i <= 1000; i++) {
                zip.putNextEntry(new ZipEntry("entry-" + i));
                zip.closeEntry();
            }
        }

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(new MockMultipartFile(
                        "file", "many.docx", "application/octet-stream", output.toByteArray())));
        assertEquals("Word文件包含过多压缩条目", exception.getMessage());
    }

    /** 明显冲突的客户端 MIME 会在读取文件内容前被拒绝。 */
    @Test
    void shouldRejectIncompatibleClientContentType() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(file("manual.pdf", "image/png", "%PDF-1.7")));
        assertEquals("文件类型与Content-Type不一致", exception.getMessage());
    }

    /** 大小在读取输入流前检查，测试无需真的分配 500 MiB 内存。 */
    @Test
    void shouldRejectOversizedFileWithoutReadingIt() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(KnowledgeFileValidator.MAX_FILE_SIZE + 1);

        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(file));
        assertEquals("文件大小不能超过500MB", exception.getMessage());
    }

    private MockMultipartFile file(String name, String contentType, String content) {
        return new MockMultipartFile(
                "file", name, contentType, content.getBytes(StandardCharsets.ISO_8859_1));
    }

    private void assertType(MultipartFile file, KnowledgeFileType expected) {
        assertEquals(expected, validator.validate(file).type());
    }
}
