package vn.edu.fpt.medicaldiagnosis.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@Slf4j
@Service
public class DocxConverterService {

    public byte[] convertDocxToPdf(MultipartFile file) {
        try {
            // 1. Tạo file tạm từ MultipartFile
            File tempDocx = File.createTempFile("temp_docx_", ".docx");
            file.transferTo(tempDocx);

            // 2. Chạy LibreOffice để convert sang PDF
            File outputDir = tempDocx.getParentFile();
            Process process = new ProcessBuilder(
                    "libreoffice",
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", outputDir.getAbsolutePath(),
                    tempDocx.getAbsolutePath()
            ).redirectErrorStream(true).start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("LibreOffice exited with code " + exitCode);
            }

            // 3. Tìm file PDF đã được tạo
            File pdfFile = new File(outputDir, tempDocx.getName().replace(".docx", ".pdf"));
            if (!pdfFile.exists()) {
                throw new FileNotFoundException("Converted PDF not found");
            }

            // 4. Đọc PDF thành byte[]
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = new FileInputStream(pdfFile)) {
                in.transferTo(out);
            }

            // 5. Cleanup
            tempDocx.delete();
            pdfFile.delete();

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Convert DOCX to PDF failed", e);
            throw new RuntimeException("Convert DOCX to PDF failed", e);
        }
    }
}
