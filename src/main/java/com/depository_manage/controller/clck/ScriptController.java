package com.depository_manage.controller.clck;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/script")
public class ScriptController {

    @PostMapping("/run-aa")
    public ResponseEntity<String> runScript() {
        try {
            // 获取类路径中的Python脚本，并解码路径
            String scriptPath = getClass().getClassLoader().getResource("scripts/aa.py").getPath();

            // 在Windows系统上，路径可能会多一个前导斜杠，需要去掉它
            if (scriptPath.startsWith("/")) {
                scriptPath = scriptPath.substring(1);
            }

            // 解码路径，防止空格等特殊字符影响路径解析

            // 打印路径以检查是否正确
            System.out.println("Script Path: " + scriptPath);

            // 使用ProcessBuilder运行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath);

            // 设置环境变量，如果需要
            processBuilder.environment().put("PYTHONUNBUFFERED", "1");

            // 启动进程
            Process process = processBuilder.start();

            // 读取脚本的输出
            String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n"));

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(500).body("脚本执行失败:\n" + error);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 输出完整的异常堆栈跟踪
            return ResponseEntity.status(500).body("脚本执行异常: " + e.getMessage());
        }
    }
}
