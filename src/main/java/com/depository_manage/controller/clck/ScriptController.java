package com.depository_manage.controller.clck;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/script")
public class ScriptController {

    @PostMapping("/run-aa")
    public ResponseEntity<List<String>> runScript() {
        try {
            // 将Python脚本从JAR中提取到临时目录
            InputStream scriptStream = getClass().getClassLoader().getResourceAsStream("scripts/aa.py");
            if (scriptStream == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            // 创建一个临时文件用于保存脚本
            Path tempScript = Files.createTempFile("aa", ".py");
            Files.copy(scriptStream, tempScript, StandardCopyOption.REPLACE_EXISTING);
            scriptStream.close();

            // 使用临时文件路径来运行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder("python", tempScript.toAbsolutePath().toString());
            processBuilder.environment().put("PYTHONUNBUFFERED", "1");

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorMessage = errorReader.lines().collect(Collectors.joining("\n"));
                System.err.println("Python script error: " + errorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            // 假设图片生成路径是 C:\\Users\\Q\\Desktop\\看板图\\20240806 下
            // 动态生成路径
            String userHomeDir = System.getProperty("user.home");
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String imageDir = userHomeDir + "\\Desktop\\看板图\\" + currentDate;
            File dir = new File(imageDir);
            if (!dir.exists() || !dir.isDirectory()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<String> images = new ArrayList<>();
            for (File file : dir.listFiles()) {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                images.add(base64Image);
            }
            Files.delete(tempScript);

            return ResponseEntity.ok(images);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
