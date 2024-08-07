package com.depository_manage.controller.clck;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/script")
public class ScriptController {

    @PostMapping("/run-aa")
    public ResponseEntity<List<String>> runScript() {
        try {
            // 先运行批处理文件，确保图片生成
            executeBatchFile();

            // 然后读取生成的图片
            return getImages();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void executeBatchFile() {
        try {
            String batchFilePath = "D:\\clck\\看板更新.bat";

            // 使用 Runtime.getRuntime().exec 来执行批处理文件
            String command = "cmd /c start " + batchFilePath;
            Process process = Runtime.getRuntime().exec(command);

            // 如果需要等待批处理文件执行完毕，可以调用 waitFor
            int exitCode = process.waitFor(); // 等待批处理文件执行完毕
            if (exitCode == 0) {
                System.out.println("Batch file executed successfully.");
            } else {
                System.err.println("Failed to execute batch file, exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to execute batch file: " + e.getMessage());
        }
    }

    @PostMapping("/get-images")
    public ResponseEntity<List<String>> getImages() {
        try {
            // 动态生成图片路径
            String userHomeDir = System.getProperty("user.home");
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String imageDir = userHomeDir + "\\Desktop\\看板图\\" + currentDate;
            File dir = new File(imageDir);

            if (!dir.exists() || !dir.isDirectory()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // 读取现有图片
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
