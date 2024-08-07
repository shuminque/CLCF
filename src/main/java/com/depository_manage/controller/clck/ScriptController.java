package com.depository_manage.controller.clck;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/script")
public class ScriptController {

    @PostMapping("/run-aa")
    public ResponseEntity<?> runScript() {
        try {
            // 获取脚本路径
            String scriptPath = getClass().getClassLoader().getResource("scripts/aa.py").getPath();
            if (scriptPath.startsWith("/")) {
                scriptPath = scriptPath.substring(1);
            }
            // 打印路径
            System.out.println("Running script at: " + scriptPath);

            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath);
            processBuilder.environment().put("PYTHONUNBUFFERED", "1");

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorMessage = errorReader.lines().collect(Collectors.joining("\n"));
                System.err.println("Python script error: " + errorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Python script failed: " + errorMessage);
            }

            // 动态生成路径
            String userHomeDir = System.getProperty("user.home");
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String imageDir = userHomeDir + "\\Desktop\\看板图\\" + currentDate;

            File dir = new File(imageDir);
            if (!dir.exists() || !dir.isDirectory()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image directory not found: " + imageDir);
            }

            List<byte[]> images = new ArrayList<>();
            for (File file : dir.listFiles()) {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                images.add(imageBytes);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseEntity<>(images, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace(); // 输出完整的异常堆栈跟踪
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

}
