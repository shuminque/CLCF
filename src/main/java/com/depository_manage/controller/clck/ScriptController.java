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
            // 指定批处理文件的绝对路径
            String batchFilePath = "D:\\clck\\看板更新.bat";

            // 打印路径用于调试
            System.out.println("Running batch file at: " + batchFilePath);

            // 使用ProcessBuilder来运行批处理文件
            ProcessBuilder processBuilder = new ProcessBuilder(batchFilePath);
            processBuilder.environment().put("PYTHONUNBUFFERED", "1");

            Process process = processBuilder.start();

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
