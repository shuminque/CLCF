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
            // 指定Python脚本的绝对路径
            String scriptPath = "D:\\clck\\aa.py";

            // 打印路径用于调试
            System.out.println("Running script at: " + scriptPath);

            // 使用ProcessBuilder来运行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath);
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

            return ResponseEntity.ok(images);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
