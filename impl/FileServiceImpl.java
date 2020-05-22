package com.example.demo.service.impl;

import com.example.demo.domain.MultipartFileParam;
import com.example.demo.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final String FILE_UPLOAD_PATH = "F:\\upload";

    private static final String FILE_SUFFIX = ".mp4";

    @Override
    public boolean fileUpload(MultipartFileParam multipartFileParam) {
        System.out.println("multipartFileParam:--->" + multipartFileParam);
        String taskId = multipartFileParam.getTaskId();

        String chunkDirectory = FILE_UPLOAD_PATH + File.separator + taskId;
        // 获取上传的文件夹
        File file = new File(chunkDirectory);
        // 1.不存在就创建文件夹
        if (!file.exists()) {
            file.mkdir();
        }
        // 1.判断分片是否已经存在，不存在在上传
        String[] fileList = file.list();
        int chunk = multipartFileParam.getChunk();
        // 文件名
        String fileName = FILE_UPLOAD_PATH + File.separator + taskId + File.separator + chunk;
        if (fileList != null && fileList.length > 0) {
            boolean isContains = Arrays.asList(fileList).contains(chunk + "");
            if (isContains) {
                // 判断文件大小是否相等
                if (multipartFileParam.getSize() == new File(fileName).length()) {
                    // 分片已经上传过了
                    return true;
                }
            }
        }
        // 2.开始上传分片
        System.out.println( "开始创建...分片为===》"+ chunk);
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "rw");
            // 复制内容
            MappedByteBuffer mappedByteBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE,
                    0,
                    multipartFileParam.getFile().getSize());

            mappedByteBuffer.put(multipartFileParam.getFile().getBytes());

            // 确保关闭
            freedMappedByteBuffer(mappedByteBuffer);

            randomAccessFile.close();

            System.out.println( "结束创建...分片为===》"+ chunk);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void freedMappedByteBuffer(final MappedByteBuffer mappedByteBuffer) {
        try {
            if (mappedByteBuffer == null) {
                return;
            }
            mappedByteBuffer.force();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner", new Class[0]);
                        //可以访问private的权限
                        getCleanerMethod.setAccessible(true);
                        //在具有指定参数的 方法对象上调用此 方法对象表示的底层方法
                        sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(mappedByteBuffer,
                                new Object[0]);
                        cleaner.clean();
                    } catch (Exception e) {
                        log.error("clean MappedByteBuffer error!!!", e);
                    }
                    log.info("clean MappedByteBuffer completed!!!");
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getUploadFile(String id) throws Exception {
        // 文件目录路径
        String fileDirectory = FILE_UPLOAD_PATH + File.separator + id;
        // 目录下面的目标文件
        String mergeFileString = fileDirectory + File.separator + id + FILE_SUFFIX;
        // 文件不存在  执行创建
        File mergeFile = new File(mergeFileString);
        if (!mergeFile.exists()) {
            File file = new File(fileDirectory);
            // 获取文件夹下面的文件列表
            String[] listFile = file.list();
            AtomicLong startPosition = new AtomicLong(0L);
            if (listFile != null && listFile.length > 0) {
                // 创建一个输出通道
                FileChannel fileOutputChannel = FileChannel.open(Paths.get(mergeFileString),
                        StandardOpenOption.READ,StandardOpenOption.WRITE,StandardOpenOption.CREATE);
                // 文件顺序排序 合成
                List<Integer> collectList = Arrays.stream(listFile).map(Integer::new)
                        .sorted(Integer::compareTo).collect(Collectors.toList());
                // 获取目录下面的所有文件名
                collectList.forEach(fileItem -> {
                    String fileName = fileDirectory + File.separator + fileItem;
                    try {
                        // 创建 输入流 通道 fileInputChannel
                        FileChannel fileInputChannel = FileChannel.open(Paths.get(fileName), StandardOpenOption.READ);
                        // 获取buffer
                        MappedByteBuffer mappedInputByteBuffer = fileInputChannel.map(FileChannel.MapMode.READ_ONLY,
                                0,
                                fileInputChannel.size());
                        // 获取输出的buffer
                        MappedByteBuffer map = fileOutputChannel.map(FileChannel.MapMode.READ_WRITE,
                                startPosition.get(),
                                fileInputChannel.size());
                        startPosition.addAndGet(fileInputChannel.size());
                        // 存入buffer中
                        map.put(mappedInputByteBuffer);
                        fileInputChannel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                fileOutputChannel.close();
            }
        }
        System.out.println( mergeFileString );
        return new FileInputStream(mergeFileString);
    }

}
