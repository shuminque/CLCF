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
    public ResponseEntity<List<byte[]>> runScript() {
        try {
            // 假设Python脚本会将生成的图片文件名输出到控制台
            String scriptPath = getClass().getClassLoader().getResource("scripts/aa.py").getPath();

            if (scriptPath.startsWith("/")) {
                scriptPath = scriptPath.substring(1);
            }

            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath);
            processBuilder.environment().put("PYTHONUNBUFFERED", "1");

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // 假设图片生成路径是 C:\Users\Q\Desktop\看板图\ 下
                String userHomeDir = System.getProperty("user.home");
                String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String imageDir = "C:\\Users\\Administrator\\Desktop\\看板图\\" + currentDate;
                File dir = new File(imageDir);
                if (!dir.exists() || !dir.isDirectory()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                }

                List<byte[]> images = new ArrayList<>();
                for (File file : dir.listFiles()) {
                    byte[] imageBytes = Files.readAllBytes(file.toPath());
                    images.add(imageBytes);
                }

                return ResponseEntity.ok(images);
            } else {
                return ResponseEntity.status(500).body(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}
